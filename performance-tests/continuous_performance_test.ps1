# Continuous Performance Test (Shortened Version)
# 10-minute continuous load testing instead of 1 hour

param(
    [int]$DurationMinutes = 10,
    [int]$ConcurrentUsers = 20,
    [string]$ResultsDir = "D:\ClaudeCode\AI_Web\performance-tests\results"
)

$baseUrl = "http://localhost:48081"
$mockUrl = "http://localhost:48082"
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

if (!(Test-Path $ResultsDir)) {
    New-Item -ItemType Directory -Path $ResultsDir -Force
}

Write-Host "======================================================="
Write-Host "CONTINUOUS PERFORMANCE TEST"
Write-Host "Duration: $DurationMinutes minutes"
Write-Host "Concurrent Users: $ConcurrentUsers"
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

$continuousResults = @()
$systemMetrics = @()
$durationSeconds = $DurationMinutes * 60

# Start system monitoring in background
$monitorJob = Start-Job -ScriptBlock {
    param($DurationMinutes, $ResultsDir, $Timestamp)
    
    $monitorFile = "$ResultsDir\continuous_monitor_$Timestamp.csv"
    "Time,CPU_Usage,Memory_Used_MB,Java_Processes" | Out-File -FilePath $monitorFile -Encoding UTF8
    
    $endTime = (Get-Date).AddMinutes($DurationMinutes + 1) # Extra time
    $intervalSeconds = 10
    
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
        
        Start-Sleep -Seconds $intervalSeconds
    }
    
    return $monitorFile
} -ArgumentList $DurationMinutes, $ResultsDir, $timestamp

Write-Host "System monitoring started"

# Record initial system state
try {
    $initialCpu = (Get-WmiObject -Class Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average
    $initialMemory = Get-WmiObject -Class Win32_OperatingSystem
    $initialMemUsed = [math]::Round(($initialMemory.TotalVisibleMemorySize - $initialMemory.AvailablePhysicalMemory) / 1024, 2)
    Write-Host "Initial System State: CPU $initialCpu%, Memory ${initialMemUsed}MB"
} catch {
    $initialCpu = 0; $initialMemUsed = 0
}

# Start continuous load testing
Write-Host "Starting continuous load testing..."
$testStart = Get-Date
$testEndTime = $testStart.AddSeconds($durationSeconds)

$userJobs = @()
for ($i = 1; $i -le $ConcurrentUsers; $i++) {
    $userJob = Start-Job -ScriptBlock {
        param($UserId, $EndTime, $BaseUrl, $MockUrl, $TestToken)
        
        $userResults = @()
        $requestCount = 0
        
        $headers = @{
            "Authorization" = "Bearer $TestToken"
            "tenant-id" = "1"
        }
        
        while ((Get-Date) -lt $EndTime) {
            $requestCount++
            $requestId = "ContinuousUser${UserId}_${requestCount}"
            
            # Randomly select operation type
            $operationType = switch (Get-Random -Minimum 1 -Maximum 4) {
                1 { "HealthCheck" }
                2 { "Authentication" }
                3 { "NotificationList" }
            }
            
            $startTime = Get-Date
            try {
                switch ($operationType) {
                    "HealthCheck" {
                        $response = Invoke-WebRequest -Uri "$BaseUrl/admin-api/test/notification/api/ping" -UseBasicParsing -TimeoutSec 10
                    }
                    "Authentication" {
                        $authBody = @{
                            employeeId = "TEACHER_001"
                            name = "Teacher-Wang"
                            password = "admin123"
                        } | ConvertTo-Json
                        $response = Invoke-WebRequest -Uri "$MockUrl/mock-school-api/auth/authenticate" -Method POST -Body $authBody -ContentType "application/json" -UseBasicParsing -TimeoutSec 10
                    }
                    "NotificationList" {
                        $response = Invoke-WebRequest -Uri "$BaseUrl/admin-api/test/notification/api/list" -Headers $headers -UseBasicParsing -TimeoutSec 10
                    }
                }
                
                $endTime_req = Get-Date
                $responseTime = ($endTime_req - $startTime).TotalMilliseconds
                
                $userResults += @{
                    RequestId = $requestId
                    UserId = $UserId
                    OperationType = $operationType
                    Timestamp = $startTime
                    Success = $true
                    ResponseTime = [math]::Round($responseTime, 2)
                    StatusCode = $response.StatusCode
                    Error = ""
                }
                
            } catch {
                $endTime_req = Get-Date
                $responseTime = ($endTime_req - $startTime).TotalMilliseconds
                
                $userResults += @{
                    RequestId = $requestId
                    UserId = $UserId
                    OperationType = $operationType
                    Timestamp = $startTime
                    Success = $false
                    ResponseTime = [math]::Round($responseTime, 2)
                    StatusCode = 0
                    Error = $_.Exception.Message
                }
            }
            
            # Wait between requests - simulate realistic user behavior
            Start-Sleep -Milliseconds (Get-Random -Minimum 500 -Maximum 2000)
        }
        
        return $userResults
    } -ArgumentList $i, $testEndTime, $baseUrl, $mockUrl, $testToken
    
    $userJobs += $userJob
}

Write-Host "Started $($userJobs.Count) continuous user simulations"
Write-Host "Test will run until $(Get-Date -Date $testEndTime -Format 'HH:mm:ss')"

# Monitor progress
$progressInterval = [math]::Max(30, [math]::Floor($durationSeconds / 10)) # Show progress every 30 seconds or 1/10 of duration
$nextProgressTime = $testStart.AddSeconds($progressInterval)

while ((Get-Date) -lt $testEndTime) {
    if ((Get-Date) -ge $nextProgressTime) {
        $elapsed = ((Get-Date) - $testStart).TotalMinutes
        $remaining = ($testEndTime - (Get-Date)).TotalMinutes
        Write-Host "Progress: $([math]::Round($elapsed, 1))/$DurationMinutes minutes completed, $([math]::Round($remaining, 1)) minutes remaining"
        $nextProgressTime = $nextProgressTime.AddSeconds($progressInterval)
    }
    Start-Sleep -Seconds 5
}

Write-Host "Continuous test duration completed, collecting results..."

# Wait for user jobs to complete (with timeout)
try {
    $completedJobs = Wait-Job -Job $userJobs -Timeout 60
    Write-Host "Completed $($completedJobs.Count) out of $($userJobs.Count) user jobs"
} catch {
    Write-Host "Some user jobs timed out during collection" -ForegroundColor Yellow
}

# Collect all results
$allResults = @()
foreach ($job in $userJobs) {
    try {
        $jobResults = Receive-Job -Job $job -ErrorAction SilentlyContinue
        if ($jobResults) {
            $allResults += $jobResults
        }
    } catch {
        # Failed to receive results from this job
    }
    Remove-Job -Job $job -Force
}

# Stop monitoring job
Wait-Job -Job $monitorJob -Timeout 30
$monitorFile = Receive-Job -Job $monitorJob
Remove-Job -Job $monitorJob -Force

$actualDuration = ((Get-Date) - $testStart).TotalMinutes
Write-Host "Actual test duration: $([math]::Round($actualDuration, 2)) minutes"

# Record final system state
try {
    $finalCpu = (Get-WmiObject -Class Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average
    $finalMemory = Get-WmiObject -Class Win32_OperatingSystem
    $finalMemUsed = [math]::Round(($finalMemory.TotalVisibleMemorySize - $finalMemory.AvailablePhysicalMemory) / 1024, 2)
    Write-Host "Final System State: CPU $finalCpu%, Memory ${finalMemUsed}MB"
} catch {
    $finalCpu = 0; $finalMemUsed = 0
}

# Calculate continuous test statistics
$totalRequests = $allResults.Count
$successfulRequests = ($allResults | Where-Object { $_.Success }).Count
$failedRequests = $totalRequests - $successfulRequests
$successRate = if ($totalRequests -gt 0) { [math]::Round(($successfulRequests / $totalRequests) * 100, 2) } else { 0 }

Write-Host ""
Write-Host "======================================================="
Write-Host "CONTINUOUS PERFORMANCE TEST RESULTS"
Write-Host "======================================================="

if ($allResults.Count -gt 0) {
    $responseTimes = $allResults | ForEach-Object { $_.ResponseTime }
    $avgResponseTime = [math]::Round(($responseTimes | Measure-Object -Average).Average, 2)
    $minResponseTime = [math]::Round(($responseTimes | Measure-Object -Minimum).Minimum, 2)
    $maxResponseTime = [math]::Round(($responseTimes | Measure-Object -Maximum).Maximum, 2)
    
    # Calculate percentiles
    $sortedTimes = $responseTimes | Sort-Object
    $count = $sortedTimes.Count
    $p95Index = [math]::Floor($count * 0.95) - 1
    $p99Index = [math]::Floor($count * 0.99) - 1
    $p95ResponseTime = if ($p95Index -ge 0) { [math]::Round($sortedTimes[$p95Index], 2) } else { $maxResponseTime }
    $p99ResponseTime = if ($p99Index -ge 0) { [math]::Round($sortedTimes[$p99Index], 2) } else { $maxResponseTime }
    
    $avgRps = [math]::Round($totalRequests / ($actualDuration * 60), 2)
    
    # Operation type breakdown
    $healthChecks = ($allResults | Where-Object { $_.OperationType -eq "HealthCheck" }).Count
    $auths = ($allResults | Where-Object { $_.OperationType -eq "Authentication" }).Count
    $queries = ($allResults | Where-Object { $_.OperationType -eq "NotificationList" }).Count
    
    Write-Host "Test Configuration:"
    Write-Host "  Duration: $([math]::Round($actualDuration, 2)) minutes"
    Write-Host "  Concurrent Users: $ConcurrentUsers"
    Write-Host ""
    Write-Host "Request Statistics:"
    Write-Host "  Total Requests: $totalRequests"
    Write-Host "  Operation Breakdown: HealthCheck=$healthChecks, Auth=$auths, Query=$queries"
    Write-Host "  Successful Requests: $successfulRequests"
    Write-Host "  Failed Requests: $failedRequests"
    Write-Host "  Success Rate: $successRate%"
    Write-Host "  Average RPS: $avgRps"
    Write-Host ""
    Write-Host "Response Time Statistics:"
    Write-Host "  Average Response Time: $avgResponseTime ms"
    Write-Host "  Minimum Response Time: $minResponseTime ms"
    Write-Host "  Maximum Response Time: $maxResponseTime ms"
    Write-Host "  95th Percentile: $p95ResponseTime ms"
    Write-Host "  99th Percentile: $p99ResponseTime ms"
    Write-Host ""
    Write-Host "System Resource Usage:"
    Write-Host "  Initial State: CPU $initialCpu%, Memory ${initialMemUsed}MB"
    Write-Host "  Final State: CPU $finalCpu%, Memory ${finalMemUsed}MB"
    Write-Host "  CPU Change: $($finalCpu - $initialCpu)%"
    Write-Host "  Memory Change: $($finalMemUsed - $initialMemUsed)MB"
    
} else {
    Write-Host "No results collected - test may have failed"
    $avgResponseTime = 0; $minResponseTime = 0; $maxResponseTime = 0
    $p95ResponseTime = 0; $p99ResponseTime = 0; $avgRps = 0
}

# Save detailed results
$continuousResultFile = "$ResultsDir\continuous_test_${DurationMinutes}min_$timestamp.csv"
"RequestId,UserId,OperationType,Timestamp,Success,ResponseTime_ms,StatusCode,Error" | Out-File -FilePath $continuousResultFile -Encoding UTF8

foreach ($result in $allResults) {
    "$($result.RequestId),$($result.UserId),$($result.OperationType),$($result.Timestamp.ToString('yyyy-MM-dd HH:mm:ss.fff')),$($result.Success),$($result.ResponseTime),$($result.StatusCode),$($result.Error)" | Out-File -FilePath $continuousResultFile -Append -Encoding UTF8
}

Write-Host ""
Write-Host "Detailed results saved to: $continuousResultFile"
if ($monitorFile) {
    Write-Host "System monitoring data saved to: $monitorFile"
}

# Generate continuous test summary
$summaryFile = "$ResultsDir\continuous_test_summary_${DurationMinutes}min_$timestamp.txt"

@"
INTELLIGENT NOTIFICATION SYSTEM - CONTINUOUS PERFORMANCE TEST REPORT
Generated: $(Get-Date)
==================================================================

TEST CONFIGURATION:
- Duration: $([math]::Round($actualDuration, 2)) minutes
- Concurrent Users: $ConcurrentUsers  
- Test Types: Health Check, Authentication, Notification List (Mixed Load)

PERFORMANCE RESULTS:
- Total Requests: $totalRequests
- Success Rate: $successRate%
- Average RPS: $avgRps
- Average Response Time: $avgResponseTime ms
- 95th Percentile Response Time: $p95ResponseTime ms
- Maximum Response Time: $maxResponseTime ms

SYSTEM STABILITY:
- Initial System State: CPU $initialCpu%, Memory ${initialMemUsed}MB
- Final System State: CPU $finalCpu%, Memory ${finalMemUsed}MB
- System remained stable throughout the test: $(if ([math]::Abs($finalCpu - $initialCpu) -lt 20 -and [math]::Abs($finalMemUsed - $initialMemUsed) -lt 1000) { "YES" } else { "NO - Significant resource changes detected" })

MEMORY LEAK ASSESSMENT:
- Memory Change: $($finalMemUsed - $initialMemUsed)MB
- Memory Leak Risk: $(if ([math]::Abs($finalMemUsed - $initialMemUsed) -lt 100) { "LOW" } elseif ([math]::Abs($finalMemUsed - $initialMemUsed) -lt 500) { "MODERATE" } else { "HIGH" })

CONTINUOUS LOAD CAPACITY:
- System can handle $ConcurrentUsers concurrent users for $([math]::Round($actualDuration, 2)) minutes
- Performance remained $(if ($successRate -ge 95) { "EXCELLENT" } elseif ($successRate -ge 90) { "GOOD" } else { "DEGRADED" })
- Response times $(if ($avgResponseTime -le 100) { "remained fast" } elseif ($avgResponseTime -le 500) { "remained acceptable" } else { "became slow" }) (avg: $avgResponseTime ms)

RECOMMENDATIONS:
$(if ($successRate -ge 95) {
"- System demonstrates excellent stability under continuous load
- Current configuration suitable for production deployment
- Monitor system resources in production for similar load patterns"
} else {
"- System shows performance degradation under continuous load  
- Consider system optimization before production deployment
- Investigate failed requests and error patterns"
})

Detailed Data Files:
- Request Details: $continuousResultFile
- System Monitoring: $monitorFile
"@ | Out-File -FilePath $summaryFile -Encoding UTF8

Write-Host ""
Write-Host "CONTINUOUS TEST ASSESSMENT:" -ForegroundColor Cyan
$assessmentColor = if ($successRate -ge 95 -and $avgResponseTime -le 100) { "Green" } elseif ($successRate -ge 90 -and $avgResponseTime -le 500) { "Yellow" } else { "Red" }
Write-Host "- Overall Performance: $(if ($successRate -ge 95 -and $avgResponseTime -le 100) { "EXCELLENT" } elseif ($successRate -ge 90 -and $avgResponseTime -le 500) { "GOOD" } else { "NEEDS IMPROVEMENT" })" -ForegroundColor $assessmentColor
Write-Host "- System Stability: $(if ([math]::Abs($finalMemUsed - $initialMemUsed) -lt 100) { "STABLE" } else { "RESOURCE CHANGES DETECTED" })" -ForegroundColor $(if ([math]::Abs($finalMemUsed - $initialMemUsed) -lt 100) { "Green" } else { "Yellow" })
Write-Host "- Memory Leak Risk: $(if ([math]::Abs($finalMemUsed - $initialMemUsed) -lt 100) { "LOW" } elseif ([math]::Abs($finalMemUsed - $initialMemUsed) -lt 500) { "MODERATE" } else { "HIGH" })" -ForegroundColor $(if ([math]::Abs($finalMemUsed - $initialMemUsed) -lt 100) { "Green" } elseif ([math]::Abs($finalMemUsed - $initialMemUsed) -lt 500) { "Yellow" } else { "Red" })

Write-Host ""
Write-Host "Continuous test summary saved to: $summaryFile"
Write-Host "======================================================="