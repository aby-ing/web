$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $projectRoot "backend"
$javaHome = "D:\IntelliJ IDEA 2025.2.1\jbr"
$maven = "D:\IntelliJ IDEA 2025.2.1\plugins\maven\lib\maven3\bin\mvn.cmd"

if (-not (Test-Path $javaHome)) {
    throw "JDK 路径不存在：$javaHome"
}

if (-not (Test-Path $maven)) {
    throw "Maven 路径不存在：$maven"
}

$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"
$dbUrl = if ($env:DB_URL) { $env:DB_URL } else { "jdbc:sqlserver://localhost:50374;databaseName=restaurant_order;encrypt=true;trustServerCertificate=true" }

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "SQL Server: $dbUrl"
Write-Host "启动地址：http://localhost:8080/"
Set-Location $backendDir
& $maven spring-boot:run
