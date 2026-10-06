param(
    [string]$DbUrl = $env:DB_URL,
    [string]$DbUsername = $env:DB_USERNAME
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if ([string]::IsNullOrWhiteSpace($DbUrl)) {
    $DbUrl = 'jdbc:mysql://localhost:3306/verificador_lotes?useSSL=false&serverTimezone=UTC'
}
if ([string]::IsNullOrWhiteSpace($DbUsername)) {
    $DbUsername = Read-Host 'Usuario de MySQL (por ejemplo, root)'
}
if ([string]::IsNullOrWhiteSpace($DbUsername)) {
    throw 'Falta el usuario de MySQL.'
}

function Read-Secret([string]$prompt) {
    $secure = Read-Host $prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

$previous = @{}
$names = @('DB_URL', 'DB_USERNAME', 'DB_PASSWORD',
           'DEMO_ACCOUNTS_ENABLED', 'DEMO_ADMIN_PASSWORD', 'DEMO_USER_PASSWORD',
           'ADMIN_EMAIL', 'ADMIN_PASSWORD', 'USUARIO_EMAIL', 'USUARIO_PASSWORD')
foreach ($name in $names) {
    $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

try {
    $env:DB_URL = $DbUrl
    $env:DB_USERNAME = $DbUsername
    if ([string]::IsNullOrEmpty($env:DB_PASSWORD)) {
        $env:DB_PASSWORD = Read-Secret 'Contraseña de MySQL'
    }
    $env:DEMO_ADMIN_PASSWORD = Read-Secret 'Elegí contraseña para demo.admin (mínimo 12 caracteres)'
    $env:DEMO_USER_PASSWORD = Read-Secret 'Elegí contraseña para demo.usuario (mínimo 12 caracteres)'
    if ($env:DEMO_ADMIN_PASSWORD.Length -lt 12 -or $env:DEMO_USER_PASSWORD.Length -lt 12) {
        throw 'Las dos contraseñas de demostración deben tener al menos 12 caracteres.'
    }
    $env:DEMO_ACCOUNTS_ENABLED = 'true'
    $env:ADMIN_EMAIL = ''
    $env:ADMIN_PASSWORD = ''
    $env:USUARIO_EMAIL = ''
    $env:USUARIO_PASSWORD = ''

    Write-Host 'Cuentas de esta base:'
    Write-Host '  ADMIN:   demo.admin@loteseguro.invalid'
    Write-Host '  USUARIO: demo.usuario@loteseguro.invalid'
    Write-Host 'Las contraseñas son las que acabás de ingresar; compartilas en privado.'
    Write-Host 'Iniciando backend en http://localhost:8080/ (Ctrl+C para detener)...'
    Push-Location $projectRoot
    try {
        & .\mvnw.cmd spring-boot:run
        if ($LASTEXITCODE -ne 0) { throw "El backend terminó con código $LASTEXITCODE." }
    } finally {
        Pop-Location
    }
} finally {
    foreach ($name in $names) {
        [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process')
    }
}
