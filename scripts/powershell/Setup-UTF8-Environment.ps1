# ============================================================================
# PowerShell UTF-8 编码环境配置脚本
# 适用于哈尔滨信息工程学院校园门户系统开发环境
# ============================================================================

Write-Host "🌐 配置PowerShell UTF-8编码环境..." -ForegroundColor Cyan

# 步骤1: 设置当前会话的编码
Write-Host "📝 步骤1: 设置当前会话编码为UTF-8" -ForegroundColor Yellow
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8

# 验证当前编码设置
Write-Host "✅ 当前输出编码: $($OutputEncoding.EncodingName)" -ForegroundColor Green
Write-Host "✅ 控制台输出编码: $([Console]::OutputEncoding.EncodingName)" -ForegroundColor Green
Write-Host "✅ 控制台输入编码: $([Console]::InputEncoding.EncodingName)" -ForegroundColor Green

# 步骤2: 创建PowerShell配置文件（永久设置）
Write-Host "📝 步骤2: 配置PowerShell配置文件" -ForegroundColor Yellow

$profilePath = $PROFILE.CurrentUserAllHosts
$profileDir = Split-Path $profilePath -Parent

# 确保配置文件目录存在
if (!(Test-Path $profileDir)) {
    New-Item -ItemType Directory -Path $profileDir -Force | Out-Null
    Write-Host "📁 创建配置文件目录: $profileDir" -ForegroundColor Blue
}

# UTF-8配置内容
$utf8Config = @"
# 哈尔滨信息工程学院校园门户系统 - PowerShell UTF-8配置
# 自动配置UTF-8编码，解决中文显示问题
`$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8

# 校园门户系统开发环境变量
`$env:CAMPUS_PORTAL_HOME = "D:\ClaudeCode\AI_Web"
`$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"

# 定义校园门户系统快捷命令
function Start-CampusPortal {
    Write-Host "🏫 启动哈尔滨信息工程学院校园门户系统..." -ForegroundColor Cyan
    Set-Location "`$env:CAMPUS_PORTAL_HOME"
    Write-Host "📍 当前目录: `$(Get-Location)" -ForegroundColor Green
}

function Test-NotificationAPI {
    param(
        [string]`$Level = "4",
        [string]`$Title = "测试通知",
        [string]`$Content = "这是一条中文测试通知，用于验证UTF-8编码是否正常工作。"
    )
    
    Write-Host "📢 测试通知API - Level `$Level" -ForegroundColor Cyan
    
    # 构造测试JSON数据
    `$jsonData = @{
        title = `$Title
        content = `$Content
        level = [int]`$Level
        targetScope = "SCHOOL_WIDE"
    } | ConvertTo-Json -Depth 3
    
    # 保存到临时文件（UTF-8编码）
    `$tempFile = "`$env:TEMP\campus-portal-test.json"
    `$jsonData | Out-File -FilePath `$tempFile -Encoding UTF8
    
    Write-Host "📝 测试数据已保存到: `$tempFile" -ForegroundColor Blue
    Write-Host "🌐 JSON数据预览:" -ForegroundColor Yellow
    Write-Host `$jsonData -ForegroundColor White
    
    # 提示用户如何使用curl测试
    Write-Host "`n🚀 使用以下命令测试API:" -ForegroundColor Green
    Write-Host "curl.exe -X POST 'http://localhost:48081/admin-api/test/notification/api/publish-database' \" -ForegroundColor White
    Write-Host "  -H 'Content-Type: application/json; charset=utf-8' \" -ForegroundColor White
    Write-Host "  -H 'Authorization: Bearer YOUR_TOKEN' \" -ForegroundColor White
    Write-Host "  -H 'tenant-id: 1' \" -ForegroundColor White
    Write-Host "  --data-binary '@`$tempFile'" -ForegroundColor White
}

# 别名定义
Set-Alias -Name campus -Value Start-CampusPortal
Set-Alias -Name testapi -Value Test-NotificationAPI

Write-Host "✅ 校园门户系统PowerShell环境已配置" -ForegroundColor Green
"@

# 检查是否已存在配置文件
if (Test-Path $profilePath) {
    $existingContent = Get-Content $profilePath -Raw -ErrorAction SilentlyContinue
    if ($existingContent -and $existingContent.Contains("校园门户系统")) {
        Write-Host "⚠️  PowerShell配置文件已存在校园门户配置，跳过写入" -ForegroundColor Orange
    } else {
        # 追加到现有配置文件
        Add-Content -Path $profilePath -Value "`n`n$utf8Config" -Encoding UTF8
        Write-Host "📝 UTF-8配置已追加到PowerShell配置文件: $profilePath" -ForegroundColor Green
    }
} else {
    # 创建新的配置文件
    $utf8Config | Out-File -FilePath $profilePath -Encoding UTF8
    Write-Host "📝 新的PowerShell配置文件已创建: $profilePath" -ForegroundColor Green
}

# 步骤3: 创建测试脚本
Write-Host "📝 步骤3: 创建中文测试脚本" -ForegroundColor Yellow

$testScriptPath = "D:\ClaudeCode\AI_Web\scripts\powershell\Test-Chinese-Display.ps1"
$testScript = @"
# 中文显示测试脚本
# 用于验证PowerShell UTF-8编码配置是否正确

Write-Host "🇨🇳 中文显示测试开始..." -ForegroundColor Cyan

# 测试1: 基本中文显示
Write-Host "📝 测试1: 基本中文字符显示" -ForegroundColor Yellow
Write-Host "哈尔滨信息工程学院校园门户系统" -ForegroundColor Green
Write-Host "学生姓名：张三、李四、王五" -ForegroundColor Blue
Write-Host "通知内容：今日下午2点在主楼101教室召开学生会议" -ForegroundColor Magenta

# 测试2: JSON格式中文数据
Write-Host "`n📝 测试2: JSON格式中文数据" -ForegroundColor Yellow
`$testJson = @{
    "校名" = "哈尔滨信息工程学院"
    "通知标题" = "期末考试安排通知"
    "通知内容" = "各位同学请注意，期末考试将于下周开始，请做好复习准备。"
    "发布者" = "教务处"
} | ConvertTo-Json -Depth 3

Write-Host `$testJson -ForegroundColor White

# 测试3: 特殊中文字符
Write-Host "`n📝 测试3: 特殊中文字符和符号" -ForegroundColor Yellow
Write-Host "特殊字符：①②③④⑤ 、，。；：？！" -ForegroundColor Cyan
Write-Host "货币符号：￥ 度量单位：℃ ℉ ㎡ ㎞" -ForegroundColor Cyan

# 测试4: 编码信息显示
Write-Host "`n📝 测试4: 当前编码设置" -ForegroundColor Yellow
Write-Host "输出编码: `$(`$OutputEncoding.EncodingName)" -ForegroundColor Green
Write-Host "控制台输出编码: `$([Console]::OutputEncoding.EncodingName)" -ForegroundColor Green
Write-Host "控制台输入编码: `$([Console]::InputEncoding.EncodingName)" -ForegroundColor Green

Write-Host "`n✅ 中文显示测试完成！" -ForegroundColor Green
Write-Host "如果以上中文字符显示正常，说明UTF-8编码配置成功。" -ForegroundColor Cyan
"@

$testScript | Out-File -FilePath $testScriptPath -Encoding UTF8
Write-Host "📝 中文测试脚本已创建: $testScriptPath" -ForegroundColor Green

# 步骤4: 测试当前配置
Write-Host "📝 步骤4: 测试当前UTF-8配置" -ForegroundColor Yellow
Write-Host "🇨🇳 中文测试：哈尔滨信息工程学院校园门户系统" -ForegroundColor Green
Write-Host "📢 通知测试：今天天气很好，适合学习编程！" -ForegroundColor Blue

# 显示完成信息
Write-Host "`n🎉 PowerShell UTF-8编码环境配置完成！" -ForegroundColor Green
Write-Host "📋 配置总结:" -ForegroundColor Cyan
Write-Host "  ✅ 当前会话UTF-8编码已设置" -ForegroundColor White
Write-Host "  ✅ PowerShell配置文件已更新 ($profilePath)" -ForegroundColor White
Write-Host "  ✅ 中文测试脚本已创建 ($testScriptPath)" -ForegroundColor White
Write-Host "  ✅ 校园门户系统快捷命令已配置" -ForegroundColor White

Write-Host "`n🚀 使用方法:" -ForegroundColor Cyan
Write-Host "  • 重启PowerShell窗口以使配置永久生效" -ForegroundColor White
Write-Host "  • 运行 'campus' 命令快速切换到项目目录" -ForegroundColor White
Write-Host "  • 运行 'testapi' 命令生成API测试数据" -ForegroundColor White
Write-Host "  • 运行 '$testScriptPath' 测试中文显示" -ForegroundColor White

Write-Host "`n📚 API测试示例（UTF-8编码）:" -ForegroundColor Cyan
Write-Host "curl.exe -X POST 'http://localhost:48081/admin-api/test/notification/api/publish-database' \\" -ForegroundColor White
Write-Host "  -H 'Content-Type: application/json; charset=utf-8' \\" -ForegroundColor White  
Write-Host "  -H 'Authorization: Bearer YOUR_TOKEN' \\" -ForegroundColor White
Write-Host "  -H 'tenant-id: 1' \\" -ForegroundColor White
Write-Host "  -d '{\"title\":\"中文测试通知\",\"content\":\"这是一条包含中文的测试通知\",\"level\":4}'" -ForegroundColor White