param(
    [Parameter(Mandatory)][ValidateSet('Player1', 'Player2')][string]$Client,
    [Parameter(Mandatory)][ValidateSet('open', 'url', 'geometry', 'center', 'access', 'add', 'remove', 'watch')][string]$Action,
    [int]$X = 0,
    [int]$Y = 120,
    [int]$Z = 3,
    [string[]]$Values = @()
)
$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$gameDir = if ($Client -eq 'Player1') { Join-Path $repoRoot 'run' } else { Join-Path $repoRoot 'run/client2' }
$target = Join-Path $gameDir 'phase1a-probe-command.txt'
$fields = @([guid]::NewGuid().ToString('N'), $Action, $X, $Y, $Z) + $Values
[IO.File]::WriteAllText("$target.next", ($fields -join "`t"), [Text.UTF8Encoding]::new($false))
[IO.File]::Move("$target.next", $target, $true)
Write-Output "Queued $Action for $Client in its repository-local dev run."
