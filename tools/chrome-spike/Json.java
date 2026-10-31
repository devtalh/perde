import java.util.*;

/** Small dependency-free JSON codec for the CDP measurement harness. */
final class Json {
    static Object parse(String s) { var p = new Parser(s); Object v = p.value(); p.ws(); if (p.i != s.length()) throw new IllegalArgumentException("Trailing JSON"); return v; }
    static String write(Object v) {
        if (v == null) return "null";
        if (v instanceof String s) { var b = new StringBuilder("\""); for (char c : s.toCharArray()) b.append(switch(c) { case '"' -> "\\\""; case '\\' -> "\\\\"; case '\n' -> "\\n"; case '\r' -> "\\r"; case '\t' -> "\\t"; default -> c < 32 ? String.format("\\u%04x", (int)c) : String.valueOf(c); }); return b.append('"').toString(); }
        if (v instanceof Map<?,?> m) return "{" + String.join(",", m.entrySet().stream().map(e -> write(e.getKey().toString()) + ":" + write(e.getValue())).toList()) + "}";
        if (v instanceof Collection<?> a) return "[" + String.join(",", a.stream().map(Json::write).toList()) + "]";
        if (v instanceof Number n && !Double.isFinite(n.doubleValue())) return "null";
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        throw new IllegalArgumentException("Not JSON: " + v.getClass());
    }
    @SuppressWarnings("unchecked") static Map<String,Object> obj(Object v) { return v instanceof Map ? (Map<String,Object>)v : Map.of(); }
    static String str(Map<String,Object> m, String k) { return Objects.toString(m.get(k), ""); }
    static final class Parser {
        final String s; int i; Parser(String s) { this.s=s; } void ws(){ while(i<s.length()&&Character.isWhitespace(s.charAt(i)))i++; }
        Object value(){ ws(); char c=s.charAt(i); if(c=='{'){i++; var m=new LinkedHashMap<String,Object>(); ws(); if(s.charAt(i)=='}'){i++;return m;} do{ws();String k=string();ws();if(s.charAt(i++)!=':')throw new IllegalArgumentException();m.put(k,value());ws();char d=s.charAt(i++);if(d=='}')return m;if(d!=',')throw new IllegalArgumentException();}while(true);}
            if(c=='['){i++;var a=new ArrayList<>();ws();if(s.charAt(i)==']'){i++;return a;}do{a.add(value());ws();char d=s.charAt(i++);if(d==']')return a;if(d!=',')throw new IllegalArgumentException();}while(true);}
            if(c=='"')return string(); for(String lit:List.of("true","false","null"))if(s.startsWith(lit,i)){i+=lit.length();return lit.equals("null")?null:lit.equals("true");}
            int start=i;while(i<s.length()&&"-+0123456789.eE".indexOf(s.charAt(i))>=0)i++;return Double.valueOf(s.substring(start,i)); }
        String string(){if(s.charAt(i++)!='"')throw new IllegalArgumentException();var b=new StringBuilder();while(true){char c=s.charAt(i++);if(c=='"')return b.toString();if(c=='\\'){c=s.charAt(i++);c=switch(c){case 'n'->'\n';case 'r'->'\r';case 't'->'\t';case 'b'->'\b';case 'f'->'\f';case 'u'->{char u=(char)Integer.parseInt(s.substring(i,i+4),16);i+=4;yield u;}default->c;};}b.append(c);}}
    }
}
