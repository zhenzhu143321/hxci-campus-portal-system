# Simple Performance Test Script
# Testing API Response Times

param(
    [int]$TestCount = 10,
    [string]$OutputFile = "D:\ClaudeCode\AI_Web\performance-tests\results\baseline_test.csv"
)

$baseUrl = "http://localhost:48081"
$mockUrl = "http://localhost:48082"

Write-Host "Starting Baseline Performance Test..."
Write-Host "Test Count: $TestCount"
Write-Host "Output File: $OutputFile"

# Get test token
$authBody = @{
    employeeId = "PRINCIPAL_001"
    name = "Principal-Zhang"  
    password = "admin123"
} | ConvertTo-Json

$authResponse = Invoke-RestMethod -Uri "$mockUrl/mock-school-api/auth/authenticate" -Method POST -Body $authBody -ContentType "application/json"
$testToken = $authResponse.data.token

Write-Host "Token obtained successfully"

# Create CSV header
"Test_Type,Request_ID,Start_Time,End_Time,Response_Time_ms,HTTP_Status,Success" | Out-File -FilePath $OutputFile -Encoding UTF8

$results = @()

# Test Health Check
Write-Host "Testing Health Check API..."
for ($i = 1; $i -le $TestCount; $i++) {
    $startTime = Get-Date
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/admin-api/test/notification/api/ping" -UseBasicParsing -TimeoutSec 30
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        
        $result = "Health_Check,$i,$($startTime.ToString('yyyy-MM-dd HH:mm:ss.fff')),$($endTime.ToString('yyyy-MM-dd HH:mm:ss.fff')),$responseTime,$($response.StatusCode),True"
        $results += $result
        $result | Out-File -FilePath $OutputFile -Append -Encoding UTF8
    } catch {
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        $result = "Health_Check,$i,$($startTime.ToString('yyyy-MM-dd HH:mm:ss.fff')),$($endTime.ToString('yyyy-MM-dd HH:mm:ss.fff')),$responseTime,0,False"
        $results += $result
        $result | Out-File -FilePath $OutputFile -Append -Encoding UTF8
    }
    Start-Sleep -Milliseconds 100
}

# Test Authentication
Write-Host "Testing Authentication API..."
for ($i = 1; $i -le $TestCount; $i++) {
    $startTime = Get-Date
    try {
        $response = Invoke-WebRequest -Uri "$mockUrl/mock-school-api/auth/authenticate" -Method POST -Body $authBody -ContentType "application/json" -UseBasicParsing -TimeoutSec 30
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        
        $result = "Authentication,$i,$($startTime.ToString('yyyy-MM-dd HH:mm:ss.fff')),$($endTime.ToString('yyyy-MM-dd HH:mm:ss.fff')),$responseTime,$($response.StatusCode),True"
        $results += $result
        $result | Out-File -FilePath $OutputFile -Append -Encoding UTF8
    } catch {
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        $result = "Authentication,$i,$($startTime.ToString('yyyy-MM-dd HH:mm:ss.fff')),$($endTime.ToString('yyyy-MM-dd HH:mm:ss.fff')),$responseTime,0,False"
        $results += $result
        $result | Out-File -FilePath $OutputFile -Append -Encoding UTF8
    }
    Start-Sleep -Milliseconds 100
}

# Test Notification List
Write-Host "Testing Notification List API..."
$headers = @{
    "Authorization" = "Bearer $testToken"
    "tenant-id" = "1"
}

for ($i = 1; $i -le $TestCount; $i++) {
    $startTime = Get-Date
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/admin-api/test/notification/api/list" -Headers $headers -UseBasicParsing -TimeoutSec 30
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        
        $result = "Notification_List,$i,$($startTime.ToString('yyyy-MM-dd HH:mm:ss.fff')),$($endTime.ToString('yyyy-MM-dd HH:mm:ss.fff')),$responseTime,$($response.StatusCode),True"
        $results += $result
        $result | Out-File -FilePath $OutputFile -Append -Encoding UTF8
    } catch {
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        $result = "Notification_List,$i,$($startTime.ToString('yyyy-MM-dd HH:mm:ss.fff')),$($endTime.ToString('yyyy-MM-dd HH:mm:ss.fff')),$responseTime,0,False"
        $results += $result
        $result | Out-File -FilePath $OutputFile -Append -Encoding UTF8
    }
    Start-Sleep -Milliseconds 100
}

# Test Notification Publish
Write-Host "Testing Notification Publish API..."
for ($i = 1; $i -le $TestCount; $i++) {
    $publishBody = @{
        title = "Baseline Test Notification $i"
        content = "Performance test notification content $i"
        level = 4
        targetScope = "ALL_SCHOOL"
    } | ConvertTo-Json
    
    $startTime = Get-Date
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/admin-api/test/notification/api/publish-database" -Method POST -Headers $headers -Body $publishBody -ContentType "application/json" -UseBasicParsing -TimeoutSec 30
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        
        $result = "Notification_Publish,$i,$($startTime.ToString('yyyy-MM-dd HH:mm:ss.fff')),$($endTime.ToString('yyyy-MM-dd HH:mm:ss.fff')),$responseTime,$($response.StatusCode),True"
        $results += $result
        $result | Out-File -FilePath $OutputFile -Append -Encoding UTF8
    } catch {
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        $result = "Notification_Publish,$i,$($startTime.ToString('yyyy-MM-dd HH:mm:ss.fff')),$($endTime.ToString('yyyy-MM-dd HH:mm:ss.fff')),$responseTime,0,False"
        $results += $result
        $result | Out-File -FilePath $OutputFile -Append -Encoding UTF8
    }
    Start-Sleep -Milliseconds 200
}

Write-Host "Baseline test completed. Results saved to: $OutputFile"

# Calculate statistics
$csvData = Import-Csv $OutputFile
$healthCheck = $csvData | Where-Object { $_.Test_Type -eq "Health_Check" }
$auth = $csvData | Where-Object { $_.Test_Type -eq "Authentication" }  
$notifList = $csvData | Where-Object { $_.Test_Type -eq "Notification_List" }
$notifPublish = $csvData | Where-Object { $_.Test_Type -eq "Notification_Publish" }

Write-Host ""
Write-Host "=== BASELINE PERFORMANCE RESULTS ==="
Write-Host ""

function Show-Stats {
    param($data, $apiName)
    if ($data.Count -gt 0) {
        $responseTimes = $data.Response_Time_ms | ForEach-Object { [double]$_ }
        $successCount = ($data | Where-Object { $_.Success -eq "True" }).Count
        $successRate = [math]::Round(($successCount / $data.Count) * 100, 2)
        
        Write-Host "$apiName API Statistics:"
        Write-Host "  Success Rate: $successRate% ($successCount/$($data.Count))"
        Write-Host "  Average Response Time: $([math]::Round(($responseTimes | Measure-Object -Average).Average, 2)) ms"
        Write-Host "  Min Response Time: $([math]::Round(($responseTimes | Measure-Object -Minimum).Minimum, 2)) ms"
        Write-Host "  Max Response Time: $([math]::Round(($responseTimes | Measure-Object -Maximum).Maximum, 2)) ms"
        Write-Host ""
    }
}

Show-Stats $healthCheck "Health Check"
Show-Stats $auth "Authentication"
Show-Stats $notifList "Notification List"
Show-Stats $notifPublish "Notification Publish"