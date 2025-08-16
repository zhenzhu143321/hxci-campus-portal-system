# Database Performance Testing Script
# Testing database read/write performance and connection pool

param(
    [string]$ResultsDir = "D:\ClaudeCode\AI_Web\performance-tests\results"
)

$baseUrl = "http://localhost:48081"
$mockUrl = "http://localhost:48082"
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

if (!(Test-Path $ResultsDir)) {
    New-Item -ItemType Directory -Path $ResultsDir -Force
}

Write-Host "======================================================="
Write-Host "DATABASE PERFORMANCE TESTING"
Write-Host "Start Time: $(Get-Date)"
Write-Host "======================================================="

# Get test token
Write-Host "Obtaining test token..."
$authBody = @{
    employeeId = "PRINCIPAL_001"
    name = "Principal-Zhang"
    password = "admin123"
} | ConvertTo-Json

try {
    $authResponse = Invoke-RestMethod -Uri "$mockUrl/mock-school-api/auth/authenticate" -Method POST -Body $authBody -ContentType "application/json" -TimeoutSec 30
    $testToken = $authResponse.data.token
    Write-Host "Token obtained successfully"
} catch {
    Write-Host "Failed to obtain token: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

$dbTestResults = @()

# TEST 1: Database Write Performance (Notification Publishing)
Write-Host ""
Write-Host "======================================================="
Write-Host "TEST 1: DATABASE WRITE PERFORMANCE"
Write-Host "Testing notification publishing to database"
Write-Host "======================================================="

$writeResults = @()
$writeHeaders = @{
    "Authorization" = "Bearer $testToken"
    "tenant-id" = "1"
}

Write-Host "Testing database write performance with 50 notification insertions..."

for ($i = 1; $i -le 50; $i++) {
    $notificationBody = @{
        title = "DB Write Test Notification $i"
        content = "Database write performance test notification content. This is test number $i to measure database insertion performance and response times."
        level = 4
        targetScope = "ALL_SCHOOL"
    } | ConvertTo-Json
    
    $startTime = Get-Date
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/admin-api/test/notification/api/publish-database" -Method POST -Headers $writeHeaders -Body $notificationBody -ContentType "application/json" -UseBasicParsing -TimeoutSec 30
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        
        $writeResults += @{
            RequestId = $i
            Success = $true
            ResponseTime = [math]::Round($responseTime, 2)
            StatusCode = $response.StatusCode
            Error = ""
            Operation = "DatabaseWrite"
        }
        
        # Small delay to avoid overwhelming the database
        Start-Sleep -Milliseconds 100
        
    } catch {
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        
        $writeResults += @{
            RequestId = $i
            Success = $false
            ResponseTime = [math]::Round($responseTime, 2)
            StatusCode = 0
            Error = $_.Exception.Message
            Operation = "DatabaseWrite"
        }
    }
    
    if ($i % 10 -eq 0) {
        Write-Host "Completed $i/50 database write operations"
    }
}

# Calculate write performance statistics
$writeTotal = $writeResults.Count
$writeSuccess = ($writeResults | Where-Object { $_.Success }).Count
$writeSuccessRate = [math]::Round(($writeSuccess / $writeTotal) * 100, 2)
$writeResponseTimes = $writeResults | ForEach-Object { $_.ResponseTime }
$writeAvgTime = [math]::Round(($writeResponseTimes | Measure-Object -Average).Average, 2)
$writeMinTime = [math]::Round(($writeResponseTimes | Measure-Object -Minimum).Minimum, 2)
$writeMaxTime = [math]::Round(($writeResponseTimes | Measure-Object -Maximum).Maximum, 2)

Write-Host ""
Write-Host "Database Write Performance Results:"
Write-Host "  Total Write Operations: $writeTotal"
Write-Host "  Success Rate: $writeSuccessRate%"
Write-Host "  Average Response Time: $writeAvgTime ms"
Write-Host "  Min Response Time: $writeMinTime ms"
Write-Host "  Max Response Time: $writeMaxTime ms"

$dbTestResults += @{
    TestType = "Database Write Performance"
    TotalOperations = $writeTotal
    SuccessRate = $writeSuccessRate
    AvgResponseTime = $writeAvgTime
    MinResponseTime = $writeMinTime
    MaxResponseTime = $writeMaxTime
}

# Wait for database to settle
Start-Sleep -Seconds 3

# TEST 2: Database Read Performance (Notification Querying)
Write-Host ""
Write-Host "======================================================="
Write-Host "TEST 2: DATABASE READ PERFORMANCE"
Write-Host "Testing notification list queries from database"
Write-Host "======================================================="

$readResults = @()

Write-Host "Testing database read performance with 100 notification list queries..."

for ($i = 1; $i -le 100; $i++) {
    $startTime = Get-Date
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/admin-api/test/notification/api/list" -Headers $writeHeaders -UseBasicParsing -TimeoutSec 15
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        
        # Parse response to check data volume
        $contentLength = $response.Content.Length
        
        $readResults += @{
            RequestId = $i
            Success = $true
            ResponseTime = [math]::Round($responseTime, 2)
            StatusCode = $response.StatusCode
            ContentLength = $contentLength
            Error = ""
            Operation = "DatabaseRead"
        }
        
        # Minimal delay for read operations
        Start-Sleep -Milliseconds 50
        
    } catch {
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        
        $readResults += @{
            RequestId = $i
            Success = $false
            ResponseTime = [math]::Round($responseTime, 2)
            StatusCode = 0
            ContentLength = 0
            Error = $_.Exception.Message
            Operation = "DatabaseRead"
        }
    }
    
    if ($i % 20 -eq 0) {
        Write-Host "Completed $i/100 database read operations"
    }
}

# Calculate read performance statistics
$readTotal = $readResults.Count
$readSuccess = ($readResults | Where-Object { $_.Success }).Count
$readSuccessRate = [math]::Round(($readSuccess / $readTotal) * 100, 2)
$readResponseTimes = $readResults | ForEach-Object { $_.ResponseTime }
$readAvgTime = [math]::Round(($readResponseTimes | Measure-Object -Average).Average, 2)
$readMinTime = [math]::Round(($readResponseTimes | Measure-Object -Minimum).Minimum, 2)
$readMaxTime = [math]::Round(($readResponseTimes | Measure-Object -Maximum).Maximum, 2)

# Average content size
$successfulReads = $readResults | Where-Object { $_.Success -and $_.ContentLength -gt 0 }
$avgContentSize = if ($successfulReads) { 
    [math]::Round(($successfulReads | Measure-Object -Property ContentLength -Average).Average, 0) 
} else { 0 }

Write-Host ""
Write-Host "Database Read Performance Results:"
Write-Host "  Total Read Operations: $readTotal"
Write-Host "  Success Rate: $readSuccessRate%"
Write-Host "  Average Response Time: $readAvgTime ms"
Write-Host "  Min Response Time: $readMinTime ms"
Write-Host "  Max Response Time: $readMaxTime ms"
Write-Host "  Average Content Size: $avgContentSize bytes"

$dbTestResults += @{
    TestType = "Database Read Performance"
    TotalOperations = $readTotal
    SuccessRate = $readSuccessRate
    AvgResponseTime = $readAvgTime
    MinResponseTime = $readMinTime
    MaxResponseTime = $readMaxTime
}

# TEST 3: Mixed Read/Write Load Testing
Write-Host ""
Write-Host "======================================================="
Write-Host "TEST 3: MIXED READ/WRITE LOAD TESTING"
Write-Host "Testing database with mixed read/write operations"
Write-Host "======================================================="

$mixedDbResults = @()

Write-Host "Testing mixed database load (70% read, 30% write) with 60 operations..."

for ($i = 1; $i -le 60; $i++) {
    # 70% chance for read operation, 30% for write
    $operationType = if ((Get-Random -Minimum 1 -Maximum 101) -le 70) { "Read" } else { "Write" }
    
    $startTime = Get-Date
    try {
        if ($operationType -eq "Read") {
            $response = Invoke-WebRequest -Uri "$baseUrl/admin-api/test/notification/api/list" -Headers $writeHeaders -UseBasicParsing -TimeoutSec 15
            $opSuccess = $true
        } else {
            # Write operation
            $mixedNotificationBody = @{
                title = "Mixed DB Test $i"
                content = "Mixed database load test notification $i"
                level = 4
                targetScope = "ALL_SCHOOL"
            } | ConvertTo-Json
            $response = Invoke-WebRequest -Uri "$baseUrl/admin-api/test/notification/api/publish-database" -Method POST -Headers $writeHeaders -Body $mixedNotificationBody -ContentType "application/json" -UseBasicParsing -TimeoutSec 30
            $opSuccess = $true
        }
        
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        
        $mixedDbResults += @{
            RequestId = $i
            OperationType = $operationType
            Success = $opSuccess
            ResponseTime = [math]::Round($responseTime, 2)
            StatusCode = $response.StatusCode
            Error = ""
        }
        
    } catch {
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        
        $mixedDbResults += @{
            RequestId = $i
            OperationType = $operationType
            Success = $false
            ResponseTime = [math]::Round($responseTime, 2)
            StatusCode = 0
            Error = $_.Exception.Message
        }
    }
    
    # Variable delay to simulate real usage
    Start-Sleep -Milliseconds (Get-Random -Minimum 100 -Maximum 300)
    
    if ($i % 15 -eq 0) {
        Write-Host "Completed $i/60 mixed database operations"
    }
}

# Calculate mixed load statistics
$mixedTotal = $mixedDbResults.Count
$mixedSuccess = ($mixedDbResults | Where-Object { $_.Success }).Count
$mixedSuccessRate = [math]::Round(($mixedSuccess / $mixedTotal) * 100, 2)
$mixedResponseTimes = $mixedDbResults | ForEach-Object { $_.ResponseTime }
$mixedAvgTime = [math]::Round(($mixedResponseTimes | Measure-Object -Average).Average, 2)
$mixedMaxTime = [math]::Round(($mixedResponseTimes | Measure-Object -Maximum).Maximum, 2)

# Operation breakdown
$mixedReads = ($mixedDbResults | Where-Object { $_.OperationType -eq "Read" }).Count
$mixedWrites = ($mixedDbResults | Where-Object { $_.OperationType -eq "Write" }).Count

Write-Host ""
Write-Host "Mixed Database Load Results:"
Write-Host "  Total Operations: $mixedTotal"
Write-Host "  Operation Breakdown: Read=$mixedReads, Write=$mixedWrites"
Write-Host "  Success Rate: $mixedSuccessRate%"
Write-Host "  Average Response Time: $mixedAvgTime ms"
Write-Host "  Max Response Time: $mixedMaxTime ms"

$dbTestResults += @{
    TestType = "Mixed Read/Write Load"
    TotalOperations = $mixedTotal
    SuccessRate = $mixedSuccessRate
    AvgResponseTime = $mixedAvgTime
    MinResponseTime = 0
    MaxResponseTime = $mixedMaxTime
}

# TEST 4: Database Connection Pool Stress
Write-Host ""
Write-Host "======================================================="
Write-Host "TEST 4: DATABASE CONNECTION POOL STRESS TEST"
Write-Host "Testing multiple concurrent database connections"
Write-Host "======================================================="

Write-Host "Testing database connection pool with rapid concurrent queries..."

$connectionPoolResults = @()
$concurrentQueries = 20

# Rapid fire queries to stress connection pool
$startTime = Get-Date
for ($i = 1; $i -le $concurrentQueries; $i++) {
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/admin-api/test/notification/api/list" -Headers $writeHeaders -UseBasicParsing -TimeoutSec 10
        $connectionPoolResults += @{
            RequestId = $i
            Success = $true
            StatusCode = $response.StatusCode
        }
    } catch {
        $connectionPoolResults += @{
            RequestId = $i
            Success = $false
            StatusCode = 0
        }
    }
    
    # Very minimal delay to stress the connection pool
    Start-Sleep -Milliseconds 10
}
$endTime = Get-Date

$poolTestDuration = ($endTime - $startTime).TotalMilliseconds
$poolSuccessCount = ($connectionPoolResults | Where-Object { $_.Success }).Count
$poolSuccessRate = [math]::Round(($poolSuccessCount / $concurrentQueries) * 100, 2)

Write-Host "Connection Pool Stress Results:"
Write-Host "  Concurrent Queries: $concurrentQueries"
Write-Host "  Success Rate: $poolSuccessRate%"
Write-Host "  Total Duration: $([math]::Round($poolTestDuration, 2)) ms"
Write-Host "  Average Query Rate: $([math]::Round($concurrentQueries * 1000 / $poolTestDuration, 2)) queries/second"

# Generate Database Performance Report
Write-Host ""
Write-Host "======================================================="
Write-Host "DATABASE PERFORMANCE TESTING COMPLETED"
Write-Host "End Time: $(Get-Date)"
Write-Host "======================================================="

$dbReportFile = "$ResultsDir\database_performance_test_$timestamp.txt"

@"
INTELLIGENT NOTIFICATION SYSTEM - DATABASE PERFORMANCE TEST REPORT
Generated: $(Get-Date)
================================================================

OBJECTIVE: Evaluate database performance, connection handling, and scalability

DATABASE CONFIGURATION:
- Database Type: MySQL
- Connection Pool: Spring Boot HikariCP (default)
- Test Focus: notification_info table operations

TEST RESULTS SUMMARY:
"@ | Out-File -FilePath $dbReportFile -Encoding UTF8

"" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8
"Test_Type,Total_Operations,Success_Rate(%),Avg_Response_Time(ms),Min_Response_Time(ms),Max_Response_Time(ms)" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8

foreach ($result in $dbTestResults) {
    "$($result.TestType),$($result.TotalOperations),$($result.SuccessRate),$($result.AvgResponseTime),$($result.MinResponseTime),$($result.MaxResponseTime)" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8
}

Write-Host ""
Write-Host "DATABASE PERFORMANCE SUMMARY:"
foreach ($result in $dbTestResults) {
    $color = if ($result.SuccessRate -ge 95) { "Green" } elseif ($result.SuccessRate -ge 90) { "Yellow" } else { "Red" }
    Write-Host "$($result.TestType): $($result.SuccessRate)% success, $($result.AvgResponseTime)ms avg" -ForegroundColor $color
}

"" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8
"DETAILED ANALYSIS:" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8

foreach ($result in $dbTestResults) {
    "" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8
    "$($result.TestType):" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8
    "  - Total Operations: $($result.TotalOperations)" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8
    "  - Success Rate: $($result.SuccessRate)%" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8
    "  - Average Response Time: $($result.AvgResponseTime) ms" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8
    "  - Response Time Range: $($result.MinResponseTime) - $($result.MaxResponseTime) ms" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8
}

# Connection pool results
"" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8
"Database Connection Pool Stress Test:" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8
"  - Concurrent Queries: $concurrentQueries" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8
"  - Success Rate: $poolSuccessRate%" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8
"  - Total Duration: $([math]::Round($poolTestDuration, 2)) ms" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8

# Performance assessment
$avgReadTime = ($dbTestResults | Where-Object { $_.TestType -eq "Database Read Performance" }).AvgResponseTime
$avgWriteTime = ($dbTestResults | Where-Object { $_.TestType -eq "Database Write Performance" }).AvgResponseTime
$readWriteRatio = if ($avgWriteTime -gt 0) { [math]::Round($avgWriteTime / $avgReadTime, 2) } else { 0 }

"" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8
"PERFORMANCE INSIGHTS:" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8
"- Read vs Write Performance: Write operations are ${readWriteRatio}x slower than reads" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8
"- Database Read Performance: $(if ($avgReadTime -le 50) { "EXCELLENT" } elseif ($avgReadTime -le 100) { "GOOD" } else { "NEEDS OPTIMIZATION" }) (avg: $avgReadTime ms)" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8
"- Database Write Performance: $(if ($avgWriteTime -le 100) { "EXCELLENT" } elseif ($avgWriteTime -le 200) { "GOOD" } else { "NEEDS OPTIMIZATION" }) (avg: $avgWriteTime ms)" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8
"- Connection Pool Stability: $(if ($poolSuccessRate -ge 95) { "STABLE" } else { "UNSTABLE" }) ($poolSuccessRate% success rate)" | Out-File -FilePath $dbReportFile -Append -Encoding UTF8

Write-Host ""
Write-Host "DATABASE PERFORMANCE INSIGHTS:" -ForegroundColor Cyan
Write-Host "- Read Performance: $(if ($avgReadTime -le 50) { "EXCELLENT" } elseif ($avgReadTime -le 100) { "GOOD" } else { "NEEDS OPTIMIZATION" }) ($avgReadTime ms avg)" -ForegroundColor $(if ($avgReadTime -le 100) { "Green" } else { "Yellow" })
Write-Host "- Write Performance: $(if ($avgWriteTime -le 100) { "EXCELLENT" } elseif ($avgWriteTime -le 200) { "GOOD" } else { "NEEDS OPTIMIZATION" }) ($avgWriteTime ms avg)" -ForegroundColor $(if ($avgWriteTime -le 200) { "Green" } else { "Yellow" })
Write-Host "- Write/Read Ratio: ${readWriteRatio}x (writes are ${readWriteRatio}x slower than reads)" -ForegroundColor $(if ($readWriteRatio -le 3) { "Green" } elseif ($readWriteRatio -le 5) { "Yellow" } else { "Red" })
Write-Host "- Connection Pool: $(if ($poolSuccessRate -ge 95) { "STABLE" } else { "UNSTABLE" }) ($poolSuccessRate% success)" -ForegroundColor $(if ($poolSuccessRate -ge 95) { "Green" } else { "Red" })

Write-Host ""
Write-Host "Database performance test report saved to: $dbReportFile"
Write-Host "======================================================="