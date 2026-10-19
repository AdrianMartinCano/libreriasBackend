# Librerías Spring Boot — multi-módulo Maven

> 🪞 **Espejo público.** El desarrollo se hace en un repositorio privado; este se actualiza periódicamente con una instantánea del código.

**17 módulos Spring Boot reutilizables** para montar APIs REST de negocio (tiendas, reservas, restauración, CMS, blog, foro, newsletter...) sin reescribir lo mismo en cada proyecto. Publicados en GitHub Packages bajo el groupId `io.github.adrianmartincano`.

**Stack:** Java 21 · Spring Boot 3.4 · Maven multi-módulo · JPA/Hibernate · JWT · Thymeleaf

Su contraparte frontend (librerías Angular) vive en [`librerias-frontend`](https://github.com/AdrianMartinCano/librerias-frontend).

---

## Módulos

| Artifact | Paquete Java | Qué hace |
|---|---|---|
| `spring-common` | `dev.pimon.common` | `ApiResponse`, `AppException`, manejo global de errores, entidad base |
| `spring-security` | `dev.pimon.security` | JWT, Spring Security, rutas públicas configurables, CORS, `@CurrentUser` |
| `spring-users` | `dev.pimon.users` | Registro, login, perfil, roles ADMIN/USER, admin por defecto, recuperar contraseña |
| `spring-google` | `dev.pimon.google` | **Entrar con Google** (Google Identity Services): verifica el `id_token`, vincula o crea la cuenta y devuelve el mismo JWT que el login normal. **Apagado por defecto**: sin `pimon.google.habilitado=true` no carga ni endpoints ni tabla |
| `spring-products` | `dev.pimon.products` | Catálogo con categorías, filtros, paginación, slug y soft delete |
| `spring-ecommerce` | `dev.pimon.ecommerce` | Pedidos, checkout, Stripe opcional, webhook |
| `spring-blog` | `dev.pimon.blog` | Blog: posts cronológicos (admin escribe, público lee) y comentarios de usuarios registrados, con nick en vivo |
| `spring-forum` | `dev.pimon.forum` | Foro de comunidad: categorías (admin), hilos y mensajes de usuarios, reacciones, citas, notificaciones, búsqueda y moderación (fijar/cerrar/mover/denuncias) |
| `spring-storage` | `dev.pimon.storage` | Subida de archivos local/S3, conversión automática a WebP |
| `spring-cms` | `dev.pimon.cms` | Páginas con bloques JSON, publicación, API pública + admin |
| `spring-reservations` | `dev.pimon.reservations` | Servicios, disponibilidad, reservas con confirmación por email; **multistaff** (varios profesionales con agenda propia, asignación "cualquiera", scoping ROLE_STAFF) |
| `spring-dining` | `dev.pimon.dining` | Reservas de mesa para bar/restaurante: zonas, mesas (aforo en comensales), turnos y **asignación de mesa best-fit + combinación** con control de concurrencia |
| `spring-menu` | `dev.pimon.menu` | Carta (categorías, platos con alérgenos/raciones) y **menú del día** publicable/clonable, con presentación tipográfica configurable. Sin imágenes subidas |
| `spring-emails` | `dev.pimon.email` | Envío SMTP, plantillas Thymeleaf con marca configurable (colores, logo) |
| `spring-newsletter` | `dev.pimon.newsletter` | Suscripciones doble opt-in, campañas con plantillas, confirm/unsubscribe |
| `spring-fx` | `dev.pimon.fx` | Tasas de cambio por API (`/api/fx`), refresco automático opcional; alimenta el formateo multi-divisa del front |
| `spring-events` | `dev.pimon.events` | Webs de eventos: confirmaciones de asistencia (RSVP) y su resumen para el organizador |

La documentación completa de cada módulo (configuración, endpoints, ejemplos) está en [docs/backend.md](docs/backend.md).

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
  <version>1.0.0</version>
</dependency>
<dependency>
  <groupId>io.github.adrianmartincano</groupId>
  <artifactId>spring-users</artifactId>
  <version>1.0.0</version>
</dependency>
```

**3.** Configura en `application.yml`:

```yaml
pimon:
  security:
    jwt-secret: "secreto-de-al-menos-32-caracteres"   # mín. 32 chars (la app no arranca si es menor)
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

El CI (GitHub Actions) publica los módulos al pushear un tag `v*`:

```bash
# 1. Sube la versión en el pom.xml raíz y en el <parent><version> de cada módulo
#    (lo hace publish.ps1, que lee la lista de módulos del propio pom padre)
# 2. Commit + tag + push
git commit -am "chore: bump version to 1.0.0"
git tag v1.0.0
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
├── google/          ← spring-google
├── products/        ← spring-products
├── ecommerce/       ← spring-ecommerce
├── blog/            ← spring-blog
├── forum/           ← spring-forum
├── storage/         ← spring-storage
├── cms/             ← spring-cms
├── reservations/    ← spring-reservations
├── dining/          ← spring-dining
├── menu/            ← spring-menu
├── emails/          ← spring-emails
├── newsletter/      ← spring-newsletter
├── fx/              ← spring-fx
├── events/          ← spring-events
└── docs/            ← documentación completa de los módulos
```
