param([Parameter(Mandatory=$true)][string]$ClientId)
$ErrorActionPreference='Stop'
if ($ClientId -notmatch '^[A-Za-z0-9-]+\.apps\.googleusercontent\.com$') { throw 'Cần OAuth Web Client ID hợp lệ, không phải Client Secret.' }
$root=Split-Path -Parent $PSScriptRoot
$properties=Join-Path $root 'gradle.properties'
$text=Get-Content -LiteralPath $properties -Raw
if ($text -match '(?m)^googleWebClientId=') { $text=[regex]::Replace($text,'(?m)^googleWebClientId=.*$',"googleWebClientId=$ClientId") }
else { $text=$text.TrimEnd()+"`ngoogleWebClientId=$ClientId`n" }
Set-Content -LiteralPath $properties -Value $text -Encoding utf8
$config='window.LINKMAP_CONFIG = '+(@{googleClientId=$ClientId}|ConvertTo-Json -Compress)+';'
Set-Content -LiteralPath (Join-Path $root 'web\dist\config.js') -Value $config -Encoding utf8
Write-Output 'Đã cấu hình Client ID công khai cho Android và web. Build lại APK và publish lại web để áp dụng.'
