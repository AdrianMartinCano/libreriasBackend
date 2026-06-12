# ==============================================================================
# publish.ps1 — Automática de versionado y publicación en GitHub Packages
# ==============================================================================
#
# Este script automatiza todo el proceso de publicación de módulos Spring Boot
# a GitHub Packages (Maven registry). Maneja dos modos:
#
# 1. MODO LOCAL (default):
#    - Incrementa versión en pom.xml (patch/minor/major)
#    - Ejecuta: mvn deploy (desde tu ordenador)
#    - Requiere: Maven + GitHub token en settings.xml
#
# 2. MODO CI (-CI flag):
#    - Incrementa versión en pom.xml
#    - Crea commit + tag (v1.0.0, v1.0.1, etc.)
#    - Hace push del tag a GitHub
#    - GitHub Actions se dispara automáticamente y publica
#
# ==============================================================================
# EJEMPLOS DE USO:
# ==============================================================================
#
#   .\publish.ps1
#   → Modo local, versión patch (1.0.0 -> 1.0.1)
#   → Ejecuta: mvn deploy en tu ordenador
#   → Todos los módulos
#
#   .\publish.ps1 minor
#   → Modo local, versión minor (1.0.0 -> 1.1.0)
#
#   .\publish.ps1 patch -CI
#   → Modo CI, versión patch
#   → Crea tag v1.0.1 y GitHub Actions publica
#
#   .\publish.ps1 patch emails,cms
#   → Modo local, solo módulos "emails" y "cms"
#   → Los otros módulos no se publican
#
# ==============================================================================

param(
    [ValidateSet("patch","minor","major")]
    [string]$BumpType = "patch",
    [string]$Only = "",
    [switch]$CI
)

Set-Location $PSScriptRoot

$allModules = @("common","security","users","products","ecommerce","storage","cms","reservations","emails")

if ($Only -and -not $CI) {
    $targetModules = $Only -split "," | ForEach-Object { $_.Trim() }
    $invalid = $targetModules | Where-Object { $_ -notin $allModules }
    if ($invalid) {
        Write-Error "Modulo(s) no reconocidos: $($invalid -join ', '). Disponibles: $($allModules -join ', ')"
        exit 1
    }
} else {
    $targetModules = $allModules
}

# Leer version actual del POM padre
if (-not (Test-Path "pom.xml")) {
    Write-Error "No se encontro pom.xml. Ejecuta el script desde la raiz del proyecto."
    exit 1
}

[xml]$pom = Get-Content "pom.xml"
$currentVersion = $pom.project.version

if (-not ($currentVersion -match '^\d+\.\d+\.\d+$')) {
    Write-Error "Version '$currentVersion' en pom.xml no tiene formato semver (X.Y.Z)."
    exit 1
}

# Calcular nueva version
$parts = $currentVersion.Split(".")
[int]$major = $parts[0]
[int]$minor = $parts[1]
[int]$patch = $parts[2]

switch ($BumpType) {
    "major" { $major++; $minor = 0; $patch = 0 }
    "minor" { $minor++; $patch = 0 }
    "patch" { $patch++ }
}

$newVersion = "$major.$minor.$patch"

# Confirmacion
Write-Host ""
if ($CI) {
    Write-Host "  Modo    : CI - GitHub Actions publicara tras el push del tag" -ForegroundColor Magenta
    Write-Host "  Modulos : todos" -ForegroundColor Cyan
} else {
    Write-Host "  Modo    : local - mvn deploy en tu ordenador" -ForegroundColor Cyan
    Write-Host "  Modulos : $($targetModules -join ', ')" -ForegroundColor Cyan
}
Write-Host "  Version : $currentVersion  ->  $newVersion  ($BumpType)" -ForegroundColor Yellow
Write-Host ""
$confirm = Read-Host "Continuar? (s/N)"
if ($confirm -notmatch '^[sS]$') { Write-Host "Cancelado."; exit 0 }

# Actualizar version en todos los POM via PowerShell
Write-Host ""
Write-Host "Actualizando version en POMs..." -ForegroundColor Cyan

# Root pom.xml
[xml]$rootPom = Get-Content "pom.xml" -Raw
$rootPom.project.version = $newVersion
$rootPom.Save((Resolve-Path "pom.xml").Path)
Write-Host "  OK pom.xml -> $newVersion"

# Modulos: actualizar <parent><version> (solo modulos de librería, no erres-api ni api-demo)
Get-ChildItem -Directory | Where-Object { (Test-Path (Join-Path $_.FullName "pom.xml")) -and ($_.Name -in $allModules) } | ForEach-Object {
    $path = (Resolve-Path (Join-Path $_.FullName "pom.xml")).Path
    [xml]$modPom = Get-Content $path -Raw
    if ($modPom.project.parent) {
        $modPom.project.parent.version = $newVersion
        $modPom.Save($path)
        Write-Host "  OK $($_.Name)/pom.xml -> $newVersion"
    }
}

# MODO CI
if ($CI) {
    Write-Host ""
    Write-Host "Creando commit y tag v$newVersion..." -ForegroundColor Magenta

    git add -u
    git commit -m "chore: bump version to $newVersion"
    if ($LASTEXITCODE -ne 0) { Write-Error "git commit fallo."; exit 1 }

    git tag "v$newVersion"
    if ($LASTEXITCODE -ne 0) { Write-Error "git tag fallo."; exit 1 }

    git push
    git push --tags
    if ($LASTEXITCODE -ne 0) { Write-Error "git push fallo."; exit 1 }

    Write-Host ""
    Write-Host "OK Tag v$newVersion pusheado. GitHub Actions publicara los modulos en unos minutos." -ForegroundColor Green
    exit 0
}

# MODO LOCAL
Write-Host ""
Write-Host "Desplegando en GitHub Packages..." -ForegroundColor Cyan
$moduleList = $targetModules -join ","
$mvnCmd = if (Get-Command mvn -ErrorAction SilentlyContinue) { "mvn" } else { "C:\apache-maven-3.9.16\bin\mvn.cmd" }
& $mvnCmd deploy -DskipTests --pl $moduleList --am

if ($LASTEXITCODE -ne 0) {
    Write-Error "mvn deploy fallo. Revisa el log de Maven."
    exit 1
}

Write-Host ""
Write-Host "OK Deploy completado. Version $newVersion en GitHub Packages Maven." -ForegroundColor Green
