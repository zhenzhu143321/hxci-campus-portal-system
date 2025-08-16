# =======================================================
# 智能通知系统并发负载测试脚本
# 使用PowerShell进行多线程并发测试
# =======================================================

param(
    [int]$ConcurrentUsers = 10,
    [int]$TestDurationSeconds = 60,
    [string]$TestType = "mixed",  # health, auth, publish, query, mixed
    [string]$OutputDir = "D:\ClaudeCode\AI_Web\performance-tests\results"
)

# 创建输出目录
if (!(Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$resultFile = "$OutputDir\concurrent_test_${TestType}_${ConcurrentUsers}users_$timestamp.csv"

Write-Host "=======================================================
智能通知系统并发负载测试
并发用户数: $ConcurrentUsers
测试持续时间: $TestDurationSeconds 秒
测试类型: $TestType
结果文件: $resultFile
======================================================="

# 测试配置
$baseUrl = "http://localhost:48081"
$mockUrl = "http://localhost:48082"

# 预定义的测试Token (校长权限)
$testToken = $null

# 获取测试Token
function Get-TestToken {
    try {
        $authBody = @{
            employeeId = "PRINCIPAL_001"
            name = "Principal-Zhang"
            password = "admin123"
        } | ConvertTo-Json

        $response = Invoke-RestMethod -Uri "$mockUrl/mock-school-api/auth/authenticate" -Method POST -Body $authBody -ContentType "application/json" -TimeoutSec 30
        
        if ($response.code -eq 0 -and $response.data.token) {
            return $response.data.token
        } else {
            Write-Host "Token获取失败: $($response.msg)"
            return $null
        }
    } catch {
        Write-Host "Token获取异常: $($_.Exception.Message)"
        return $null
    }
}

# 执行单个API请求的函数
function Invoke-ApiRequest {
    param(
        [string]$Url,
        [string]$Method = "GET",
        [hashtable]$Headers = @{},
        [string]$Body = $null,
        [int]$RequestId
    )
    
    $startTime = Get-Date
    try {
        if ($Method -eq "POST" -and $Body) {
            $response = Invoke-WebRequest -Uri $Url -Method $Method -Headers $Headers -Body $Body -ContentType "application/json" -TimeoutSec 30 -UseBasicParsing
        } else {
            $response = Invoke-WebRequest -Uri $Url -Method $Method -Headers $Headers -TimeoutSec 30 -UseBasicParsing
        }
        
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        
        return @{
            RequestId = $RequestId
            Timestamp = $startTime.ToString("yyyy-MM-dd HH:mm:ss.fff")
            StatusCode = $response.StatusCode
            ResponseTime = [math]::Round($responseTime, 2)
            Success = $true
            Error = ""
            ContentLength = $response.Content.Length
        }
    } catch {
        $endTime = Get-Date
        $responseTime = ($endTime - $startTime).TotalMilliseconds
        
        return @{
            RequestId = $RequestId
            Timestamp = $startTime.ToString("yyyy-MM-dd HH:mm:ss.fff")
            StatusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { 0 }
            ResponseTime = [math]::Round($responseTime, 2)
            Success = $false
            Error = $_.Exception.Message
            ContentLength = 0
        }
    }
}

# 不同类型的测试函数
function Test-HealthCheck {
    param([int]$RequestId)
    return Invoke-ApiRequest -Url "$baseUrl/admin-api/test/notification/api/ping" -RequestId $RequestId
}

function Test-Authentication {
    param([int]$RequestId)
    $body = @{
        employeeId = "TEACHER_001"
        name = "Teacher-Wang"
        password = "admin123"
    } | ConvertTo-Json
    
    return Invoke-ApiRequest -Url "$mockUrl/mock-school-api/auth/authenticate" -Method "POST" -Body $body -RequestId $RequestId
}

function Test-NotificationPublish {
    param([int]$RequestId, [string]$Token)
    $headers = @{
        "Authorization" = "Bearer $Token"
        "tenant-id" = "1"
    }
    
    $body = @{
        title = "性能测试通知 #$RequestId"
        content = "这是并发测试通知，请求ID: $RequestId，时间戳: $(Get-Date)"
        level = 4
        targetScope = "ALL_SCHOOL"
    } | ConvertTo-Json
    
    return Invoke-ApiRequest -Url "$baseUrl/admin-api/test/notification/api/publish-database" -Method "POST" -Headers $headers -Body $body -RequestId $RequestId
}

function Test-NotificationQuery {
    param([int]$RequestId, [string]$Token)
    $headers = @{
        "Authorization" = "Bearer $Token"
        "tenant-id" = "1"
    }
    
    return Invoke-ApiRequest -Url "$baseUrl/admin-api/test/notification/api/list" -Headers $headers -RequestId $RequestId
}

# 获取测试Token
Write-Host "获取测试Token..."
$testToken = Get-TestToken
if (-not $testToken) {
    Write-Host "无法获取测试Token，退出测试" -ForegroundColor Red
    exit 1
}
Write-Host "Token获取成功"

# 创建结果收集器
$results = [System.Collections.Concurrent.ConcurrentBag[object]]::new()

# 创建CSV头部
"RequestId,Timestamp,TestType,StatusCode,ResponseTime_ms,Success,Error,ContentLength" | Out-File -FilePath $resultFile -Encoding UTF8

Write-Host "开始并发测试..."
$testStartTime = Get-Date

# 创建并启动并发任务
$jobs = @()
for ($i = 1; $i -le $ConcurrentUsers; $i++) {
    $job = Start-Job -ScriptBlock {
        param($UserId, $TestType, $TestDurationSeconds, $BaseUrl, $MockUrl, $TestToken)
        
        # 在Job中重新定义函数
        function Invoke-ApiRequest {
            param(
                [string]$Url,
                [string]$Method = "GET",
                [hashtable]$Headers = @{},
                [string]$Body = $null,
                [int]$RequestId
            )
            
            $startTime = Get-Date
            try {
                if ($Method -eq "POST" -and $Body) {
                    $response = Invoke-WebRequest -Uri $Url -Method $Method -Headers $Headers -Body $Body -ContentType "application/json" -TimeoutSec 30 -UseBasicParsing
                } else {
                    $response = Invoke-WebRequest -Uri $Url -Method $Method -Headers $Headers -TimeoutSec 30 -UseBasicParsing
                }
                
                $endTime = Get-Date
                $responseTime = ($endTime - $startTime).TotalMilliseconds
                
                return @{
                    RequestId = $RequestId
                    Timestamp = $startTime.ToString("yyyy-MM-dd HH:mm:ss.fff")
                    StatusCode = $response.StatusCode
                    ResponseTime = [math]::Round($responseTime, 2)
                    Success = $true
                    Error = ""
                    ContentLength = $response.Content.Length
                }
            } catch {
                $endTime = Get-Date
                $responseTime = ($endTime - $startTime).TotalMilliseconds
                
                return @{
                    RequestId = $RequestId
                    Timestamp = $startTime.ToString("yyyy-MM-dd HH:mm:ss.fff")
                    StatusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { 0 }
                    ResponseTime = [math]::Round($responseTime, 2)
                    Success = $false
                    Error = $_.Exception.Message
                    ContentLength = 0
                }
            }
        }
        
        function Test-HealthCheck {
            param([int]$RequestId, [string]$BaseUrl)
            return Invoke-ApiRequest -Url "$BaseUrl/admin-api/test/notification/api/ping" -RequestId $RequestId
        }
        
        function Test-Authentication {
            param([int]$RequestId, [string]$MockUrl)
            $body = @{
                employeeId = "TEACHER_001"
                name = "Teacher-Wang"
                password = "admin123"
            } | ConvertTo-Json
            
            return Invoke-ApiRequest -Url "$MockUrl/mock-school-api/auth/authenticate" -Method "POST" -Body $body -RequestId $RequestId
        }
        
        function Test-NotificationQuery {
            param([int]$RequestId, [string]$BaseUrl, [string]$Token)
            $headers = @{
                "Authorization" = "Bearer $Token"
                "tenant-id" = "1"
            }
            
            return Invoke-ApiRequest -Url "$BaseUrl/admin-api/test/notification/api/list" -Headers $headers -RequestId $RequestId
        }
        
        # 用户并发测试逻辑
        $userResults = @()
        $endTime = (Get-Date).AddSeconds($TestDurationSeconds)
        $requestCounter = 0
        
        while ((Get-Date) -lt $endTime) {
            $requestCounter++
            $requestId = "${UserId}_${requestCounter}"
            
            $result = switch ($TestType) {
                "health" { Test-HealthCheck -RequestId $requestId -BaseUrl $BaseUrl }
                "auth" { Test-Authentication -RequestId $requestId -MockUrl $MockUrl }
                "query" { Test-NotificationQuery -RequestId $requestId -BaseUrl $BaseUrl -Token $TestToken }
                "mixed" {
                    $testChoice = Get-Random -Minimum 1 -Maximum 4
                    switch ($testChoice) {
                        1 { Test-HealthCheck -RequestId $requestId -BaseUrl $BaseUrl }
                        2 { Test-Authentication -RequestId $requestId -MockUrl $MockUrl }
                        3 { Test-NotificationQuery -RequestId $requestId -BaseUrl $BaseUrl -Token $TestToken }
                    }
                }
                default { Test-HealthCheck -RequestId $requestId -BaseUrl $BaseUrl }
            }
            
            $result.TestType = $TestType
            $userResults += $result
            
            # 随机等待时间模拟真实用户行为
            Start-Sleep -Milliseconds (Get-Random -Minimum 100 -Maximum 1000)
        }
        
        return $userResults
    } -ArgumentList $i, $TestType, $TestDurationSeconds, $baseUrl, $mockUrl, $testToken
    
    $jobs += $job
}

# 等待所有任务完成
Write-Host "等待所有并发任务完成..."
$jobs | Wait-Job | Out-Null

# 收集结果
$allResults = @()
foreach ($job in $jobs) {
    $jobResults = Receive-Job -Job $job
    $allResults += $jobResults
    Remove-Job -Job $job
}

# 将结果写入CSV
foreach ($result in $allResults) {
    "$($result.RequestId),$($result.Timestamp),$($result.TestType),$($result.StatusCode),$($result.ResponseTime),$($result.Success),$($result.Error),$($result.ContentLength)" | Out-File -FilePath $resultFile -Append -Encoding UTF8
}

$testEndTime = Get-Date
$totalTestTime = ($testEndTime - $testStartTime).TotalSeconds

# 计算统计信息
$totalRequests = $allResults.Count
$successfulRequests = ($allResults | Where-Object { $_.Success }).Count
$failedRequests = $totalRequests - $successfulRequests
$successRate = [math]::Round(($successfulRequests / $totalRequests) * 100, 2)

$responseTimes = $allResults | ForEach-Object { $_.ResponseTime }
$avgResponseTime = [math]::Round(($responseTimes | Measure-Object -Average).Average, 2)
$minResponseTime = [math]::Round(($responseTimes | Measure-Object -Minimum).Minimum, 2)
$maxResponseTime = [math]::Round(($responseTimes | Measure-Object -Maximum).Maximum, 2)

# 计算百分位数
$sortedTimes = $responseTimes | Sort-Object
$count = $sortedTimes.Count
$p95Index = [math]::Floor($count * 0.95) - 1
$p99Index = [math]::Floor($count * 0.99) - 1
$p95ResponseTime = if ($p95Index -ge 0) { [math]::Round($sortedTimes[$p95Index], 2) } else { 0 }
$p99ResponseTime = if ($p99Index -ge 0) { [math]::Round($sortedTimes[$p99Index], 2) } else { 0 }

$rps = [math]::Round($totalRequests / $totalTestTime, 2)

# 输出测试结果
Write-Host ""
Write-Host "======================================================="
Write-Host "并发负载测试完成"
Write-Host "======================================================="
Write-Host "测试配置:"
Write-Host "  - 并发用户数: $ConcurrentUsers"
Write-Host "  - 测试类型: $TestType"
Write-Host "  - 测试持续时间: $TestDurationSeconds 秒"
Write-Host "  - 实际运行时间: $([math]::Round($totalTestTime, 2)) 秒"
Write-Host ""
Write-Host "请求统计:"
Write-Host "  - 总请求数: $totalRequests"
Write-Host "  - 成功请求: $successfulRequests"
Write-Host "  - 失败请求: $failedRequests"
Write-Host "  - 成功率: $successRate%"
Write-Host "  - 平均RPS: $rps"
Write-Host ""
Write-Host "响应时间统计 (毫秒):"
Write-Host "  - 平均响应时间: $avgResponseTime ms"
Write-Host "  - 最小响应时间: $minResponseTime ms"
Write-Host "  - 最大响应时间: $maxResponseTime ms"
Write-Host "  - 95%响应时间: $p95ResponseTime ms"
Write-Host "  - 99%响应时间: $p99ResponseTime ms"
Write-Host ""
Write-Host "详细结果已保存至: $resultFile"
Write-Host "======================================================="

# 保存测试总结
$summaryFile = "$OutputDir\test_summary_${TestType}_${ConcurrentUsers}users_$timestamp.txt"
@"
智能通知系统并发负载测试总结
测试时间: $(Get-Date)
======================================================

测试配置:
- 并发用户数: $ConcurrentUsers
- 测试类型: $TestType  
- 测试持续时间: $TestDurationSeconds 秒
- 实际运行时间: $([math]::Round($totalTestTime, 2)) 秒

请求统计:
- 总请求数: $totalRequests
- 成功请求: $successfulRequests
- 失败请求: $failedRequests
- 成功率: $successRate%
- 平均RPS: $rps

响应时间统计 (毫秒):
- 平均响应时间: $avgResponseTime ms
- 最小响应时间: $minResponseTime ms
- 最大响应时间: $maxResponseTime ms
- 95%响应时间: $p95ResponseTime ms
- 99%响应时间: $p99ResponseTime ms

详细数据文件: $resultFile
"@ | Out-File -FilePath $summaryFile -Encoding UTF8

Write-Host "测试总结已保存至: $summaryFile"