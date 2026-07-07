$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $projectRoot "backend"
$java = "D:\IntelliJ IDEA 2025.2.1\jbr\bin\java.exe"
$jar = Join-Path $backendDir "target\restaurant-order-system-1.0.0.jar"

if (-not (Test-Path $java)) {
    throw "Java 路径不存在：$java"
}

if (-not (Test-Path $jar)) {
    throw "JAR 不存在，请先运行：.\run.ps1 或在 backend 目录执行 mvn package"
}

$dbUrl = if ($env:DB_URL) { $env:DB_URL } else { "jdbc:sqlserver://localhost:50374;databaseName=restaurant_order;encrypt=true;trustServerCertificate=true" }
Write-Host "SQL Server: $dbUrl"
Write-Host "启动地址：http://localhost:8080/"
Set-Location $backendDir
& $java -jar $jar
