# Shared Backend Libraries — Java/Spring Boot

Librería multi-módulo Maven de **componentes Spring Boot reutilizables** para construir APIs REST profesionales.

Incluye autenticación JWT, gestión de usuarios, e-commerce, CMS, reservas, almacenamiento y más.

**Stack:** Java 21+ · Spring Boot 3.4+ · Maven multi-módulo · JPA/Hibernate · JWT · Thymeleaf

Publicados en GitHub Packages bajo el groupId `io.github.adrianmartincano`.

**Frontend companion:** [`librerias-frontend`](https://github.com/AdrianMartinCano/librerias-frontend) — librerías Angular compartidas

---

## Módulos

| Artifact | Paquete Java | Qué hace |
|---|---|---|
| `spring-common` | `dev.pimon.common` | `ApiResponse`, `AppException`, manejo global de errores, entidad base |
| `spring-security` | `dev.pimon.security` | JWT, Spring Security, rutas públicas configurables, CORS, `@CurrentUser` |
| `spring-users` | `dev.pimon.users` | Registro, login, perfil, roles ADMIN/USER, admin por defecto |
| `spring-products` | `dev.pimon.products` | Catálogo con categorías, filtros, paginación, slug y soft delete |
| `spring-ecommerce` | `dev.pimon.ecommerce` | Pedidos, checkout, Stripe opcional, webhook |
| `spring-storage` | `dev.pimon.storage` | Subida de archivos local/S3, conversión automática a WebP |
| `spring-cms` | `dev.pimon.cms` | Páginas con bloques JSON, publicación, API pública + admin |
| `spring-reservations` | `dev.pimon.reservations` | Servicios, disponibilidad, reservas con confirmación por email |
| `spring-emails` | `dev.pimon.email` | Envío SMTP, plantillas Thymeleaf con marca configurable (colores, logo) |
| `spring-newsletter` | `dev.pimon.newsletter` | Suscripciones doble opt-in, campañas con plantillas, confirm/unsubscribe |

La documentación completa de cada módulo (configuración, endpoints, ejemplos) está en [docs/backend.md](docs/backend.md).

📖 **[PUBLISHING.md](./PUBLISHING.md)** — Guía completa: cómo publicar en GitHub Packages (Maven)

---

## Usarlos en tu proyecto

**1.** Configura el repositorio de GitHub Packages en tu `pom.xml` (o en `settings.xml`):

```xml
<repositories>
  <repository>
    <id>github-adrianmartincano</id>
    <url>https://maven.pkg.github.com/AdrianMartinCano/librerias-backend</url>
  </repository>
</repositories>
```

> Necesitas un token de GitHub con `read:packages` en tu `~/.m2/settings.xml` (server id `github-adrianmartincano`).

**2.** Añade los módulos que necesites:

```xml
<dependency>
  <groupId>io.github.adrianmartincano</groupId>
  <artifactId>spring-security</artifactId>
  <version>1.1.12</version>
</dependency>
<dependency>
  <groupId>io.github.adrianmartincano</groupId>
  <artifactId>spring-users</artifactId>
  <version>1.1.12</version>
</dependency>
```

**3.** Configura en `application.yml`:

```yaml
pimon:
  security:
    jwt-secret: "secreto-de-al-menos-32-caracteres"
    public-paths: ["/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**"]
    cors:
      allowed-origins: ["http://localhost:4200"]
  users:
    create-default-admin: true
    admin-email: admin@miapp.com
    admin-password: "Admin1234!"
```

Con solo eso ya tienes registro, login JWT, perfil, admin de usuarios y control de acceso (`@PreAuthorize`, `@CurrentUser`). Los módulos se autoconfiguran — no hace falta ninguna anotación extra.

Ejemplo real de proyecto consumidor: `girandoMaderaBack` (tienda Girando Madera).

---

## Desarrollo local

```bash
# Compilar e instalar todo en el repo local de Maven
mvn clean install -DskipTests

# Compilar un módulo concreto (con sus dependencias)
mvn clean install -pl newsletter -am -DskipTests
```

---

## Publicar una nueva versión

El CI (GitHub Actions) publica los 10 módulos al pushear un tag `v*`:

```bash
# 1. Sube la versión en el pom.xml raíz y en el <parent><version> de los 10 módulos
# 2. Commit + tag + push
git commit -am "chore: bump version to 1.1.13"
git tag v1.1.13
git push && git push --tags
```

---

## Estructura del proyecto

```
librerias-backend/
├── pom.xml          ← POM padre (versión, módulos, dependencias comunes)
├── common/          ← spring-common
├── security/        ← spring-security
├── users/           ← spring-users
├── products/        ← spring-products
├── ecommerce/       ← spring-ecommerce
├── storage/         ← spring-storage
├── cms/             ← spring-cms
├── reservations/    ← spring-reservations
├── emails/          ← spring-emails
├── newsletter/      ← spring-newsletter
└── docs/            ← documentación completa de los módulos
```
