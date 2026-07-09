$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $projectRoot "backend"
$jar = Join-Path $backendDir "target\restaurant-order-system-1.0.0.jar"

function Resolve-Java {
    if ($env:JAVA_HOME) {
        $javaFromHome = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (Test-Path $javaFromHome) {
            return $javaFromHome
        }
    }

    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if (-not $javaCommand) {
        throw "Java was not found. Install JDK 21+ or set JAVA_HOME."
    }

    return $javaCommand.Source
}

if (-not (Test-Path $jar)) {
    throw "JAR was not found. Run .\run.ps1 first, or run 'mvn -DskipTests package' in the backend directory."
}

if (-not $env:DB_USERNAME) { $env:DB_USERNAME = "root" }
if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD = "123456" }
if (-not $env:DB_URL) {
    $env:DB_URL = "jdbc:mysql://localhost:3306/restaurant_order?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
}

$java = Resolve-Java

Write-Host "Java=$java"
Write-Host "MySQL=$env:DB_URL"
Write-Host "URL=http://localhost:8080/"

Set-Location $backendDir
& $java -jar $jar
