
param (
    [int]$Users = 50,
    [int]$RampUp = 10,
    [int]$Duration = 30
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$DockerComposeFile = Join-Path $ScriptDir "..\docker\docker-compose-load-test.yml"
$ResultsDir = Join-Path $ScriptDir "results"
$LogFile = Join-Path $ScriptDir "debug.log"

Start-Transcript -Path $LogFile -Append

try {
    Write-Output "Starting Reliable Load Test..."
    
    # 1. Start Infrastructure
    try {
        docker-compose -f $DockerComposeFile up -d postgres redis mock-service gateway
    }
    catch {
        Write-Error "Failed to start services via docker-compose."
        exit 1
    }
    
    Write-Output "Waiting 15s for services..."
    Start-Sleep -Seconds 15

    # 2. Build Test Image (avoids volume mount issues)
    Write-Output "Building test image..."
    docker build -t loadtest-jmeter-custom -f Dockerfile.jmeter .
    
    # 3. Clean environment
    docker rm -f jmeter-loadtest-run 2>$null
    if (Test-Path $ResultsDir) { Remove-Item -Path $ResultsDir -Recurse -Force }
    New-Item -ItemType Directory -Path $ResultsDir | Out-Null
    
    # 4. Run Test Container
    # We run it detached (-d) then wait (to avoid terminal issues), or run attached and let transcript capture
    # But transcript can interfere with docker run output sometimes.
    # We will run it and capture logs? No, rely on docker container state.
    
    Write-Output "Running JMeter..."
    $ExitCode = 0
    
    # Run container (no volumes used for simplicity, we copy results out later)
    # Network must be attached
    $Cmd = "docker run --name jmeter-loadtest-run " +
    "--network loadtest-network " +
    "loadtest-jmeter-custom " +
    "-n -t /tests/test-plan.jmx " +
    "-l /results/results.jtl " +
    "-e -o /results/report " +
    "-Jthreads=$Users -Jrampup=$RampUp -Jduration=$Duration " +
    "-JHOST=loadtest-gateway -JPORT=8080"
           
    Invoke-Expression $Cmd
    
    if ($LASTEXITCODE -ne 0) {
        Write-Error "JMeter exited with code $LASTEXITCODE"
        $ExitCode = $LASTEXITCODE
    }
    
    # 5. Extract Results
    Write-Output "Extracting results..."
    try {
        docker cp jmeter-loadtest-run:/results/report "$($ResultsDir)/report"
        docker cp jmeter-loadtest-run:/results/results.jtl "$($ResultsDir)/results.jtl"
    }
    catch {
        Write-Error "Failed to copy results: $_"
    }
    
    # 6. Cleanup container
    docker rm jmeter-loadtest-run | Out-Null
    
    # 7. Verify
    if (Test-Path "$ResultsDir\report\index.html") {
        Write-Output "SUCCESS: Report generated."
        Invoke-Item "$ResultsDir\report\index.html"
    }
    else {
        Write-Error "FAILURE: Report not found."
    }

}
catch {
    Write-Error $_
}
finally {
    Stop-Transcript
}
