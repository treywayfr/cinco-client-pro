$ErrorActionPreference = 'Stop'
# Windows PowerShell 5.1 may otherwise negotiate an obsolete TLS version.
try { [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12 } catch {}
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

$localGradle = Join-Path $projectRoot '.tools\gradle-8.10.2\bin\gradle.bat'
$globalGradle = Get-Command gradle -ErrorAction SilentlyContinue
$useGlobal = $false
if ($null -ne $globalGradle) {
    try {
        $versionOutput = & gradle -v 2>&1 | Out-String
        if ($versionOutput -match 'Gradle 8\.10\.2') { $useGlobal = $true }
    } catch { $useGlobal = $false }
}

if ($useGlobal) {
    & gradle build
    exit $LASTEXITCODE
}

if (-not (Test-Path $localGradle)) {
    $toolsDir = Join-Path $projectRoot '.tools'
    New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
    $archive = Join-Path $toolsDir 'gradle-8.10.2-bin.zip'
    Write-Host 'Downloading Gradle 8.10.2 from the official Gradle distribution site...'
    Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.10.2-bin.zip' -OutFile $archive
    Expand-Archive -LiteralPath $archive -DestinationPath $toolsDir -Force
    Remove-Item -LiteralPath $archive -Force
}

if (-not (Test-Path $localGradle)) {
    throw 'Gradle download/extraction did not create .tools\gradle-8.10.2\bin\gradle.bat.'
}
& $localGradle build
exit $LASTEXITCODE
