<#
.SYNOPSIS
  Resumes an interrupted initial Chromium checkout for the CODEC-1 build.

.DESCRIPTION
  automate-git.py with --no-chromium-history only runs hooks for a *new*
  checkout. If its initial `gclient sync` was interrupted, this script does
  what automate-git.py would have done, in a resumable way:
    1. gclient sync --nohooks --no-history with limited parallelism (retried),
    2. apply CEF's runhooks.patch (once, marker file in logs\),
    3. gclient runhooks (retried),
    4. delete src\out created by sync,
    5. run run-automate.ps1 -Stage sources (copies src\cef, branch config).
  Re-running is safe.
#>
param([string]$Workspace = 'D:\cef', [int]$Jobs = 4, [int]$Attempts = 6)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'cef-env.ps1') -Workspace $Workspace

$chromium = Join-Path $ws 'build\chromium'
$src = Join-Path $chromium 'src'
$python = Join-Path $depotTools 'python3.bat'

function Invoke-Retried([string]$label, [scriptblock]$block) {
    for ($i = 1; $i -le $Attempts; $i++) {
        Write-Output "[$((Get-Date).ToString('s'))] $label attempt $i"
        & $block
        if ($LASTEXITCODE -eq 0) { return }
        Write-Output "[$((Get-Date).ToString('s'))] $label failed with $LASTEXITCODE"
    }
    throw "$label failed after $Attempts attempts"
}

$started = Get-Date
$code = 1
try {
    Push-Location $chromium
    Invoke-Retried 'gclient sync' { & gclient.bat sync --nohooks --no-history --jobs $Jobs }

    $marker = Join-Path $ws 'logs\runhooks-patch.done'
    if (-not (Test-Path $marker)) {
        & $python (Join-Path $ws 'build\cef\tools\patcher.py') --patch-file (Join-Path $ws 'build\cef\patch\patches\runhooks') --patch-dir $src
        if ($LASTEXITCODE -ne 0) { throw "runhooks.patch failed: $LASTEXITCODE" }
        Set-Content -Path $marker -Value (Get-Date).ToString('s')
    }

    Invoke-Retried 'gclient runhooks' { & gclient.bat runhooks --jobs $Jobs }
    Pop-Location

    $out = Join-Path $src 'out'
    if ((Test-Path $out) -and -not (Test-Path (Join-Path $out 'cef.branch'))) { Remove-Item -Recurse -Force $out }

    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'run-automate.ps1') -Stage sources -Workspace $Workspace
    $code = $LASTEXITCODE
} catch {
    Write-Output "ERROR: $_"
    $code = 1
} finally {
    $ended = Get-Date
    Write-Output "[$($ended.ToString('s'))] resume exit=$code elapsed=$([int]($ended - $started).TotalMinutes) min"
    Set-Content -Path (Join-Path $ws 'logs\resume.exitcode') -Value "$code $($ended.ToString('s'))"
}
exit $code
