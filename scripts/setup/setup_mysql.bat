@echo off
chcp 65001 >nul 2>&1

echo ==========================================
echo 配置MySQL数据库
echo ==========================================

REM 设置MySQL路径
set "MYSQL_PATH=C:\tools\mysql\current\bin"
if not exist "%MYSQL_PATH%" (
    set "MYSQL_PATH=C:\Program Files\MySQL\MySQL Server 9.2\bin"
)
if not exist "%MYSQL_PATH%" (
    set "MYSQL_PATH=C:\ProgramData\chocolatey\lib\mysql\tools\mysql-9.2.0-winx64\bin"
)
if not exist "%MYSQL_PATH%" (
    set "MYSQL_PATH=C:\tools\mysql\mysql-9.2.0-winx64\bin"
)

echo MySQL路径: %MYSQL_PATH%

echo.
echo 创建数据库和用户...
echo 提示：请输入MySQL root密码（如果是首次安装，可能没有密码，直接按回车）

REM 创建数据库脚本
(
echo CREATE DATABASE IF NOT EXISTS `ruoyi-vue-pro` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
echo.
echo -- 确保用户存在并设置密码
echo CREATE USER IF NOT EXISTS 'root'@'localhost' IDENTIFIED BY '123456';
echo CREATE USER IF NOT EXISTS 'yudao'@'localhost' IDENTIFIED BY '123456';
echo.
echo -- 授权
echo GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost';
echo GRANT ALL PRIVILEGES ON `ruoyi-vue-pro`.* TO 'yudao'@'localhost';
echo.
echo FLUSH PRIVILEGES;
echo.
echo -- 显示数据库
echo SHOW DATABASES;
echo.
echo SELECT 'MySQL配置完成' as Status;
) > setup_mysql.sql

echo.
echo 执行MySQL配置...
"%MYSQL_PATH%\mysql.exe" -u root -p < setup_mysql.sql

if errorlevel 1 (
    echo.
    echo 尝试无密码连接...
    "%MYSQL_PATH%\mysql.exe" -u root < setup_mysql.sql
    
    if errorlevel 1 (
        echo.
        echo 错误：无法连接到MySQL。请检查：
        echo 1. MySQL服务是否运行
        echo 2. MySQL路径是否正确
        echo 3. root用户密码
        pause
        exit /b 1
    )
)

echo.
echo 配置完成！数据库信息：
echo 数据库名: ruoyi-vue-pro
echo 用户名: root
echo 密码: 123456
echo 端口: 3306

del setup_mysql.sql

pause