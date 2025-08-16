# ============================================================================
# PowerShell UTF-8 Configuration for Campus Portal System
# Harbin Institute of Information Engineering
# ============================================================================

# Set UTF-8 encoding for current session
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8

# Campus Portal System environment variables
$env:CAMPUS_PORTAL_HOME = "D:\ClaudeCode\AI_Web"
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"

# Quick navigation function
function Start-CampusPortal {
    Write-Host "Starting Campus Portal System..." -ForegroundColor Cyan
    Set-Location "$env:CAMPUS_PORTAL_HOME"
    Write-Host "Current directory: $(Get-Location)" -ForegroundColor Green
}

# API testing function (English content to avoid encoding issues)
function Test-NotificationAPI {
    param(
        [string]$Level = "4",
        [string]$Title = "Test Notification",
        [string]$Content = "This is a test notification to verify UTF-8 encoding functionality."
    )
    
    Write-Host "Testing Notification API - Level $Level" -ForegroundColor Cyan
    
    # Build test JSON data
    $jsonData = @{
        title = $Title
        content = $Content
        level = [int]$Level
        targetScope = "SCHOOL_WIDE"
        publisherRole = "PRINCIPAL"
    } | ConvertTo-Json -Depth 3
    
    # Save to temp file with UTF-8 encoding
    $tempFile = "$env:TEMP\campus-portal-test.json"
    [System.IO.File]::WriteAllText($tempFile, $jsonData, [System.Text.Encoding]::UTF8)
    
    Write-Host "Test data saved to: $tempFile" -ForegroundColor Blue
    Write-Host "JSON data preview:" -ForegroundColor Yellow
    Write-Host $jsonData -ForegroundColor White
    
    # Show curl command example
    Write-Host "`nUse the following command to test API:" -ForegroundColor Green
    Write-Host "curl.exe -X POST 'http://localhost:48081/admin-api/test/notification/api/publish-database' \\" -ForegroundColor White
    Write-Host "  -H 'Content-Type: application/json; charset=utf-8' \\" -ForegroundColor White
    Write-Host "  -H 'Authorization: Bearer YOUR_TOKEN' \\" -ForegroundColor White
    Write-Host "  -H 'tenant-id: 1' \\" -ForegroundColor White
    Write-Host "  --data-binary '@$tempFile'" -ForegroundColor White
}

# Create aliases
Set-Alias -Name campus -Value Start-CampusPortal
Set-Alias -Name testapi -Value Test-NotificationAPI

Write-Host "Campus Portal System PowerShell environment configured successfully!" -ForegroundColor Green