<#
.SYNOPSIS
  Runs CEF's automate-git.py for the Perde codec build (CODEC-1 / LEA-200).

.DESCRIPTION
  Everything (depot_tools, Chromium/CEF sources, outputs, caches, temp files)
  stays inside one workspace folder (default D:\cef). cef-env.ps1 redirects
  the process environment (HOME, USERPROFILE, APPDATA, LOCALAPPDATA, TEMP,
  vpython/CIPD caches) to that workspace so tools do not write into the real
  user profile. Nothing system-wide is changed: no PATH/registry/Defender
  edits, no installs.

  Re-running the same command resumes: automate-git.py reuses an existing
  checkout, and the build stage re-runs ninja/siso incrementally. If the
  initial `gclient sync` was interrupted, use resume-sync.ps1 first.

  Stages:
    sources  depot_tools update + Chromium/CEF checkout + hooks, no build.
    build    gn gen (via CEF's gclient_hook.py) + Release x64 build + minimal
             binary distribution, using the official Windows GN args plus
             proprietary codecs.

  Start it detached, for example:
    Start-Process powershell.exe -WindowStyle Hidden -ArgumentList `
      '-NoProfile','-ExecutionPolicy','Bypass','-File','tools\codec-build\run-automate.ps1','-Stage','sources' `
      -RedirectStandardOutput D:\cef\logs\sources.out.log -RedirectStandardError D:\cef\logs\sources.err.log
#>
param(
    [Parameter(Mandatory = $true)][ValidateSet('sources', 'build')][string]$Stage,
    [string]$Workspace = 'D:\cef',
    # CEF 146.0.10+g8219561+chromium-146.0.7680.179 (branch 7680).
    [string]$CefBranch = '7680',
    [string]$CefCommit = '82195616d8405e6081a0d90924707b82aa9e4141',
    # Extra GN args appended after the official ones (only for documented fallbacks).
    [string]$ExtraGnDefines = ''
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'cef-env.ps1') -Workspace $Workspace -ExtraGnDefines $ExtraGnDefines

if (-not (Test-Path (Join-Path $depotTools '.git'))) {
    & git.exe clone https://chromium.googlesource.com/chromium/tools/depot_tools.git $depotTools
    if ($LASTEXITCODE -ne 0) { throw "depot_tools clone failed: $LASTEXITCODE" }
}

$automate = Join-Path $ws 'automate\automate-git.py'
if (-not (Test-Path $automate)) {
    New-Item -ItemType Directory -Force -Path (Split-Path $automate) | Out-Null
    Invoke-WebRequest -UseBasicParsing -OutFile $automate `
        -Uri "https://raw.githubusercontent.com/chromiumembedded/cef/$CefCommit/tools/automate/automate-git.py"
}

$common = @(
    "--download-dir=$(Join-Path $ws 'build')",
    "--depot-tools-dir=$depotTools",
    "--branch=$CefBranch",
    "--checkout=$CefCommit",
    '--x64-build',
    '--no-debug-build',
    '--with-pgo-profiles',
    '--no-chromium-history'
)
if ($Stage -eq 'sources') {
    $stageArgs = @('--no-build', '--no-distrib')
} else {
    $stageArgs = @(
        '--no-update', '--no-depot-tools-update', '--force-build',
        '--build-target=cefsimple', '--build-log-file',
        '--minimal-distrib', '--minimal-distrib-only',
        '--no-distrib-docs', '--no-distrib-archive', '--force-distrib'
    )
}

# depot_tools' bundled Python once bootstrapped, otherwise the existing Python.
$python = Join-Path $depotTools 'python3.bat'
if (-not (Test-Path $python)) { $python = (Get-Command python.exe -ErrorAction Stop).Source }

$started = Get-Date
Write-Output "[$($started.ToString('s'))] stage=$Stage python=$python"
Write-Output "GN_DEFINES=$env:GN_DEFINES"
Write-Output "automate-git.py $($common + $stageArgs -join ' ')"
& $python $automate @common @stageArgs
$code = $LASTEXITCODE
$ended = Get-Date
Write-Output "[$($ended.ToString('s'))] stage=$Stage exit=$code elapsed=$([int]($ended - $started).TotalMinutes) min"
Set-Content -Path (Join-Path $ws "logs\$Stage.exitcode") -Value "$code $($ended.ToString('s'))"
exit $code
