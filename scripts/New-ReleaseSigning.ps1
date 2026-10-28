param([string]$SigningDirectory = (Join-Path $env:USERPROFILE '.codex/signing/coelho-music'))
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$properties = Join-Path $projectRoot 'signing.properties'
if (Test-Path -LiteralPath $properties) { throw 'signing.properties already exists. Existing signing credentials will not be overwritten.' }
$keyFile = Join-Path $SigningDirectory 'coelho-music-release.p12'
if (Test-Path -LiteralPath $keyFile) { throw 'Release key already exists. Configure its credentials instead of replacing the identity.' }
$localJdk = Get-ChildItem -LiteralPath "$projectRoot/.tools/jdk" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
$keytool = if ($localJdk) { Join-Path $localJdk.FullName 'bin/keytool.exe' } else { Join-Path $env:JAVA_HOME 'bin/keytool.exe' }
New-Item -ItemType Directory -Force -Path $SigningDirectory | Out-Null
$sid = [Security.Principal.WindowsIdentity]::GetCurrent().User.Value
& icacls.exe $SigningDirectory /inheritance:r /grant:r "*${sid}:(OI)(CI)F" | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Unable to restrict access to the key directory.' }
$bytes = New-Object byte[] 48
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$password = [Convert]::ToBase64String($bytes)
try {
    $env:COELHO_KEYGEN_PASSWORD = $password
    & $keytool -genkeypair -noprompt -keystore $keyFile -storetype PKCS12 -storepass:env COELHO_KEYGEN_PASSWORD -keypass:env COELHO_KEYGEN_PASSWORD -alias coelho-music -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity 10000 -dname 'CN=Coelho Music'
    if ($LASTEXITCODE -ne 0) { throw 'Key generation failed.' }
    $escapedPath = $keyFile.Replace('\', '/').Replace(':', '\:')
    $content = "STORE_FILE=$escapedPath`nSTORE_PASSWORD=$password`nKEY_ALIAS=coelho-music`nKEY_PASSWORD=$password`n"
    [IO.File]::WriteAllText($properties, $content, [Text.UTF8Encoding]::new($false))
    & icacls.exe $properties /inheritance:r /grant:r "*${sid}:F" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Unable to restrict access to signing.properties.' }
    New-Item -ItemType Directory -Force -Path "$projectRoot/security" | Out-Null
    & $keytool -exportcert -rfc -keystore $keyFile -storepass:env COELHO_KEYGEN_PASSWORD -alias coelho-music -file "$projectRoot/security/release-certificate.pem"
    if ($LASTEXITCODE -ne 0) { throw 'Unable to export public certificate.' }
    Write-Output "Own release key generated at $keyFile. Back up this key and signing.properties securely; never publish them."
} finally {
    Remove-Item Env:COELHO_KEYGEN_PASSWORD -ErrorAction SilentlyContinue
    $password = $null
    $content = $null
    [Array]::Clear($bytes, 0, $bytes.Length)
}
