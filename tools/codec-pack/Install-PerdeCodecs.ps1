<#
.SYNOPSIS
  Installs (or restores) codec-enabled CEF runtime files in a Minecraft instance's MCEF folder.

.DESCRIPTION
  Install:
    1. Checks <Instance>\config\mcef-modern\jcef: install.lock present and
       build_meta.json release_tag/platform equal to the pack manifest.
    2. Refuses to run while any file in jcef is locked (Minecraft running).
    3. Backs up jcef to jcef-original-backup (once; an existing backup is kept
       and verified) with a SHA-256 inventory.
    4. Replaces ONLY files that exist both in jcef and in the pack. jcef.dll,
       jcef_helper.exe, build_meta.json, install.lock and licenses are never
       touched, so MCEF does not re-download (it only checks install.lock and
       build_meta.json).
    5. Verifies every replaced file against the pack manifest and writes
       config\mcef-modern\perde-codecs-installed.json (old/new SHA-256).
  -Restore copies the backup back over jcef and verifies it against the
  backup inventory. -Status only reports.

  The pack contains patent-encumbered H.264/AAC decoders: private use only,
  never upload or share it publicly.

.EXAMPLE
  .\Install-PerdeCodecs.ps1 -Instance "C:\path\to\instance\.minecraft" -Pack D:\cef\pack\perde-codecs-....zip
.EXAMPLE
  .\Install-PerdeCodecs.ps1 -Instance "C:\path\to\instance\.minecraft" -Restore
#>
[CmdletBinding(DefaultParameterSetName = 'Install')]
param(
    [Parameter(Mandatory = $true)][string]$Instance,
    [Parameter(ParameterSetName = 'Install', Mandatory = $true)][string]$Pack,
    [Parameter(ParameterSetName = 'Restore', Mandatory = $true)][switch]$Restore,
    [Parameter(ParameterSetName = 'Status', Mandatory = $true)][switch]$Status
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$mcefDir = Join-Path (Resolve-Path $Instance).Path 'config\mcef-modern'
$jcef = Join-Path $mcefDir 'jcef'
$backup = Join-Path $mcefDir 'jcef-original-backup'
$backupInventory = Join-Path $backup 'perde-backup-inventory.json'
$installedRecord = Join-Path $mcefDir 'perde-codecs-installed.json'

function Get-Sha256([string]$path) { (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLower() }

function Get-BuildMeta([string]$dir) {
    $metaPath = Join-Path $dir 'build_meta.json'
    if (-not (Test-Path $metaPath)) { throw "Missing $metaPath (MCEF natives not installed; start the game once first)" }
    if (-not (Test-Path (Join-Path $dir 'install.lock'))) { throw "Missing install.lock in $dir (incomplete MCEF install)" }
    return Get-Content -Raw $metaPath | ConvertFrom-Json
}

function Assert-NotLocked([string]$dir) {
    Get-ChildItem -LiteralPath $dir -Recurse -File | Where-Object { $_.Extension -in '.dll', '.exe' } | ForEach-Object {
        $file = $_.FullName
        try {
            $s = [System.IO.File]::Open($file, 'Open', 'ReadWrite', 'None')
            $s.Close()
        } catch {
            throw "File in use: $file. Close Minecraft (and any jcef_helper.exe) first."
        }
    }
}

function Get-Inventory([string]$dir) {
    $root = (Resolve-Path $dir).Path
    Get-ChildItem -LiteralPath $root -Recurse -File | Where-Object { $_.Name -ne 'perde-backup-inventory.json' } | Sort-Object FullName | ForEach-Object {
        [pscustomobject]@{ path = $_.FullName.Substring($root.Length + 1).Replace('\', '/'); size = $_.Length; sha256 = Get-Sha256 $_.FullName }
    }
}

function Test-AgainstInventory([string]$dir, $inventory) {
    $bad = @()
    foreach ($f in $inventory) {
        $p = Join-Path $dir $f.path
        if (-not (Test-Path -LiteralPath $p)) { $bad += "missing $($f.path)"; continue }
        if ((Get-Sha256 $p) -ne $f.sha256) { $bad += "hash $($f.path)" }
    }
    return $bad
}

if (-not (Test-Path $jcef)) { throw "No MCEF native folder at $jcef" }
$meta = Get-BuildMeta $jcef
Write-Output "MCEF natives: $($meta.release_tag) ($($meta.platform))"

if ($Status) {
    Write-Output "Backup:    $(if (Test-Path $backupInventory) { $backup } else { 'none' })"
    if (Test-Path $installedRecord) {
        $rec = Get-Content -Raw $installedRecord | ConvertFrom-Json
        $bad = Test-AgainstInventory $jcef ($rec.files | ForEach-Object { [pscustomobject]@{ path = $_.path; sha256 = $_.newSha256 } })
        if ($bad.Count -eq 0) { Write-Output "Codecs:    installed ($($rec.files.Count) files, hashes OK, $($rec.installed))" }
        else { Write-Output "Codecs:    record present but $($bad.Count) file(s) differ (restored or overwritten)" }
    } else { Write-Output 'Codecs:    not installed by this script' }
    return
}

Assert-NotLocked $jcef

if ($Restore) {
    if (-not (Test-Path $backupInventory)) { throw "No backup with inventory at $backup" }
    $inv = Get-Content -Raw $backupInventory | ConvertFrom-Json
    $bad = Test-AgainstInventory $backup $inv.files
    if ($bad.Count -gt 0) { throw "Backup is damaged: $($bad -join ', ')" }
    foreach ($f in $inv.files) {
        $dest = Join-Path $jcef $f.path
        New-Item -ItemType Directory -Force -Path (Split-Path $dest) | Out-Null
        Copy-Item -LiteralPath (Join-Path $backup $f.path) -Destination $dest -Force
    }
    $bad = Test-AgainstInventory $jcef $inv.files
    if ($bad.Count -gt 0) { throw "Restore verification failed: $($bad -join ', ')" }
    if (Test-Path $installedRecord) { Remove-Item -Force $installedRecord }
    Write-Output "Restored $($inv.files.Count) files from $backup (hashes verified). The backup is kept."
    return
}

# ---- Install ----
$zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path $Pack).Path)
try {
    $manifestEntry = $zip.Entries | Where-Object { $_.FullName.Replace('\', '/') -eq 'perde-codec-manifest.json' } | Select-Object -First 1
    if (-not $manifestEntry) { throw 'Pack has no perde-codec-manifest.json' }
    $reader = New-Object System.IO.StreamReader($manifestEntry.Open())
    $manifest = $reader.ReadToEnd() | ConvertFrom-Json
    $reader.Close()

    if ($manifest.releaseTag -ne $meta.release_tag -or $manifest.platform -ne $meta.platform) {
        throw "Version mismatch: pack is for $($manifest.releaseTag) ($($manifest.platform)), instance has $($meta.release_tag) ($($meta.platform)). Build a pack for this MCEF version."
    }

    # Backup once; an existing backup must belong to the same MCEF version and be intact.
    if (Test-Path $backup) {
        if (-not (Test-Path $backupInventory)) { throw "$backup exists without perde-backup-inventory.json; move it away manually" }
        $inv = Get-Content -Raw $backupInventory | ConvertFrom-Json
        if ($inv.releaseTag -ne $meta.release_tag) { throw "Existing backup is for $($inv.releaseTag); move it away manually" }
        $bad = Test-AgainstInventory $backup $inv.files
        if ($bad.Count -gt 0) { throw "Existing backup is damaged: $($bad -join ', ')" }
        Write-Output "Backup:    kept existing $backup"
    } else {
        if (Test-Path $installedRecord) { throw "Codecs already installed but no backup exists; restore stock MCEF natives first" }
        Copy-Item -LiteralPath $jcef -Destination $backup -Recurse
        $inv = [ordered]@{ releaseTag = $meta.release_tag; created = (Get-Date).ToString('s'); files = @(Get-Inventory $backup) }
        $inv | ConvertTo-Json -Depth 4 | Set-Content -Path $backupInventory -Encoding utf8
        Write-Output "Backup:    $backup ($($inv.files.Count) files)"
    }

    $never = @('jcef.dll', 'jcef_helper.exe', 'build_meta.json', 'install.lock', 'README.txt', 'LICENSE.txt', 'gluegen.LICENSE.txt', 'jogl.LICENSE.txt')
    $replaced = @()
    foreach ($f in $manifest.files) {
        if ($never -contains $f.path) { continue }
        $dest = Join-Path $jcef ($f.path.Replace('/', '\'))
        if (-not (Test-Path -LiteralPath $dest)) { continue }
        $entry = $zip.Entries | Where-Object { $_.FullName.Replace('\', '/') -eq $f.path } | Select-Object -First 1
        if (-not $entry) { throw "Pack manifest lists $($f.path) but the zip has no such entry" }
        # Original (stock) hash from the backup inventory, so a re-install still records stock -> codec.
        $orig = $inv.files | Where-Object { $_.path -eq $f.path } | Select-Object -First 1
        if ($orig) { $old = $orig.sha256 } else { $old = Get-Sha256 $dest }
        [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $dest, $true)
        $new = Get-Sha256 $dest
        if ($new -ne $f.sha256) { throw "Hash mismatch after copy: $($f.path). Run -Restore." }
        $replaced += [pscustomobject]@{ path = $f.path; oldSha256 = $old; newSha256 = $new; size = $f.size }
    }
} finally {
    $zip.Dispose()
}

$record = [ordered]@{ installed = (Get-Date).ToString('s'); pack = (Resolve-Path $Pack).Path; releaseTag = $meta.release_tag; cefVersion = $manifest.cefVersion; files = $replaced }
$record | ConvertTo-Json -Depth 4 | Set-Content -Path $installedRecord -Encoding utf8
Write-Output "Replaced:  $($replaced.Count) files (all hashes verified). Record: $installedRecord"
Write-Output 'Kept:      jcef.dll, jcef_helper.exe, build_meta.json, install.lock (MCEF will not re-download).'
