import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.imageio.ImageIO;

/** Local-only measurement controller. Chrome is always owned, isolated, and launched without activation. */
public final class ChromeSpike {
    static final Path ROOT=Path.of("").toAbsolutePath().normalize(), RUN=ROOT.resolve("run/chrome-spike");
    static final ExecutorService WORK=Executors.newCachedThreadPool();
    static final Map<String,Engine> ENGINES=new ConcurrentHashMap<>();
    static final String TOKEN=UUID.randomUUID().toString();
    static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    static final String MUTE="(()=>{const quiet=e=>{if(!e.muted)e.muted=true;if(e.volume!==0)e.volume=0;};new MutationObserver(()=>document.querySelectorAll('video,audio').forEach(quiet)).observe(document,{childList:true,subtree:true});for(const event of ['play','volumechange'])document.addEventListener(event,e=>{if(e.target instanceof HTMLMediaElement)quiet(e.target)},true);document.querySelectorAll('video,audio').forEach(quiet);})()";
    static Path safe(String s){Path p=ROOT.resolve(s).normalize();if(!p.startsWith(RUN))throw new IllegalArgumentException("Output must be under run/chrome-spike");return p;}
    static Map<String,Object> map(Object... kv){var m=new LinkedHashMap<String,Object>();for(int i=0;i<kv.length;i+=2)m.put(kv[i].toString(),kv[i+1]);return m;}
    static void save(Path p,Object v)throws IOException{Files.createDirectories(p.getParent());Files.writeString(p,Json.write(v));}
    static String ps(String script,String... args)throws Exception{var a=new ArrayList<>(List.of("powershell.exe","-NoProfile","-NonInteractive","-ExecutionPolicy","Bypass","-File",ROOT.resolve(script).toString()));a.addAll(List.of(args));var p=new ProcessBuilder(a).redirectError(ProcessBuilder.Redirect.INHERIT).start();String out=new String(p.getInputStream().readAllBytes());if(p.waitFor()!=0)throw new IOException("PowerShell failed");return out.trim();}
    public static void main(String[] args)throws Exception{
        Files.createDirectories(RUN);ImageIO.setUseCache(false);
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.setExecutor(WORK);
        server.createContext("/cmd",x->{try{if(!TOKEN.equals(x.getRequestHeaders().getFirst("X-Perde-Spike-Token"))){x.sendResponseHeaders(403,-1);return;}if(!x.getRequestMethod().equals("POST"))throw new IllegalArgumentException("POST only");var q=Json.obj(Json.parse(new String(x.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)));Object result=dispatch(q);byte[] b=Json.write(map("ok",true,"result",result)).getBytes(java.nio.charset.StandardCharsets.UTF_8);x.sendResponseHeaders(200,b.length);x.getResponseBody().write(b);}catch(Throwable t){byte[] b=Json.write(map("ok",false,"error",t.toString())).getBytes();x.sendResponseHeaders(500,b.length);x.getResponseBody().write(b);}finally{x.close();}});
        server.start();save(RUN.resolve("controller.json"),map("port",server.getAddress().getPort(),"pid",ProcessHandle.current().pid(),"token",TOKEN));
        Runtime.getRuntime().addShutdownHook(new Thread(()->ENGINES.values().forEach(e->{try{e.close();}catch(Exception ignored){}})));
        System.out.println("Controller: "+server.getAddress()+" Java PID "+ProcessHandle.current().pid());
    }
    static Object dispatch(Map<String,Object> q)throws Exception{
        String op=Json.str(q,"op"),id=Json.str(q,"id");
        if(op.equals("start")){if(ENGINES.containsKey(id))throw new IllegalStateException("Already started");var e=new Engine(q);ENGINES.put(id,e);try{return e.launch();}catch(Exception ex){e.close();ENGINES.remove(id);throw ex;}}
        var e=ENGINES.get(id);if(e==null)throw new IllegalArgumentException("Unknown engine "+id);
        return switch(op){
            case "cdp"->e.cdp.call(Json.str(q,"method"),Json.obj(q.get("params")),q.containsKey("session")?Json.str(q,"session"):e.page);
            case "eval"->e.eval(Json.str(q,"expression"),q.containsKey("session")?Json.str(q,"session"):e.page,q.get("contextId"));
            case "contexts"->new ArrayList<>(e.contexts.values());
            case "snapshot"->e.snapshot();
            case "stream"->e.stream(q);
            case "stop"->{Object s=e.close();ENGINES.remove(id);yield s;}
            case "crash"->{e.remember();e.root.destroyForcibly();Thread.sleep(600);yield map("killedOwnedPid",e.root.pid(),"alive",e.root.isAlive());}
            default->throw new IllegalArgumentException(op);
        };
    }
    static final class Engine {
        final Map<String,Object> config;final String id;final Path dir,profile;final Set<ProcessHandle> owned=ConcurrentHashMap.newKeySet();
        final Map<String,Map<String,Object>> contexts=new ConcurrentHashMap<>();final Set<String> sessions=ConcurrentHashMap.newKeySet();
        ProcessHandle root;Process bridge;Cdp cdp;String page;PrintWriter events;volatile Stream stream;volatile boolean closing;
        Engine(Map<String,Object> q)throws IOException{config=q;id=Json.str(q,"id");dir=safe("run/chrome-spike/"+id);Files.createDirectories(dir);profile=dir.resolve("profile-"+UUID.randomUUID());events=new PrintWriter(Files.newBufferedWriter(dir.resolve("events.jsonl")));}
        synchronized void log(String kind,Object v){events.println(Json.write(map("at",Instant.now().toString(),"kind",kind,"data",v)));events.flush();}
        Object launch()throws Exception{
            var a=new ArrayList<>(List.of("--user-data-dir="+profile,"--remote-debugging-address=127.0.0.1","--remote-debugging-port=0","--no-first-run","--no-default-browser-check","--disable-background-mode","--disable-component-update","--disable-sync","--autoplay-policy=no-user-gesture-required","--window-size=640,480","--window-position=-32000,-32000","--disable-features=CalculateNativeWinOcclusion"));
            if(Json.str(config,"mode").equals("headless"))a.add("--headless=new");
            if(config.get("extension") instanceof String s&&!s.isEmpty())a.add("--load-extension="+safe(s));
            if(Boolean.TRUE.equals(config.get("unsafe")))a.add("--enable-unsafe-extension-debugging");
            if(!Boolean.TRUE.equals(config.get("audible")))a.add("--mute-audio");a.add("about:blank");var launch=dir.resolve("launch.json");boolean pipe=Boolean.TRUE.equals(config.get("pipe"));if(pipe)a.remove("--remote-debugging-port=0");Path handshake=dir.resolve("pipe-"+UUID.randomUUID()+".json");save(launch,map("executable",config.get("executable"),"profile",profile.toString(),"arguments",a,"pipe",pipe,"handshake",handshake.toString()));
            long begin=System.nanoTime();long pid;Map<String,Object> hs=Map.of();
            if(pipe){bridge=new ProcessBuilder("powershell.exe","-NoProfile","-NonInteractive","-ExecutionPolicy","Bypass","-File",ROOT.resolve("tools/chrome-spike/launch.ps1").toString(),launch.toString()).redirectOutput(dir.resolve("bridge.log").toFile()).redirectErrorStream(true).start();for(int n=0;n<200&&!Files.exists(handshake);n++){if(!bridge.isAlive())throw new IOException("Pipe bridge exited");Thread.sleep(100);}hs=Json.obj(Json.parse(Files.readString(handshake)));pid=((Number)hs.get("pid")).longValue();}else pid=Long.parseLong(ps("tools/chrome-spike/launch.ps1",launch.toString()));
            root=ProcessHandle.of(pid).orElseThrow();owned.add(root);
            if(pipe)cdp=new Cdp(((Number)hs.get("port")).intValue(),this);else{
                Path portFile=profile.resolve("DevToolsActivePort");for(int n=0;n<300&&!Files.exists(portFile);n++){if(!root.isAlive())throw new IOException("Chrome exited before CDP");Thread.sleep(100);}
                var lines=Files.readAllLines(portFile);String port=lines.getFirst();cdp=new Cdp(URI.create("ws://127.0.0.1:"+port+lines.get(1)),this);
                String protocol=HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/json/protocol")).build(),HttpResponse.BodyHandlers.ofString()).body();Files.writeString(dir.resolve("protocol.json"),protocol);
            }
            cdp.call("Target.setDiscoverTargets",map("discover",true),null);
            cdp.call("Target.setAutoAttach",map("autoAttach",true,"waitForDebuggerOnStart",true,"flatten",true),null);
            var targets=Json.obj(cdp.call("Target.getTargets",Map.of(),null));
            for(Object t:(List<?>)targets.get("targetInfos")){var tm=Json.obj(t);if(Json.str(tm,"type").equals("page")){String target=Json.str(tm,"targetId");for(int n=0;n<100&&page==null;n++)Thread.sleep(50);if(page==null){page=Json.str(Json.obj(cdp.call("Target.attachToTarget",map("targetId",target,"flatten",true),null)),"sessionId");configure(page);}break;}}
            if(page==null)throw new IOException("No page");Thread.sleep(500);
            cdp.call("Emulation.setDeviceMetricsOverride",map("width",1280,"height",720,"deviceScaleFactor",1,"mobile",false),page);
            Object result=map("id",id,"pid",pid,"startupMs",(System.nanoTime()-begin)/1e6,"mode",config.get("mode"),"version",cdp.call("Browser.getVersion",Map.of(),null),"snapshot",snapshot(),"profile",ROOT.relativize(profile).toString());save(dir.resolve("startup.json"),result);log("startup",result);return result;
        }
        void configure(String s)throws Exception{if(!sessions.add(s))return;try{cdp.call("Runtime.enable",Map.of(),s);cdp.call("Runtime.addBinding",map("name","perdeSpike"),s);cdp.call("Page.enable",Map.of(),s);cdp.call("Page.addScriptToEvaluateOnNewDocument",map("source",MUTE),s);cdp.call("Network.enable",Map.of(),s);cdp.call("Log.enable",Map.of(),s);cdp.call("Media.enable",Map.of(),s);cdp.call("Target.setAutoAttach",map("autoAttach",true,"waitForDebuggerOnStart",true,"flatten",true),s);}finally{cdp.call("Runtime.runIfWaitingForDebugger",Map.of(),s);}}
        void event(Map<String,Object> m){String method=Json.str(m,"method"),s=Json.str(m,"sessionId");var p=Json.obj(m.get("params"));
            if(method.equals("Target.attachedToTarget")){String child=Json.str(p,"sessionId");var info=Json.obj(p.get("targetInfo"));log(method,p);WORK.submit(()->{try{if(Json.str(info,"type").equals("page")&&page==null)page=child;if(Json.str(info,"type").equals("page")||Json.str(info,"type").equals("iframe")){if(Json.str(info,"type").equals("page")&&page!=null&&!page.equals(child)){cdp.call("Target.closeTarget",map("targetId",info.get("targetId")),null);log("popupClosed",info);return;}configure(child);}else cdp.call("Runtime.runIfWaitingForDebugger",Map.of(),child);}catch(Exception ex){log("attachError",ex.toString());}});}
            else if(method.equals("Runtime.executionContextCreated")){var c=Json.obj(p.get("context"));if(Boolean.TRUE.equals(Json.obj(c.get("auxData")).get("isDefault")))contexts.put(s+":"+c.get("id"),map("session",s,"context",c));}
            else if(method.equals("Runtime.executionContextDestroyed"))contexts.remove(s+":"+p.get("executionContextId"));
            else if(method.equals("Runtime.executionContextsCleared"))contexts.entrySet().removeIf(e->Json.str(e.getValue(),"session").equals(s));
            else if(method.equals("Target.detachedFromTarget")){String detached=Json.str(p,"sessionId");contexts.entrySet().removeIf(e->Json.str(e.getValue(),"session").equals(detached));sessions.remove(detached);}
            else if(method.equals("Page.screencastFrame")){Stream capture=stream;cdp.send("Page.screencastFrameAck",map("sessionId",p.get("sessionId")),s);if(capture!=null)capture.frame(p);}
            else if(method.equals("Fetch.requestPaused")){String url=Json.str(Json.obj(p.get("request")),"url");log("fetchBlocked",map("url",url));cdp.send("Fetch.failRequest",map("requestId",p.get("requestId"),"errorReason","BlockedByClient"),s);}
            else if(method.equals("Page.javascriptDialogOpening"))cdp.send("Page.handleJavaScriptDialog",map("accept",false),s);
            else if(method.startsWith("Media.")||method.equals("Page.frameNavigated")||method.equals("Page.frameRequestedNavigation")||method.equals("Runtime.bindingCalled")||method.equals("Network.loadingFailed")||method.equals("Runtime.exceptionThrown")||method.equals("Log.entryAdded"))log(method,map("session",s,"params",p));
        }
        Object eval(String expression,String s,Object context)throws Exception{var p=map("expression",expression,"returnByValue",true,"awaitPromise",true);if(context!=null)p.put("contextId",context);return cdp.call("Runtime.evaluate",p,s);}
        void remember(){if(root!=null){owned.add(root);root.descendants().forEach(owned::add);}}
        Object snapshot()throws Exception{remember();var ps=new ArrayList<>();long cpu=0;for(var p:owned)if(p.isAlive()){long ns=p.info().totalCpuDuration().orElse(Duration.ZERO).toNanos();cpu+=ns;ps.add(map("pid",p.pid(),"cpuMs",ns/1e6));}return map("processes",ps,"chromeCpuMs",cpu/1e6,"javaCpuMs",ProcessHandle.current().info().totalCpuDuration().orElse(Duration.ZERO).toNanos()/1e6,"javaPid",ProcessHandle.current().pid());}
        Object stream(Map<String,Object> q)throws Exception{int seconds=((Number)q.getOrDefault("seconds",15)).intValue();if(seconds<1||seconds>60)throw new IllegalArgumentException("1..60s");String name=Json.str(q,"name");if(!name.matches("[a-zA-Z0-9_-]+"))throw new IllegalArgumentException("Invalid stream name");var capture=new Stream(dir,name);Object before=snapshot();long start=System.nanoTime();stream=capture;
            try{cdp.call("Page.startScreencast",map("format","jpeg","quality",q.getOrDefault("quality",80),"maxWidth",q.getOrDefault("width",1280),"maxHeight",q.getOrDefault("height",720),"everyNthFrame",1),page);Thread.sleep(seconds*1000L);}finally{cdp.call("Page.stopScreencast",Map.of(),page);stream=null;}
            Thread.sleep(300);Object result=capture.finish((System.nanoTime()-start)/1e9,before,snapshot());save(dir.resolve(name+"-summary.json"),result);log("stream",result);return result;}
        synchronized Object close()throws Exception{if(closing)return Map.of();closing=true;remember();if(cdp!=null&&root!=null&&root.isAlive())try{cdp.call("Browser.close",Map.of(),null);}catch(Exception ignored){}for(int n=0;n<30&&owned.stream().anyMatch(ProcessHandle::isAlive);n++){remember();Thread.sleep(100);}for(var p:owned)if(p.isAlive())p.destroy();Thread.sleep(300);for(var p:owned)if(p.isAlive())p.destroyForcibly();Thread.sleep(500);var result=map("ownedPids",owned.stream().map(ProcessHandle::pid).toList(),"remaining",owned.stream().filter(ProcessHandle::isAlive).map(ProcessHandle::pid).toList());log("cleanup",result);save(dir.resolve("cleanup.json"),result);if(cdp!=null)cdp.close();events.close();return result;}
    }
    static final class Cdp implements WebSocket.Listener,AutoCloseable {
        final Engine engine;final Map<Long,CompletableFuture<Object>> pending=new ConcurrentHashMap<>();final AtomicLong ids=new AtomicLong();final StringBuilder buffer=new StringBuilder();WebSocket ws;Socket pipe;CompletableFuture<WebSocket> sends=CompletableFuture.completedFuture(null);
        Cdp(URI uri,Engine e){engine=e;ws=HTTP.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(8)).buildAsync(uri,this).join();}
        Cdp(int port,Engine e)throws IOException{engine=e;pipe=new Socket(InetAddress.getLoopbackAddress(),port);WORK.submit(()->{try{var in=pipe.getInputStream();var b=new ByteArrayOutputStream();int n;while((n=in.read())>=0){if(n==0){receive(b.toString(java.nio.charset.StandardCharsets.UTF_8));b.reset();}else b.write(n);}}catch(Exception ex){pending.values().forEach(f->f.completeExceptionally(ex));}});}
        synchronized void transmit(String data){if(pipe!=null){try{var out=pipe.getOutputStream();out.write(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));out.write(0);out.flush();}catch(IOException ex){throw new UncheckedIOException(ex);}}else sends=sends.handle((v,t)->null).thenCompose(v->ws.sendText(data,true));}
        void send(String method,Map<String,Object> params,String session){var m=map("id",ids.incrementAndGet(),"method",method,"params",params);if(session!=null&&!session.isEmpty())m.put("sessionId",session);transmit(Json.write(m));}
        Object call(String method,Map<String,Object> params,String session)throws Exception{long id=ids.incrementAndGet();var f=new CompletableFuture<Object>();pending.put(id,f);var m=map("id",id,"method",method,"params",params);if(session!=null&&!session.isEmpty())m.put("sessionId",session);transmit(Json.write(m));try{return f.get(20,TimeUnit.SECONDS);}catch(Exception ex){engine.log("commandFailure",map("method",method,"session",session,"error",ex.toString()));throw ex;}finally{pending.remove(id);}}
        public void onOpen(WebSocket w){w.request(1);}
        void receive(String json){var m=Json.obj(Json.parse(json));if(m.get("id") instanceof Number n){var f=pending.get(n.longValue());if(f!=null){if(m.containsKey("error"))f.completeExceptionally(new IOException(Json.write(m.get("error"))));else f.complete(m.get("result"));}}else engine.event(m);}
        public CompletionStage<?> onText(WebSocket w,CharSequence data,boolean last){buffer.append(data);if(last){receive(buffer.toString());buffer.setLength(0);}w.request(1);return null;}
        public void onError(WebSocket w,Throwable t){pending.values().forEach(f->f.completeExceptionally(t));engine.log("websocketError",t.toString());}
        public CompletionStage<?> onClose(WebSocket w,int code,String reason){pending.values().forEach(f->f.completeExceptionally(new IOException("CDP closed "+code)));return null;}
        public void close(){if(ws!=null)ws.abort();if(pipe!=null)try{pipe.close();}catch(IOException ignored){}}
    }
    static final class Stream {
        final Path dir;final String name;final List<double[]> rows=Collections.synchronizedList(new ArrayList<>());final ExecutorService decode=Executors.newSingleThreadExecutor();final AtomicInteger index=new AtomicInteger();
        Stream(Path d,String n){dir=d;name=n;}
        void frame(Map<String,Object> p){double received=System.currentTimeMillis()/1000.0;long nano=System.nanoTime();int i=index.incrementAndGet();byte[] bytes=Base64.getDecoder().decode(Json.str(p,"data"));double source=((Number)Json.obj(p.get("metadata")).getOrDefault("timestamp",0)).doubleValue();decode.submit(()->{try{long begin=System.nanoTime();var img=ImageIO.read(new ByteArrayInputStream(bytes));double imageMs=(System.nanoTime()-begin)/1e6;double stbMs=stb(bytes);rows.add(new double[]{i,nano/1e9,bytes.length,(received-source)*1000,imageMs,stbMs,img.getWidth(),img.getHeight()});if(i==30||i==1){Files.write(dir.resolve(name+"-frame"+i+".jpg"),bytes);ImageIO.write(img,"png",dir.resolve(name+"-frame"+i+".png").toFile());}}catch(Exception ex){System.err.println("Decode: "+ex);}});}
        double stb(byte[] bytes){try{var cls=Class.forName("org.lwjgl.stb.STBImage");var b=ByteBuffer.allocateDirect(bytes.length).put(bytes).flip();var w=ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();var h=ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();var c=ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();long start=System.nanoTime();var out=(ByteBuffer)cls.getMethod("stbi_load_from_memory",ByteBuffer.class,IntBuffer.class,IntBuffer.class,IntBuffer.class,int.class).invoke(null,b,w,h,c,4);double ms=(System.nanoTime()-start)/1e6;if(out==null)return -1;cls.getMethod("stbi_image_free",ByteBuffer.class).invoke(null,out);return ms;}catch(Throwable ex){return -1;}}
        Object finish(double duration,Object before,Object after)throws Exception{decode.shutdown();decode.awaitTermination(20,TimeUnit.SECONDS);rows.sort(Comparator.comparingDouble(r->r[1]));try(var out=Files.newBufferedWriter(dir.resolve(name+"-frames.csv"))){out.write("frame,receiveMonotonicSec,jpegBytes,receiveMinusCaptureMs,imageIoMs,stbMs,width,height\n");for(var r:rows)out.write(String.join(",",Arrays.stream(r).mapToObj(Double::toString).toList())+"\n");}var fps=new ArrayList<Double>();for(int n=1;n<rows.size();n++)fps.add(1/(rows.get(n)[1]-rows.get(n-1)[1]));var result=map("seconds",duration,"frames",rows.size(),"averageFps",rows.size()/duration,"medianIntervalFps",percent(fps,.5),"p10IntervalFps",percent(fps,.1),"before",before,"after",after);for(int k=2;k<=5;k++){int col=k;var v=rows.stream().map(r->r[col]).toList();result.put(List.of("bytes","latencyMs","imageIoMs","stbMs").get(k-2),map("median",percent(v,.5),"p10",percent(v,.1),"p90",percent(v,.9)));}return result;}
        double percent(List<Double> values,double p){if(values.isEmpty())return 0;var v=new ArrayList<>(values);Collections.sort(v);return v.get((int)Math.floor((v.size()-1)*p));}
    }
}
