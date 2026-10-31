$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Set-Location $repo
$env:TEMP = Join-Path $repo 'run/chrome-spike/tmp'
$env:TMP = $env:TEMP
New-Item -ItemType Directory -Force $env:TEMP,run/chrome-spike/classes | Out-Null
javac -d run/chrome-spike/classes tools/chrome-spike/Json.java tools/chrome-spike/ChromeSpike.java
if($LASTEXITCODE -ne 0){throw 'Compilation failed'}
# Reuse the Minecraft-resolved LWJGL jars; the harness also works without these optional jars.
$jars=Get-ChildItem .gradle-user-home/caches/modules-2/files-2.1/org.lwjgl/lwjgl/3.4.1,.gradle-user-home/caches/modules-2/files-2.1/org.lwjgl/lwjgl-stb/3.4.1 -Recurse -Filter '*.jar' -ErrorAction SilentlyContinue | Where-Object { $_.Name -match '^lwjgl(-stb)?-3.4.1(-(unsafe|natives-windows))?\.jar$' }
$cp='run/chrome-spike/classes;' + (($jars | ForEach-Object FullName) -join ';')
java --enable-native-access=ALL-UNNAMED "-Djava.io.tmpdir=$env:TEMP" "-Dorg.lwjgl.system.SharedLibraryExtractPath=$env:TEMP/lwjgl" -cp $cp ChromeSpike
