@echo off
REM Run Load Test Visualization

SET RESULTS_FILE=%1
SET OUTPUT_DIR=%2

IF "%RESULTS_FILE%"=="" (
    SET RESULTS_FILE=results\results.jtl
)

IF "%OUTPUT_DIR%"=="" (
    SET OUTPUT_DIR=graphs
)

echo "Running Visualization Script..."
python visualize_metrics.py %RESULTS_FILE% --output %OUTPUT_DIR%

echo "Generated Graphs in %OUTPUT_DIR%/"
pause
