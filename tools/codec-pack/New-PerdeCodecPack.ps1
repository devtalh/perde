<#
.SYNOPSIS
  Packs codec-enabled CEF runtime files into a PRIVATE local zip with a SHA-256 manifest.

.DESCRIPTION
  Reads a CEF binary distribution (the folder that contains Release\ and
  Resources\, as produced by CEF's make_distrib / automate-git.py) and writes
  a zip laid out like MCEF's native directory (config\mcef-modern\jcef):
  Release\* and Resources\* at the zip root, locales\*.pak under locales\.
  Only runtime files are packed: *.dll, *.bin, *.dat, *.pak, *.json.
  Import libraries, symbols, headers and sample executables are skipped.

  The zip contains patent-encumbered H.264/AAC decoders. It is for private use
  only: never commit, push, upload or share it. The script refuses to write
  the zip inside a git working tree.

.EXAMPLE
  .\New-PerdeCodecPack.ps1 -DistribDir D:\cef\build\chromium\src\cef\binary_distrib\cef_binary_146.0.10+g8219561+chromium-146.0.7680.179_windows64_minimal
#>
param(
    [Parameter(Mandatory = $true)][string]$DistribDir,
    [string]$OutputDir = 'D:\cef\pack',
    # The MCEF/jcefmaven release_tag this pack is built for (must match build_meta.json).
    [string]$ReleaseTag = 'jcef-d3de827+cef-146.0.10+g8219561+chromium-146.0.7680.179',
    [string]$GnArgsFile = ''
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$DistribDir = (Resolve-Path $DistribDir).Path
$release = Join-Path $DistribDir 'Release'
$resources = Join-Path $DistribDir 'Resources'
if (-not (Test-Path (Join-Path $release 'libcef.dll'))) { throw "No Release\libcef.dll under $DistribDir" }
if (-not (Test-Path $resources)) { throw "No Resources folder under $DistribDir" }

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$OutputDir = (Resolve-Path $OutputDir).Path

# Safety: never place codec binaries inside a git working tree.
Push-Location $OutputDir
try {
    $inside = & git.exe rev-parse --is-inside-work-tree 2>$null
} catch { $inside = $null } finally { Pop-Location }
if ($inside -eq 'true') { throw "Refusing to write codec files inside a git working tree: $OutputDir" }

$runtimeExt = @('.dll', '.bin', '.dat', '.pak', '.json')
$staging = Join-Path $OutputDir 'staging'
if (Test-Path $staging) { Remove-Item -Recurse -Force $staging }
New-Item -ItemType Directory -Force -Path (Join-Path $staging 'locales') | Out-Null

$files = @()
foreach ($src in @($release, $resources)) {
    Get-ChildItem -Path $src -Recurse -File | Where-Object { $runtimeExt -contains $_.Extension.ToLower() } | ForEach-Object {
        $rel = $_.FullName.Substring($src.Length + 1)
        if ($rel.Contains('\') -and -not $rel.StartsWith('locales\')) { return }
        $dest = Join-Path $staging $rel
        Copy-Item -LiteralPath $_.FullName -Destination $dest
        $files += [pscustomobject]@{
            path   = $rel.Replace('\', '/')
            size   = $_.Length
            sha256 = (Get-FileHash -LiteralPath $dest -Algorithm SHA256).Hash.ToLower()
        }
    }
}

$cefVersion = ''
$cefHeader = Join-Path $DistribDir 'include\cef_version.h'
if (Test-Path $cefHeader) {
    $m = Select-String -Path $cefHeader -Pattern '#define CEF_VERSION "([^"]+)"' | Select-Object -First 1
    if ($m) { $cefVersion = $m.Matches[0].Groups[1].Value }
}
$gnArgs = ''
if ($GnArgsFile -and (Test-Path $GnArgsFile)) { $gnArgs = (Get-Content -Raw $GnArgsFile) }

$manifest = [ordered]@{
    kind        = 'perde-codec-pack'
    warning     = 'PRIVATE: contains H.264/AAC decoders. Never upload or share publicly.'
    releaseTag  = $ReleaseTag
    platform    = 'windows-amd64'
    cefVersion  = $cefVersion
    distribDir  = $DistribDir
    created     = (Get-Date).ToString('s')
    gnArgs      = $gnArgs
    files       = $files | Sort-Object path
}
$manifest | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $staging 'perde-codec-manifest.json') -Encoding utf8

$safeVersion = ($cefVersion -replace '[^A-Za-z0-9.+-]', '_')
if (-not $safeVersion) { $safeVersion = 'unknown' }
$zip = Join-Path $OutputDir "perde-codecs-cef-$safeVersion-windows-amd64.zip"
if (Test-Path $zip) { Remove-Item -Force $zip }
[System.IO.Compression.ZipFile]::CreateFromDirectory($staging, $zip, [System.IO.Compression.CompressionLevel]::Optimal, $false)
$zipHash = (Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLower()
Set-Content -Path "$zip.sha256" -Value "$zipHash  $(Split-Path -Leaf $zip)" -Encoding ascii
Remove-Item -Recurse -Force $staging

Write-Output "Pack:     $zip"
Write-Output "SHA-256:  $zipHash"
Write-Output "Files:    $($files.Count)"
Write-Output 'PRIVATE: never commit, upload or share this zip.'
