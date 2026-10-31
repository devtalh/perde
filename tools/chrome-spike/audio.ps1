param([string]$Id,[switch]$Tone)
. $PSScriptRoot/common.ps1
Add-Type @'
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
public static class SpikeAudio {
 [ComImport,Guid("BCDE0395-E52F-467C-8E3D-C4579291692E")] class Enumerator {}
 [ComImport,Guid("A95664D2-9614-4F35-A746-DE8DB63617E6"),InterfaceType(ComInterfaceType.InterfaceIsIUnknown)] interface IE { int EnumAudioEndpoints(int f,int mask,out IntPtr a);int GetDefaultAudioEndpoint(int f,int role,out ID d); }
 [ComImport,Guid("D666063F-1587-4E43-81F1-B948E807363F"),InterfaceType(ComInterfaceType.InterfaceIsIUnknown)] interface ID { int Activate(ref Guid iid,int cls,IntPtr p,[MarshalAs(UnmanagedType.IUnknown)] out object result); }
 [ComImport,Guid("77AA99A0-1BD6-484F-8BC7-2C654C9A9B6F"),InterfaceType(ComInterfaceType.InterfaceIsIUnknown)] interface IM {int GetAudioSessionControl(IntPtr g,int f,out IntPtr c);int GetSimpleAudioVolume(IntPtr g,int f,out IntPtr c);int GetSessionEnumerator(out ISE e);}
 [ComImport,Guid("E2F5BB11-0570-40CA-ACDD-3AA01277DEE8"),InterfaceType(ComInterfaceType.InterfaceIsIUnknown)] interface ISE {int GetCount(out int n);int GetSession(int n,out IC c);}
 [ComImport,Guid("BFB7FF88-7239-4FC9-8FA2-07C950BE9C6D"),InterfaceType(ComInterfaceType.InterfaceIsIUnknown)] interface IC {
  int GetState(out int s);int GetDisplayName(out IntPtr s);int SetDisplayName(IntPtr s,IntPtr g);int GetIconPath(out IntPtr s);int SetIconPath(IntPtr s,IntPtr g);int GetGroupingParam(out Guid g);int SetGroupingParam(IntPtr a,IntPtr b);int RegisterAudioSessionNotification(IntPtr p);int UnregisterAudioSessionNotification(IntPtr p);int GetSessionIdentifier(out IntPtr s);int GetSessionInstanceIdentifier(out IntPtr s);int GetProcessId(out uint pid);int IsSystemSoundsSession();int SetDuckingPreference(bool o);
 }
 [ComImport,Guid("C02216F6-8C67-4B5B-9D00-D008E73E0064"),InterfaceType(ComInterfaceType.InterfaceIsIUnknown)] interface Meter {int GetPeakValue(out float p);int GetMeteringChannelCount(out int n);int GetChannelsPeakValues(int n,IntPtr p);int QueryHardwareSupport(out int s);}
 public static string[] Read(int[] own) {var output=new List<string>();ID d;Marshal.ThrowExceptionForHR(((IE)new Enumerator()).GetDefaultAudioEndpoint(0,1,out d));object manager;var guid=typeof(IM).GUID;Marshal.ThrowExceptionForHR(d.Activate(ref guid,23,IntPtr.Zero,out manager));ISE en;Marshal.ThrowExceptionForHR(((IM)manager).GetSessionEnumerator(out en));int n;en.GetCount(out n);for(int i=0;i<n;i++){IC c;en.GetSession(i,out c);uint pid;c.GetProcessId(out pid);if(Array.IndexOf(own,(int)pid)<0)continue;float peak;int state;c.GetState(out state);((Meter)c).GetPeakValue(out peak);output.Add(pid+","+state+","+peak.ToString(System.Globalization.CultureInfo.InvariantCulture));}return output.ToArray();}
}
'@
$snapshot=C @{op='snapshot';id=$Id};$ownPids=@($snapshot.processes | ForEach-Object {[int]$_.pid})
$samples=@()
if($Tone){
  # Exactly one bounded two-second, 0.05 gain output check per engine. Native audio-clock stop is independent of page timers.
  $js="(()=>{const c=new AudioContext();const o=new OscillatorNode(c,{frequency:440});const g=new GainNode(c,{gain:0.05});o.connect(g).connect(c.destination);o.start();o.stop(c.currentTime+2);o.onended=()=>c.close();c.resume();return {state:c.state,gain:g.gain.value,maxSeconds:2}})()"
  $toneResult=E $Id $js
}
for($i=0;$i -lt 26;$i++){ $samples+=@{at=[DateTimeOffset]::UtcNow.ToString('o');sessions=@([SpikeAudio]::Read($ownPids))};Start-Sleep -Milliseconds 100 }
Save "run/chrome-spike/$Id/audio$(if($Tone){'-tone'}else{'-muted'}).json" @{tone=$toneResult;ownedPids=$ownPids;samples=$samples}
$samples | ForEach-Object { $_.sessions } | Sort-Object -Unique
