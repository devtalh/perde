param([Parameter(Mandatory=$true)][string]$Config)
$ErrorActionPreference = 'Stop'
$c = Get-Content -LiteralPath $Config -Raw | ConvertFrom-Json
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$profile = [IO.Path]::GetFullPath($c.profile)
if (-not $profile.StartsWith((Join-Path $repo 'run/chrome-spike/'), [StringComparison]::OrdinalIgnoreCase)) { throw 'Profile outside spike directory' }
if (Test-Path -LiteralPath $profile) { throw 'Profile must be fresh' }
New-Item -ItemType Directory -Path $profile | Out-Null
$env:TEMP = Join-Path $repo 'run/chrome-spike/tmp'
$env:TMP = $env:TEMP
New-Item -ItemType Directory -Force $env:TEMP | Out-Null
Add-Type @'
using System;
using System.Runtime.InteropServices;
using System.Text;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Threading.Tasks;
using Microsoft.Win32.SafeHandles;
public static class SpikeLaunch {
 [StructLayout(LayoutKind.Sequential, CharSet=CharSet.Unicode)] public struct SI { public int cb; public string reserved, desktop, title; public int x,y,xSize,ySize,xCount,yCount,fill,flags; public short show,reserved2; public IntPtr reservedPtr,input,output,error; }
 [StructLayout(LayoutKind.Sequential)] public struct PI { public IntPtr process,thread; public int pid,tid; }
 [DllImport("kernel32.dll",CharSet=CharSet.Unicode,SetLastError=true)] public static extern bool CreateProcess(string app,StringBuilder command,IntPtr pa,IntPtr ta,bool inherit,int flags,IntPtr env,string cwd,ref SI si,out PI pi);
 [DllImport("kernel32.dll")] public static extern bool CloseHandle(IntPtr h);
 [StructLayout(LayoutKind.Sequential)] struct SA {public int length;public IntPtr descriptor;public int inherit;}
 [DllImport("kernel32.dll",SetLastError=true)] static extern bool CreatePipe(out IntPtr read,out IntPtr write,ref SA sa,int size);
 [DllImport("kernel32.dll",SetLastError=true)] static extern bool SetHandleInformation(IntPtr h,int mask,int flags);
 public static int Start(string exe,string args,string cwd) { var si=new SI();si.cb=Marshal.SizeOf(si);si.flags=1;si.show=4;PI pi;if(!CreateProcess(exe,new StringBuilder("\""+exe+"\" "+args),IntPtr.Zero,IntPtr.Zero,false,0x08000000,IntPtr.Zero,cwd,ref si,out pi))throw new System.ComponentModel.Win32Exception();CloseHandle(pi.thread);CloseHandle(pi.process);return pi.pid; }
 public static void Pipe(string exe,string args,string cwd,string handshake) {
  var sa=new SA();sa.length=Marshal.SizeOf(sa);sa.inherit=1;IntPtr cr,pw,pr,cw;
  if(!CreatePipe(out cr,out pw,ref sa,0)||!CreatePipe(out pr,out cw,ref sa,0))throw new System.ComponentModel.Win32Exception();
  SetHandleInformation(pw,1,0);SetHandleInformation(pr,1,0);
  var si=new SI();si.cb=Marshal.SizeOf(si);si.flags=1;si.show=4;PI pi;
  args+=" --remote-debugging-pipe --remote-debugging-io-pipes="+cr.ToInt64()+","+cw.ToInt64();
  if(!CreateProcess(exe,new StringBuilder("\""+exe+"\" "+args),IntPtr.Zero,IntPtr.Zero,true,0x08000000,IntPtr.Zero,cwd,ref si,out pi))throw new System.ComponentModel.Win32Exception();
  CloseHandle(cr);CloseHandle(cw);CloseHandle(pi.thread);CloseHandle(pi.process);
  using(var input=new FileStream(new SafeFileHandle(pr,true),FileAccess.Read,4096,false))using(var output=new FileStream(new SafeFileHandle(pw,true),FileAccess.Write,1,false)) {
   var listener=new TcpListener(IPAddress.Loopback,0);listener.Start();File.WriteAllText(handshake,"{\"pid\":"+pi.pid+",\"port\":"+((IPEndPoint)listener.LocalEndpoint).Port+"}");
   var accept=listener.AcceptTcpClientAsync();if(!accept.Wait(30000)){listener.Stop();return;}
   using(var client=accept.Result){listener.Stop();using(var net=client.GetStream()){var a=Task.Run(()=>input.CopyTo(net));var b=Task.Run(()=>net.CopyTo(output));Task.WaitAny(a,b);}}
  }
 }
}
'@
# SW_SHOWNOACTIVATE. No activation, desktop input, real profile, or shell window.
$argsText = ($c.arguments | ForEach-Object { if ($_ -match '"') { throw 'Unexpected quote' }; '"' + $_ + '"' }) -join ' '
if($c.pipe){[SpikeLaunch]::Pipe($c.executable,$argsText,$repo,$c.handshake)}else{[SpikeLaunch]::Start($c.executable, $argsText, $repo)}
