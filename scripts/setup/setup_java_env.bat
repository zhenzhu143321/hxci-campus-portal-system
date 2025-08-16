@echo off
echo 正在设置Java开发环境...

REM 设置JAVA_HOME
set JAVA_HOME=C:\ProgramData\chocolatey\lib\openjdk17\tools\jdk
set M2_HOME=C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.11

REM 设置PATH
set PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%

echo JAVA_HOME=%JAVA_HOME%
echo M2_HOME=%M2_HOME%
echo PATH已更新

REM 验证安装
echo.
echo 验证Java安装:
java -version
echo.
echo 验证Maven安装:
mvn -version

echo.
echo 环境配置完成！可以开始Java开发工作。
echo.
echo 使用方法：
echo   编译项目: mvn clean compile
echo   运行测试: mvn test
echo   启动应用: mvn spring-boot:run
echo.
pause