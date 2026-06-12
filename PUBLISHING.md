# Publishing Guide — Backend Libraries

Cómo publicar los módulos Maven en **GitHub Packages** (Maven repository privado de GitHub).

## 🤔 ¿Por qué GitHub Packages?

✅ **Ventajas:**
- Almacenamiento gratuito en GitHub
- No necesita credenciales Maven Central
- Publicación automática desde GitHub Actions
- Versionado privado o público
- Integración perfecta con Git tags

❌ **Alternativas rechazadas:**
- Maven Central: Requiere JIRA, proceso manual, auditoría
- JFrog Artifactory: Pago para private
- Sonatype Nexus: Complejo de configurar

## 📦 Estructura de módulos

Cada módulo Maven se publica bajo el groupId `io.github.adrianmartincano`:

```
io.github.adrianmartincano:spring-common:1.0.0
io.github.adrianmartincano:spring-security:1.0.0
io.github.adrianmartincano:spring-products:1.0.0
... etc
```

### Ubicación en el proyecto

```
.
├── pom.xml                          ← POM padre (define modules)
├── common/
│   ├── pom.xml                      ← groupId: io.github.adrianmartincano
│   │                                   artifactId: spring-common
│   │                                   version: 1.0.0
│   └── src/
├── security/
│   ├── pom.xml
│   └── src/
├── products/
└── ...
```

## 🔑 Requisitos previos

### 1. Token de GitHub

Necesitas un **GitHub Personal Access Token (PAT)** con permisos:
> Genéralo en tu ordenador local una sola vez, luego guárdalo en `~/.m2/settings.xml`
- `write:packages` (publicar paquetes)
- `read:packages` (descargar paquetes)

**Crear el token:**
1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Generate new token (classic)
3. Nombre: `maven-publish`
4. Scopes: marcar `write:packages`, `read:packages`
5. Copiar el token (no se puede volver a ver)

### 2. Archivo `settings.xml` en `~/.m2/`

Si no existe, crear `~/.m2/settings.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">

  <servers>
    <server>
      <id>github-adrianmartincano</id>
      <username>TuUsuarioGitHub</username>
      <password>ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxx</password>
    </server>
  </servers>

  <profiles>
    <profile>
      <id>github</id>
      <repositories>
        <repository>
          <id>central</id>
          <url>https://repo1.maven.org/maven2</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>false</enabled></snapshots>
        </repository>
        <repository>
          <id>github-adrianmartincano</id>
          <url>https://maven.pkg.github.com/AdrianMartinCano/librerias-backend</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>false</enabled></snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>

  <activeProfiles>
    <activeProfile>github</activeProfile>
  </activeProfiles>

</settings>
```

### 3. Configuración en `pom.xml`

En el `pom.xml` raíz, agregar la sección de distribución:

```xml
<distributionManagement>
  <repository>
    <id>github-adrianmartincano</id>
    <name>GitHub Packages</name>
    <url>https://maven.pkg.github.com/AdrianMartinCano/librerias-backend</url>
  </repository>
  <snapshotRepository>
    <id>github-adrianmartincano</id>
    <name>GitHub Packages</name>
    <url>https://maven.pkg.github.com/AdrianMartinCano/librerias-backend</url>
  </snapshotRepository>
</distributionManagement>
```

## 🚀 Publicar manualmente

### Paso 1: Verificar que todo compila

Ejecuta esto en tu ordenador:

```bash
# Compilar y ejecutar tests
mvn clean test
```

Asegúrate de que no hay errores antes de continuar.

### Paso 2: Actualizar versión (si es necesario)

En `pom.xml` raíz:

```xml
<version>1.0.0</version>  <!-- Cambiar a 1.0.1, etc -->
```

También en cada módulo que tenga versión explícita.

### Paso 3: Publicar a GitHub Packages

```bash
# Publicar todos los módulos
mvn deploy

# O un módulo específico
mvn deploy -f common/pom.xml
```

**Salida esperada:**
```
[INFO] Uploading to github-adrianmartincano: 
  https://maven.pkg.github.com/AdrianMartinCano/librerias-backend
[INFO] Uploaded io/github/adrianmartincano/spring-common/1.0.0/spring-common-1.0.0.jar
```

### Paso 4: Verificar publicación

```bash
# Ver las versiones publicadas
curl -H "Authorization: token ghp_xxx" \
  https://api.github.com/repos/AdrianMartinCano/librerias-backend/packages

# O simplemente intentar descargar
mvn dependency:get -Dartifact=io.github.adrianmartincano:spring-common:1.0.0
```

## 🤖 Publicar automáticamente (GitHub Actions)

Crear `.github/workflows/publish.yml`:

```yaml
name: Publish to GitHub Packages

on:
  push:
    tags:
      - 'v*'  # Publicar cuando crees un tag v1.0.0, v1.0.1, etc

jobs:
  publish:
    runs-on: ubuntu-latest
    
    permissions:
      contents: read
      packages: write
    
    steps:
      - uses: actions/checkout@v3
      
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      
      - name: Publish to GitHub Packages
        run: mvn deploy
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

**Uso:**
```bash
# Hacer un tag
git tag v1.0.0
git push origin v1.0.0

# GitHub Actions publica automáticamente
```

## 📝 Versionado

Cada cambio importante debería resultar en una nueva versión:

```
1.0.0 → Versión inicial
1.0.1 → Bug fix
1.1.0 → Nueva funcionalidad
2.0.0 → Breaking change (API incompatible)
```

**Cambiar versión en Maven:**

```bash
# Interactivo
mvn versions:set

# Manual en pom.xml
<version>1.0.1</version>
```

## ⚙️ Configuración en proyecto consumidor

Los usuarios que usan tus librerías necesitan:

### 1. Configurar `settings.xml`

```xml
<servers>
  <server>
    <id>github-adrianmartincano</id>
    <username>SuUsuarioGitHub</username>
    <password>ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxx</password>
  </server>
</servers>

<repositories>
  <repository>
    <id>github-adrianmartincano</id>
    <url>https://maven.pkg.github.com/AdrianMartinCano/librerias-backend</url>
  </repository>
</repositories>
```

### 2. Agregar dependencia en `pom.xml`

```xml
<dependency>
  <groupId>io.github.adrianmartincano</groupId>
  <artifactId>spring-security</artifactId>
  <version>1.0.0</version>
</dependency>
```

### 3. Descargar e instalar

```bash
mvn clean install
```

## 🔒 Seguridad

⚠️ **IMPORTANTE:**

- ❌ Nunca commitas `settings.xml` con tu token
- ✅ Usa `~/.m2/settings.xml` (fuera del proyecto)
- ✅ En GitHub Actions, usa `secrets.GITHUB_TOKEN` (automático)
- ✅ Regenera el token si lo expones accidentalmente
- ✅ Usa `.gitignore` para excluir archivos sensibles

```gitignore
# .gitignore
settings.xml
*.key
.env
```

## 📜 Scripts helper

Crear `publish.ps1` (PowerShell):

```powershell
param(
    [string]$Version = "1.0.0",
    [switch]$Deploy = $false
)

Write-Host "Building version $Version..."
mvn clean test

if ($Deploy) {
    Write-Host "Deploying to GitHub Packages..."
    mvn deploy
    Write-Host "✓ Published successfully"
} else {
    Write-Host "Build successful. Use -Deploy flag to publish"
}
```

**Uso:**
```bash
.\publish.ps1 -Version 1.0.1 -Deploy
```

## 🐛 Troubleshooting

### "401 Unauthorized"
```
Causa: Token inválido o expirado
Solución:
1. Regenerar token en GitHub
2. Actualizar settings.xml
3. mvn clean deploy
```

### "403 Forbidden"
```
Causa: Token no tiene permisos de escribir
Solución:
1. Ir a GitHub PAT settings
2. Marcar "write:packages"
3. Regenerar token
```

### "Failed to execute goal org.apache.maven.plugins:maven-deploy-plugin"
```
Causa: distributionManagement no configurado
Solución:
1. Agregar <distributionManagement> en pom.xml
2. Asegurar que la URL es correcta
```

## 📚 Referencias

- [GitHub Maven Packages docs](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)
- [Maven Deploy Plugin](https://maven.apache.org/plugins/maven-deploy-plugin/)
- [Maven Settings Reference](https://maven.apache.org/settings.html)

---

**Resumen rápido:**

```bash
# Setup (una sola vez)
# Editar ~/.m2/settings.xml con token

# Publicar
mvn clean test
mvn deploy

# O con tag automático
git tag v1.0.0
git push origin v1.0.0
# GitHub Actions publica automáticamente
```
