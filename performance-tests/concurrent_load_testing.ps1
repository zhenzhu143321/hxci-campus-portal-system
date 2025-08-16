# Concurrent Load Testing Script
# Progressive load testing from 5 to 500 concurrent users

param(
    [string]$ResultsDir = "D:\ClaudeCode\AI_Web\performance-tests\results"
)

$baseUrl = "http://localhost:48081"
$mockUrl = "http://localhost:48082"
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

# Ensure results directory exists
if (!(Test-Path $ResultsDir)) {
    New-Item -ItemType Directory -Path $ResultsDir -Force
}

Write-Host "=======================================================
CONCURRENT LOAD TESTING SUITE
Start Time: $(Get-Date)
======================================================="

# Get test token once
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

# Test configurations: [Users, Duration, Test Description]
$testConfigs = @(
    @{Users=5; Duration=30; Description="Light Load"},
    @{Users=10; Duration=30; Description="Medium Load"},
    @{Users=20; Duration=30; Description="Heavy Load"},
    @{Users=50; Duration=30; Description="High Load"},
    @{Users=100; Duration=30; Description="Stress Load"},
    @{Users=200; Duration=30; Description="Extreme Load"}
)

$allResults = @()

foreach ($config in $testConfigs) {
    $users = $config.Users
    $duration = $config.Duration
    $description = $config.Description
    
    Write-Host ""
    Write-Host "======================================================="
    Write-Host "Testing: $description ($users concurrent users, $duration seconds)"
    Write-Host "Start Time: $(Get-Date)"
    Write-Host "======================================================="
    
    $testStartTime = Get-Date
    
    # Start monitoring in background
    $monitorJob = Start-Job -ScriptBlock {
        param($Duration, $ResultsDir, $Users, $Timestamp)
        
        $monitorFile = "$ResultsDir\monitor_${Users}users_$Timestamp.csv"
        "Time,CPU_Usage,Memory_MB,Java_Processes" | Out-File -FilePath $monitorFile -Encoding UTF8
        
        $endTime = (Get-Date).AddSeconds($Duration + 10) # Extra time for monitoring
        
        while ((Get-Date) -lt $endTime) {
            try {
                $cpu = (Get-WmiObject -Class Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average
                $memory = Get-WmiObject -Class Win32_OperatingSystem
                $memUsed = [math]::Round(($memory.TotalVisibleMemorySize - $memory.AvailablePhysicalMemory) / 1024, 2)
                $javaProcs = (Get-Process -Name "java" -ErrorAction SilentlyContinue).Count
                
                $timeStr = Get-Date -Format "HH:mm:ss"
                "$timeStr,$cpu,$memUsed,$javaProcs" | Out-File -FilePath $monitorFile -Append -Encoding UTF8
                
            } catch {
                $timeStr = Get-Date -Format "HH:mm:ss"
                "$timeStr,N/A,N/A,N/A" | Out-File -FilePath $monitorFile -Append -Encoding UTF8
            }
            
            Start-Sleep -Seconds 2
        }
    } -ArgumentList $duration, $ResultsDir, $users, $timestamp
    
    # Create result collectors
    $requests = [System.Collections.Concurrent.ConcurrentBag[object]]::new()
    
    # Start concurrent user jobs
    $userJobs = @()
    for ($i = 1; $i -le $users; $i++) {
        $userJob = Start-Job -ScriptBlock {
            param($UserId, $Duration, $BaseUrl, $MockUrl, $TestToken)
            
            $userRequests = @()
            $endTime = (Get-Date).AddSeconds($Duration)
            $requestCount = 0
            
            while ((Get-Date) -lt $endTime) {
                $requestCount++
                $requestId = "User${UserId}_Req${requestCount}"
                
                # Randomly select API to test (mixed load)
                $apiChoice = Get-Random -Minimum 1 -Maximum 4
                
                $startTime = Get-Date
                try {
                    switch ($apiChoice) {
                        1 {
                            # Health Check
                            $response = Invoke-WebRequest -Uri "$BaseUrl/admin-api/test/notification/api/ping" -UseBasicParsing -TimeoutSec 30
                            $apiType = "HealthCheck"
                        }
                        2 {
                            # Authentication
                            $authBody = @{
                                employeeId = "TEACHER_001"
                                name = "Teacher-Wang"
                                password = "admin123"
                            } | ConvertTo-Json
                            $response = Invoke-WebRequest -Uri "$MockUrl/mock-school-api/auth/authenticate" -Method POST -Body $authBody -ContentType "application/json" -UseBasicParsing -TimeoutSec 30
                            $apiType = "Authentication"
                        }
                        3 {
                            # Notification List
                            $headers = @{
                                "Authorization" = "Bearer $TestToken"
                                "tenant-id" = "1"
                            }
                            $response = Invoke-WebRequest -Uri "$BaseUrl/admin-api/test/notification/api/list" -Headers $headers -UseBasicParsing -TimeoutSec 30
                            $apiType = "NotificationList"
                        }
                    }
                    
                    $endTime = Get-Date
                    $responseTime = ($endTime - $startTime).TotalMilliseconds
                    
                    $userRequests += @{
                        RequestId = $requestId
                        UserId = $UserId
                        ApiType = $apiType
                        StartTime = $startTime
                        EndTime = $endTime
                        ResponseTime = [math]::Round($responseTime, 2)
                        StatusCode = $response.StatusCode
                        Success = $true
                        Error = ""
                    }
                    
                } catch {
                    $endTime = Get-Date
                    $responseTime = ($endTime - $startTime).TotalMilliseconds
                    
                    $userRequests += @{
                        RequestId = $requestId
                        UserId = $UserId
                        ApiType = if ($apiChoice -eq 1) { "HealthCheck" } elseif ($apiChoice -eq 2) { "Authentication" } else { "NotificationList" }
                        StartTime = $startTime
                        EndTime = $endTime
                        ResponseTime = [math]::Round($responseTime, 2)
                        StatusCode = 0
                        Success = $false
                        Error = $_.Exception.Message
                    }
                }
                
                # Random delay between requests (100-1000ms)
                Start-Sleep -Milliseconds (Get-Random -Minimum 100 -Maximum 1000)
            }
            
            return $userRequests
        } -ArgumentList $i, $duration, $baseUrl, $mockUrl, $testToken
        
        $userJobs += $userJob
    }
    
    # Wait for all user jobs to complete
    Write-Host "Waiting for $users concurrent users to complete..."
    $userJobs | Wait-Job | Out-Null
    
    # Collect results from all user jobs
    $testResults = @()
    foreach ($job in $userJobs) {
        $jobResults = Receive-Job -Job $job
        $testResults += $jobResults
        Remove-Job -Job $job
    }
    
    # Stop monitoring
    $monitorJob | Stop-Job
    $monitorJob | Wait-Job
    Remove-Job -Job $monitorJob
    
    $testEndTime = Get-Date
    $actualDuration = ($testEndTime - $testStartTime).TotalSeconds
    
    # Calculate statistics
    $totalRequests = $testResults.Count
    $successfulRequests = ($testResults | Where-Object { $_.Success }).Count
    $failedRequests = $totalRequests - $successfulRequests
    $successRate = if ($totalRequests -gt 0) { [math]::Round(($successfulRequests / $totalRequests) * 100, 2) } else { 0 }
    
    if ($testResults.Count -gt 0) {
        $responseTimes = $testResults | ForEach-Object { $_.ResponseTime }
        $avgResponseTime = [math]::Round(($responseTimes | Measure-Object -Average).Average, 2)
        $minResponseTime = [math]::Round(($responseTimes | Measure-Object -Minimum).Minimum, 2)
        $maxResponseTime = [math]::Round(($responseTimes | Measure-Object -Maximum).Maximum, 2)
        
        # Calculate percentiles
        $sortedTimes = $responseTimes | Sort-Object
        $count = $sortedTimes.Count
        $p95Index = [math]::Floor($count * 0.95) - 1
        $p99Index = [math]::Floor($count * 0.99) - 1
        $p95ResponseTime = if ($p95Index -ge 0 -and $p95Index -lt $count) { [math]::Round($sortedTimes[$p95Index], 2) } else { $maxResponseTime }
        $p99ResponseTime = if ($p99Index -ge 0 -and $p99Index -lt $count) { [math]::Round($sortedTimes[$p99Index], 2) } else { $maxResponseTime }
        
        $rps = [math]::Round($totalRequests / $actualDuration, 2)
    } else {
        $avgResponseTime = 0; $minResponseTime = 0; $maxResponseTime = 0
        $p95ResponseTime = 0; $p99ResponseTime = 0; $rps = 0
    }
    
    # Display results
    Write-Host ""
    Write-Host "Results for $description ($users users):"
    Write-Host "  Duration: $([math]::Round($actualDuration, 2)) seconds"
    Write-Host "  Total Requests: $totalRequests"
    Write-Host "  Successful: $successfulRequests"
    Write-Host "  Failed: $failedRequests"
    Write-Host "  Success Rate: $successRate%"
    Write-Host "  Requests/Second: $rps"
    Write-Host "  Avg Response Time: $avgResponseTime ms"
    Write-Host "  Min Response Time: $minResponseTime ms"
    Write-Host "  Max Response Time: $maxResponseTime ms"
    Write-Host "  95th Percentile: $p95ResponseTime ms"
    Write-Host "  99th Percentile: $p99ResponseTime ms"
    
    # Save detailed results to CSV
    $resultFile = "$ResultsDir\concurrent_${users}users_${timestamp}.csv"
    "RequestId,UserId,ApiType,StartTime,ResponseTime_ms,StatusCode,Success,Error" | Out-File -FilePath $resultFile -Encoding UTF8
    
    foreach ($result in $testResults) {
        "$($result.RequestId),$($result.UserId),$($result.ApiType),$($result.StartTime.ToString('yyyy-MM-dd HH:mm:ss.fff')),$($result.ResponseTime),$($result.StatusCode),$($result.Success),$($result.Error)" | Out-File -FilePath $resultFile -Append -Encoding UTF8
    }
    
    # Store summary for final report
    $summary = @{
        Users = $users
        Description = $description
        Duration = $actualDuration
        TotalRequests = $totalRequests
        SuccessfulRequests = $successfulRequests
        FailedRequests = $failedRequests
        SuccessRate = $successRate
        RPS = $rps
        AvgResponseTime = $avgResponseTime
        MinResponseTime = $minResponseTime
        MaxResponseTime = $maxResponseTime
        P95ResponseTime = $p95ResponseTime
        P99ResponseTime = $p99ResponseTime
        ResultFile = $resultFile
    }
    
    $allResults += $summary
    
    Write-Host "Detailed results saved to: $resultFile"
    
    # Wait between tests to allow system recovery
    if ($users -lt 200) {
        Write-Host "Waiting 10 seconds for system recovery..."
        Start-Sleep -Seconds 10
    }
}

# Generate final summary report
Write-Host ""
Write-Host "======================================================="
Write-Host "CONCURRENT LOAD TESTING COMPLETE"
Write-Host "End Time: $(Get-Date)"
Write-Host "======================================================="

$summaryReportFile = "$ResultsDir\concurrent_load_test_summary_$timestamp.txt"

$report = @"
INTELLIGENT NOTIFICATION SYSTEM - CONCURRENT LOAD TEST REPORT
Generated: $(Get-Date)
=============================================================

TEST ENVIRONMENT:
- Main Service: http://localhost:48081
- Mock API Service: http://localhost:48082  
- Test Types: Health Check, Authentication, Notification List (Mixed Load)
- Operating System: Windows

SUMMARY RESULTS:
"@

$report | Out-File -FilePath $summaryReportFile -Encoding UTF8

"" | Out-File -FilePath $summaryReportFile -Append -Encoding UTF8
"Users,Description,Duration(s),Total_Requests,Success_Rate(%),RPS,Avg_Response(ms),95th_Percentile(ms),99th_Percentile(ms)" | Out-File -FilePath $summaryReportFile -Append -Encoding UTF8

foreach ($result in $allResults) {
    Write-Host "$($result.Users) users ($($result.Description)): $($result.SuccessRate)% success, $($result.RPS) RPS, $($result.AvgResponseTime)ms avg"
    
    "$($result.Users),$($result.Description),$($result.Duration),$($result.TotalRequests),$($result.SuccessRate),$($result.RPS),$($result.AvgResponseTime),$($result.P95ResponseTime),$($result.P99ResponseTime)" | Out-File -FilePath $summaryReportFile -Append -Encoding UTF8
}

"" | Out-File -FilePath $summaryReportFile -Append -Encoding UTF8
"DETAILED ANALYSIS:" | Out-File -FilePath $summaryReportFile -Append -Encoding UTF8

foreach ($result in $allResults) {
    "" | Out-File -FilePath $summaryReportFile -Append -Encoding UTF8
    "$($result.Users) Concurrent Users ($($result.Description)):" | Out-File -FilePath $summaryReportFile -Append -Encoding UTF8
    "  - Duration: $($result.Duration) seconds" | Out-File -FilePath $summaryReportFile -Append -Encoding UTF8
    "  - Total Requests: $($result.TotalRequests)" | Out-File -FilePath $summaryReportFile -Append -Encoding UTF8
    "  - Success Rate: $($result.SuccessRate)%" | Out-File -FilePath $summaryReportFile -Append -Encoding UTF8
    "  - Requests per Second: $($result.RPS)" | Out-File -FilePath $summaryReportFile -Append -Encoding UTF8
    "  - Average Response Time: $($result.AvgResponseTime) ms" | Out-File -FilePath $summaryReportFile -Append -Encoding UTF8
    "  - Response Time Range: $($result.MinResponseTime) - $($result.MaxResponseTime) ms" | Out-File -FilePath $summaryReportFile -Append -Encoding UTF8
    "  - 95th Percentile: $($result.P95ResponseTime) ms" | Out-File -FilePath $summaryReportFile -Append -Encoding UTF8
    "  - 99th Percentile: $($result.P99ResponseTime) ms" | Out-File -FilePath $summaryReportFile -Append -Encoding UTF8
    "  - Detailed Results: $($result.ResultFile)" | Out-File -FilePath $summaryReportFile -Append -Encoding UTF8
}

Write-Host ""
Write-Host "Final summary report saved to: $summaryReportFile"
Write-Host "======================================================="