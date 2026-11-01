param([string]$Device = 'emulator-5554')
# Adds public test URLs to an installed debug build on the selected test emulator.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$samples = @(
    @('https://github.com/android/compose-samples','ComposeSamples','SEND'),
    @('https://github.com/android/architecture-samples','Architecture','SEND'),
    @('https://github.com/JetBrains/kotlin','Kotlin','SEND'),
    @('https://www.facebook.com/Google','GoogleFacebook','SEND'),
    @('https://www.facebook.com/AndroidOfficial','AndroidFacebook','SEND'),
    @('https://www.tiktok.com/@tiktok','TikTok','PROCESS_TEXT'),
    @('https://drive.google.com','GoogleDrive','SEND'),
    @('https://www.google.com','Google','SEND'),
    @('https://httpbin.org/status/410','GoneExample','SEND')
)
foreach ($sample in $samples) {
    $extra = if ($sample[2] -eq 'PROCESS_TEXT') { 'android.intent.extra.PROCESS_TEXT' } else { 'android.intent.extra.TEXT' }
    & $adb -s $Device shell am start -a "android.intent.action.$($sample[2])" -t text/plain --es $extra $sample[0] --es android.intent.extra.SUBJECT $sample[1] -n vn.linkmap/.CaptureActivity | Out-Null
    $uiPath = Join-Path $root '.tmp\smoke-ui.xml'
    $saveNode = $null
    for ($attempt = 0; $attempt -lt 6 -and -not $saveNode; $attempt++) {
        & $adb -s $Device shell uiautomator dump /sdcard/linkmap-test.xml | Out-Null
        if ($LASTEXITCODE -eq 0) {
            & $adb -s $Device pull /sdcard/linkmap-test.xml $uiPath 2>$null | Out-Null
            [xml]$view = Get-Content -LiteralPath $uiPath
            $saveNode = $view.SelectSingleNode('//node[@text="Thêm vào bản đồ" or @text="Lưu bookmark"]')
        }
    }
    if (-not $saveNode) { throw "Missing save button: $($sample[1])" }
    $coords = [regex]::Matches($saveNode.bounds, '\d+') | ForEach-Object { [int]$_.Value }
    $tapX = [int](($coords[0] + $coords[2]) / 2)
    $tapY = [int](($coords[1] + $coords[3]) / 2)
    & $adb -s $Device shell input tap $tapX $tapY
    Write-Output "Captured $($sample[1]) via $($sample[2])"
}
& $adb -s $Device shell am start -n vn.linkmap/.MainActivity
