# Librerías Spring Boot — Backend

Multi-módulo Maven en `F:\librerias\librerias-backend`.
Usa Spring Boot 3.4.1, Java 21, JPA/Hibernate, Lombok y MapStruct.

---

## Coordenadas Maven y paquetes Java

| Concepto | Valor | Dónde aparece |
|---|---|---|
| **GroupId Maven** | `io.github.adrianmartincano` | `pom.xml`, dependencias de otros proyectos |
| **ArtifactId Maven** | `spring-common`, `spring-security`... | `pom.xml`, dependencias de otros proyectos |
| **Paquete Java** | `dev.pimon.common`, `dev.pimon.security`... | `import` en código Java, autoconfiguration |
| **Config prefix** | `pimon:` | `application.yml` — `pimon.security.*`, `pimon.email.*`... |
| **Versión** | ver `<version>` en `pom.xml` raíz | campo `<version>` al declarar dependencias |

---

## Estructura del proyecto

```
librerias-backend/
  pom.xml              ← POM padre (groupId, versión, módulos, dependencias comunes)
  common/              ← BaseEntity, ApiResponse, manejo de errores   → paquete: dev.pimon.common
  security/            ← JWT, Spring Security, CORS, @CurrentUser      → paquete: dev.pimon.security
  users/               ← Registro, perfil, roles ADMIN/USER            → paquete: dev.pimon.users
  products/            ← Catálogo: productos y categorías              → paquete: dev.pimon.products
  ecommerce/           ← Pedidos y pagos con Stripe                   → paquete: dev.pimon.ecommerce
  storage/             ← Subida de imágenes a disco, conversión WebP  → paquete: dev.pimon.storage
  cms/                 ← Páginas CMS con bloques JSON                 → paquete: dev.pimon.cms
  reservations/        ← Servicios, disponibilidad y reservas          → paquete: dev.pimon.reservations
  emails/              ← Emails HTML con Thymeleaf, marca configurable → paquete: dev.pimon.email
  newsletter/          ← Suscripciones doble opt-in y campañas         → paquete: dev.pimon.newsletter
```

---

## Cómo usar los módulos

### 1. Añadir las dependencias en tu `pom.xml`

Los módulos están publicados en **GitHub Packages**. Para usarlos desde un proyecto nuevo,
configura primero el repositorio y las credenciales — ver [github-packages.md](github-packages.md)
para la guía completa. El resumen rápido:

```xml
<!-- pom.xml del proyecto — declarar el repositorio -->
<repositories>
    <repository>
        <id>github-adrianmartincano</id>
        <url>https://maven.pkg.github.com/AdrianMartinCano/librerias-backend</url>
    </repository>
</repositories>
```

```xml
<!-- Dependencias — usar la versión publicada actual -->
<dependency>
    <groupId>io.github.adrianmartincano</groupId>
    <artifactId>spring-security</artifactId>
    <version>1.0.5</version>
</dependency>
<dependency>
    <groupId>io.github.adrianmartincano</groupId>
    <artifactId>spring-users</artifactId>
    <version>1.0.5</version>
</dependency>
<!-- Solo los módulos que necesites -->
```

> **Desarrollo local:** si quieres probar cambios sin publicar, instala los módulos en tu
> repositorio local de Maven con `mvn clean install -DskipTests` desde `F:\librerias\librerias-backend`
> y omite el bloque `<repositories>` en el proyecto consumidor.

### 2. Cada módulo se autoconfigura

En cuanto añades la dependencia, el módulo registra sus beans, filtros y controladores
automáticamente gracias a Spring Boot autoconfiguration. Solo hay que añadir las propiedades
en `application.yml`.

### 3. `application.yml` completo con todos los módulos

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/miapp
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

pimon:
  security:
    jwt-secret: "mi-secreto-de-al-menos-32-caracteres-aqui"   # ⚠️ OBLIGATORIO cambiar en producción
    access-token-expiration: 86400000   # 24 horas en ms
    public-paths:
      - "/api/auth/**"
      - "/api/products/**"
      - "/api/categories/**"
      - "/api/cms/**"          # /api/cms/pages y /api/cms/pages/{slug}
      - "/api/services"
      - "/api/availability"
      - "/api/bookings"
      - "/api/payments/webhook"    # webhook de Stripe — DEBE ser público (Stripe no envía JWT)
    cors:
      allowed-origins:
        - "http://localhost:4200"
        - "https://miapp.com"

  users:
    create-default-admin: true
    admin-email: admin@miapp.com
    admin-password: Admin1234!
    admin-name: Administrador

  storage:
    upload-dir: /var/uploads
    base-url: https://miapp.com/uploads
    max-size-mb: 10
    allowed-types:
      - image/jpeg
      - image/png
      - image/webp
    webp:
      enabled: true
      quality: 85

  email:
    enabled: true
    from: no-reply@miapp.com
    from-name: Mi Aplicación
    admin-email: admin@miapp.com

  ecommerce:
    currency: EUR
    stripe:
      secret-key: ${STRIPE_SECRET_KEY}
      webhook-secret: ${STRIPE_WEBHOOK_SECRET}
      enabled: true

  reservations:
    work-start: "09:00"
    work-end: "20:00"
    work-days: [TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY]
    slot-interval-minutes: 60

# spring.mail se añade bajo la misma clave spring: del bloque anterior
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USER}
    password: ${MAIL_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```

> **Atención YAML:** no copies este bloque tal cual. En un `application.yml` real las claves
> `spring.datasource` y `spring.mail` deben estar bajo la **misma entrada `spring:`**, no en
> dos bloques separados (el segundo sobreescribiría al primero). Este fragmento las muestra
> separadas por claridad — en tu fichero real, únelas bajo un solo `spring:`.

### 4. Clase principal

Sin anotaciones especiales — los módulos se autoconfiguan solos:

```java
@SpringBootApplication
public class MiApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(MiApiApplication.class, args);
    }
}
```

### 5. Compilar y arrancar

```powershell
cd F:\librerias\librerias-backend

# Instalar todos los módulos en el repositorio local de Maven:
mvn clean install -DskipTests
```

Para probarlos, crea una Spring Boot application que los importe (p. ej. `girandoMaderaBack`
en el proyecto girandomadera) y arráncala con `mvn spring-boot:run`. La API queda en
`http://localhost:8080` y Swagger UI en `http://localhost:8080/swagger-ui.html`.

---

## Dependencias entre módulos

```
spring-common
   ↑
spring-security   spring-storage
   ↑                 ↑
spring-users      spring-cms     spring-emails
   ↑                               ↑
spring-products               spring-reservations
   ↑
spring-ecommerce
```

- `spring-ecommerce` depende de `spring-products` para validar stock al crear un pedido.
- `spring-reservations` depende de `spring-emails` para enviar confirmaciones automáticamente al crear o actualizar una reserva. Para deshabilitar el envío sin quitar la dependencia: `pimon.email.enabled: false`.

---

## `spring-common` — Base común

**ArtifactId:** `spring-common`  
**Paquete:** `dev.pimon.common`

Módulo que deben importar todos los demás. Aporta las clases base y utilidades transversales.
Se autoconfigura con `CommonAutoConfiguration`, que habilita `@EnableJpaAuditing`.

### BaseEntity

Todas las entidades extienden esta clase. Aporta automáticamente:

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `String` (UUID) | Generado automáticamente en `@PrePersist` |
| `createdAt` | `LocalDateTime` | Rellenado por JPA Auditing al crear |
| `updatedAt` | `LocalDateTime` | Actualizado por JPA Auditing en cada save |

```java
import dev.pimon.common.entity.BaseEntity;

@Entity
@Table(name = "mis_cosas")
@Getter @Setter @NoArgsConstructor
public class MiEntidad extends BaseEntity {
    @Column(nullable = false)
    private String nombre;
}
```

### ApiResponse\<T\>

Respuesta estándar de la API. **Todos** los endpoints devuelven este tipo.
El `GlobalExceptionHandler` también lo usa para los errores, así el frontend siempre recibe
la misma estructura independientemente de si hay error o no.

```java
import dev.pimon.common.dto.ApiResponse;

// En un @RestController:
return ResponseEntity.ok(ApiResponse.ok(datos));
return ResponseEntity.ok(ApiResponse.ok(datos, "Creado correctamente"));
return ResponseEntity.ok(ApiResponse.noContent("Eliminado correctamente"));
return ResponseEntity.badRequest().body(ApiResponse.error("Mensaje de error"));
return ResponseEntity.badRequest().body(ApiResponse.error("Validación fallida", mapaDeErrores));
```

JSON de éxito:

```json
{
  "success": true,
  "message": "Creado correctamente",
  "data": { ... }
}
```

JSON de error:

```json
{
  "success": false,
  "message": "Producto no encontrado: silla-nordica"
}
```

JSON de error de validación (con mapa de campos):

```json
{
  "success": false,
  "message": "Error de validación",
  "errors": {
    "email": "El email no es válido",
    "password": "Debe tener al menos 6 caracteres"
  }
}
```

### PageResponse\<T\>

Para respuestas paginadas. El JSON resultante tiene la siguiente forma:

```json
{
  "success": true,
  "data": {
    "items": [ ... ],
    "total": 156,
    "page": 0,
    "pageSize": 20,
    "totalPages": 8,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

Uso en un servicio:

```java
import dev.pimon.common.dto.PageResponse;

// Convertir un Page<Dto> de Spring en PageResponse<Dto>:
Page<ProductDto> page = productRepo.findAll(pageable).map(ProductMapper::toDto);
return ApiResponse.ok(PageResponse.from(page));
```

### AppException

Excepción de negocio con código HTTP. El `GlobalExceptionHandler` la captura y la convierte
en un `ApiResponse.error()` con el código HTTP correcto.

```java
import dev.pimon.common.exception.AppException;

throw AppException.notFound("Producto no encontrado: " + slug);     // 404
throw AppException.badRequest("El stock no puede ser negativo");     // 400
throw AppException.conflict("El email ya está registrado");          // 409
throw AppException.unauthorized("Token expirado");                   // 401
throw AppException.forbidden("Sin permisos para esta acción");       // 403
throw AppException.internalError("Error al procesar el pago");       // 500

// También se puede crear con código HTTP y code personalizados:
throw new AppException(HttpStatus.PAYMENT_REQUIRED, "SALDO_INSUFICIENTE", "Saldo insuficiente");
```

### GlobalExceptionHandler

Se registra automáticamente. Captura y transforma en `ApiResponse` los siguientes casos:

| Excepción | Código HTTP | Qué dispara |
|---|---|---|
| `AppException` | El del `AppException` | Cualquier `throw AppException.*()` en el código |
| `MethodArgumentNotValidException` | 400 | Fallo de `@Valid` en un `@RequestBody` |
| `MethodArgumentTypeMismatchException` | 400 | Parámetro de URL con tipo incorrecto (ej. texto donde se espera UUID) |
| `NoResourceFoundException` | 404 | Archivo estático no encontrado (`/uploads/...`) |
| `Exception` | 500 | Cualquier otro error no controlado |

En el caso de validación devuelve además el mapa de errores por campo (ver `ApiResponse` arriba).

---

## `spring-security` — Autenticación JWT

**ArtifactId:** `spring-security`  
**Paquete:** `dev.pimon.security`  
**Depende de:** `spring-common`

### Qué configura automáticamente

- **Spring Security** con JWT, sin sesiones (`SessionCreationPolicy.STATELESS`)
- **CORS** con los orígenes de `pimon.security.cors.allowed-origins`
- **CSRF desactivado** (innecesario con JWT)
- **`@EnableMethodSecurity`** — habilita `@PreAuthorize` en los controllers
- Filtro **`JwtAuthFilter`** que valida el token en cada petición
- Errores **401 y 403 en formato JSON** (no el HTML por defecto de Spring)
- Bean **`PasswordEncoder`** (BCrypt delegating) disponible para inyectar en tus servicios
- Argumentos `@CurrentUser` en métodos de controlador

### Endpoints registrados

```
POST /api/auth/login    → devuelve JWT
GET  /api/auth/me       → datos del usuario autenticado
```

### POST /api/auth/login

```
POST /api/auth/login
Content-Type: application/json

{ "email": "usuario@ejemplo.com", "password": "MiPassword123" }
```

Respuesta `200 OK`:

```json
{
  "success": true,
  "message": "Login correcto",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400000,
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "usuario@ejemplo.com",
    "roles": ["ROLE_USER"]
  }
}
```

> `id` en la respuesta es el **UUID** del usuario. El JWT lo usa como `subject` (`sub`).
> La respuesta de login no incluye `name` — para obtenerlo llama a `GET /api/users/me`.

Respuesta `401` si las credenciales son incorrectas:

```json
{ "success": false, "message": "Email o contraseña incorrectos" }
```

### GET /api/auth/me

```
GET /api/auth/me
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

Respuesta:

```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "usuario@ejemplo.com",
    "roles": ["ROLE_USER"]
  }
}
```

### Contenido del token JWT

El token incluye estos claims:

| Claim | Valor | Descripción |
|---|---|---|
| `sub` | UUID del usuario | Identificador único — lo que devuelve `user.getId()` |
| `email` | email del usuario | |
| `roles` | `["ROLE_USER"]` | Lista de roles |
| `iat` | timestamp | Fecha de emisión |
| `exp` | timestamp | Fecha de expiración |

### Errores de seguridad en formato JSON

A diferencia del comportamiento por defecto de Spring, todos los errores de seguridad devuelven JSON. Hay dos manejadores independientes que producen el mismo formato:

| Situación | Código | Manejador |
|---|---|---|
| Sin token o token inválido/expirado | 401 | `accessDeniedHandler` en `SecurityConfig` |
| Ruta bloqueada por `authorizeHttpRequests` | 403 | `accessDeniedHandler` en `SecurityConfig` |
| Método bloqueado por `@PreAuthorize` | 403 | `SecurityExceptionHandler` (`@RestControllerAdvice`) |

```
401:
{ "success": false, "message": "No autenticado — incluye el token JWT en el header Authorization: Bearer <token>" }

403 (cualquier causa):
{ "success": false, "message": "Acceso denegado — no tienes permisos para este recurso" }
```

> El `SecurityExceptionHandler` intercepta los 403 de `@PreAuthorize` con prioridad máxima (`@Order(HIGHEST_PRECEDENCE)`) para que no sean capturados por el `GlobalExceptionHandler` genérico.

### Configurar rutas públicas

```yaml
pimon:
  security:
    public-paths:
      - "/api/auth/**"           # login, register, me — siempre público
      - "/api/products/**"       # catálogo público
      - "/api/categories/**"     # categorías públicas
      - "/api/cms/**"            # páginas CMS públicas
      - "/api/services"          # servicios disponibles para reservar
      - "/api/availability"      # consulta de slots
      - "/api/bookings"          # crear reserva sin autenticar
      - "/api/payments/webhook"  # Stripe webhook — DEBE ser público
      - "/uploads/**"            # archivos estáticos servidos por Spring (si no usas Nginx)
      - "/swagger-ui/**"         # docs de la API
      - "/v3/api-docs/**"
```

Todo lo no listado requiere token JWT automáticamente.

> Si sirves los archivos subidos a través de **Nginx** (recomendado en producción), Nginx los
> sirve directamente sin pasar por Spring — no necesitas `/uploads/**` en `public-paths`.
> Si Spring sirve los archivos (desarrollo local sin Nginx), añade la ruta para que sean accesibles.

> **Al definir `public-paths` en tu `application.yml` reemplazas la lista entera**, no la amplías.
> Incluye siempre `/api/auth/**` y `/swagger-ui/**` aunque ya sean defecto — si los omites,
> quedarán protegidos por JWT.

### Configurar CORS

```yaml
pimon:
  security:
    cors:
      allowed-origins:
        - "http://localhost:4200"
        - "https://miapp.com"
      allowed-methods:           # opcional — estos son los valores por defecto
        - GET
        - POST
        - PUT
        - PATCH
        - DELETE
        - OPTIONS
```

Si no defines `allowed-methods`, se usan esos seis métodos por defecto. Solo necesitas
sobrescribirlos si quieres restringir o añadir métodos distintos.

### @CurrentUser en controllers

Inyecta el usuario autenticado directamente como parámetro:

El record `AuthUser` expone estos métodos:

| Método | Tipo retorno | Descripción |
|---|---|---|
| `user.id()` | `String` | UUID del usuario (subject del JWT) |
| `user.email()` | `String` | Email del usuario |
| `user.roles()` | `Collection<String>` | Lista de roles (`"ROLE_USER"`, `"ROLE_ADMIN"`...) |
| `user.hasRole("ROLE_ADMIN")` | `boolean` | Comprueba si tiene un rol específico |
| `user.isAdmin()` | `boolean` | Equivale a `hasRole("ROLE_ADMIN")` |

```java
import dev.pimon.security.annotation.CurrentUser;
import dev.pimon.security.model.AuthUser;

@GetMapping("/mi-perfil")
public ResponseEntity<ApiResponse<UserDto>> miPerfil(@CurrentUser AuthUser user) {
    return ResponseEntity.ok(ApiResponse.ok(userService.getById(user.id())));
}

@GetMapping("/solo-admins")
public ResponseEntity<ApiResponse<List<ReporteDto>>> reportes(@CurrentUser AuthUser user) {
    // Comprobación manual de rol (alternativa a @PreAuthorize):
    if (!user.isAdmin()) {
        throw AppException.forbidden("Solo los administradores pueden ver reportes");
    }
    return ResponseEntity.ok(ApiResponse.ok(reporteService.getAll()));
}

@PostMapping("/mis-cosas")
public ResponseEntity<ApiResponse<CosaDto>> crearCosa(
        @CurrentUser AuthUser user,
        @Valid @RequestBody CosaRequest req) {
    // Disponible junto a @RequestBody sin ningún problema
    return ResponseEntity.ok(ApiResponse.ok(cosaService.crear(user.id(), req)));
}
```

Lanza 401 automáticamente si la ruta no está en `public-paths` y el usuario no está autenticado.

### @PreAuthorize con roles

```java
import org.springframework.security.access.prepost.PreAuthorize;

// Solo ADMIN:
@GetMapping("/admin/estadisticas")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<...> estadisticas() { ... }

// ADMIN o EDITOR:
@PutMapping("/admin/cms/{id}")
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
public ResponseEntity<...> editarPagina(@PathVariable String id, ...) { ... }
```

También se puede poner `@PreAuthorize` en la clase entera para proteger todos sus métodos:

```java
@RestController
@RequestMapping("/api/admin/productos")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductosController { ... }
```

### Usar el PasswordEncoder en tu código

El módulo registra un bean `PasswordEncoder`. Puedes inyectarlo en tus propios servicios:

```java
@Service
@RequiredArgsConstructor
public class MiServicio {
    private final PasswordEncoder passwordEncoder;

    public void cambiarPin(String userId, String nuevoPin) {
        String hash = passwordEncoder.encode(nuevoPin);
        // guardar en BD
    }
}
```

---

## `spring-users` — Usuarios y roles

**ArtifactId:** `spring-users`  
**Paquete:** `dev.pimon.users`  
**Depende de:** `spring-common`, `spring-security`

### Roles disponibles

```java
// Enum Role:
ROLE_USER    // usuario estándar — acceso a su propio perfil y recursos
ROLE_ADMIN   // administrador — acceso total
ROLE_EDITOR  // puede gestionar contenido (CMS, reservas) pero no usuarios
```

Un usuario puede tener varios roles a la vez.

### Entidad `AppUser` y tablas

```
app_users
  id          VARCHAR(36) PK  — UUID generado automáticamente
  email       VARCHAR UNIQUE NOT NULL
  password    VARCHAR NOT NULL        — hash BCrypt
  name        VARCHAR NOT NULL
  active      BOOLEAN DEFAULT true    — false = cuenta desactivada
  created_at  TIMESTAMP
  updated_at  TIMESTAMP

user_roles                            — tabla de join para la colección de roles
  user_id     FK → app_users.id
  role        VARCHAR  — 'ROLE_USER', 'ROLE_ADMIN', 'ROLE_EDITOR'
```

> Se llama `AppUser` (no `User`) para evitar conflicto con
> `org.springframework.security.core.userdetails.User` de Spring.

### Cómo funciona la autenticación internamente

`UserService` implementa `UserDetailsService` de Spring Security. Cuando el usuario hace login:

1. Spring Security llama a `loadUserByUsername(email)`
2. Se busca el usuario en BD por email
3. Se construye el `UserDetails` con `username = userId (UUID)` — **no el email**
4. El JWT se genera con `sub = userId`, `email = email`, `roles = [...]`
5. En peticiones posteriores, `@CurrentUser` extrae el `AuthUser` del JWT con `id = userId`

### UserDto — representación pública

```java
record UserDto(
    String          id,        // UUID
    String          email,
    String          name,
    Set<String>     roles,     // ["ROLE_USER", "ROLE_ADMIN"]
    boolean         active,
    LocalDateTime   createdAt
) {}
// La contraseña nunca se expone en ningún DTO
```

### Endpoints de registro (público)

```
POST /api/auth/register
Content-Type: application/json

{
  "name": "Juan García",
  "email": "juan@ejemplo.com",
  "password": "MiPassword12"
}
```

> La contraseña requiere **mínimo 8 caracteres**. Con menos devuelve 400:
> `{ "errors": { "password": "La contraseña debe tener al menos 8 caracteres" } }`

Respuesta `201 Created`:

```json
{
  "success": true,
  "message": "Cuenta creada correctamente",
  "data": {
    "id": "uuid",
    "email": "juan@ejemplo.com",
    "name": "Juan García",
    "roles": ["ROLE_USER"],
    "active": true,
    "createdAt": "2025-06-15T10:30:00"
  }
}
```

Error `409` si el email ya existe:

```json
{ "success": false, "message": "Ya existe una cuenta con el email: juan@ejemplo.com" }
```

### Endpoints de perfil (usuario autenticado)

```
GET /api/users/me           → ver mi perfil
PUT /api/users/me           → actualizar nombre
PUT /api/users/me/password  → cambiar contraseña
```

Request `PUT /api/users/me` — solo acepta `name`:

```json
{ "name": "Juan García López" }
```

Request `PUT /api/users/me/password`:

```json
{
  "currentPassword": "MiPasswordActual",
  "newPassword": "MiNuevoPassword12"
}
```

> `newPassword` requiere mínimo 8 caracteres.

Error `400` si la contraseña actual es incorrecta:

```json
{ "success": false, "message": "La contraseña actual no es correcta" }
```

### Endpoints de administración (requiere `ROLE_ADMIN`)

```
GET    /api/admin/users              → listar todos (paginado, ?page=0&size=20)
GET    /api/admin/users/{id}         → detalle de un usuario
PUT    /api/admin/users/{id}         → editar nombre, roles y estado
DELETE /api/admin/users/{id}         → eliminar permanentemente
```

Request `PUT /api/admin/users/{id}`:

```json
{
  "name": "Nombre Editado",
  "roles": ["ROLE_USER", "ROLE_ADMIN"],
  "active": true
}
```

`name` es **obligatorio** (`@NotBlank`). `roles` y `active` son opcionales — si no se envían (`null`), no se modifican. Si se envía `roles: []` (array vacío), el usuario quedará sin ningún rol. Si se envía `active: false`, la cuenta queda desactivada sin eliminarla.

### Admin creado automáticamente al arrancar

Si `create-default-admin: true` y la tabla `app_users` está vacía, crea el admin:

```yaml
pimon:
  users:
    create-default-admin: true
    admin-email: admin@miapp.com
    admin-password: Admin1234!
    admin-name: Administrador
```

Útil para el primer arranque. En producción, cambiar la contraseña inmediatamente.

---

## `spring-products` — Catálogo de productos

**ArtifactId:** `spring-products`  
**Paquete:** `dev.pimon.products`  
**Depende de:** `spring-common`, `spring-security`

### Entidades y tablas

```
categories
  id          VARCHAR(36) PK
  name        VARCHAR NOT NULL UNIQUE
  slug        VARCHAR UNIQUE            — generado de name automáticamente
  description TEXT                     — descripción opcional
  image_url   VARCHAR                  — imagen opcional para listados visuales
  active      BOOLEAN DEFAULT true     — false = soft delete
  created_at, updated_at

products
  id             VARCHAR(36) PK
  name           VARCHAR NOT NULL
  slug           VARCHAR UNIQUE NOT NULL — generado de name, ej. "silla-nordica"
  description    TEXT
  price          DECIMAL NOT NULL
  original_price DECIMAL                 — null = sin descuento
  stock          INT DEFAULT 0
  image_url      VARCHAR
  badge          VARCHAR                 — "Nuevo", "-25%", "Oferta"...
  active         BOOLEAN DEFAULT true    — false = soft delete
  category_id    FK → categories.id
  created_at, updated_at
```

El `slug` se genera automáticamente de `name` al crear. Si ya existe, añade sufijo:
`"Silla Nórdica"` → `"silla-nordica"` → `"silla-nordica-2"` si ya existía.

El `DELETE` es un **soft delete**: pone `active = false`, el registro permanece en BD.
El endpoint público `GET /api/products` solo devuelve productos con `active = true`.

### Endpoints públicos de productos

```
GET /api/products           → listado con filtros y paginación
GET /api/products/{slug}    → detalle de un producto por su slug
```

**Parámetros de `GET /api/products`:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `search` | `string` | Búsqueda en nombre y descripción |
| `category` | `string` | Slug de la categoría (no el nombre) |
| `minPrice` | `number` | Precio mínimo |
| `maxPrice` | `number` | Precio máximo |
| `inStock` | `boolean` | `true` = solo con stock > 0 |
| `page` | `int` | Página (0-indexada, default 0) |
| `size` | `int` | Resultados por página (default 20) |
| `sort` | `string` | `price,asc` / `price,desc` / `name,asc` / `createdAt,desc` |

Ejemplo:
```
GET /api/products?search=silla&category=mobiliario&minPrice=50&maxPrice=300&inStock=true&page=0&size=12&sort=price,asc
```

### Endpoints públicos de categorías

```
GET /api/categories          → lista de todas las categorías activas
GET /api/categories/{slug}   → detalle de una categoría por slug
```

Respuesta de `GET /api/categories`:

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "name": "Mobiliario",
      "slug": "mobiliario",
      "description": "Mesas, sillas y estanterías",
      "imageUrl": "https://miapp.com/uploads/cms/mobiliario.webp"
    },
    {
      "id": "uuid",
      "name": "Iluminación",
      "slug": "iluminacion",
      "description": null,
      "imageUrl": null
    }
  ]
}
```

`description` e `imageUrl` son opcionales y pueden ser `null`.

### DTO de respuesta de producto (`ProductDto`)

```json
{
  "id": "uuid",
  "name": "Silla Nórdica",
  "slug": "silla-nordica",
  "description": "Silla de diseño escandinavo con madera maciza...",
  "price": 299.99,
  "originalPrice": 399.99,
  "discount": 25,
  "inStock": true,
  "stock": 15,
  "imageUrl": "https://miapp.com/uploads/products/silla.webp",
  "badge": "-25%",
  "category": { "id": "uuid", "name": "Mobiliario", "slug": "mobiliario", "description": null, "imageUrl": null },
  "createdAt": "2025-01-15T10:30:00"
}
```

### Endpoints de administración de productos (requiere `ROLE_ADMIN`)

```
GET    /api/admin/products/{id}   → obtener por ID (útil para el panel de edición)
POST   /api/admin/products        → crear producto
PUT    /api/admin/products/{id}   → actualizar producto
DELETE /api/admin/products/{id}   → soft delete (active = false)
```

Request de creación (`POST /api/admin/products`):

```json
{
  "name": "Silla Nórdica",
  "description": "Silla de diseño escandinavo...",
  "price": 299.99,
  "originalPrice": 399.99,
  "stock": 15,
  "imageUrl": "https://miapp.com/uploads/products/silla.webp",
  "badge": "-25%",
  "categoryId": "uuid-de-la-categoria"
}
```

Campos obligatorios en creación: `name` (`@NotBlank`), `description` (`@NotBlank`), `price` (positivo), `stock` (≥ 0), `categoryId` (`@NotBlank`).

Request de actualización (`PUT /api/admin/products/{id}`): todos los campos son opcionales — solo se actualizan los enviados.

### Endpoints de administración de categorías (requiere `ROLE_ADMIN`)

```
POST   /api/admin/categories        → crear categoría
PUT    /api/admin/categories/{id}   → actualizar
DELETE /api/admin/categories/{id}   → desactiva (soft delete)
```

Request de creación/actualización:

```json
{
  "name": "Mobiliario",
  "description": "Mesas, sillas y estanterías",
  "imageUrl": "https://miapp.com/uploads/cms/mobiliario.webp"
}
```

Solo `name` es obligatorio. `description` e `imageUrl` son opcionales.

---

## `spring-storage` — Almacenamiento de ficheros

**ArtifactId:** `spring-storage`  
**Paquete:** `dev.pimon.storage`  
**Depende de:** `spring-common`, `spring-security`

Sube imágenes al disco del servidor. Si `cwebp` está instalado en el sistema, convierte
automáticamente las imágenes a WebP antes de guardarlas (mejor compresión, mismo tamaño visual).

### Flujo interno de una subida

1. Valida tipo MIME y tamaño contra la configuración
2. Si `webp.enabled = true` y el archivo es una imagen (no SVG):
   - Guarda el original en un archivo temporal
   - Ejecuta `cwebp` para convertir a WebP
   - Si `cwebp` no está disponible, guarda el original sin convertir
3. Genera un nombre de archivo con UUID para evitar colisiones
4. Guarda en `upload-dir/subdir/uuid.webp` (o `uuid.ext` si no hay conversión)
5. Devuelve la URL pública: `base-url/subdir/uuid.webp`

### Endpoint de subida

```
POST /api/files/upload?subdir=products
Content-Type: multipart/form-data

file: <el archivo>
```

Parámetros query:

| Parámetro | Valores | Descripción |
|---|---|---|
| `subdir` | `products`, `users`, `cms`, `misc` | Carpeta de destino (default: `misc`) |

Respuesta `200 OK`:

```json
{
  "success": true,
  "message": "Imagen subida y convertida a WebP (45 KB)",
  "data": {
    "url": "https://miapp.com/uploads/products/a3f4b2c1-...webp",
    "filename": "a3f4b2c1-...webp",
    "format": "webp",
    "sizeBytes": 46080,
    "convertedToWebP": true
  }
}
```

Errores posibles:

```json
{ "success": false, "message": "Tipo de archivo no permitido: image/bmp. Permitidos: [image/jpeg, ...]" }
{ "success": false, "message": "El archivo supera el tamaño máximo de 10 MB" }
{ "success": false, "message": "El archivo está vacío" }
```

### Papelera — eliminación y restauración (requiere `ROLE_ADMIN`)

El borrado es **soft delete**: los archivos no se eliminan permanentemente, se mueven a
`upload-dir/trash/{subdir}/`. Desde ahí se pueden restaurar con el endpoint de restore.

#### Mover a la papelera

```
DELETE /api/admin/files/{subdir}/{filename}
```

Ejemplo:

```
DELETE /api/admin/files/cms/a3f4b2c1-....jpg
```

Respuesta `200 OK`:

```json
{ "success": true, "message": "Archivo movido a la papelera: a3f4b2c1-....jpg" }
```

El archivo pasa de `uploads/cms/a3f4b2c1-....jpg` → `uploads/trash/cms/a3f4b2c1-....jpg`.
Si ya existía otro archivo con el mismo nombre en la papelera, se añade un sufijo numérico
automáticamente (`_1`, `_2`...) para no sobreescribir nada.

#### Restaurar desde la papelera

```
POST /api/admin/files/trash/{subdir}/{filename}/restore
```

Ejemplo:

```
POST /api/admin/files/trash/cms/a3f4b2c1-....jpg/restore
```

Respuesta `200 OK`:

```json
{
  "success": true,
  "message": "Archivo restaurado: a3f4b2c1-....jpg",
  "data": "https://miapp.com/uploads/cms/a3f4b2c1-....jpg"
}
```

El campo `data` contiene la URL pública del archivo restaurado, lista para volver a usarla
en la imagen de un producto, bloque CMS, etc.

Errores posibles:

```json
{ "success": false, "message": "Archivo no encontrado en la papelera: a3f4b2c1-....jpg" }
{ "success": false, "message": "Ya existe un archivo con ese nombre en el destino: a3f4b2c1-....jpg" }
```

> **Pendiente:** limpieza automática de la papelera con `@Scheduled`. Por ahora los archivos en
> `trash/` permanecen indefinidamente hasta que se restauren o se borren manualmente del servidor.

### Casos de uso de la papelera

| Situación | Acción recomendada |
|---|---|
| Admin borra una imagen del editor CMS por error | `DELETE` → mueve a trash. Restaurar con `POST .../restore` |
| Se sustituye la imagen de un producto por otra nueva | `DELETE` el nombre antiguo — queda en trash como backup |
| Se elimina un producto entero | `DELETE` sus imágenes — quedan en `trash/products/` |
| Se confirma que ya no se necesita nada del trash | Borrar manualmente los archivos del servidor vía SSH o panel |
| Misma imagen borrada dos veces (doble clic accidental) | La segunda llamada devuelve `404` — el archivo ya está en trash |

> **Nota de seguridad:** `trash` es un subdirectorio reservado. No se pueden subir archivos
> directamente a ese subdir — el endpoint de upload lo rechaza con `400`.

### Estructura de directorios en disco

```
uploads/
  cms/               ← imágenes activas del editor CMS
  products/          ← imágenes activas de productos
  users/             ← avatares activos de usuarios
  misc/              ← subidas sin categoría
  trash/
    cms/             ← imágenes CMS eliminadas (recuperables)
    products/        ← imágenes de productos eliminadas
    users/           ← avatares eliminados
```

### Servir archivos estáticos en desarrollo

Spring sirve los archivos de la carpeta `uploads/` automáticamente en desarrollo a través de
`StaticResourcesConfig`, que mapea `/uploads/**` al directorio físico. No necesitas Nginx
en local — los archivos subidos son accesibles directamente en:

```
http://localhost:8080/uploads/{subdir}/{uuid}.{ext}
```

En producción, Nginx debe servir `/uploads/` como alias de la carpeta de disco para mayor rendimiento (configuración en la sección Nginx de abajo).

### Configuración

```yaml
pimon:
  storage:
    upload-dir: /var/uploads         # por defecto: ./uploads (relativo al directorio de trabajo)
    base-url: https://miapp.com/uploads  # por defecto: http://localhost:8080/uploads
    max-size-mb: 10
    allowed-types:
      - image/jpeg
      - image/png
      - image/gif
      - image/webp
      - image/svg+xml
    webp:
      enabled: true
      quality: 85       # 0-100. 85 es un buen equilibrio calidad/tamaño
      cwebp-path: cwebp # ruta al binario. Por defecto busca en el PATH
```

### Instalar cwebp en el servidor

```bash
# Ubuntu / Debian:
sudo apt-get install webp

# Verificar:
cwebp -version
```

Si `cwebp` no está disponible, los archivos se guardan con su extensión original sin error.
Si `webp.enabled: false`, nunca intenta la conversión.

### Servir archivos con Nginx

```nginx
server {
    listen 443 ssl;
    server_name miapp.com;

    location /uploads/ {
        alias /var/uploads/;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## `spring-cms` — Páginas CMS

**ArtifactId:** `spring-cms`  
**Paquete:** `dev.pimon.cms`  
**Depende de:** `spring-common`, `spring-security`

Almacena páginas completas con sus bloques de contenido serializado como JSON.
El frontend las renderiza con el componente `<lib-cms-page>` de `@org/cms`.

### Entidad y tabla

```
cms_pages
  id           VARCHAR(36) PK
  slug         VARCHAR UNIQUE NOT NULL  — identificador URL-friendly, ej. "home", "sobre-nosotros"
  title        VARCHAR NOT NULL
  description  VARCHAR                  — para SEO (meta description)
  blocks_json  TEXT NOT NULL DEFAULT '[]'  — array JSON de bloques
  published    BOOLEAN DEFAULT false    — false = borrador, no visible en la API pública
  created_at, updated_at
```

Los bloques se guardan como JSON en la columna `blocks_json`. Son exactamente los mismos tipos
que usa `@org/cms` en el frontend: `hero`, `features`, `faq`, `pricing`, `gallery`, etc.

> La API pública solo devuelve páginas con `published = true`.
> Los borradores (`published = false`) solo son visibles en los endpoints de admin.

### Endpoints públicos

```
GET /api/cms/pages           → listar páginas publicadas (sin bloques — para menús/navegación)
GET /api/cms/pages/{slug}    → página completa con todos sus bloques (solo si published = true)
```

Añadir a `public-paths`: `"/api/cms/**"`

Respuesta de `GET /api/cms/pages` (lista):

```json
{
  "success": true,
  "data": [
    { "id": "uuid", "slug": "home", "title": "Inicio", "description": "Bienvenido a MiApp", "published": true, "updatedAt": "..." },
    { "id": "uuid", "slug": "sobre-nosotros", "title": "Sobre nosotros", "published": true, "updatedAt": "..." }
  ]
}
```

Respuesta de `GET /api/cms/pages/home`:

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "slug": "home",
    "title": "Inicio",
    "description": "Bienvenido a MiApp",
    "blocks": [
      {
        "type": "hero",
        "data": {
          "title": "Tu solución digital",
          "image": "https://miapp.com/uploads/cms/hero.webp",
          "cta": { "label": "Ver más", "href": "/servicios" }
        }
      },
      { "type": "features", "data": { "..." : "..." } }
    ],
    "updatedAt": "2025-06-15T10:00:00"
  }
}
```

Error si la página no existe o no está publicada → `404`.

### Endpoints de administración (requiere `ROLE_ADMIN` o `ROLE_EDITOR`)

```
GET    /api/admin/cms/pages                        → listar todas (publicadas Y borradores)
GET    /api/admin/cms/pages/{id}                   → obtener por ID (incluye bloques)
POST   /api/admin/cms/pages                        → crear página (empieza como borrador si published=false)
PUT    /api/admin/cms/pages/{id}                   → actualizar contenido
PATCH  /api/admin/cms/pages/{id}/toggle-published  → publicar / despublicar
DELETE /api/admin/cms/pages/{id}                   → eliminar permanentemente
```

Request de creación/actualización:

```json
{
  "slug": "sobre-nosotros",
  "title": "Sobre Nosotros",
  "description": "Conoce el equipo detrás de MiApp",
  "published": true,
  "blocks": [
    {
      "type": "hero",
      "data": {
        "title": "Sobre nosotros",
        "image": "https://miapp.com/uploads/cms/equipo.webp",
        "align": "center"
      }
    },
    {
      "type": "text-image",
      "data": {
        "title": "Quiénes somos",
        "text": "Somos un equipo apasionado por el desarrollo web...",
        "image": "https://miapp.com/uploads/cms/oficina.webp",
        "imagePosition": "right"
      }
    },
    {
      "type": "features",
      "data": {
        "title": "Nuestros valores",
        "cols": 3,
        "items": [
          { "icon": "⚡", "title": "Rapidez", "text": "Entregamos en tiempo récord" },
          { "icon": "🎨", "title": "Diseño", "text": "Interfaces modernas" },
          { "icon": "🔒", "title": "Seguridad", "text": "Código auditado" }
        ]
      }
    }
  ]
}
```

Error `409` si el slug ya está en uso al crear.

### Integración frontend-backend

**Renderizar páginas publicadas** (lado público de la web):

```typescript
// Angular: cargar la página por slug y renderizarla
ngOnInit(): void {
  this.http.get<any>('/api/cms/pages/home').subscribe(res => {
    this.pagina = res.data;   // CmsPage — lista de bloques incluida
  });
}
```

```html
<lib-cms-page [page]="pagina" />
```

**Administrar páginas** (panel de admin):

La librería `@org/cms-editor` proporciona el panel de administración completo. Se conecta
directamente a los endpoints `/api/admin/cms/pages` y añade vista previa en tiempo real.
Consulta la [documentación de `@org/cms-editor`](./frontend.md#orgcms-editor--panel-de-administración-cms).

```typescript
// app.routes.ts — ruta de admin con editor CMS
{
  path: 'admin',
  loadComponent: () => import('./pages/admin/admin.page').then(m => m.AdminPage),
},
{
  path: 'cms-preview',   // necesario para el iframe de previsualización
  loadComponent: () => import('@org/cms-editor').then(m => m.CmsPreviewReceiverComponent),
},
```

```typescript
// admin.page.ts
import { CmsPagesManagerComponent } from '@org/cms-editor';

@Component({
  standalone: true,
  imports: [CmsPagesManagerComponent],
  template: `<lib-cms-pages-manager />`,
})
export class AdminPage {}
```

---

## `spring-reservations` — Sistema de reservas

**ArtifactId:** `spring-reservations`  
**Paquete:** `dev.pimon.reservations`  
**Depende de:** `spring-common`, `spring-security`, `spring-emails`

> `spring-reservations` envía emails automáticamente al crear una reserva (`pending`) y al
> cambiar su estado (`confirmed`, `cancelled`, `completed`). Para deshabilitar solo los emails
> sin quitar la dependencia, pon `pimon.email.enabled: false` en `application.yml`.

### Entidades y tablas

```
reservation_services           ← nombre real de la tabla en BD
  id                  VARCHAR(36) PK
  name                VARCHAR NOT NULL
  description         TEXT
  duration_minutes    INT NOT NULL        — duración del servicio en minutos
  price               DECIMAL            — null = precio a consultar
  image_url           VARCHAR
  color               VARCHAR            — color del badge en el calendario (#hex)
  active              BOOLEAN DEFAULT true
  created_at, updated_at

bookings
  id            VARCHAR(36) PK
  service_id    FK → reservation_services.id
  date          DATE NOT NULL           — LocalDate → '2025-06-15'
  time          VARCHAR(5) NOT NULL     — String "HH:mm" → '10:30'
  client_name   VARCHAR NOT NULL
  client_email  VARCHAR NOT NULL
  client_phone  VARCHAR NOT NULL
  notes         TEXT
  status        VARCHAR                 — enum: PENDING, CONFIRMED, CANCELLED, COMPLETED
                                          devuelto en minúsculas en la API: 'pending', etc.
  created_at, updated_at
```

### Cómo se calculan los slots de disponibilidad

El servicio `AvailabilityService` genera slots automáticamente según la configuración:

- Empieza en `work-start` y avanza de `slot-interval-minutes` en `slot-interval-minutes`
- El último slot posible es `work-end - duration_minutes` del servicio (para que quepa la cita)
- Marca como no disponibles los slots que ya tienen una reserva ese día

Ejemplo con `work-start: 09:00`, `work-end: 20:00`, `slot-interval-minutes: 60`,
servicio de 90 minutos:
- Slots generados: 09:00, 10:00, 11:00 ... 18:00 (el 19:00 no cabe porque acabaría a las 20:30)
- Si hay una reserva a las 10:00 → ese slot aparece con `available: false`

### Endpoints públicos (sin autenticación)

#### Listar servicios

```
GET /api/services
```

Respuesta:

```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "name": "Corte de pelo",
      "description": "Corte clásico o moderno a elegir",
      "durationMinutes": 45,
      "price": 25.00,
      "imageUrl": null,
      "color": "#6366f1"
    },
    {
      "id": "uuid",
      "name": "Tinte completo",
      "description": null,
      "durationMinutes": 120,
      "price": null,
      "imageUrl": null,
      "color": "#f59e0b"
    }
  ]
}
```

> `price: null` significa "precio a consultar" — el campo es opcional al crear el servicio.

#### Consultar disponibilidad de slots

```
GET /api/availability?serviceId=uuid&date=2025-06-15
```

Parámetros obligatorios:
- `serviceId` — UUID del servicio (obtenido de `GET /api/services`)
- `date` — fecha en formato ISO: `YYYY-MM-DD`

Respuesta:

```json
{
  "success": true,
  "data": [
    { "time": "09:00", "available": true,  "label": "09:00 h" },
    { "time": "10:00", "available": false, "label": "10:00 h" },
    { "time": "11:00", "available": true,  "label": "11:00 h" },
    { "time": "12:00", "available": true,  "label": "12:00 h" }
  ]
}
```

> `label` siempre tiene el formato `"HH:mm h"` (generado automáticamente). El frontend puede mostrarlo directamente o usar `time` para construir su propio label.

#### Crear reserva

```
POST /api/bookings
Content-Type: application/json

{
  "serviceId":   "uuid-del-servicio",
  "date":        "2025-06-15",
  "time":        "11:00",
  "clientName":  "María García",
  "clientEmail": "maria@ejemplo.com",
  "clientPhone": "+34 600 123 456",
  "notes":       "Prefiero franja de mañana"
}
```

Validaciones del request:
- `serviceId` — obligatorio
- `date` — obligatorio, formato exacto `YYYY-MM-DD` (validado con `@Pattern`)
- `time` — obligatorio, formato exacto `HH:mm` (validado con `@Pattern`)
- `clientName`, `clientEmail`, `clientPhone` — obligatorios
- `notes` — opcional

Si el formato de `date` o `time` no coincide → `400 Bad Request: "Formato: YYYY-MM-DD"` / `"Formato: HH:mm"`.

Respuesta `201 Created`:

```json
{
  "success": true,
  "message": "Reserva creada correctamente",
  "data": {
    "id": "uuid",
    "service": {
      "id": "uuid",
      "name": "Corte de pelo",
      "description": "Corte clásico o moderno a elegir",
      "durationMinutes": 45,
      "price": 25.0,
      "imageUrl": null,
      "color": "#6366f1"
    },
    "date": "2025-06-15",
    "time": "11:00",
    "clientName": "María García",
    "clientEmail": "maria@ejemplo.com",
    "clientPhone": "+34 600 123 456",
    "notes": "Prefiero franja de mañana",
    "status": "pending",
    "createdAt": "2025-06-10T12:30:00"
  }
}
```

### Endpoints de administración (requiere `ROLE_ADMIN` o `ROLE_EDITOR`)

```
GET    /api/admin/reservations/services           → listar todos (incluye inactivos)
POST   /api/admin/reservations/services           → crear servicio
PUT    /api/admin/reservations/services/{id}      → actualizar servicio
PATCH  /api/admin/reservations/services/{id}/toggle-active  → activar/desactivar

GET    /api/admin/reservations/bookings           → listar reservas (paginado)
GET    /api/admin/reservations/bookings/{id}      → detalle de una reserva
PATCH  /api/admin/reservations/bookings/{id}/status  → cambiar estado
```

Request de creación/actualización de servicio:

```json
{
  "name": "Coloración completa",
  "description": "Tinte de raíz a puntas",
  "durationMinutes": 120,
  "price": 65.00,
  "imageUrl": "https://miapp.com/uploads/misc/tinte.webp",
  "color": "#f59e0b"
}
```

Campos del request:

| Campo | Obligatorio | Descripción |
|---|---|---|
| `name` | Sí (`@NotBlank`) | Nombre del servicio |
| `durationMinutes` | Sí (mínimo 5) | Duración en minutos |
| `description` | No | Descripción visible al cliente |
| `price` | No | Precio (>= 0). `null` = precio a consultar |
| `imageUrl` | No | URL de imagen del servicio |
| `color` | No | Color hex del badge en el calendario (`#f59e0b`) |

Parámetros de `GET /api/admin/reservations/bookings`:
- `status` — filtrar por estado: `pending`, `confirmed`, `cancelled`, `completed` (opcional)
- `page`, `size`, `sort` — paginación estándar

Request de cambio de estado:

```json
{ "status": "confirmed" }
```

Estados válidos: `pending`, `confirmed`, `cancelled`, `completed`.

### Configuración

```yaml
pimon:
  reservations:
    work-start: "09:00"          # hora de inicio de la jornada
    work-end: "20:00"            # hora de fin de la jornada
    work-days:                   # días con disponibilidad (nombre en inglés, mayúsculas)
      - TUESDAY
      - WEDNESDAY
      - THURSDAY
      - FRIDAY
      - SATURDAY
    slot-interval-minutes: 30    # intervalo entre slots (30 → 09:00, 09:30, 10:00...)
```

---

## `spring-emails` — Envío de emails

**ArtifactId:** `spring-emails`  
**Paquete:** `dev.pimon.email`  
**Depende de:** `spring-common`

> **Módulo independiente.** No requiere `spring-reservations` ni ningún otro módulo de negocio.
> Puedes incluirlo en cualquier proyecto Spring Boot e inyectar `EmailService` donde necesites.

Usa JavaMail y Thymeleaf para enviar emails HTML. Con `enabled: false` solo registra en el log
sin enviar, lo que facilita el desarrollo local.

### Configuración

```yaml
pimon:
  email:
    enabled: true                    # false → solo log, no envía emails (útil en dev)
    from: no-reply@miapp.com
    from-name: Mi Aplicación
    admin-email: admin@miapp.com     # recibe notificaciones de reservas y formularios de contacto
    # Marca para las plantillas integradas (newsletter, etc.) — opcional
    brand-name: Mi Negocio           # nombre mostrado en la cabecera de los emails
    primary-color: "#2c1a0a"         # color de cabecera/fondo
    accent-color: "#c17f3c"          # color de botones y enlaces
    logo-url: "https://miapp.com/logo.png"

# ⚠️ Si admin-email está vacío o no configurado, sendBookingAdminNotification() y
# sendContactForm() no envían nada — ni log, ni error. Asegúrate de configurarlo.

spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USER}
    password: ${MAIL_PASSWORD}       # para Gmail: contraseña de aplicación, no la real
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```

> **Gmail:** el `password` debe ser una contraseña de aplicación, no la contraseña de la cuenta.
> Crear en: Google Account → Security → Verificación en dos pasos → Contraseñas de aplicación.

### Uso independiente — casos comunes sin reservaciones

Basta con inyectar `EmailService` en cualquier `@Service` o `@RestController` tuyo:

```java
import dev.pimon.email.service.EmailService;

@Service
@RequiredArgsConstructor
public class MiServicio {
    private final EmailService emailService;
    // ...
}
```

#### Email de bienvenida al registrarse

```java
// Llamar desde tu servicio de registro, justo después de crear el usuario
public void enviarBienvenida(String nombre, String email) {
    emailService.sendTemplate(
        email,
        "¡Bienvenido a Mi Aplicación!",
        "bienvenida",          // src/main/resources/templates/bienvenida.html
        Map.of("nombre", nombre)
    );
}
```

Plantilla `src/main/resources/templates/bienvenida.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
  <h2>¡Hola, <span th:text="${nombre}">Usuario</span>!</h2>
  <p>Tu cuenta ha sido creada correctamente. Ya puedes acceder a la plataforma.</p>
  <a href="https://miapp.com/inicio"
     style="background:#1976d2;color:#fff;padding:12px 24px;border-radius:4px;text-decoration:none">
    Acceder ahora
  </a>
</body>
</html>
```

#### Recuperación de contraseña

```java
public void enviarRecuperacion(String email, String token) {
    String url = "https://miapp.com/reset-password?token=" + token;
    emailService.sendTemplate(
        email,
        "Restablece tu contraseña",
        "recuperar-password",
        Map.of("url", url, "expiraEn", "1 hora")
    );
}
```

#### Formulario de contacto (endpoint completo)

```java
// ContactoController.java — endpoint público que recibe el formulario
@RestController
@RequestMapping("/api/contacto")
@RequiredArgsConstructor
public class ContactoController {

    private final EmailService emailService;

    public record ContactoRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @Email @NotBlank(message = "El email no es válido") String email,
        String telefono,   // opcional
        @NotBlank(message = "El mensaje es obligatorio") String mensaje
    ) {}

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> contacto(
            @Valid @RequestBody ContactoRequest req) {

        ContactFormData data = ContactFormData.builder()
            .fromName(req.nombre())
            .fromEmail(req.email())
            .phone(req.telefono())
            .subject("Consulta desde la web")
            .message(req.mensaje())
            .build();

        emailService.sendContactForm(data);  // va al admin-email de application.yml

        return ResponseEntity.ok(ApiResponse.noContent("Mensaje enviado correctamente"));
    }
}
```

Añadir a `public-paths` para que no requiera login:

```yaml
pimon:
  security:
    public-paths:
      - "/api/contacto"
```

#### Notificación arbitraria (HTML directo)

```java
// Para casos puntuales donde no vale la pena crear una plantilla:
emailService.sendHtml(
    "cliente@ejemplo.com",
    "Tu pedido ha sido enviado",
    """
    <h2>¡Tu pedido está en camino!</h2>
    <p>Número de seguimiento: <strong>%s</strong></p>
    <p>Entrega estimada: %s</p>
    """.formatted(trackingCode, fechaEstimada)
);
```

### EmailService — métodos disponibles (referencia completa)

| Método | Cuándo usarlo |
|---|---|
| `sendBookingConfirmation(data)` | Nueva reserva — al cliente |
| `sendBookingAdminNotification(data)` | Nueva reserva — al admin |
| `sendBookingStatusUpdate(data)` | Cambio de estado de reserva — al cliente |
| `sendContactForm(data)` | Formulario de contacto — al admin |
| `sendTemplate(to, subject, template, vars)` | Cualquier email con plantilla Thymeleaf propia |
| `sendHtml(to, subject, html)` | Cualquier email con HTML directo |

Todos los métodos de reserva tienen una sobrecarga que acepta `@Nullable String customHtml`
para sustituir la plantilla predefinida por HTML propio.

### Métodos de reserva con datos preformateados

> **Si usas `spring-reservations` no necesitas llamar a estos métodos** — la librería los llama
> automáticamente. Úsalos solo si construyes tu propio sistema de reservas sin `spring-reservations`.

#### Confirmación de reserva al cliente (uso manual)

Campos de `BookingEmailData`:

| Campo | Obligatorio | Descripción |
|---|---|---|
| `clientName` | Sí | Nombre del cliente |
| `clientEmail` | Sí | Email del cliente (destino del email) |
| `serviceName` | Sí | Nombre del servicio reservado |
| `date` | Sí | Fecha en formato `YYYY-MM-DD` |
| `time` | Sí | Hora en formato `HH:mm` |
| `status` | Sí | `'pending'` \| `'confirmed'` \| `'cancelled'` \| `'completed'` |
| `notes` | No | Notas adicionales del cliente |
| `studioName` | No | Nombre del negocio (aparece en la firma) |
| `studioEmail` | No | Email del negocio |
| `studioPhone` | No | Teléfono del negocio |

```java
// Solo necesario si NO usas spring-reservations:
BookingEmailData data = BookingEmailData.builder()
    .clientName("María García")
    .clientEmail("maria@ejemplo.com")
    .serviceName("Corte de pelo")
    .date("2025-06-15")      // formato 'YYYY-MM-DD'
    .time("11:00")           // formato 'HH:mm'
    .status("pending")       // 'pending' | 'confirmed' | 'cancelled' | 'completed'
    // Datos del negocio que aparecen en la firma del email (opcionales):
    .studioName("Mi Estudio")
    .studioEmail("info@miestudio.com")
    .studioPhone("+34 900 123 456")
    .build();

emailService.sendBookingConfirmation(data);       // email al cliente
emailService.sendBookingAdminNotification(data);  // notificación al admin

// Con HTML personalizado en lugar de la plantilla predefinida:
// emailService.sendBookingConfirmation(data, "<h1>Reserva recibida</h1>");
```

#### Cambio de estado de una reserva

```java
// Cuando el admin confirma, cancela o marca como completada:
BookingEmailData data = BookingEmailData.builder()
    .clientName(...)
    .clientEmail(...)
    .serviceName(...)
    .date(...)
    .time(...)
    .status("confirmed")   // "confirmed" | "cancelled" | "completed"
    .studioName(...)
    .build();

email.sendBookingStatusUpdate(data);
// El asunto cambia automáticamente según status:
// "confirmed"  → "✓ Cita confirmada — Corte de pelo"
// "cancelled"  → "Cita cancelada — Corte de pelo"
// "completed"  → "¡Gracias por tu visita!"

// Con HTML personalizado en lugar de la plantilla predefinida:
// email.sendBookingStatusUpdate(data, "<h1>Tu cita ha sido confirmada</h1>");
```

#### Formulario de contacto

```java
import dev.pimon.email.dto.ContactFormData;

ContactFormData data = ContactFormData.builder()
    .fromName("Carlos López")
    .fromEmail("carlos@ejemplo.com")
    .phone("+34 600 999 888")    // opcional
    .subject("Consulta sobre precios")
    .message("Hola, me gustaría saber los precios de...")
    .build();

email.sendContactForm(data);
// Se envía al admin-email configurado en application.yml

// Con HTML personalizado:
// email.sendContactForm(data, "<h1>Nuevo mensaje</h1><p>De: " + data.getFromName() + "</p>");
```

#### Plantilla personalizada

```java
// Envía un email usando una plantilla Thymeleaf propia:
email.sendTemplate(
    "destinatario@ejemplo.com",
    "Verifica tu email",
    "verificar-email",    // nombre del archivo en resources/templates/ (sin .html)
    Map.of(
        "nombre", "Juan",
        "url", "https://miapp.com/verificar/token-abc123",
        "expiraEn", "24 horas"
    )
);
```

#### HTML directo (sin plantilla)

```java
String html = "<h1>Hola " + nombre + "!</h1><p>Tu código es: <b>" + codigo + "</b></p>";
email.sendHtml("destinatario@ejemplo.com", "Tu código de acceso", html);
```

### Crear plantillas Thymeleaf personalizadas

Las plantillas van en `src/main/resources/templates/` de **tu aplicación** (no de la librería):

```html
<!-- src/main/resources/templates/verificar-email.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background: #f5f5f5; }
    .card  { background: #fff; border-radius: 8px; padding: 32px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
    .btn   { display: inline-block; background: #1976d2; color: #fff; padding: 12px 28px; border-radius: 4px; text-decoration: none; font-weight: 600; }
    .muted { color: #888; font-size: 0.9em; }
  </style>
</head>
<body>
  <div class="card">
    <h2>Verifica tu email</h2>
    <p>Hola, <strong th:text="${nombre}">Usuario</strong>!</p>
    <p>Haz clic en el botón para verificar tu dirección de email:</p>
    <a th:href="${url}" class="btn">Verificar email</a>
    <p class="muted">Este enlace expira en <span th:text="${expiraEn}">24 horas</span>.</p>
    <p class="muted">Si no solicitaste esto, ignora este email.</p>
  </div>
</body>
</html>
```

---

## `spring-ecommerce` — Pedidos y pagos

**ArtifactId:** `spring-ecommerce`  
**Paquete:** `dev.pimon.ecommerce`  
**Depende de:** `spring-common`, `spring-security`, `spring-products`

### Entidades y tablas

```
orders
  id                      VARCHAR(36) PK
  user_id                 VARCHAR(36) NOT NULL    — UUID del usuario que compra
  user_email              VARCHAR NOT NULL
  total                   DECIMAL NOT NULL
  status                  VARCHAR NOT NULL        — ver enum OrderStatus
  stripe_payment_intent_id VARCHAR                — pi_xxxxx (null si stripe.enabled=false)
  stripe_client_secret     VARCHAR                — pi_xxxxx_secret_yyy (para el frontend)
  shipping_name           VARCHAR
  shipping_address        VARCHAR
  shipping_city           VARCHAR
  shipping_postal_code    VARCHAR
  shipping_country        VARCHAR NOT NULL
  shipping_phone          VARCHAR
  created_at, updated_at

order_items
  id                VARCHAR(36) PK
  order_id          FK → orders.id
  product_id        VARCHAR(36)     — snapshot del ID del producto
  product_name      VARCHAR         — snapshot del nombre (persiste aunque el producto se borre)
  product_image_url VARCHAR         — snapshot de la imagen
  unit_price        DECIMAL         — snapshot del precio en el momento de la compra
  quantity          INT
```

### Estados de un pedido (`OrderStatus`)

| Estado | Descripción |
|---|---|
| `PENDING_PAYMENT` | Creado, esperando que Stripe confirme el pago |
| `PAID` | Stripe confirmó el pago — el webhook actualizó el estado |
| `PROCESSING` | El negocio está preparando el envío |
| `SHIPPED` | Enviado — en camino al cliente |
| `DELIVERED` | Entregado al cliente |
| `CANCELLED` | Cancelado (puede ser por el cliente o por fallo de pago) |
| `REFUNDED` | Devuelto |

### Flujo de pago con Stripe

```
1. Frontend llama:  POST /api/checkout/create-intent
2. Backend valida los productos (stock, precio actual) y los descuenta del stock
3. Backend crea la orden en BD con estado PENDING_PAYMENT
4. Backend crea un PaymentIntent en Stripe con el total en céntimos
5. Backend responde: { orderId, clientSecret, total, currency }
6. Frontend usa Stripe.js con el clientSecret → el usuario introduce la tarjeta
7. Stripe llama al webhook: POST /api/payments/webhook
8. Backend valida la firma del webhook y marca la orden como PAID
9. Si el pago falla → la orden queda en CANCELLED
```

> El webhook `/api/payments/webhook` **debe estar en `public-paths`** — Stripe no envía JWT.

### POST /api/checkout/create-intent (requiere autenticación)

```
POST /api/checkout/create-intent
Authorization: Bearer <token>
Content-Type: application/json

{
  "items": [
    { "productId": "uuid", "quantity": 2 },
    { "productId": "uuid", "quantity": 1 }
  ],
  "shippingName":       "María García",
  "shippingAddress":    "Calle Mayor 1",
  "shippingCity":       "Madrid",
  "shippingPostalCode": "28001",
  "shippingCountry":    "ES",
  "shippingPhone":      "+34 600 123 456"
}
```

Validaciones: `items` es obligatorio y no puede estar vacío (`@NotEmpty`). `shippingCountry` es obligatorio (`@NotBlank`). El resto de campos de envío son opcionales.

Respuesta `200 OK`:

```json
{
  "success": true,
  "message": "PaymentIntent creado. Usa el clientSecret con Stripe.js para completar el pago.",
  "data": {
    "orderId": "uuid",
    "clientSecret": "pi_3xxx_secret_yyy",
    "total": 624.99,
    "currency": "EUR"
  }
}
```

Errores posibles:

```json
{ "success": false, "message": "Producto no disponible: uuid" }
{ "success": false, "message": "Stock insuficiente para: Silla Nórdica (disponible: 3)" }
```

> Si `stripe.enabled: false`, el sistema simula el pago y devuelve un clientSecret ficticio.
> Útil durante el desarrollo para probar el flujo sin cuenta de Stripe.

### Integración con Stripe.js en el frontend

```typescript
// Después de recibir el clientSecret del backend:
import { loadStripe } from '@stripe/stripe-js';

const stripe = await loadStripe('pk_test_...'); // clave pública de Stripe
const elements = stripe.elements({ clientSecret });
const paymentElement = elements.create('payment');
paymentElement.mount('#payment-element');

// Al enviar el formulario:
const { error } = await stripe.confirmPayment({
  elements,
  confirmParams: { return_url: 'https://miapp.com/pedido-confirmado' },
});
```

### Endpoints de pedidos del usuario autenticado

```
GET /api/orders/my         → mis pedidos (paginado, ?page=0&size=10)
GET /api/orders/my/{id}    → detalle de un pedido mío
```

El endpoint `/api/orders/my/{id}` verifica que el pedido pertenece al usuario. Si intenta acceder
al pedido de otro usuario → `403 Forbidden`.

**Estructura del `OrderDto`** (respuesta de ambos endpoints):

```json
{
  "id": "uuid",
  "userId": "uuid-del-usuario",
  "userEmail": "usuario@ejemplo.com",
  "items": [
    {
      "productId": "uuid",
      "productName": "Silla Nórdica",
      "productImageUrl": "https://miapp.com/uploads/products/silla.webp",
      "unitPrice": 299.99,
      "quantity": 2,
      "subtotal": 599.98
    }
  ],
  "total": 599.98,
  "status": "PENDING_PAYMENT",
  "shippingName": "María García",
  "shippingAddress": "Calle Mayor 1",
  "shippingCity": "Madrid",
  "shippingCountry": "ES",
  "createdAt": "2025-06-15T10:30:00"
}
```

> El `status` se devuelve en MAYÚSCULAS (enum Java). Los estados son: `PENDING_PAYMENT`, `PAID`,
> `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `REFUNDED`.

### Endpoints de administración (requiere `ROLE_ADMIN`)

```
GET    /api/admin/orders         → todos los pedidos (paginado)
GET    /api/admin/orders/{id}    → detalle de cualquier pedido
PUT    /api/admin/orders/{id}/status  → cambiar estado
```

El estado se pasa como **query parameter** (no como body):

```
PUT /api/admin/orders/uuid/status?status=PROCESSING
```

Estados que el admin puede asignar manualmente: `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `REFUNDED`.

### Configuración de Stripe

```yaml
pimon:
  ecommerce:
    currency: EUR              # moneda ISO 4217 en minúsculas
    stripe:
      secret-key: ${STRIPE_SECRET_KEY}         # sk_test_... o sk_live_...
      webhook-secret: ${STRIPE_WEBHOOK_SECRET} # whsec_...
      enabled: true            # false = modo simulado sin llamadas reales a Stripe
```

Para registrar el webhook en Stripe Dashboard:
- URL: `https://tudominio.com/api/payments/webhook`
- Eventos a escuchar: `payment_intent.succeeded`, `payment_intent.payment_failed`

---

## `spring-newsletter` — Newsletter y campañas

**ArtifactId:** `spring-newsletter`
**Paquete:** `dev.pimon.newsletter`
**Depende de:** `spring-common`, `spring-emails`

Suscripciones con **doble opt-in** (alta → email de confirmación → clic → bienvenida), baja por enlace,
y envío de **campañas** a los suscriptores confirmados con plantillas HTML parametrizadas con la marca
del negocio. Es el par backend de `@adrianmartincano/ng-newsletter`.

### Configuración

```yaml
pimon:
  newsletter:
    base-url: https://api.midominio.com                                   # para construir los enlaces de los emails
    confirm-redirect-url: https://www.midominio.com/?newsletter=confirmado
    unsubscribe-redirect-url: https://www.midominio.com/?newsletter=baja
    error-redirect-url: https://www.midominio.com/?newsletter=error      # enlace inválido/caducado/ya usado
  security:
    public-paths:
      - "/api/newsletter/**"    # los endpoints públicos deben estar abiertos
```

> Requiere `spring-emails` configurado (`spring.mail.*` + `pimon.email.enabled: true`).
> Los campos `pimon.email.brand-name / primary-color / accent-color / logo-url` personalizan
> las plantillas de los emails (confirmación, bienvenida y campañas).

### Endpoints públicos

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/newsletter/subscribe` | Alta: `{ "email": "...", "source": "footer" }`. Crea el suscriptor en estado `pending` y envía el email de confirmación. |
| GET | `/api/newsletter/confirm?token=...` | Enlace del email. Confirma y redirige a `confirm-redirect-url` (o a `error-redirect-url` si el token no es válido). |
| GET | `/api/newsletter/unsubscribe?token=...` | Baja por enlace. Redirige a `unsubscribe-redirect-url` (o a `error-redirect-url`). |

```bash
curl -X POST https://api.midominio.com/api/newsletter/subscribe \
  -H "Content-Type: application/json" \
  -d '{"email":"persona@ejemplo.com","source":"popup-home"}'

# → { "success": true, "message": "¡Casi listo! Revisa tu email...",
#     "data": { "email": "persona@ejemplo.com", "status": "pending", "alreadySubscribed": false } }
```

### Endpoints de administración (rol ADMIN)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/admin/newsletter/subscribers?status=confirmed&page=0` | Lista paginada (filtro: `pending`/`confirmed`/`unsubscribed`). |
| GET | `/api/admin/newsletter/subscribers/export?status=confirmed` | Export CSV. |
| GET | `/api/admin/newsletter/campaign-templates` | Catálogo de plantillas de campaña disponibles. |
| POST | `/api/admin/newsletter/campaigns/preview` | Renderiza el HTML de una campaña sin enviarla: `{ subject, body, templateId }`. |
| POST | `/api/admin/newsletter/campaigns` | Envía la campaña a todos los confirmados: `{ subject, body, templateId }`. |
| GET | `/api/admin/newsletter/campaigns` | Historial de campañas enviadas. |

### Flujo completo

```
Usuario escribe su email (lib-newsletter-form / popup)
        ↓ POST /api/newsletter/subscribe
Estado: pending + email "Confirma tu suscripción" (plantilla con marca)
        ↓ clic en el botón del email
GET /api/newsletter/confirm?token=...
        ↓ 302 → https://web/?newsletter=confirmado  (el front muestra un toast)
Estado: confirmed + email de bienvenida
        ↓ (más adelante) el admin redacta una campaña con plantilla
POST /api/admin/newsletter/campaigns → email a todos los confirmados,
        cada uno con su enlace de baja personalizado
```

### Entidades

- `NewsletterSubscriber` — email único, `status` (`PENDING/CONFIRMED/UNSUBSCRIBED`), `source`,
  tokens de confirmación y de baja, timestamps.
- `NewsletterCampaign` — asunto, cuerpo, nº de destinatarios, fecha de envío.

---

## Ejemplo de aplicación completa

Un proyecto que use todos los módulos declara estas dependencias en su `pom.xml`:

```xml
<dependencies>
    <dependency><groupId>io.github.adrianmartincano</groupId><artifactId>spring-security</artifactId></dependency>
    <dependency><groupId>io.github.adrianmartincano</groupId><artifactId>spring-users</artifactId></dependency>
    <dependency><groupId>io.github.adrianmartincano</groupId><artifactId>spring-products</artifactId></dependency>
    <dependency><groupId>io.github.adrianmartincano</groupId><artifactId>spring-ecommerce</artifactId></dependency>
    <dependency><groupId>io.github.adrianmartincano</groupId><artifactId>spring-storage</artifactId></dependency>
    <dependency><groupId>io.github.adrianmartincano</groupId><artifactId>spring-cms</artifactId></dependency>
    <dependency><groupId>io.github.adrianmartincano</groupId><artifactId>spring-reservations</artifactId></dependency>
    <dependency><groupId>io.github.adrianmartincano</groupId><artifactId>spring-emails</artifactId></dependency>

    <!-- Base de datos -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <!-- O H2 para desarrollo rápido en memoria: -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```
