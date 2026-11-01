param()
$ErrorActionPreference='Stop'
if (-not $IsWindows) { throw 'Local signing setup uses Windows DPAPI. On other systems provide the LINKMAP_KEYSTORE_* environment variables.' }
$taskRoot=Split-Path -Parent $PSScriptRoot
$taskSigning=Join-Path $taskRoot '.signing'
$taskStore=Join-Path $taskSigning 'linkmap-upload.p12'
$taskProtected=Join-Path $taskSigning 'linkmap-upload.password.dpapi'
if ((Test-Path -LiteralPath $taskStore) -or (Test-Path -LiteralPath $taskProtected)) { throw 'Signing files already exist. Reuse them; never replace an established signing key.' }
$taskJdk=if ($env:JAVA_HOME) {$env:JAVA_HOME} else {'C:\Program Files\Android\Android Studio\jbr'}
$taskKeytool=Join-Path $taskJdk 'bin\keytool.exe'
if (-not (Test-Path -LiteralPath $taskKeytool)) { throw 'Set JAVA_HOME to JDK 21.' }
New-Item -ItemType Directory -Path $taskSigning -Force | Out-Null
$taskSid=[System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value
& icacls.exe $taskSigning /inheritance:r /grant:r "*$($taskSid):(OI)(CI)F" '*S-1-5-18:(OI)(CI)F' | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Could not restrict signing directory access.' }
$taskBytes=[byte[]]::new(36)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($taskBytes)
$taskPassword=[Convert]::ToBase64String($taskBytes)
try {
    $env:LINKMAP_KEYTOOL_PASSWORD=$taskPassword
    & $taskKeytool -genkeypair -keystore $taskStore -storetype PKCS12 -alias linkmap-upload -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity 10000 -dname 'CN=LinkMap Galaxy, OU=Release, O=LinkMap, C=VN' -storepass:env LINKMAP_KEYTOOL_PASSWORD -keypass:env LINKMAP_KEYTOOL_PASSWORD -noprompt
    if ($LASTEXITCODE -ne 0) { throw 'Key generation failed. Do not overwrite any generated keystore.' }
    $taskSecure=ConvertTo-SecureString $taskPassword -AsPlainText -Force
    $taskSecure | ConvertFrom-SecureString | Set-Content -LiteralPath $taskProtected -Encoding ascii
    Write-Output 'Release key created in .signing. Password protected with Windows DPAPI for this Windows account. Nothing was uploaded.'
} finally {
    Remove-Item Env:LINKMAP_KEYTOOL_PASSWORD -ErrorAction SilentlyContinue
    $taskPassword=$null
    [Array]::Clear($taskBytes,0,$taskBytes.Length)
}
