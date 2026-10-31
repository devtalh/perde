param(
    [Parameter(Mandatory)][ValidateSet('load', 'quit', 'f2', 'command', 'leave', 'close', 'aim', 'walk', 'open', 'panel-url', 'geometry', 'equip', 'place', 'break', 'status', 'policy', 'evaluate', 'raw-url', 'devtools', 'connect', 'chat-audit', 'panel-playback', 'panel-audit', 'raw-playback', 'playback-status', 'clock-flood')][string]$Action,
    [string[]]$Values = @(),
    [ValidateSet('Player1', 'Player2')][string]$Player = 'Player1'
)
$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$candidates = @(Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" | Where-Object {
    $_.Name -match '^javaw?\.exe$' -and
    $_.CommandLine.Contains($repoRoot, [StringComparison]::OrdinalIgnoreCase) -and
    $_.CommandLine.Contains('net.fabricmc.devlaunchinjector.Main') -and
    $_.CommandLine.Contains('-Dfabric.addMods=') -and
    $_.CommandLine.Contains('phase2-probe') -and
    $_.CommandLine.Contains("--username $Player")
})
if ($candidates.Count -ne 1) { throw "Expected exactly one repository F2-1 fixture client; found $($candidates.Count)." }
$target = Join-Path $repoRoot $(if ($Player -eq 'Player2') { 'run/client2/phase2-command.txt' } else { 'run/phase2-command.txt' })
$fields = @([guid]::NewGuid().ToString('N'), $Action) + $Values
[IO.File]::WriteAllText("$target.next", ($fields -join "`t"), [Text.UTF8Encoding]::new($false))
[IO.File]::Move("$target.next", $target, $true)
Write-Output "Queued $Action for verified repository DLI PID $($candidates[0].ProcessId)."
