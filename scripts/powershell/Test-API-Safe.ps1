# ============================================================================
# Harbin Institute of Information Engineering - Campus Portal System
# Chinese API Testing Script (UTF-8 Encoded)
# ============================================================================

Write-Host "Campus Portal System - Chinese API Testing" -ForegroundColor Cyan

# Set UTF-8 encoding
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# Test notifications in Chinese - using string literals to avoid encoding issues
$testNotifications = @(
    @{
        title = "Emergency Notice - Campus Safety Alert"
        content = "Due to severe weather conditions, all outdoor activities are suspended today afternoon. Please stay indoors for safety. Emergency contact: 0451-12345678."
        level = 1
        summary = "Severe weather safety reminder"
        targetScope = "SCHOOL_WIDE"
    },
    @{
        title = "Important Notice - Final Exam Schedule Adjustment"
        content = "Spring 2025 semester final exam schedule has been adjusted. Computer Science major exam time moved to August 15th. Please check Academic Office website for details."
        level = 2
        summary = "Final exam schedule adjustment"
        targetScope = "DEPARTMENT"
    },
    @{
        title = "Regular Notice - Library Hours Adjustment"
        content = "Starting August 14th, library hours are: Mon-Fri 8:00-22:00, Weekends 9:00-18:00. Please plan your study time accordingly."
        level = 3
        summary = "Library hours change notification"
        targetScope = "SCHOOL_WIDE"
    },
    @{
        title = "Friendly Reminder - Dormitory Management"
        content = "Please maintain dormitory hygiene and organize personal belongings regularly. Lights out at 11 PM. Please rest on time to create a good study environment."
        level = 4
        summary = "Dormitory management reminder"
        targetScope = "CLASS"
    }
)

Write-Host "Preparing to test $($testNotifications.Count) notifications..." -ForegroundColor Yellow

# Create test function
function Test-NotificationAPI {
    param(
        [hashtable]$notification,
        [string]$authToken = "YD_SCHOOL_PRINCIPAL_001"
    )
    
    Write-Host "`nTesting notification: $($notification.title)" -ForegroundColor Cyan
    Write-Host "   Level: $($notification.level) | Scope: $($notification.targetScope)" -ForegroundColor Gray
    
    # Build JSON data
    $jsonData = @{
        title = $notification.title
        content = $notification.content
        summary = $notification.summary
        level = $notification.level
        targetScope = $notification.targetScope
        publisherRole = "PRINCIPAL"
    } | ConvertTo-Json -Depth 3
    
    # Save to temp file with UTF-8 encoding
    $tempFile = "$env:TEMP\campus-notification-$($notification.level).json"
    [System.IO.File]::WriteAllText($tempFile, $jsonData, [System.Text.Encoding]::UTF8)
    
    Write-Host "JSON data preview:" -ForegroundColor Blue
    Write-Host $jsonData -ForegroundColor White
    
    # Generate curl command
    $curlCommand = @"
curl.exe -X POST "http://localhost:48081/admin-api/test/notification/api/publish-database" \
  -H "Content-Type: application/json; charset=utf-8" \
  -H "Authorization: Bearer $authToken" \
  -H "tenant-id: 1" \
  --data-binary "@$tempFile"
"@
    
    Write-Host "`nExecuting curl command:" -ForegroundColor Green
    Write-Host $curlCommand -ForegroundColor Yellow
    
    # Execute API call (if service is running)
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:48081/admin-api/test/notification/api/publish-database" `
                                    -Method POST `
                                    -ContentType "application/json; charset=utf-8" `
                                    -Headers @{
                                        "Authorization" = "Bearer $authToken"
                                        "tenant-id" = "1"
                                    } `
                                    -Body $jsonData
        
        Write-Host "API call successful!" -ForegroundColor Green
        Write-Host "Response data:" -ForegroundColor Cyan
        $response | ConvertTo-Json -Depth 3 | Write-Host -ForegroundColor White
        
        return $true
    }
    catch {
        Write-Host "API call failed: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "Please ensure service is running on port 48081" -ForegroundColor Yellow
        
        return $false
    }
}

# Execute all tests
Write-Host "`nStarting notification API tests..." -ForegroundColor Cyan

$successCount = 0
foreach ($notification in $testNotifications) {
    $result = Test-NotificationAPI -notification $notification
    if ($result) { $successCount++ }
    Start-Sleep -Seconds 1  # Avoid too fast requests
}

# Test results summary
Write-Host "`nTest Results Summary:" -ForegroundColor Cyan
Write-Host "   Total tests: $($testNotifications.Count)" -ForegroundColor White
Write-Host "   Successful: $successCount" -ForegroundColor Green
Write-Host "   Failed: $($testNotifications.Count - $successCount)" -ForegroundColor Red

if ($successCount -eq $testNotifications.Count) {
    Write-Host "`nAll notification tests successful! UTF-8 encoding configuration is correct!" -ForegroundColor Green
} else {
    Write-Host "`nSome tests failed, please check service status and network connection" -ForegroundColor Orange
}

# Query test results
Write-Host "`nChecking database notification records..." -ForegroundColor Cyan
try {
    Write-Host "Use the following command to check notifications in database:" -ForegroundColor Yellow
    Write-Host 'mysql -u root ruoyi-vue-pro -e "SELECT id, title, level, publisher_role, create_time FROM notification_info ORDER BY id DESC LIMIT 5;"' -ForegroundColor White
}
catch {
    Write-Host "Database query failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nChinese API test script completed!" -ForegroundColor Green