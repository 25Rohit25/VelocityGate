
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$AbsTestDir = $ScriptDir -replace "\\", "/"
docker run --rm -v "$($AbsTestDir):/tests" --entrypoint ls justb4/jmeter:5.5 -l /tests
