$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $projectRoot "backend"

function Resolve-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        return $env:JAVA_HOME
    }

    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if (-not $javaCommand) {
        throw "Java was not found. Install JDK 21+ or set JAVA_HOME."
    }

    $javaBin = Split-Path -Parent $javaCommand.Source
    return Split-Path -Parent $javaBin
}

function Resolve-Maven {
    if ($env:MAVEN_CMD -and (Test-Path $env:MAVEN_CMD)) {
        return $env:MAVEN_CMD
    }

    if ($env:MAVEN_HOME) {
        $mavenFromHome = Join-Path $env:MAVEN_HOME "bin\mvn.cmd"
        if (Test-Path $mavenFromHome) {
            return $mavenFromHome
        }
    }

    $mavenFromD = Get-ChildItem -Path "D:\Maven" -Recurse -Filter "mvn.cmd" -ErrorAction SilentlyContinue |
            Select-Object -First 1
    if ($mavenFromD) {
        return $mavenFromD.FullName
    }

    $mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if (-not $mavenCommand) {
        $mavenCommand = Get-Command mvn -ErrorAction SilentlyContinue
    }

    if (-not $mavenCommand) {
        throw "Maven was not found. Install Maven or set MAVEN_HOME/MAVEN_CMD."
    }

    return $mavenCommand.Source
}

$env:JAVA_HOME = Resolve-JavaHome
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

if (-not $env:DB_USERNAME) { $env:DB_USERNAME = "root" }
if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD = "123456" }
if (-not $env:DB_URL) {
    $env:DB_URL = "jdbc:mysql://localhost:3306/restaurant_order?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
}

$maven = Resolve-Maven

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "Maven=$maven"
Write-Host "MySQL=$env:DB_URL"
Write-Host "URL=http://localhost:8080/"

Set-Location $backendDir
& $maven spring-boot:run
