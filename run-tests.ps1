$ErrorActionPreference = "Stop"

function Summarize-JUnitReports($folder, $title) {
    $files = Get-ChildItem -Path $folder -Filter "*.xml" -ErrorAction SilentlyContinue
    if (-not $files) {
        Write-Host "[$title] Report XML bulunamadı: $folder"
        return
    }

    $totalTests = 0
    $totalFailures = 0
    $totalErrors = 0
    $totalSkipped = 0

    foreach ($f in $files) {
        [xml]$xml = Get-Content $f.FullName
        $suite = $xml.testsuite
        if ($suite -ne $null) {
            $totalTests += [int]$suite.tests
            $totalFailures += [int]$suite.failures
            $totalErrors += [int]$suite.errors
            $totalSkipped += [int]$suite.skipped
        }
    }

    $passed = $totalTests - ($totalFailures + $totalErrors + $totalSkipped)

    Write-Host ""
    Write-Host "==================== $title ===================="
    Write-Host "Tests   : $totalTests"
    Write-Host "Passed  : $passed"
    Write-Host "Failures: $totalFailures"
    Write-Host "Errors  : $totalErrors"
    Write-Host "Skipped : $totalSkipped"
    Write-Host "Reports : $folder"
    Write-Host "================================================"
}

Write-Host "1) UNIT TESTS çalıştırılıyor..."
mvn -DskipITs=true -DskipUnitTests=false test
Summarize-JUnitReports "target\surefire-reports" "UNIT TEST REPORT"

Write-Host ""
Write-Host "2) INTEGRATION TESTS çalıştırılıyor..."
mvn -DskipUnitTests=true -DskipITs=false verify
Summarize-JUnitReports "target\failsafe-reports" "INTEGRATION TEST REPORT"

Write-Host ""
Write-Host "Bitti ✅"
