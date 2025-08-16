# Stress Testing Script - Finding System Breaking Point
# Progressive stress testing and peak burst testing

param(
    [string]$ResultsDir = "D:\ClaudeCode\AI_Web\performance-tests\results"
)

$baseUrl = "http://localhost:48081"
$mockUrl = "http://localhost:48082"
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

if (!(Test-Path $ResultsDir)) {
    New-Item -ItemType Directory -Path $ResultsDir -Force
}

Write-Host "=======================================================
STRESS TESTING SUITE - FINDING BREAKING POINT
Start Time: $(Get-Date)
======================================================="

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

$stressResults = @()

# PHASE 1: Progressive Stress Testing
Write-Host ""
Write-Host "======================================================="
Write-Host "PHASE 1: PROGRESSIVE STRESS TESTING"
Write-Host "Starting from 10 users, adding 20 users every 30 seconds"
Write-Host "======================================================="

$progressiveResults = @()
$currentUsers = 10
$maxUsers = 500
$incremental = 20
$testDuration = 30

while ($currentUsers -le $maxUsers) {
    Write-Host ""
    Write-Host "Testing $currentUsers concurrent users for $testDuration seconds..."
    $testStart = Get-Date
    
    # Track system resources before test
    try {
        $preCpu = (Get-WmiObject -Class Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average
        $preMemory = Get-WmiObject -Class Win32_OperatingSystem
        $preMemUsed = [math]::Round(($preMemory.TotalVisibleMemorySize - $preMemory.AvailablePhysicalMemory) / 1024, 2)
    } catch {
        $preCpu = "N/A"; $preMemUsed = "N/A"
    }
    
    $requests = [System.Collections.Concurrent.ConcurrentBag[object]]::new()
    $failureDetected = $false
    
    # Start user jobs
    $userJobs = @()
    for ($i = 1; $i -le $currentUsers; $i++) {
        $userJob = Start-Job -ScriptBlock {
            param($UserId, $Duration, $BaseUrl, $MockUrl, $TestToken)
            
            $userRequests = @()
            $endTime = (Get-Date).AddSeconds($Duration)
            $requestCount = 0
            
            while ((Get-Date) -lt $endTime) {
                $requestCount++
                $requestId = "StressUser${UserId}_${requestCount}"
                
                $startTime = Get-Date
                try {
                    # Focus on health check for stress testing (lightest load)
                    $response = Invoke-WebRequest -Uri "$BaseUrl/admin-api/test/notification/api/ping" -UseBasicParsing -TimeoutSec 10
                    $endTime = Get-Date
                    $responseTime = ($endTime - $startTime).TotalMilliseconds
                    
                    $userRequests += @{
                        RequestId = $requestId
                        UserId = $UserId
                        ResponseTime = [math]::Round($responseTime, 2)
                        StatusCode = $response.StatusCode
                        Success = $true
                        Error = ""
                        Timestamp = $startTime
                    }
                    
                } catch {
                    $endTime = Get-Date
                    $responseTime = ($endTime - $startTime).TotalMilliseconds
                    
                    $userRequests += @{
                        RequestId = $requestId
                        UserId = $UserId
                        ResponseTime = [math]::Round($responseTime, 2)
                        StatusCode = 0
                        Success = $false
                        Error = $_.Exception.Message
                        Timestamp = $startTime
                    }
                }
                
                # Minimal delay for stress testing
                Start-Sleep -Milliseconds 50
            }
            
            return $userRequests
        } -ArgumentList $i, $testDuration, $baseUrl, $mockUrl, $testToken
        
        $userJobs += $userJob
    }
    
    # Monitor for timeouts and failures
    $jobTimeout = $testDuration + 30 # Extra time for job completion
    try {
        $completed = Wait-Job -Job $userJobs -Timeout $jobTimeout
        if ($completed.Count -ne $userJobs.Count) {
            $failureDetected = $true
            Write-Host "WARNING: Some jobs timed out or failed to complete" -ForegroundColor Yellow
        }
    } catch {
        $failureDetected = $true
        Write-Host "ERROR: Job execution failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    # Collect results
    $testResults = @()
    foreach ($job in $userJobs) {
        try {
            $jobResults = Receive-Job -Job $job -ErrorAction SilentlyContinue
            if ($jobResults) {
                $testResults += $jobResults
            }
        } catch {
            # Job failed
        }
        Remove-Job -Job $job -Force
    }
    
    $testEnd = Get-Date
    $actualDuration = ($testEnd - $testStart).TotalSeconds
    
    # Track system resources after test
    try {
        $postCpu = (Get-WmiObject -Class Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average
        $postMemory = Get-WmiObject -Class Win32_OperatingSystem
        $postMemUsed = [math]::Round(($postMemory.TotalVisibleMemorySize - $postMemory.AvailablePhysicalMemory) / 1024, 2)
    } catch {
        $postCpu = "N/A"; $postMemUsed = "N/A"
    }
    
    # Calculate metrics
    $totalRequests = $testResults.Count
    $successfulRequests = ($testResults | Where-Object { $_.Success }).Count
    $successRate = if ($totalRequests -gt 0) { [math]::Round(($successfulRequests / $totalRequests) * 100, 2) } else { 0 }
    
    if ($testResults.Count -gt 0) {
        $responseTimes = $testResults | ForEach-Object { $_.ResponseTime }
        $avgResponseTime = [math]::Round(($responseTimes | Measure-Object -Average).Average, 2)
        $maxResponseTime = [math]::Round(($responseTimes | Measure-Object -Maximum).Maximum, 2)
        $rps = [math]::Round($totalRequests / $actualDuration, 2)
    } else {
        $avgResponseTime = 0; $maxResponseTime = 0; $rps = 0
    }
    
    # Determine if system is failing
    $systemFailing = ($successRate -lt 95) -or ($avgResponseTime -gt 5000) -or ($maxResponseTime -gt 30000) -or $failureDetected
    
    Write-Host "Results: $successRate% success, $avgResponseTime ms avg, $maxResponseTime ms max, $rps RPS"
    Write-Host "System Resources: CPU $preCpu% -> $postCpu%, Memory ${preMemUsed}MB -> ${postMemUsed}MB"
    
    if ($systemFailing) {
        Write-Host "SYSTEM STRESS THRESHOLD DETECTED!" -ForegroundColor Red
        Write-Host "Breaking point found at approximately $currentUsers concurrent users" -ForegroundColor Red
    }
    
    # Store results
    $progressiveResults += @{
        Users = $currentUsers
        Duration = $actualDuration
        TotalRequests = $totalRequests
        SuccessRate = $successRate
        AvgResponseTime = $avgResponseTime
        MaxResponseTime = $maxResponseTime
        RPS = $rps
        PreCPU = $preCpu
        PostCPU = $postCpu
        PreMemory = $preMemUsed
        PostMemory = $postMemUsed
        SystemFailing = $systemFailing
        FailureDetected = $failureDetected
    }
    
    # Stop if system is clearly failing
    if ($systemFailing -and $currentUsers -gt 50) {
        Write-Host "Stopping progressive test due to system stress" -ForegroundColor Yellow
        break
    }
    
    $currentUsers += $incremental
    
    # Recovery time between tests
    if ($currentUsers -le $maxUsers) {
        Write-Host "Waiting 5 seconds for system recovery..."
        Start-Sleep -Seconds 5
    }
}

# PHASE 2: Peak Burst Testing
Write-Host ""
Write-Host "======================================================="
Write-Host "PHASE 2: PEAK BURST TESTING"
Write-Host "Instant high-load burst tests"
Write-Host "======================================================="

$burstTests = @(
    @{Users=500; Duration=10; Description="500 User Burst"},
    @{Users=1000; Duration=10; Description="1000 User Burst"}
)

$burstResults = @()

foreach ($burst in $burstTests) {
    $burstUsers = $burst.Users
    $burstDuration = $burst.Duration
    $description = $burst.Description
    
    Write-Host ""
    Write-Host "Testing: $description ($burstUsers users for $burstDuration seconds)"
    $testStart = Get-Date
    
    # Pre-test system state
    try {
        $preCpu = (Get-WmiObject -Class Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average
        $preMemory = Get-WmiObject -Class Win32_OperatingSystem
        $preMemUsed = [math]::Round(($preMemory.TotalVisibleMemorySize - $preMemory.AvailablePhysicalMemory) / 1024, 2)
    } catch {
        $preCpu = "N/A"; $preMemUsed = "N/A"
    }
    
    Write-Host "Pre-test system state: CPU $preCpu%, Memory ${preMemUsed}MB"
    
    $burstFailures = 0
    $burstJobsStarted = 0
    $burstJobsCompleted = 0
    
    # Start burst load
    $burstJobs = @()
    for ($i = 1; $i -le $burstUsers; $i++) {
        try {
            $burstJob = Start-Job -ScriptBlock {
                param($UserId, $Duration, $BaseUrl)
                
                $startTime = Get-Date
                try {
                    $response = Invoke-WebRequest -Uri "$BaseUrl/admin-api/test/notification/api/ping" -UseBasicParsing -TimeoutSec 15
                    $endTime = Get-Date
                    $responseTime = ($endTime - $startTime).TotalMilliseconds
                    
                    return @{
                        UserId = $UserId
                        Success = $true
                        ResponseTime = [math]::Round($responseTime, 2)
                        StatusCode = $response.StatusCode
                        Error = ""
                    }
                } catch {
                    $endTime = Get-Date
                    $responseTime = ($endTime - $startTime).TotalMilliseconds
                    
                    return @{
                        UserId = $UserId
                        Success = $false
                        ResponseTime = [math]::Round($responseTime, 2)
                        StatusCode = 0
                        Error = $_.Exception.Message
                    }
                }
            } -ArgumentList $i, $burstDuration, $baseUrl
            
            $burstJobs += $burstJob
            $burstJobsStarted++
            
        } catch {
            $burstFailures++
            Write-Host "Failed to start job ${i}: $($_.Exception.Message)"
        }
    }
    
    Write-Host "Started $burstJobsStarted jobs out of $burstUsers requested"
    
    # Wait for burst jobs with extended timeout
    $burstTimeout = $burstDuration + 60
    try {
        $completedJobs = Wait-Job -Job $burstJobs -Timeout $burstTimeout
        $burstJobsCompleted = $completedJobs.Count
    } catch {
        Write-Host "Burst test timeout or error: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    # Collect burst results
    $burstTestResults = @()
    foreach ($job in $burstJobs) {
        try {
            $jobResult = Receive-Job -Job $job -ErrorAction SilentlyContinue
            if ($jobResult) {
                $burstTestResults += $jobResult
            }
        } catch {
            # Failed to receive job result
        }
        Remove-Job -Job $job -Force
    }
    
    $testEnd = Get-Date
    $actualDuration = ($testEnd - $testStart).TotalSeconds
    
    # Post-test system state
    try {
        $postCpu = (Get-WmiObject -Class Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average
        $postMemory = Get-WmiObject -Class Win32_OperatingSystem
        $postMemUsed = [math]::Round(($postMemory.TotalVisibleMemorySize - $postMemory.AvailablePhysicalMemory) / 1024, 2)
    } catch {
        $postCpu = "N/A"; $postMemUsed = "N/A"
    }
    
    # Calculate burst metrics
    $totalBurstRequests = $burstTestResults.Count
    $successfulBurstRequests = ($burstTestResults | Where-Object { $_.Success }).Count
    $burstSuccessRate = if ($totalBurstRequests -gt 0) { [math]::Round(($successfulBurstRequests / $totalBurstRequests) * 100, 2) } else { 0 }
    
    if ($burstTestResults.Count -gt 0) {
        $burstResponseTimes = $burstTestResults | ForEach-Object { $_.ResponseTime }
        $avgBurstResponseTime = [math]::Round(($burstResponseTimes | Measure-Object -Average).Average, 2)
        $maxBurstResponseTime = [math]::Round(($burstResponseTimes | Measure-Object -Maximum).Maximum, 2)
        $burstRps = [math]::Round($totalBurstRequests / $actualDuration, 2)
    } else {
        $avgBurstResponseTime = 0; $maxBurstResponseTime = 0; $burstRps = 0
    }
    
    Write-Host "Burst Results:"
    Write-Host "  Jobs Started: $burstJobsStarted / $burstUsers"
    Write-Host "  Jobs Completed: $burstJobsCompleted / $burstJobsStarted"
    Write-Host "  Requests Processed: $totalBurstRequests"
    Write-Host "  Success Rate: $burstSuccessRate%"
    Write-Host "  Average Response Time: $avgBurstResponseTime ms"
    Write-Host "  Max Response Time: $maxBurstResponseTime ms"
    Write-Host "  RPS: $burstRps"
    Write-Host "  System Impact: CPU $preCpu% -> $postCpu%, Memory ${preMemUsed}MB -> ${postMemUsed}MB"
    
    $burstResults += @{
        Users = $burstUsers
        Description = $description
        JobsStarted = $burstJobsStarted
        JobsCompleted = $burstJobsCompleted
        TotalRequests = $totalBurstRequests
        SuccessRate = $burstSuccessRate
        AvgResponseTime = $avgBurstResponseTime
        MaxResponseTime = $maxBurstResponseTime
        RPS = $burstRps
        PreCPU = $preCpu
        PostCPU = $postCpu
        PreMemory = $preMemUsed
        PostMemory = $postMemUsed
        Duration = $actualDuration
    }
    
    Write-Host "Waiting 15 seconds for system recovery..."
    Start-Sleep -Seconds 15
}

# Generate Stress Test Report
Write-Host ""
Write-Host "======================================================="
Write-Host "STRESS TESTING COMPLETE"
Write-Host "======================================================="

$stressReportFile = "$ResultsDir\stress_test_report_$timestamp.txt"

$report = @"
INTELLIGENT NOTIFICATION SYSTEM - STRESS TEST REPORT
Generated: $(Get-Date)
====================================================

OBJECTIVE: Find system breaking point and peak load capacity

TEST ENVIRONMENT:
- Main Service: http://localhost:48081
- Mock API Service: http://localhost:48082
- Operating System: Windows
- Test Focus: Health Check API (lightest load)

PHASE 1: PROGRESSIVE STRESS TESTING RESULTS:
"@

$report | Out-File -FilePath $stressReportFile -Encoding UTF8

foreach ($result in $progressiveResults) {
    $status = if ($result.SystemFailing) { "FAILING" } else { "STABLE" }
    "$($result.Users) users: $($result.SuccessRate)% success, $($result.AvgResponseTime)ms avg, $($result.RPS) RPS - $status" | Out-File -FilePath $stressReportFile -Append -Encoding UTF8
}

"" | Out-File -FilePath $stressReportFile -Append -Encoding UTF8
"PHASE 2: PEAK BURST TESTING RESULTS:" | Out-File -FilePath $stressReportFile -Append -Encoding UTF8

foreach ($result in $burstResults) {
    "$($result.Description): $($result.JobsCompleted)/$($result.JobsStarted) jobs completed, $($result.SuccessRate)% success, $($result.AvgResponseTime)ms avg" | Out-File -FilePath $stressReportFile -Append -Encoding UTF8
}

Write-Host "Stress test report saved to: $stressReportFile"

# Display key findings
Write-Host ""
Write-Host "KEY FINDINGS:"
$failingPoint = $progressiveResults | Where-Object { $_.SystemFailing } | Select-Object -First 1
if ($failingPoint) {
    Write-Host "- System breaking point: ~$($failingPoint.Users) concurrent users" -ForegroundColor Red
} else {
    Write-Host "- System remained stable up to $($progressiveResults[-1].Users) concurrent users" -ForegroundColor Green
}

$bestPerformance = $progressiveResults | Sort-Object RPS -Descending | Select-Object -First 1
Write-Host "- Peak performance: $($bestPerformance.RPS) RPS at $($bestPerformance.Users) users"

$burstResult1000 = $burstResults | Where-Object { $_.Users -eq 1000 }
if ($burstResult1000) {
    Write-Host "- 1000 user burst: $($burstResult1000.SuccessRate)% success rate"
}

Write-Host ""
Write-Host "======================================================="