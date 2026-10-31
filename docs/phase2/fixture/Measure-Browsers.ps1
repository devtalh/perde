param([Parameter(Mandatory)][string]$Stage)
$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$clients = @(Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object {
    $_.Name -eq 'java.exe' -and $_.CommandLine.Contains($repoRoot, [StringComparison]::OrdinalIgnoreCase) -and
    $_.CommandLine.Contains('net.fabricmc.devlaunchinjector.Main') -and $_.CommandLine.Contains('phase2-probe') -and
    $_.CommandLine.Contains('--username Player1')
})
if ($clients.Count -ne 1) { throw 'Expected exactly one repository F2-1 client.' }
$clientPid = $clients[0].ProcessId
$rows = for ($sample = 1; $sample -le 10; $sample++) {
    # CIM uses backslashes; normalize before checking the repository-owned native path.
    $helpers = @(Get-CimInstance Win32_Process -Filter "Name='jcef_helper.exe' AND ParentProcessId=$clientPid" | Where-Object {
        $_.Name -eq 'jcef_helper.exe' -and $_.ParentProcessId -eq $clientPid -and
        $_.ExecutablePath -and
        $_.ExecutablePath.Replace('\','/').StartsWith(($repoRoot.Replace('\','/') + '/run/config/mcef-modern/'), [StringComparison]::OrdinalIgnoreCase)
    })
    $total = 0L
    foreach ($helper in $helpers) {
        $process = Get-Process -Id $helper.ProcessId -ErrorAction SilentlyContinue
        if ($process) { $total += $process.WorkingSet64 }
    }
    [pscustomobject]@{Stage=$Stage;Sample=$sample;UTC=[datetime]::UtcNow.ToString('o');ClientPID=$clientPid;
        JavaBytes=(Get-Process -Id $clientPid).WorkingSet64;HelperCount=$helpers.Count;HelperPIDs=($helpers.ProcessId -join ';');HelperBytes=$total}
    Start-Sleep -Milliseconds 250
}
$output = Join-Path $repoRoot 'run/phase2-verification/memory.csv'
$rows | Export-Csv -Path $output -Append -NoTypeInformation
$rows | Measure-Object HelperBytes -Average | ForEach-Object { "${Stage}: helper count $($rows.HelperCount -join ','), mean $([math]::Round($_.Average / 1MB, 2)) MiB" }
