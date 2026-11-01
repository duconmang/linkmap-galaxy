param([switch]$DeviceTests)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$taskTemp = Join-Path $projectRoot '.tmp'
New-Item -ItemType Directory -Path $taskTemp -Force | Out-Null
$env:TEMP = $taskTemp
$env:TMP = $taskTemp
if (-not $env:JAVA_HOME) {
    $bundledJdk = 'C:\Program Files\Android\Android Studio\jbr'
    if (Test-Path -LiteralPath (Join-Path $bundledJdk 'bin\java.exe')) { $env:JAVA_HOME = $bundledJdk }
    else { throw 'Cần đặt JAVA_HOME trỏ đến JDK 21.' }
}
if (-not $env:ANDROID_HOME) {
    $sdkPath = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    if (Test-Path -LiteralPath $sdkPath) { $env:ANDROID_HOME = $sdkPath }
    else { throw 'Cần đặt ANDROID_HOME trỏ đến Android SDK có platform 36.' }
}
$env:JAVA_OPTS = "-Djava.io.tmpdir=`"$taskTemp`""
$gradleArgs = @(':app:assembleDebug', ':core:test', ':app:lintDebug', '--console', 'plain', "-Dorg.gradle.jvmargs=-Xmx3072m -Dfile.encoding=UTF-8 -Djava.io.tmpdir=`"$taskTemp`"")
if ($DeviceTests) { $gradleArgs += ':app:connectedDebugAndroidTest' }
Push-Location $projectRoot
try { & (Join-Path $projectRoot 'gradlew.bat') @gradleArgs; $buildExit = $LASTEXITCODE }
finally { Pop-Location }
if ($buildExit -ne 0) { exit $buildExit }
Write-Output 'APK: app/build/outputs/apk/debug/app-debug.apk'
