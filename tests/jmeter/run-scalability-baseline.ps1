param(
    [string]$JMeterBin = "jmeter",
    [string]$HostName = "localhost",
    [int]$Port = 8081,
    [int[]]$Users = @(1, 5, 10, 30, 50),
    [int]$Loops = 5,
    [string]$Year = "2025",
    [string]$Competition = "BaselineCompetition",
    [string]$AwardLevel = "BaselineAwardLevel",
    [string]$Major = ""
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
    $runtimePlan = Join-Path $runDir "users-$u.jmx"

    $planText = Get-Content $plan -Raw
    $planText = $planText -replace '<stringProp name="ThreadGroup.num_threads">.*?</stringProp>', "<stringProp name=`"ThreadGroup.num_threads`">$u</stringProp>"
    $planText = $planText -replace '<stringProp name="ThreadGroup.ramp_time">.*?</stringProp>', "<stringProp name=`"ThreadGroup.ramp_time`">$ramp</stringProp>"
    $planText = $planText -replace '<stringProp name="LoopController.loops">.*?</stringProp>', "<stringProp name=`"LoopController.loops`">$Loops</stringProp>"
    Set-Content -Path $runtimePlan -Value $planText -Encoding UTF8

    Write-Host "Running scalability baseline: users=$u loops=$Loops ramp=$ramp"

    $runArgs = @(
        "-n",
        "-t", $runtimePlan,
        "-l", $jtl,
        "-Jhost=$HostName",
        "-Jport=$Port",
        "-Jusers=$u",
        "-Jramp=$ramp",
        "-Jloops=$Loops",
        "-Jyear=$Year",
        "-Jcompetition=$Competition",
        "-JawardLevel=$AwardLevel",
        "-Jmajor=$Major"
    )

    & $JMeterBin @runArgs

    if ($LASTEXITCODE -ne 0) {
        throw "JMeter failed for users=$u"
    }

    $sampleCount = [Math]::Max(0, (Get-Content $jtl | Measure-Object -Line).Lines - 1)
    if ($sampleCount -le 0) {
        throw "JMeter produced no samples for users=$u. Check the JMX Thread Group and backend status."
    }

    & $JMeterBin @("-g", $jtl, "-o", $html)

    if ($LASTEXITCODE -ne 0) {
        throw "JMeter report generation failed for users=$u"
    }
}

Write-Host ""
Write-Host "Scalability baseline finished."
Write-Host "Results directory: $runDir"
