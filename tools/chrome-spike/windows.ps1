param([int]$OwnedPid)
Add-Type @'
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
public static class SpikeWindows {
 [StructLayout(LayoutKind.Sequential)] public struct Rect { public int left,top,right,bottom; }
 delegate bool EnumProc(IntPtr h,IntPtr p);
 delegate bool MonitorProc(IntPtr h,IntPtr dc,ref Rect r,IntPtr p);
 [DllImport("user32.dll")] static extern bool EnumWindows(EnumProc fn,IntPtr p);
 [DllImport("user32.dll")] static extern uint GetWindowThreadProcessId(IntPtr h,out int pid);
 [DllImport("user32.dll")] static extern bool GetWindowRect(IntPtr h,out Rect r);
 [DllImport("user32.dll")] static extern bool IsWindowVisible(IntPtr h);
 [DllImport("user32.dll")] static extern IntPtr GetForegroundWindow();
 [DllImport("user32.dll")] static extern bool EnumDisplayMonitors(IntPtr dc,IntPtr clip,MonitorProc fn,IntPtr p);
 public static string[] Read(int own) { var result=new List<string>();var monitors=new List<Rect>();EnumDisplayMonitors(IntPtr.Zero,IntPtr.Zero,delegate(IntPtr h,IntPtr dc,ref Rect r,IntPtr p){monitors.Add(r);result.Add("monitor,"+r.left+","+r.top+","+r.right+","+r.bottom);return true;},IntPtr.Zero);EnumWindows((h,p)=>{int pid;GetWindowThreadProcessId(h,out pid);if(pid==own){Rect r;GetWindowRect(h,out r);bool hit=false;foreach(var m in monitors)if(r.left<m.right&&r.right>m.left&&r.top<m.bottom&&r.bottom>m.top)hit=true;result.Add(h.ToInt64()+","+r.left+","+r.top+","+r.right+","+r.bottom+","+IsWindowVisible(h)+","+(h==GetForegroundWindow())+",intersectsMonitor="+hit);}return true;},IntPtr.Zero);return result.ToArray(); }
}
'@
[SpikeWindows]::Read($OwnedPid)
