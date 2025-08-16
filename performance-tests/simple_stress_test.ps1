# Simplified Stress Testing Script
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
Write-Host "SIMPLIFIED STRESS TESTING"
Write-Host "Start Time: $(Get-Date)"
Write-Host "======================================================="

# Test configurations for stress testing
$stressLevels = @(
    @{Users=50; Duration=15; Description="Moderate Stress"},
    @{Users=100; Duration=15; Description="High Stress"},
    @{Users=200; Duration=15; Description="Very High Stress"},
    @{Users=300; Duration=15; Description="Extreme Stress"},
    @{Users=500; Duration=10; Description="Maximum Stress"}
)

$stressResults = @()

foreach ($level in $stressLevels) {
    $users = $level.Users
    $duration = $level.Duration
    $description = $level.Description
    
    Write-Host ""
    Write-Host "Testing: $description ($users users, $duration seconds)"
    Write-Host "Start time: $(Get-Date -Format 'HH:mm:ss')"
    
    # Pre-test system resources
    try {
        $preCpu = (Get-WmiObject -Class Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average
        $preMemory = Get-WmiObject -Class Win32_OperatingSystem
        $preMemUsed = [math]::Round(($preMemory.TotalVisibleMemorySize - $preMemory.AvailablePhysicalMemory) / 1024, 2)
    } catch {
        $preCpu = 0; $preMemUsed = 0
    }
    
    Write-Host "Pre-test: CPU $preCpu%, Memory ${preMemUsed}MB"
    
    # Use Apache Bench (ab) if available, otherwise use simple curl
    $testStart = Get-Date
    $successCount = 0
    $totalRequests = 0
    $responseTimes = @()
    
    # Simple concurrent testing using background jobs
    $jobs = @()
    $jobResults = @()
    
    # Start multiple batches to simulate concurrent load
    $batchSize = [math]::Min(50, $users) # Limit job count to avoid PowerShell limits
    $batches = [math]::Ceiling($users / $batchSize)
    
    for ($batch = 1; $batch -le $batches; $batch++) {
        $usersInBatch = if ($batch -eq $batches) { $users - (($batch - 1) * $batchSize) } else { $batchSize }
        
        $job = Start-Job -ScriptBlock {
            param($BatchId, $UsersInBatch, $Duration, $BaseUrl)
            
            $batchResults = @()
            $endTime = (Get-Date).AddSeconds($Duration)
            
            for ($u = 1; $u -le $UsersInBatch; $u++) {
                $requestCount = 0
                $userEndTime = (Get-Date).AddSeconds([math]::Min($Duration, 10))
                
                while ((Get-Date) -lt $userEndTime) {
                    $requestCount++
                    $startTime = Get-Date
                    
                    try {
                        $response = Invoke-WebRequest -Uri "$BaseUrl/admin-api/test/notification/api/ping" -UseBasicParsing -TimeoutSec 5
                        $endTime_req = Get-Date
                        $responseTime = ($endTime_req - $startTime).TotalMilliseconds
                        
                        $batchResults += @{
                            BatchId = $BatchId
                            UserId = $u
                            RequestId = $requestCount
                            Success = $true
                            ResponseTime = [math]::Round($responseTime, 2)
                            StatusCode = $response.StatusCode
                        }
                    } catch {
                        $endTime_req = Get-Date
                        $responseTime = ($endTime_req - $startTime).TotalMilliseconds
                        
                        $batchResults += @{
                            BatchId = $BatchId
                            UserId = $u
                            RequestId = $requestCount
                            Success = $false
                            ResponseTime = [math]::Round($responseTime, 2)
                            StatusCode = 0
                        }
                    }
                    
                    Start-Sleep -Milliseconds 100
                }
            }
            
            return $batchResults
        } -ArgumentList $batch, $usersInBatch, $duration, $baseUrl
        
        $jobs += $job
    }
    
    Write-Host "Started $($jobs.Count) batches for $users total users"
    
    # Wait for jobs with timeout
    $jobTimeout = $duration + 30
    try {
        $completedJobs = Wait-Job -Job $jobs -Timeout $jobTimeout
        Write-Host "Completed $($completedJobs.Count) out of $($jobs.Count) batches"
        
        # Collect results
        foreach ($job in $jobs) {
            try {
                $batchResults = Receive-Job -Job $job -ErrorAction SilentlyContinue
                if ($batchResults) {
                    $jobResults += $batchResults
                }
            } catch {
                # Failed to receive results
            }
            Remove-Job -Job $job -Force
        }
        
    } catch {
        Write-Host "Job execution failed or timed out" -ForegroundColor Yellow
        # Clean up jobs
        $jobs | Stop-Job -ErrorAction SilentlyContinue
        $jobs | Remove-Job -Force -ErrorAction SilentlyContinue
    }
    
    $testEnd = Get-Date
    $actualDuration = ($testEnd - $testStart).TotalSeconds
    
    # Post-test system resources
    try {
        $postCpu = (Get-WmiObject -Class Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average
        $postMemory = Get-WmiObject -Class Win32_OperatingSystem
        $postMemUsed = [math]::Round(($postMemory.TotalVisibleMemorySize - $postMemory.AvailablePhysicalMemory) / 1024, 2)
    } catch {
        $postCpu = 0; $postMemUsed = 0
    }
    
    # Calculate statistics
    $totalRequests = $jobResults.Count
    $successfulRequests = ($jobResults | Where-Object { $_.Success }).Count
    $failedRequests = $totalRequests - $successfulRequests
    $successRate = if ($totalRequests -gt 0) { [math]::Round(($successfulRequests / $totalRequests) * 100, 2) } else { 0 }
    
    if ($jobResults.Count -gt 0) {
        $allResponseTimes = $jobResults | ForEach-Object { $_.ResponseTime }
        $avgResponseTime = [math]::Round(($allResponseTimes | Measure-Object -Average).Average, 2)
        $minResponseTime = [math]::Round(($allResponseTimes | Measure-Object -Minimum).Minimum, 2)
        $maxResponseTime = [math]::Round(($allResponseTimes | Measure-Object -Maximum).Maximum, 2)
        $rps = [math]::Round($totalRequests / $actualDuration, 2)
        
        # Calculate 95th percentile
        $sortedTimes = $allResponseTimes | Sort-Object
        $p95Index = [math]::Floor($sortedTimes.Count * 0.95) - 1
        $p95ResponseTime = if ($p95Index -ge 0 -and $p95Index -lt $sortedTimes.Count) { 
            [math]::Round($sortedTimes[$p95Index], 2) 
        } else { 
            $maxResponseTime 
        }
    } else {
        $avgResponseTime = 0; $minResponseTime = 0; $maxResponseTime = 0; $p95ResponseTime = 0; $rps = 0
    }
    
    # Assess system health
    $systemHealthy = ($successRate -ge 95) -and ($avgResponseTime -lt 1000) -and ($maxResponseTime -lt 10000)
    $healthStatus = if ($systemHealthy) { "HEALTHY" } else { "STRESSED" }
    
    Write-Host "Results:"
    Write-Host "  Duration: $([math]::Round($actualDuration, 2)) seconds"
    Write-Host "  Total Requests: $totalRequests"
    Write-Host "  Successful: $successfulRequests"
    Write-Host "  Failed: $failedRequests"  
    Write-Host "  Success Rate: $successRate%"
    Write-Host "  RPS: $rps"
    Write-Host "  Response Time: Avg=$avgResponseTime ms, Min=$minResponseTime ms, Max=$maxResponseTime ms"
    Write-Host "  95th Percentile: $p95ResponseTime ms"
    Write-Host "  System Resources: CPU $preCpu% -> $postCpu%, Memory ${preMemUsed}MB -> ${postMemUsed}MB"
    Write-Host "  System Status: $healthStatus" -ForegroundColor $(if ($systemHealthy) { "Green" } else { "Yellow" })
    
    # Store results
    $stressResults += @{
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
        PreCPU = $preCpu
        PostCPU = $postCpu
        PreMemory = $preMemUsed
        PostMemory = $postMemUsed
        SystemHealthy = $systemHealthy
        HealthStatus = $healthStatus
    }
    
    # Save detailed results
    $detailFile = "$ResultsDir\stress_${users}users_$timestamp.csv"
    "BatchId,UserId,RequestId,Success,ResponseTime_ms,StatusCode" | Out-File -FilePath $detailFile -Encoding UTF8
    foreach ($result in $jobResults) {
        "$($result.BatchId),$($result.UserId),$($result.RequestId),$($result.Success),$($result.ResponseTime),$($result.StatusCode)" | Out-File -FilePath $detailFile -Append -Encoding UTF8
    }
    Write-Host "Detailed results saved to: $detailFile"
    
    # Wait for recovery if more tests remaining
    if ($users -ne $stressLevels[-1].Users) {
        Write-Host "Waiting 10 seconds for system recovery..."
        Start-Sleep -Seconds 10
    }
}

# Generate final stress test report
Write-Host ""
Write-Host "======================================================="
Write-Host "STRESS TESTING COMPLETED"
Write-Host "End Time: $(Get-Date)"
Write-Host "======================================================="

$reportFile = "$ResultsDir\stress_test_summary_$timestamp.txt"

@"
INTELLIGENT NOTIFICATION SYSTEM - STRESS TEST SUMMARY
Generated: $(Get-Date)
=====================================================

OBJECTIVE: Determine system stress limits and performance degradation points

TEST METHODOLOGY:
- Progressive stress testing from 50 to 500 concurrent users
- Focus on system health check API (lightest possible load)
- Monitor system resources (CPU, Memory) during testing
- Measure response times, success rates, and throughput

STRESS TEST RESULTS:
"@ | Out-File -FilePath $reportFile -Encoding UTF8

"" | Out-File -FilePath $reportFile -Append -Encoding UTF8
"Users,Description,Success_Rate,RPS,Avg_Response_ms,Max_Response_ms,95th_Percentile_ms,System_Status" | Out-File -FilePath $reportFile -Append -Encoding UTF8

foreach ($result in $stressResults) {
    "$($result.Users),$($result.Description),$($result.SuccessRate),$($result.RPS),$($result.AvgResponseTime),$($result.MaxResponseTime),$($result.P95ResponseTime),$($result.HealthStatus)" | Out-File -FilePath $reportFile -Append -Encoding UTF8
}

Write-Host ""
Write-Host "STRESS TEST SUMMARY:"
foreach ($result in $stressResults) {
    $statusColor = if ($result.SystemHealthy) { "Green" } else { "Yellow" }
    Write-Host "$($result.Users) users: $($result.SuccessRate)% success, $($result.RPS) RPS, $($result.AvgResponseTime)ms avg - $($result.HealthStatus)" -ForegroundColor $statusColor
}

# Identify breaking points
$healthyResults = $stressResults | Where-Object { $_.SystemHealthy }
$stressedResults = $stressResults | Where-Object { -not $_.SystemHealthy }

"" | Out-File -FilePath $reportFile -Append -Encoding UTF8
"ANALYSIS:" | Out-File -FilePath $reportFile -Append -Encoding UTF8

if ($healthyResults) {
    $maxHealthyUsers = ($healthyResults | Measure-Object -Property Users -Maximum).Maximum
    $maxHealthyRPS = ($healthyResults | Measure-Object -Property RPS -Maximum).Maximum
    "- System remains healthy up to: $maxHealthyUsers concurrent users" | Out-File -FilePath $reportFile -Append -Encoding UTF8
    "- Peak healthy performance: $maxHealthyRPS RPS" | Out-File -FilePath $reportFile -Append -Encoding UTF8
    Write-Host ""
    Write-Host "KEY FINDINGS:" -ForegroundColor Cyan
    Write-Host "- System remains healthy up to: $maxHealthyUsers concurrent users" -ForegroundColor Green
    Write-Host "- Peak healthy performance: $maxHealthyRPS RPS" -ForegroundColor Green
}

if ($stressedResults) {
    $minStressedUsers = ($stressedResults | Measure-Object -Property Users -Minimum).Minimum
    "- System stress threshold: $minStressedUsers concurrent users" | Out-File -FilePath $reportFile -Append -Encoding UTF8
    Write-Host "- System stress threshold: $minStressedUsers concurrent users" -ForegroundColor Yellow
}

$bestResult = $stressResults | Sort-Object RPS -Descending | Select-Object -First 1
"- Best overall performance: $($bestResult.RPS) RPS at $($bestResult.Users) users" | Out-File -FilePath $reportFile -Append -Encoding UTF8
Write-Host "- Best overall performance: $($bestResult.RPS) RPS at $($bestResult.Users) users" -ForegroundColor Cyan

Write-Host ""
Write-Host "Full stress test report saved to: $reportFile"
Write-Host "======================================================="