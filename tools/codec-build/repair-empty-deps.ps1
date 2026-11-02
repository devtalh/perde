<#
.SYNOPSIS
  Repairs dependency repos that gclient created but never finished fetching.

.DESCRIPTION
  After network failures during `gclient sync --no-history`, some dependency
  folders contain an initialised .git with a remote but no commits (no HEAD).
  Later syncs skip them because the folder already exists, and hooks/builds
  then fail (for example missing v8 files). For every git dependency listed by
  `gclient revinfo` whose HEAD is missing or differs from the DEPS revision,
  this script fetches exactly that revision with depth 1 and checks it out
  (detached), which is the state gclient itself produces. Nothing is deleted.
  Re-running is safe: repos already at the right revision are skipped.
#>
param([string]$Workspace = 'D:\cef', [int]$Attempts = 5)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'cef-env.ps1') -Workspace $Workspace

$chromium = Join-Path $ws 'build\chromium'
Push-Location $chromium
$revinfo = & gclient.bat revinfo
Pop-Location
if ($LASTEXITCODE -ne 0) { throw "gclient revinfo failed: $LASTEXITCODE" }

$failed = @()
foreach ($line in $revinfo) {
    if ($line -notmatch '^([^:]+): (\S+)@([0-9a-f]{40})$') { continue }
    $path = $Matches[1]; $url = $Matches[2]; $rev = $Matches[3]
    $dir = Join-Path $chromium $path
    if (-not (Test-Path (Join-Path $dir '.git'))) { continue }
    $head = & git.exe -C $dir rev-parse --verify --quiet HEAD
    if ($LASTEXITCODE -eq 0 -and $head -eq $rev) { continue }

    $ok = $false
    for ($i = 1; $i -le $Attempts -and -not $ok; $i++) {
        Write-Output "[$((Get-Date).ToString('s'))] $path fetch $rev (attempt $i)"
        Remove-Item -Force -ErrorAction SilentlyContinue (Join-Path $dir '.git\shallow.lock'), (Join-Path $dir '.git\index.lock')
        & git.exe -C $dir -c core.deltaBaseCacheLimit=2g fetch --no-tags --depth=1 -- $url $rev
        if ($LASTEXITCODE -eq 0) {
            & git.exe -C $dir -c advice.detachedHead=false checkout --force --quiet FETCH_HEAD
            if ($LASTEXITCODE -eq 0) { $ok = $true }
        }
        if (-not $ok) { Start-Sleep -Seconds 20 }
    }
    if ($ok) { Write-Output "[$((Get-Date).ToString('s'))] $path OK" } else { $failed += $path }
}

if ($failed.Count -gt 0) {
    Write-Output "FAILED: $($failed -join ', ')"
    Set-Content -Path (Join-Path $ws 'logs\repair.exitcode') -Value "1 $((Get-Date).ToString('s'))"
    exit 1
}
Write-Output 'All git dependencies are at their DEPS revision.'
Set-Content -Path (Join-Path $ws 'logs\repair.exitcode') -Value "0 $((Get-Date).ToString('s'))"
exit 0
