# publish.ps1 — Sube la version y publica modulos Spring Boot a GitHub Packages Maven
#
# Modos de uso:
#   .\publish.ps1              -> patch local:  sube 1.0.0->1.0.1, mvn deploy en esta maquina
#   .\publish.ps1 minor        -> minor local
#   .\publish.ps1 patch -CI    -> patch + CI:   sube version, crea tag, GitHub Actions publica
#   .\publish.ps1 minor -CI    -> minor + CI
#   .\publish.ps1 patch emails,cms -> solo esos modulos (solo modo local)

param(
    [ValidateSet("patch","minor","major")]
    [string]$BumpType = "patch",
    [string]$Only = "",
    [switch]$CI
)

Set-Location $PSScriptRoot

# Los modulos salen del <modules> del POM padre, no de una lista escrita aqui.
#
# La lista a mano se olvidaba al anadir un modulo, y el fallo NO era "no se publica": era
# que su <parent><version> se quedaba sin actualizar mientras el resto subia, asi que
# seguia compilando contra la version anterior de sus dependencias sin que nada avisara.
# Pasó al anadir `google`. Ahora hay un unico sitio donde se declaran los modulos.
if (-not (Test-Path "pom.xml")) {
    Write-Error "No se encontro pom.xml. Ejecuta el script desde la raiz del proyecto."
    exit 1
}
[xml]$pomModulos = Get-Content "pom.xml" -Raw
$allModules = @($pomModulos.project.modules.module)

if ($allModules.Count -eq 0) {
    Write-Error "El pom.xml padre no declara ningun <module>."
    exit 1
}

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
    Write-Host "  Modo    : local - mvn deploy en esta maquina" -ForegroundColor Cyan
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
    if ($LASTEXITCODE -ne 0) { Write-Error "git push fallo."; exit 1 }
    git push origin "v$newVersion"   # solo el tag nuevo (--tags empujaba tags viejos y fallaba)
    if ($LASTEXITCODE -ne 0) { Write-Error "git push del tag v$newVersion fallo."; exit 1 }

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
