
param (
    [int]$Users = 50,
    [int]$RampUp = 10,
    [int]$Duration = 30
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$LogFile = Join-Path $ScriptDir "debug_run.log"

Start-Transcript -Path $LogFile -Append

try {
    Write-Output "Starting Load Test Script..."
    
    $DockerComposeFile = Join-Path $ScriptDir "..\docker\docker-compose-load-test.yml"
    $ResultsDir = Join-Path $ScriptDir "results"
    
    if (Test-Path $ResultsDir) { Remove-Item -Path $ResultsDir -Recurse -Force }
    New-Item -ItemType Directory -Path $ResultsDir | Out-Null
    
    Write-Output "Starting infrastructure..."
    docker-compose -f $DockerComposeFile up -d postgres redis mock-service gateway
    
    Write-Output "Waiting 15s for startup..."
    Start-Sleep -Seconds 15
    
    # Run JMeter with absolute paths
    # Note: On Windows Docker Desktop, C:\Users\... is mounted.
    # We use lower-case drive letter just in case: c:/Users/...
    $AbsTestDir = $ScriptDir -replace "\\", "/"
    # Ensure drive letter is correct format for docker
    # If path starts with C:, docker might expect /c/... or C:/... depending on config.
    # Usually standard Windows path works if shared.
    
    $AbsResultsDir = $ResultsDir -replace "\\", "/"
    
    $JMeterImage = "justb4/jmeter:5.5"
    
    Write-Output "Running JMeter..."
    Write-Output "Test Dir: $AbsTestDir"
    Write-Output "Results Dir: $AbsResultsDir"

    # Use docker run directly
    docker run --rm --name jmeter-loadtest `
        --network loadtest-network `
        -v "$($AbsTestDir):/tests" `
        -v "$($AbsResultsDir):/results" `
        $JMeterImage `
        -n -t /tests/test-plan.jmx `
        -l /results/results.jtl `
        -e -o /results/report `
        -Jthreads=$Users -Jrampup=$RampUp -Jduration=$Duration `
        -JHOST=loadtest-gateway -JPORT=8080

    if ($LASTEXITCODE -ne 0) {
        Write-Error "JMeter exited with code $LASTEXITCODE"
    }

    if (Test-Path "$ResultsDir\report\index.html") {
        Write-Output "SUCCESS: Report generated."
        Invoke-Item "$ResultsDir\report\index.html"
    }
    else {
        Write-Error "FAILURE: Report index.html not found."
    }

}
catch {
    Write-Error $_
}
finally {
    Stop-Transcript
}
