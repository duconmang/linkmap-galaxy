param()
$ErrorActionPreference='Stop'
$taskRoot=Split-Path -Parent $PSScriptRoot
$taskTemp=Join-Path $taskRoot '.tmp'
New-Item -ItemType Directory -Path $taskTemp -Force | Out-Null
$env:TEMP=$taskTemp; $env:TMP=$taskTemp
if (-not $env:JAVA_HOME) { $env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' }
if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME=Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$taskUsesLocal=-not $env:LINKMAP_KEYSTORE_FILE
$taskPrevious=@{}
$taskNames=@('LINKMAP_KEYSTORE_FILE','LINKMAP_KEYSTORE_PASSWORD','LINKMAP_KEY_ALIAS','LINKMAP_KEY_PASSWORD')
foreach ($taskName in $taskNames) { $taskPrevious[$taskName]=[Environment]::GetEnvironmentVariable($taskName,'Process') }
try {
    if ($taskUsesLocal) {
        $taskProtected=Join-Path $taskRoot '.signing\linkmap-upload.password.dpapi'
        if (-not (Test-Path -LiteralPath $taskProtected)) { throw 'Run scripts/init-release-signing.ps1 once, or provide signing environment variables.' }
        $taskSecure=(Get-Content -LiteralPath $taskProtected -Raw).Trim() | ConvertTo-SecureString
        $taskPtr=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($taskSecure)
        try { $env:LINKMAP_KEYSTORE_PASSWORD=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($taskPtr) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($taskPtr) }
        $env:LINKMAP_KEYSTORE_FILE=Join-Path $taskRoot '.signing\linkmap-upload.p12'
        $env:LINKMAP_KEY_ALIAS='linkmap-upload'
        $env:LINKMAP_KEY_PASSWORD=$env:LINKMAP_KEYSTORE_PASSWORD
    }
    $env:JAVA_OPTS="-Djava.io.tmpdir=`"$taskTemp`""
    Push-Location $taskRoot
    try {
        & (Join-Path $taskRoot 'gradlew.bat') :app:assembleRelease :app:bundleRelease :core:test :app:lintRelease --console plain "-Dorg.gradle.jvmargs=-Xmx3072m -Dfile.encoding=UTF-8 -Djava.io.tmpdir=`"$taskTemp`""
        if ($LASTEXITCODE -ne 0) { throw 'Release build failed.' }
    } finally { Pop-Location }
    Write-Output 'APK: app/build/outputs/apk/release/app-release.apk'
    Write-Output 'AAB: app/build/outputs/bundle/release/app-release.aab'
} finally {
    foreach ($taskName in $taskNames) { [Environment]::SetEnvironmentVariable($taskName,$taskPrevious[$taskName],'Process') }
}
