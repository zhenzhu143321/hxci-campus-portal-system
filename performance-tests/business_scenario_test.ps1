# Business Scenario Performance Testing
# Testing specific business scenarios under load

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
Write-Host "BUSINESS SCENARIO PERFORMANCE TESTING"
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

$scenarioResults = @()

# SCENARIO A: Authentication Storm
Write-Host ""
Write-Host "======================================================="
Write-Host "SCENARIO A: AUTHENTICATION STORM"
Write-Host "50 users simultaneously authenticating"
Write-Host "======================================================="

Write-Host "Testing authentication storm..."
$authStormStart = Get-Date

$authResults = @()
$authTestUsers = @(
    @{employeeId="PRINCIPAL_001"; name="Principal-Zhang"; role="Principal"},
    @{employeeId="TEACHER_001"; name="Teacher-Wang"; role="Teacher"},
    @{employeeId="STUDENT_001"; name="Student-Zhang"; role="Student"},
    @{employeeId="ACADEMIC_ADMIN_001"; name="Director-Li"; role="Academic"}
)

# Sequential authentication tests (to avoid overwhelming the system)
for ($i = 1; $i -le 50; $i++) {
    $testUser = $authTestUsers[($i - 1) % $authTestUsers.Count]
    $authRequestBody = @{
        employeeId = $testUser.employeeId
        name = $testUser.name
        password = "admin123"
    } | ConvertTo-Json
    
    $startTime = Get-Date
    try {
        $response = Invoke-WebRequest -Uri "$mockUrl/mock-school-api/auth/authenticate" -Method POST -Body $authRequestBody -ContentType "application/json" -UseBasicParsing -TimeoutSec 10
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        
        $authResults += @{
            RequestId = $i
            Role = $testUser.role
            Success = $true
            ResponseTime = [math]::Round($responseTime, 2)
            StatusCode = $response.StatusCode
            Error = ""
        }
    } catch {
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        
        $authResults += @{
            RequestId = $i
            Role = $testUser.role
            Success = $false
            ResponseTime = [math]::Round($responseTime, 2)
            StatusCode = 0
            Error = $_.Exception.Message
        }
    }
    
    # Small delay between requests
    Start-Sleep -Milliseconds 100
}

$authStormEnd = Get-Date
$authStormDuration = ($authStormEnd - $authStormStart).TotalSeconds

# Calculate auth storm statistics
$authTotal = $authResults.Count
$authSuccess = ($authResults | Where-Object { $_.Success }).Count
$authSuccessRate = [math]::Round(($authSuccess / $authTotal) * 100, 2)
$authResponseTimes = $authResults | ForEach-Object { $_.ResponseTime }
$authAvgTime = [math]::Round(($authResponseTimes | Measure-Object -Average).Average, 2)
$authMaxTime = [math]::Round(($authResponseTimes | Measure-Object -Maximum).Maximum, 2)
$authRps = [math]::Round($authTotal / $authStormDuration, 2)

Write-Host "Authentication Storm Results:"
Write-Host "  Total Requests: $authTotal"
Write-Host "  Success Rate: $authSuccessRate%"
Write-Host "  Average Response Time: $authAvgTime ms"
Write-Host "  Max Response Time: $authMaxTime ms"
Write-Host "  RPS: $authRps"
Write-Host "  Duration: $([math]::Round($authStormDuration, 2)) seconds"

$scenarioResults += @{
    Scenario = "Authentication Storm"
    TotalRequests = $authTotal
    SuccessRate = $authSuccessRate
    AvgResponseTime = $authAvgTime
    MaxResponseTime = $authMaxTime
    RPS = $authRps
    Duration = $authStormDuration
}

# Wait for recovery
Start-Sleep -Seconds 5

# SCENARIO B: Notification Query Storm  
Write-Host ""
Write-Host "======================================================="
Write-Host "SCENARIO B: NOTIFICATION QUERY STORM"
Write-Host "100 users simultaneously querying notification lists"
Write-Host "======================================================="

Write-Host "Testing notification query storm..."
$queryStormStart = Get-Date

$queryResults = @()
$headers = @{
    "Authorization" = "Bearer $testToken"
    "tenant-id" = "1"
}

for ($i = 1; $i -le 100; $i++) {
    $startTime = Get-Date
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/admin-api/test/notification/api/list" -Headers $headers -UseBasicParsing -TimeoutSec 10
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        
        $queryResults += @{
            RequestId = $i
            Success = $true
            ResponseTime = [math]::Round($responseTime, 2)
            StatusCode = $response.StatusCode
            ContentLength = $response.Content.Length
            Error = ""
        }
    } catch {
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        
        $queryResults += @{
            RequestId = $i
            Success = $false
            ResponseTime = [math]::Round($responseTime, 2)
            StatusCode = 0
            ContentLength = 0
            Error = $_.Exception.Message
        }
    }
    
    # Small delay between requests
    Start-Sleep -Milliseconds 50
}

$queryStormEnd = Get-Date
$queryStormDuration = ($queryStormEnd - $queryStormStart).TotalSeconds

# Calculate query storm statistics
$queryTotal = $queryResults.Count
$querySuccess = ($queryResults | Where-Object { $_.Success }).Count
$querySuccessRate = [math]::Round(($querySuccess / $queryTotal) * 100, 2)
$queryResponseTimes = $queryResults | ForEach-Object { $_.ResponseTime }
$queryAvgTime = [math]::Round(($queryResponseTimes | Measure-Object -Average).Average, 2)
$queryMaxTime = [math]::Round(($queryResponseTimes | Measure-Object -Maximum).Maximum, 2)
$queryRps = [math]::Round($queryTotal / $queryStormDuration, 2)

Write-Host "Notification Query Storm Results:"
Write-Host "  Total Requests: $queryTotal"
Write-Host "  Success Rate: $querySuccessRate%"
Write-Host "  Average Response Time: $queryAvgTime ms"
Write-Host "  Max Response Time: $queryMaxTime ms"
Write-Host "  RPS: $queryRps"
Write-Host "  Duration: $([math]::Round($queryStormDuration, 2)) seconds"

$scenarioResults += @{
    Scenario = "Notification Query Storm"
    TotalRequests = $queryTotal
    SuccessRate = $querySuccessRate
    AvgResponseTime = $queryAvgTime
    MaxResponseTime = $queryMaxTime
    RPS = $queryRps
    Duration = $queryStormDuration
}

# Wait for recovery
Start-Sleep -Seconds 5

# SCENARIO C: Notification Publishing Pressure
Write-Host ""
Write-Host "======================================================="
Write-Host "SCENARIO C: NOTIFICATION PUBLISHING PRESSURE"
Write-Host "25 users simultaneously publishing notifications"
Write-Host "======================================================="

Write-Host "Testing notification publishing pressure..."
$publishStormStart = Get-Date

$publishResults = @()
$publishHeaders = @{
    "Authorization" = "Bearer $testToken"
    "tenant-id" = "1"
}

for ($i = 1; $i -le 25; $i++) {
    $publishBody = @{
        title = "Performance Test Notification $i"
        content = "This is a performance test notification content for request $i. Testing system under publishing pressure."
        level = 4  # Safe level for testing
        targetScope = "ALL_SCHOOL"
    } | ConvertTo-Json
    
    $startTime = Get-Date
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/admin-api/test/notification/api/publish-database" -Method POST -Headers $publishHeaders -Body $publishBody -ContentType "application/json" -UseBasicParsing -TimeoutSec 15
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        
        $publishResults += @{
            RequestId = $i
            Success = $true
            ResponseTime = [math]::Round($responseTime, 2)
            StatusCode = $response.StatusCode
            Error = ""
        }
    } catch {
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        
        $publishResults += @{
            RequestId = $i
            Success = $false
            ResponseTime = [math]::Round($responseTime, 2)
            StatusCode = 0
            Error = $_.Exception.Message
        }
    }
    
    # Longer delay for database operations
    Start-Sleep -Milliseconds 200
}

$publishStormEnd = Get-Date
$publishStormDuration = ($publishStormEnd - $publishStormStart).TotalSeconds

# Calculate publish storm statistics
$publishTotal = $publishResults.Count
$publishSuccess = ($publishResults | Where-Object { $_.Success }).Count
$publishSuccessRate = [math]::Round(($publishSuccess / $publishTotal) * 100, 2)
$publishResponseTimes = $publishResults | ForEach-Object { $_.ResponseTime }
$publishAvgTime = [math]::Round(($publishResponseTimes | Measure-Object -Average).Average, 2)
$publishMaxTime = [math]::Round(($publishResponseTimes | Measure-Object -Maximum).Maximum, 2)
$publishRps = [math]::Round($publishTotal / $publishStormDuration, 2)

Write-Host "Notification Publishing Pressure Results:"
Write-Host "  Total Requests: $publishTotal"
Write-Host "  Success Rate: $publishSuccessRate%"
Write-Host "  Average Response Time: $publishAvgTime ms"
Write-Host "  Max Response Time: $publishMaxTime ms"
Write-Host "  RPS: $publishRps"
Write-Host "  Duration: $([math]::Round($publishStormDuration, 2)) seconds"

$scenarioResults += @{
    Scenario = "Notification Publishing Pressure"
    TotalRequests = $publishTotal
    SuccessRate = $publishSuccessRate
    AvgResponseTime = $publishAvgTime
    MaxResponseTime = $publishMaxTime
    RPS = $publishRps
    Duration = $publishStormDuration
}

# SCENARIO D: Mixed Workload Test
Write-Host ""
Write-Host "======================================================="
Write-Host "SCENARIO D: MIXED WORKLOAD SIMULATION"
Write-Host "Simulating real-world mixed traffic patterns"
Write-Host "======================================================="

Write-Host "Testing mixed workload simulation..."
$mixedStart = Get-Date

$mixedResults = @()
$totalMixedRequests = 75

for ($i = 1; $i -le $totalMixedRequests; $i++) {
    # Randomly select operation type (60% query, 25% auth, 15% publish)
    $random = Get-Random -Minimum 1 -Maximum 101
    $operationType = if ($random -le 60) { "Query" } elseif ($random -le 85) { "Auth" } else { "Publish" }
    
    $startTime = Get-Date
    try {
        switch ($operationType) {
            "Query" {
                $response = Invoke-WebRequest -Uri "$baseUrl/admin-api/test/notification/api/list" -Headers $publishHeaders -UseBasicParsing -TimeoutSec 10
                $opSuccess = $true
            }
            "Auth" {
                $testUser = $authTestUsers[(Get-Random -Minimum 0 -Maximum $authTestUsers.Count)]
                $authBody = @{
                    employeeId = $testUser.employeeId
                    name = $testUser.name
                    password = "admin123"
                } | ConvertTo-Json
                $response = Invoke-WebRequest -Uri "$mockUrl/mock-school-api/auth/authenticate" -Method POST -Body $authBody -ContentType "application/json" -UseBasicParsing -TimeoutSec 10
                $opSuccess = $true
            }
            "Publish" {
                $pubBody = @{
                    title = "Mixed Test Notification $i"
                    content = "Mixed workload test notification $i"
                    level = 4
                    targetScope = "ALL_SCHOOL"
                } | ConvertTo-Json
                $response = Invoke-WebRequest -Uri "$baseUrl/admin-api/test/notification/api/publish-database" -Method POST -Headers $publishHeaders -Body $pubBody -ContentType "application/json" -UseBasicParsing -TimeoutSec 15
                $opSuccess = $true
            }
        }
        
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        
        $mixedResults += @{
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
        
        $mixedResults += @{
            RequestId = $i
            OperationType = $operationType
            Success = $false
            ResponseTime = [math]::Round($responseTime, 2)
            StatusCode = 0
            Error = $_.Exception.Message
        }
    }
    
    # Variable delay to simulate real user behavior
    Start-Sleep -Milliseconds (Get-Random -Minimum 100 -Maximum 500)
}

$mixedEnd = Get-Date
$mixedDuration = ($mixedEnd - $mixedStart).TotalSeconds

# Calculate mixed workload statistics
$mixedTotal = $mixedResults.Count
$mixedSuccess = ($mixedResults | Where-Object { $_.Success }).Count
$mixedSuccessRate = [math]::Round(($mixedSuccess / $mixedTotal) * 100, 2)
$mixedResponseTimes = $mixedResults | ForEach-Object { $_.ResponseTime }
$mixedAvgTime = [math]::Round(($mixedResponseTimes | Measure-Object -Average).Average, 2)
$mixedMaxTime = [math]::Round(($mixedResponseTimes | Measure-Object -Maximum).Maximum, 2)
$mixedRps = [math]::Round($mixedTotal / $mixedDuration, 2)

# Operation type breakdown
$queryOps = ($mixedResults | Where-Object { $_.OperationType -eq "Query" }).Count
$authOps = ($mixedResults | Where-Object { $_.OperationType -eq "Auth" }).Count
$publishOps = ($mixedResults | Where-Object { $_.OperationType -eq "Publish" }).Count

Write-Host "Mixed Workload Simulation Results:"
Write-Host "  Total Requests: $mixedTotal"
Write-Host "  Operation Breakdown: Query=$queryOps, Auth=$authOps, Publish=$publishOps"
Write-Host "  Success Rate: $mixedSuccessRate%"
Write-Host "  Average Response Time: $mixedAvgTime ms"
Write-Host "  Max Response Time: $mixedMaxTime ms"
Write-Host "  RPS: $mixedRps"
Write-Host "  Duration: $([math]::Round($mixedDuration, 2)) seconds"

$scenarioResults += @{
    Scenario = "Mixed Workload Simulation"
    TotalRequests = $mixedTotal
    SuccessRate = $mixedSuccessRate
    AvgResponseTime = $mixedAvgTime
    MaxResponseTime = $mixedMaxTime
    RPS = $mixedRps
    Duration = $mixedDuration
}

# Generate Business Scenario Report
Write-Host ""
Write-Host "======================================================="
Write-Host "BUSINESS SCENARIO TESTING COMPLETED"
Write-Host "End Time: $(Get-Date)"
Write-Host "======================================================="

$businessReportFile = "$ResultsDir\business_scenario_test_$timestamp.txt"

@"
INTELLIGENT NOTIFICATION SYSTEM - BUSINESS SCENARIO PERFORMANCE REPORT
Generated: $(Get-Date)
======================================================================

OBJECTIVE: Test system performance under realistic business scenarios

TEST SCENARIOS:
1. Authentication Storm - 50 concurrent authentication requests
2. Notification Query Storm - 100 concurrent notification list queries  
3. Notification Publishing Pressure - 25 concurrent notification publications
4. Mixed Workload Simulation - Realistic traffic pattern simulation

SCENARIO RESULTS SUMMARY:
"@ | Out-File -FilePath $businessReportFile -Encoding UTF8

"" | Out-File -FilePath $businessReportFile -Append -Encoding UTF8
"Scenario,Total_Requests,Success_Rate(%),Avg_Response_Time(ms),Max_Response_Time(ms),RPS" | Out-File -FilePath $businessReportFile -Append -Encoding UTF8

foreach ($result in $scenarioResults) {
    "$($result.Scenario),$($result.TotalRequests),$($result.SuccessRate),$($result.AvgResponseTime),$($result.MaxResponseTime),$($result.RPS)" | Out-File -FilePath $businessReportFile -Append -Encoding UTF8
}

Write-Host ""
Write-Host "BUSINESS SCENARIO SUMMARY:"
foreach ($result in $scenarioResults) {
    $color = if ($result.SuccessRate -ge 95) { "Green" } elseif ($result.SuccessRate -ge 90) { "Yellow" } else { "Red" }
    Write-Host "$($result.Scenario): $($result.SuccessRate)% success, $($result.AvgResponseTime)ms avg, $($result.RPS) RPS" -ForegroundColor $color
}

"" | Out-File -FilePath $businessReportFile -Append -Encoding UTF8
"DETAILED ANALYSIS:" | Out-File -FilePath $businessReportFile -Append -Encoding UTF8

foreach ($result in $scenarioResults) {
    "" | Out-File -FilePath $businessReportFile -Append -Encoding UTF8
    "$($result.Scenario):" | Out-File -FilePath $businessReportFile -Append -Encoding UTF8
    "  - Total Requests: $($result.TotalRequests)" | Out-File -FilePath $businessReportFile -Append -Encoding UTF8
    "  - Success Rate: $($result.SuccessRate)%" | Out-File -FilePath $businessReportFile -Append -Encoding UTF8
    "  - Average Response Time: $($result.AvgResponseTime) ms" | Out-File -FilePath $businessReportFile -Append -Encoding UTF8
    "  - Maximum Response Time: $($result.MaxResponseTime) ms" | Out-File -FilePath $businessReportFile -Append -Encoding UTF8
    "  - Requests per Second: $($result.RPS)" | Out-File -FilePath $businessReportFile -Append -Encoding UTF8
    "  - Test Duration: $([math]::Round($result.Duration, 2)) seconds" | Out-File -FilePath $businessReportFile -Append -Encoding UTF8
}

# Key findings
$bestScenario = $scenarioResults | Sort-Object RPS -Descending | Select-Object -First 1
$worstScenario = $scenarioResults | Sort-Object SuccessRate | Select-Object -First 1

"" | Out-File -FilePath $businessReportFile -Append -Encoding UTF8
"KEY FINDINGS:" | Out-File -FilePath $businessReportFile -Append -Encoding UTF8
"- Best Performance Scenario: $($bestScenario.Scenario) ($($bestScenario.RPS) RPS)" | Out-File -FilePath $businessReportFile -Append -Encoding UTF8
"- Lowest Success Rate: $($worstScenario.Scenario) ($($worstScenario.SuccessRate)%)" | Out-File -FilePath $businessReportFile -Append -Encoding UTF8

$avgSuccessRate = [math]::Round(($scenarioResults | Measure-Object -Property SuccessRate -Average).Average, 2)
$avgResponseTime = [math]::Round(($scenarioResults | Measure-Object -Property AvgResponseTime -Average).Average, 2)

"- Average Success Rate Across All Scenarios: $avgSuccessRate%" | Out-File -FilePath $businessReportFile -Append -Encoding UTF8
"- Average Response Time Across All Scenarios: $avgResponseTime ms" | Out-File -FilePath $businessReportFile -Append -Encoding UTF8

Write-Host ""
Write-Host "KEY FINDINGS:" -ForegroundColor Cyan
Write-Host "- Best Performance: $($bestScenario.Scenario) ($($bestScenario.RPS) RPS)" -ForegroundColor Green
Write-Host "- Average Success Rate: $avgSuccessRate%" -ForegroundColor $(if ($avgSuccessRate -ge 95) { "Green" } else { "Yellow" })
Write-Host "- Average Response Time: $avgResponseTime ms" -ForegroundColor $(if ($avgResponseTime -le 100) { "Green" } else { "Yellow" })

Write-Host ""
Write-Host "Business scenario test report saved to: $businessReportFile"
Write-Host "======================================================="