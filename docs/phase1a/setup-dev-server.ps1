$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$serverDir = Join-Path $repoRoot 'run/server'
New-Item -ItemType Directory -Force -Path $serverDir | Out-Null
$propertiesPath = Join-Path $serverDir 'server.properties'
$lines = if (Test-Path -LiteralPath $propertiesPath) { @(Get-Content -LiteralPath $propertiesPath) } else { @() }
$settings = [ordered]@{
    'online-mode' = 'false'
    'enforce-secure-profile' = 'false'
    'server-ip' = '127.0.0.1'
    'server-port' = '25570'
}
foreach ($key in $settings.Keys) {
    $pattern = '^' + [regex]::Escape($key) + '='
    $lines = @($lines | Where-Object { $_ -notmatch $pattern })
    $lines += "$key=$($settings[$key])"
}
[IO.File]::WriteAllLines($propertiesPath, $lines, [Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText((Join-Path $serverDir 'eula.txt'), "eula=true`n", [Text.UTF8Encoding]::new($false))
Write-Output 'Prepared repository-local run/server on 127.0.0.1:25570 (EULA accepted for this dev server).'
