param([switch]$Checks, [switch]$Release)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $localJdk = Get-ChildItem -LiteralPath "$PSScriptRoot/.tools/jdk" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName }
    $variant = if ($Release) { 'Release' } else { 'Debug' }
    $tasks = @(":app:assemble$variant")
    if ($Checks) { $tasks += @(":app:test${variant}UnitTest", ":app:lint$variant") }
    $localGradle = "$PSScriptRoot/.tools/gradle-8.13/bin/gradle.bat"
    if (Test-Path -LiteralPath $localGradle) { & $localGradle @tasks --console=plain }
    else { & "$PSScriptRoot/gradlew.bat" @tasks --console=plain }
    if ($LASTEXITCODE -ne 0) { throw "O Gradle terminou com erro: $LASTEXITCODE" }
} finally { Pop-Location }
