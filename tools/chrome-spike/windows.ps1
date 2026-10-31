param([int]$OwnedPid)
Add-Type @'
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
public static class SpikeWindows {
 [StructLayout(LayoutKind.Sequential)] public struct Rect { public int left,top,right,bottom; }
 delegate bool EnumProc(IntPtr h,IntPtr p);
 [DllImport("user32.dll")] static extern bool EnumWindows(EnumProc fn,IntPtr p);
 [DllImport("user32.dll")] static extern uint GetWindowThreadProcessId(IntPtr h,out int pid);
 [DllImport("user32.dll")] static extern bool GetWindowRect(IntPtr h,out Rect r);
 [DllImport("user32.dll")] static extern bool IsWindowVisible(IntPtr h);
 [DllImport("user32.dll")] static extern IntPtr GetForegroundWindow();
 public static string[] Read(int own) { var result=new List<string>();EnumWindows((h,p)=>{int pid;GetWindowThreadProcessId(h,out pid);if(pid==own){Rect r;GetWindowRect(h,out r);result.Add(h.ToInt64()+","+r.left+","+r.top+","+r.right+","+r.bottom+","+IsWindowVisible(h)+","+(h==GetForegroundWindow()));}return true;},IntPtr.Zero);return result.ToArray(); }
}
'@
[SpikeWindows]::Read($OwnedPid)
