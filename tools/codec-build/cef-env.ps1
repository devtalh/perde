# Dot-sourced by the CODEC-1 build scripts. Redirects this process tree's
# profile, temp and tool caches into the workspace and sets the build
# environment. Changes nothing outside the workspace and nothing system-wide.
param([string]$Workspace = 'D:\cef', [string]$ExtraGnDefines = '')

$script:ws = [System.IO.Path]::GetFullPath($Workspace)
$wsHome = Join-Path $ws 'home'
foreach ($d in @($wsHome, (Join-Path $wsHome 'AppData\Local'), (Join-Path $wsHome 'AppData\Roaming'),
        (Join-Path $wsHome '.cache'), (Join-Path $ws 'tmp'), (Join-Path $ws 'cache\vpython-root'),
        (Join-Path $ws 'cache\cipd'), (Join-Path $ws 'logs'), (Join-Path $ws 'build'))) {
    New-Item -ItemType Directory -Force -Path $d | Out-Null
}

# Workspace-local git config. Chromium requires autocrlf=false on Windows; the
# system Git for Windows config sets autocrlf=true. HOME points here, so the
# real user's global config is neither read nor changed.
$gitConfig = Join-Path $wsHome '.gitconfig'
if (-not (Test-Path $gitConfig)) {
    @('[core]', "`tautocrlf = false", "`tfilemode = false", "`tpreloadindex = true", "`tfscache = true",
        "`tlongpaths = true", '[branch]', "`tautosetuprebase = always") | Set-Content -Path $gitConfig -Encoding ascii
}

$env:HOME = $wsHome
$env:USERPROFILE = $wsHome
$env:LOCALAPPDATA = Join-Path $wsHome 'AppData\Local'
$env:APPDATA = Join-Path $wsHome 'AppData\Roaming'
$env:XDG_CACHE_HOME = Join-Path $wsHome '.cache'
$env:TEMP = Join-Path $ws 'tmp'
$env:TMP = $env:TEMP
$env:VPYTHON_VIRTUALENV_ROOT = Join-Path $ws 'cache\vpython-root'
$env:CIPD_CACHE_DIR = Join-Path $ws 'cache\cipd'
$env:DEPOT_TOOLS_METRICS = '0'
$env:DEPOT_TOOLS_WIN_TOOLCHAIN = '0'
$env:GYP_MSVS_VERSION = '2022'
$env:PYTHONUNBUFFERED = '1'
# Official CEF Windows x64 GN_DEFINES (chromiumembedded.github.io/cef/automated_build_setup)
# plus proprietary codecs for H.264/AAC.
$env:GN_DEFINES = ('is_official_build=true proprietary_codecs=true ffmpeg_branding=Chrome ' + $ExtraGnDefines).Trim()
$env:CEF_ARCHIVE_FORMAT = 'zip'

$script:depotTools = Join-Path $ws 'depot_tools'
if (Test-Path $depotTools) { $env:PATH = $depotTools + ';' + $env:PATH }
