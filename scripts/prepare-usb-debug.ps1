param([string]$Serial)

$ErrorActionPreference = 'Stop'
$adb = Get-Command adb -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
if ($adb) {
    $adbPath = $adb.Source
} else {
    $adbPath = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
    if (-not (Test-Path -LiteralPath $adbPath -PathType Leaf)) {
        throw 'ADB belum ditemukan. Pasang Android SDK Platform-Tools atau tambahkan adb ke PATH.'
    }
}

$health = Invoke-RestMethod -Uri 'http://127.0.0.1:8080/health' -TimeoutSec 5
if ($health.status -ne 'ok' -or $health.mode -ne 'demo') {
    throw 'API lokal harus aktif dalam mode demo untuk pengujian dengan data sintetis.'
}
Write-Output 'API lokal siap (mode demo).'

$devices = & $adbPath devices
if ($LASTEXITCODE -ne 0) { throw 'ADB gagal membaca perangkat.' }
$phones = @($devices | ForEach-Object {
    if ($_ -match '^([^\s]+)\s+device\s*$' -and $Matches[1] -notlike 'emulator-*') {
        $Matches[1]
    }
})
if ($Serial) {
    if ($Serial -notin $phones) { throw "HP '$Serial' belum terhubung atau belum diotorisasi." }
} elseif ($phones.Count -eq 1) {
    $Serial = $phones[0]
} elseif ($phones.Count -eq 0) {
    throw 'HP belum terbaca. Hubungkan USB dan setujui izin USB debugging di HP.'
} else {
    throw 'Lebih dari satu perangkat tersambung. Jalankan ulang dengan -Serial <serial dari adb devices>.'
}

$mappings = & $adbPath -s $Serial reverse --list
if ($LASTEXITCODE -ne 0) { throw 'ADB gagal membaca pemetaan USB.' }
$portMappings = @($mappings | Where-Object { $_ -match '\s+tcp:8080\s+' })
if ($portMappings.Count -gt 0 -and $portMappings -notmatch '\s+tcp:8080\s+tcp:8080\s*$') {
    throw 'Port 8080 di HP sudah dipakai pemetaan lain.'
}
if ($portMappings.Count -eq 0) {
    & $adbPath -s $Serial reverse tcp:8080 tcp:8080 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Pemetaan USB port 8080 gagal.' }
}

$probe = & $adbPath -s $Serial shell '(printf "GET /health HTTP/1.0\r\n\r\n"; sleep 2) | toybox nc -w 5 -q 2 127.0.0.1 8080'
if ($LASTEXITCODE -ne 0 -or ($probe -join "`n") -notmatch 'HTTP/1\.[01] 200 OK') {
    throw 'HP belum bisa mengakses API lokal lewat USB.'
}

Write-Output "HP $Serial berhasil mencapai API lokal (HTTP 200)."
Write-Output 'Gunakan APK debug yang dibangun dengan -PrekapApiBaseUrl=http://127.0.0.1:8080.'
