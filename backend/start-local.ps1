param(
    [ValidateRange(1, 65535)][int]$Port = 8000,
    [switch]$Demo
)
$ErrorActionPreference = 'Stop'
$backendRoot = $PSScriptRoot
$localSettings = Join-Path (Split-Path $backendRoot -Parent) 'backend.local.properties'
if (-not (Test-Path -LiteralPath $localSettings)) {
    Set-Content -LiteralPath $localSettings -Value "MENTOR_DEBUG_BACKEND_URL=http://10.0.2.2:$Port" -Encoding ascii
    Write-Host 'Created local Android debug configuration. Sync Gradle and run app again.'
}
$pythonPath = Join-Path $backendRoot '.venv/Scripts/python.exe'
if (-not (Test-Path -LiteralPath $pythonPath)) {
    if (Get-Command py -ErrorAction SilentlyContinue) {
        & py -3 -m venv (Join-Path $backendRoot '.venv')
    } elseif (Get-Command python -ErrorAction SilentlyContinue) {
        & python -m venv (Join-Path $backendRoot '.venv')
    } else { throw 'Install Python 3.11 or newer, then run this script again.' }
    if ($LASTEXITCODE -ne 0) { throw 'Could not create the backend Python environment.' }
    & $pythonPath -m pip install -r (Join-Path $backendRoot 'requirements-lock.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Could not install backend dependencies.' }
}
$env:MENTOR_DATA_DIR = Join-Path $backendRoot 'data'
$env:MENTOR_DEMO_MODE = if ($Demo) { '1' } else { '0' }
Write-Host "AI Mentor backend: http://127.0.0.1:$Port"
Write-Host "Android Emulator: http://10.0.2.2:$Port"
Write-Host 'Keep this window open while using the local app. Press Ctrl+C to stop.'
& $pythonPath -m uvicorn aimentor.main:app --app-dir $backendRoot --host 0.0.0.0 --port $Port
exit $LASTEXITCODE
