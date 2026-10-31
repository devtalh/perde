param(
    [Parameter(Mandatory)][ValidateSet('Player1', 'Player2')][string]$Client,
    [Parameter(Mandatory)][ValidateSet('open', 'url', 'geometry', 'center', 'access', 'add', 'remove', 'watch', 'close', 'mine', 'collect', 'status', 'f2', 'quit')][string]$Action,
    [int]$X = 0,
    [int]$Y = 120,
    [int]$Z = 3,
    [string[]]$Values = @()
)
$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$candidates = @(Get-CimInstance Win32_Process | Where-Object {
    $_.Name -match '^javaw?\.exe$' -and
    $_.CommandLine.Contains($repoRoot, [StringComparison]::OrdinalIgnoreCase) -and
    $_.CommandLine.Contains('net.fabricmc.devlaunchinjector.Main') -and
    $_.CommandLine.Contains('-Dfabric.addMods=') -and
    $_.CommandLine.Contains("--username $Client")
})
if ($candidates.Count -ne 1) {
    throw "Expected exactly one repository DLI fixture client for $Client; found $($candidates.Count)."
}
$gameDir = if ($Client -eq 'Player1') { Join-Path $repoRoot 'run' } else { Join-Path $repoRoot 'run/client2' }
$target = Join-Path $gameDir 'phase1a-probe-command.txt'
$fields = @([guid]::NewGuid().ToString('N'), $Action, $X, $Y, $Z) + $Values
[IO.File]::WriteAllText("$target.next", ($fields -join "`t"), [Text.UTF8Encoding]::new($false))
[IO.File]::Move("$target.next", $target, $true)
Write-Output "Queued $Action for $Client (verified repository DLI PID $($candidates[0].ProcessId))."
