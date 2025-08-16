# =======================================================
# PowerShell版本系统监控脚本 (Windows原生)
# 实时监控Java服务性能
# =======================================================

param(
    [string]$OutputFile = "performance_monitor.csv",
    [int]$DurationMinutes = 10,
    [int]$IntervalSeconds = 5
)

Write-Host "=======================================================
智能通知系统性能监控
持续时间: $DurationMinutes 分钟
监控间隔: $IntervalSeconds 秒
输出文件: $OutputFile
======================================================="

# 创建CSV头部
"Timestamp,CPU_Usage(%),Memory_Usage_MB,Available_Memory_MB,Java_Processes,Port_48081_Connections,Port_48082_Connections,Java_Memory_Usage_MB" | Out-File -FilePath $OutputFile -Encoding UTF8

$endTime = (Get-Date).AddMinutes($DurationMinutes)

while ((Get-Date) -lt $endTime) {
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    
    # CPU使用率
    try {
        $cpuUsage = (Get-WmiObject -Class Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average
        $cpuUsage = [math]::Round($cpuUsage, 2)
    } catch {
        $cpuUsage = "N/A"
    }
    
    # 内存信息
    try {
        $memory = Get-WmiObject -Class Win32_OperatingSystem
        $totalMemoryMB = [math]::Round($memory.TotalVisibleMemorySize / 1024, 2)
        $availableMemoryMB = [math]::Round($memory.AvailablePhysicalMemory / 1024, 2)
        $usedMemoryMB = [math]::Round($totalMemoryMB - $availableMemoryMB, 2)
    } catch {
        $usedMemoryMB = "N/A"
        $availableMemoryMB = "N/A"
    }
    
    # Java进程数量和内存使用
    try {
        $javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
        $javaProcessCount = $javaProcesses.Count
        if ($javaProcesses) {
            $javaMemoryUsage = [math]::Round(($javaProcesses | Measure-Object WorkingSet -Sum).Sum / 1MB, 2)
        } else {
            $javaMemoryUsage = 0
        }
    } catch {
        $javaProcessCount = 0
        $javaMemoryUsage = "N/A"
    }
    
    # 端口连接数
    try {
        $port48081Connections = (netstat -an | Select-String ":48081" | Select-String "ESTABLISHED").Count
        $port48082Connections = (netstat -an | Select-String ":48082" | Select-String "ESTABLISHED").Count
    } catch {
        $port48081Connections = "N/A"
        $port48082Connections = "N/A"
    }
    
    # 输出到控制台
    Write-Host "$((Get-Date -Format 'HH:mm:ss')) - CPU: $cpuUsage%, RAM使用: ${usedMemoryMB}MB, RAM可用: ${availableMemoryMB}MB, Java进程: $javaProcessCount, Java内存: ${javaMemoryUsage}MB, 48081连接: $port48081Connections, 48082连接: $port48082Connections"
    
    # 写入CSV
    "$timestamp,$cpuUsage,$usedMemoryMB,$availableMemoryMB,$javaProcessCount,$port48081Connections,$port48082Connections,$javaMemoryUsage" | Out-File -FilePath $OutputFile -Append -Encoding UTF8
    
    Start-Sleep -Seconds $IntervalSeconds
}

Write-Host "监控完成！数据已保存至: $OutputFile"