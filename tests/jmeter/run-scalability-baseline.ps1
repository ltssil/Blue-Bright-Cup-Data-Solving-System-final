param(
    [string]$JMeterBin = "jmeter",
    [string]$HostName = "localhost",
    [int]$Port = 8081,
    [int[]]$Users = @(1, 5, 10, 30, 50),
    [int]$Loops = 5,
    [string]$Year = "2025",
    [string]$Competition = "蓝桥杯大赛",
    [string]$AwardLevel = "省部级单项",
    [string]$Major = "软件工程"
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$plan = Join-Path $scriptDir "scalability-baseline.jmx"
$resultRoot = Join-Path $scriptDir "results"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$runDir = Join-Path $resultRoot "baseline-$timestamp"

New-Item -ItemType Directory -Force -Path $runDir | Out-Null

foreach ($u in $Users) {
    $ramp = [Math]::Max(1, $u)
    $jtl = Join-Path $runDir "users-$u.jtl"
    $html = Join-Path $runDir "users-$u-html"

    Write-Host "Running scalability baseline: users=$u loops=$Loops ramp=$ramp"

    & $JMeterBin `
        -n `
        -t $plan `
        -l $jtl `
        -e `
        -o $html `
        -Jhost=$HostName `
        -Jport=$Port `
        -Jusers=$u `
        -Jramp=$ramp `
        -Jloops=$Loops `
        -Jyear=$Year `
        -Jcompetition=$Competition `
        -JawardLevel=$AwardLevel `
        -Jmajor=$Major

    if ($LASTEXITCODE -ne 0) {
        throw "JMeter failed for users=$u"
    }
}

Write-Host ""
Write-Host "Scalability baseline finished."
Write-Host "Results directory: $runDir"
