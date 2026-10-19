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
  blog/                ← Blog: posts + comentarios                    → paquete: dev.pimon.blog
  forum/               ← Foro: categorías, hilos, mensajes            → paquete: dev.pimon.forum
  storage/             ← Subida de imágenes a disco, conversión WebP  → paquete: dev.pimon.storage
  cms/                 ← Páginas CMS con bloques JSON                 → paquete: dev.pimon.cms
  reservations/        ← Servicios, disponibilidad y reservas (citas)  → paquete: dev.pimon.reservations
  dining/              ← Reservas de mesa: zonas, mesas, turnos        → paquete: dev.pimon.dining
  menu/                ← Carta y menú del día (sin imágenes)           → paquete: dev.pimon.menu
  emails/              ← Emails HTML con Thymeleaf, marca configurable → paquete: dev.pimon.email
  newsletter/          ← Suscripciones doble opt-in y campañas         → paquete: dev.pimon.newsletter
  google/              ← Entrar con Google (apagado por defecto)       → paquete: dev.pimon.google
  fx/                  ← Tasas de cambio para el front multi-divisa    → paquete: dev.pimon.fx
  events/              ← Webs de eventos: RSVP y resumen               → paquete: dev.pimon.events
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
    <version>0.0.1</version>
</dependency>
<dependency>
    <groupId>io.github.adrianmartincano</groupId>
    <artifactId>spring-users</artifactId>
    <version>0.0.1</version>
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
    jwt-secret: "mi-secreto-de-al-menos-32-caracteres-aqui"   # ⚠️ mín. 32 caracteres; la app NO arranca si es más corto, y avisa si usas el de por defecto
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
    capacity: 1                # reservas que pueden solaparse a la vez (recursos en paralelo)

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
   ↑    ↑                          ↑
   |    spring-google    spring-reservations   spring-dining   spring-menu
spring-products
   ↑
spring-ecommerce
```

- `spring-ecommerce` depende de `spring-products` para validar stock al crear un pedido.
- `spring-google` depende de `spring-users` (la cuenta que vincula o crea) y de `spring-security` (el JWT que emite). Va en módulo aparte y no dentro de ninguno de los dos por dos motivos: `spring-security` **no tiene JPA** y el vínculo necesita tabla, y meterlo en `spring-users` obligaría a **todas** las webs a tener `cuentas_google` —su autoconfiguración registra el paquete entero en JPA— aunque no usen Google.
- `spring-reservations` depende de `spring-emails` para enviar confirmaciones automáticamente al crear o actualizar una reserva. Para deshabilitar el envío sin quitar la dependencia: `pimon.email.enabled: false`.
- `spring-dining` depende de `spring-emails` por consistencia con el patrón de reservas (el envío de confirmaciones lo cablea la app consumidora; el servicio no lo acopla).
- `spring-menu` solo depende de `spring-common` y `spring-security` — **no** usa `spring-storage` (carta sin imágenes subidas) ni `spring-products`.
- `spring-blog` y `spring-forum` dependen de `spring-common` y `spring-security` (el autor sale del JWT).
- `spring-newsletter` depende de `spring-emails` (confirmaciones doble opt-in y envío de campañas).
- `spring-fx` y `spring-events` solo dependen de `spring-common`.

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

JSON de error (incluye `code`, un código estable para que el frontend ramifique sin depender del texto):

```json
{
  "success": false,
  "message": "Producto no encontrado: silla-nordica",
  "code": "NOT_FOUND"
}
```

> El campo `code` es opcional (`@JsonInclude(NON_NULL)`): solo aparece en errores. Lo rellena el
> `GlobalExceptionHandler` con el `code` del `AppException` o con un código fijo por tipo de error
> (`VALIDATION_ERROR`, `DATA_CONFLICT`, `MALFORMED_REQUEST`, `FILE_TOO_LARGE`, `UNAUTHENTICATED`,
> `ACCESS_DENIED`…).

JSON de error de validación (con mapa de campos):

```json
{
  "success": false,
  "message": "Error de validación",
  "code": "VALIDATION_ERROR",
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
| `AppException` | El del `AppException` | Cualquier `throw AppException.*()` en el código (propaga su `code`) |
| `MethodArgumentNotValidException` | 400 | Fallo de `@Valid` en un `@RequestBody` |
| `ConstraintViolationException` | 400 | Fallo de `@Validated` en `@PathVariable`/`@RequestParam` |
| `MissingServletRequestParameterException` | 400 | Falta un query param obligatorio |
| `MethodArgumentTypeMismatchException` | 400 | Parámetro de URL con tipo incorrecto (ej. texto donde se espera UUID) |
| `HttpMessageNotReadableException` | 400 | Cuerpo JSON ausente o mal formado |
| `DataIntegrityViolationException` | 409 | Conflicto de integridad en BD (unique/FK/not-null); el detalle se loguea pero **no** se expone |
| `MaxUploadSizeExceededException` | 413 | Archivo subido por encima del límite |
| `NoResourceFoundException` | 404 | Archivo estático no encontrado (`/uploads/...`) |
| `Exception` | 500 | Cualquier otro error no controlado |

En el caso de validación devuelve además el mapa de errores por campo (ver `ApiResponse` arriba).
Todas las respuestas de error incluyen el campo `code`.

---

## Configuración editable en caliente (`app_settings`)

Ajustes de **negocio** que el administrador cambia desde el panel **sin reiniciar** (a diferencia de
`application.yml`, que es de dev/ops). Viven en la tabla `app_settings`, **una fila por módulo**, con
el valor como **JSON**. `application.yml` aporta los **defaults/semilla**; la fila de BD es el valor vivo.

- Mecanismo genérico: `dev.pimon.common.settings.SettingsStore` (cacheado en memoria; cae a defaults si
  no hay fila). `store.get(key, Pojo.class, defaults)` / `store.save(key, pojo)`.
- **Nunca** secretos ni *bootstrap* en esta tabla. Caché por nodo → en multi-instancia usar `evict(...)`.

| Módulo | Ajustes | Endpoints (ADMIN) |
|---|---|---|
| `reservations` | **horario por día con turnos partidos** (`weeklyHours`), intervalo, `capacity` y **`blockedDates`** (vacaciones/festivos) | `GET/PUT /api/admin/reservations/settings` |
| `dining` | aforo total por turno (`totalCapacity`), antelación mín/máx y **`blockedDates`** (días cerrados) | `GET/PUT /api/admin/dining/settings` |
| `menu` | **presentación**: fuentes (whitelist), color de acento, layout, moneda, mostrar precios/alérgenos | `GET/PUT /api/admin/menu/presentation` |

`AvailabilityService`/`BookingService` leen estos ajustes: horario **por día** con **varios tramos**
(turno partido); día sin tramos = cerrado; fecha en `blockedDates` (vacaciones) = sin reservas. Todo
en caliente, sin reiniciar. `application.yml` aporta el horario por defecto (cada día laborable con un
tramo `work-start`–`work-end`). Añadir un ajuste = añadir un campo al POJO (sin migración, JSON).
**Decisión:** lo usan los módulos donde el dueño cambia cosas a menudo (reservas, restauración, carta);
el mecanismo es genérico y cualquier módulo puede engancharse añadiendo un POJO de ajustes.

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

## `spring-google` — Entrar con Google

**ArtifactId:** `spring-google`  
**Paquete:** `dev.pimon.google`  
**Depende de:** `spring-common`, `spring-security`, `spring-users`

Login con **Google Identity Services**: el navegador consigue un `id_token`, lo manda aquí, y
a cambio recibe **el mismo JWT que emite `/api/auth/login`**. A partir de ahí todo funciona
igual: no hay nada que refrescar contra Google y la caducidad de sesión es la tuya.

### Apagado por defecto, y eso importa

```yaml
pimon:
  google:
    habilitado: true                                   # por defecto false
    client-id: 123-miweb.apps.googleusercontent.com    # obligatorio si habilitado
```

Con `habilitado: false` **la autoconfiguración no carga nada**: ni el endpoint, ni el
decodificador, ni la entidad JPA. Es deliberado y tiene dos consecuencias buenas:

- Una aplicación con `ddl-auto: validate` **no exige la tabla** `cuentas_google` mientras no
  encienda Google.
- `GET /api/auth/google/config` responde **404**, que el front debe leer como «aquí no hay
  login con Google» y no pintar el botón.

Si lo enciendes **sin `client-id`, la aplicación no arranca** (`@NotBlank`). Es a propósito:
sin client-id no se puede comprobar el `aud`, y sin esa comprobación el login con Google es
una puerta trasera al login normal.

### Endpoints

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/api/auth/google/config` | El `client_id` con el que pintar el botón |
| `POST` | `/api/auth/google` | Cambia el `id_token` por una sesión (`LoginResponse`) |
| `GET` | `/api/auth/google/vinculada` | Si **tu** cuenta entró por Google (requiere sesión) |

> ⚠️ Añade `/api/auth/google` a tus `public-paths` de `spring-security`, o el login
> responderá 401 antes de llegar al controlador.

El cuerpo del POST es `{ "credential": "<id_token>" }` — se llama `credential` porque es el
nombre exacto del campo que devuelve la librería de Google en el front; renombrarlo aquí solo
añadiría una traducción que hay que recordar en los dos lados.

### Las cuatro comprobaciones del token

`VerificadorTokenGoogle` es **la clase que sostiene todo esto**. Un `id_token` es solo un JWT:
aceptarlo sin verificar del todo permite que cualquiera se fabrique uno con el correo de otra
persona.

1. **Firma** — que lo emitió Google, contra sus claves públicas.
2. **Caducidad** — un token robado deja de valer en una hora.
3. **`aud` = tu `client-id`** — que ese token se emitió *para tu aplicación*. Sin esto valdría
   uno emitido para cualquier otra web que use Google.
4. **`email_verified` = true** — protege las cuentas que **ya existen**: la vinculación es por
   correo, así que aceptar uno sin verificar permitiría apropiarse de la cuenta de alguien
   registrado con contraseña.

Las cuatro son obligatorias. Quitar cualquiera convierte el endpoint en un agujero. Los
rechazos **siempre devuelven el mismo mensaje**: distinguir «caducado» de «no es para esta
app» solo le sirve a quien esté probando a ver qué cuela.

### Qué pasa con la cuenta

| Situación | Qué se hace |
|---|---|
| Ya había vínculo (por `sub`) | Entra. Si cambió su correo en Google, se actualiza el nuestro |
| Existe cuenta con ese correo, sin vínculo | **Se vincula** — seguro solo porque ya se exigió `email_verified` |
| No hay nada | Se crea la cuenta, sin pedir nada más |

**Se busca por `sub`, no por el correo.** El `sub` de Google no cambia nunca; el correo sí.
Buscando por correo, quien se cambiara la dirección de Gmail acabaría en una cuenta nueva en
vez de en la suya.

**A las cuentas creadas así se les pone una contraseña imposible**: el hash de 32 bytes
aleatorios que se tiran sin guardarlos. No es un hueco — no existe ninguna cadena que produzca
ese hash y que alguien pueda escribir. Quien quiera además una contraseña normal puede pedirla
por «he olvidado mi contraseña», que le llega al correo que Google acaba de verificar que es
suyo.

Y una cuenta **dada de baja no entra**, con el mismo criterio que el login normal: si se
dejara pasar, Google sería la forma de saltarse la baja.

### La migración la escribes tú

Las librerías traen la entidad, **no** las migraciones. Con `ddl-auto: update` la tabla se
crea sola; con `validate` hace falta este SQL:

```sql
CREATE TABLE cuentas_google (
    id              varchar(36)     NOT NULL,
    created_at      timestamp(6)    NOT NULL,
    updated_at      timestamp(6)    NOT NULL,
    user_id         varchar(36)     NOT NULL,
    -- Identificador de la persona en Google. Estable de por vida.
    sub             varchar(255)    NOT NULL,
    -- Con qué dirección se vinculó. Informativo: si cambia en Google, se actualiza.
    email           varchar(255)    NOT NULL,
    vinculada_en    timestamp(6)    NOT NULL,
    CONSTRAINT cuentas_google_pkey PRIMARY KEY (id),
    -- Un `sub` apunta a una sola cuenta, y una cuenta se vincula con un solo Google. Sin
    -- estos dos únicos, dos peticiones simultáneas del mismo usuario crearían dos vínculos.
    CONSTRAINT uk_cuentas_google_sub  UNIQUE (sub),
    CONSTRAINT uk_cuentas_google_user UNIQUE (user_id),
    -- Al borrar la cuenta desaparece su vínculo.
    CONSTRAINT fk_cuentas_google_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE
);
```

**Tabla aparte, no una columna en `app_users`**: esa tabla es de `spring-users`, y añadirle
columnas rompe `ddl-auto: validate` al arrancar porque la entidad de la librería no las
conocería.

### Lo que se lleva tiempo no es el código

- **La consola de Google**: un cliente OAuth **por dominio**. Sin revisión de Google si los
  permisos son `openid email profile`.
- **La CSP**: `script-src`, `frame-src` y `connect-src` con `accounts.google.com`. Si falta
  uno, **el botón no aparece y el error de consola no dice cuál**.

### Por qué Nimbus y no `google-api-client`

Pesa muchísimo menos y es lo que ya usa Spring Security para decodificar JWT. El decodificador
se construye con `withJwkSetUri` y **no** con `JwtDecoders.fromIssuerLocation`, aunque la
segunda sea más corta: esa se conecta a Google **al crear el bean**, o sea al arrancar la
aplicación, y si Google no contestara —o el arranque fuera sin red, como en los tests— la
aplicación entera no levantaría por una función secundaria. Con `withJwkSetUri` la primera
conexión ocurre en el primer login, y Nimbus cachea las claves y las vuelve a pedir cuando
Google las rota.

El validador acepta **dos emisores** (`https://accounts.google.com` y `accounts.google.com`):
Google usa una u otra forma según el flujo, las dos son suyas, y el validador de un solo emisor
que trae Spring no sirve tal cual.

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
  created_at, updated_at

product_categories                        — desde 0.10.0: un producto, VARIAS categorías
  product_id     FK → products.id        (PK compuesta, ON DELETE CASCADE)
  category_id    FK → categories.id      (PK compuesta, SIN cascada a propósito:
                                           borrar una categoría con productos dentro lo
                                           rechaza CategoryService explicando qué hacer)
```

> **Hasta la 0.9.1 era `products.category_id`, una sola categoría.** Cambió porque obliga a
> elegir: un producto de temporada —un llavero de Halloween— pertenece a su categoría de
> siempre **y** a la de la campaña, y con una sola, meterlo en la campaña lo saca de la
> otra. Ver `decisiones/0003-un-producto-puede-estar-en-varias-categorias.md`.
>
> Al subir desde una versión anterior hay que **crear la tabla y copiar los datos**:
> `INSERT INTO product_categories (product_id, category_id) SELECT id, category_id FROM products WHERE category_id IS NOT NULL;`

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
| `category` | `string` | Slug de la categoría (no el nombre). Devuelve los productos que la tengan **entre** las suyas; la consulta lleva `distinct` porque el join produce una fila por categoría |
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
  "categoryId": "uuid-de-la-categoria",
  "active": false
}
```

Campos obligatorios en creación: `name` (`@NotBlank`), `description` (`@NotBlank`), `price` (positivo), `stock` (≥ 0), `categoryId` (`@NotBlank`).

#### `active` al crear: prepararlo antes de venderlo

**Omitirlo crea el producto publicado**, que es como se comportaba esto antes de que el campo
existiera (0.9.0): quien ya llamaba a este endpoint no cambia nada.

Mandar `false` lo crea **oculto al catálogo público**, para poder terminarlo antes de que se
vea. Hace falta más de lo que parece: en una tienda con datos que cuelgan del producto —fotos,
medidas, variantes— nada de eso puede existir hasta que el producto está guardado, porque
todos necesitan su id. Sin este campo, ese rato se pasa con el producto **a la venta y a
medias**.

> Salió de un caso real en pimon3d: una pieza recién creada aparecía en la tienda sin fotos ni
> medidas, y ahí sin medidas ni siquiera se le puede calcular el envío — o sea, a la venta sin
> poder venderse. Su panel mandaba `active: false` y **se ignoraba en silencio**, porque este
> DTO no tenía el campo.

Request de actualización (`PUT /api/admin/products/{id}`): todos los campos son opcionales — solo se actualizan los enviados.

### Endpoints de administración de categorías (requiere `ROLE_ADMIN`)

```
POST   /api/admin/categories        → crear (o recuperar una retirada con ese nombre)
PUT    /api/admin/categories/{id}   → actualizar
DELETE /api/admin/categories/{id}   → BORRA de verdad, si no tiene productos dentro
```

#### Borrar una categoría (cambió en 0.4.2)

Hasta la 0.4.1, `DELETE` solo ponía `active = false`. Como el panel únicamente pinta las
activas, **parecía borrada pero seguía ahí ocupando su nombre**: al intentar crearla otra
vez saltaba «Ya existe una categoría con ese nombre», refiriéndose a algo que no se veía
por ningún lado. El nombre quedaba quemado sin salida.

Ahora se borra de verdad. Y si tiene productos, **no se borra y se dice por qué**:

```
No se puede borrar «Figuras»: tiene 3 a la venta y 1 en la papelera.
Cámbialos de categoría primero.
```

Los productos apuntan a la categoría con clave ajena, así que borrarla con contenido los
dejaría colgados. **Cuentan también los de la papelera**, porque se pueden restaurar: si su
categoría hubiera desaparecido, la restauración devolvería un producto roto. Por eso el
mensaje los menciona aparte — en la lista de productos no se ven, y sin decirlo el aviso
parecería equivocado.

`POST` recupera una categoría retirada que tenga ese slug, en vez de rechazar el alta. Es
lo que arregla las que quedaron desactivadas antes de este cambio: se vuelven a dar de alta
y reaparecen con los datos nuevos.

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

1. Valida tipo MIME, tamaño y **firma binaria real** (magic bytes) contra la configuración
2. Si `webp.enabled = true`, el archivo es una imagen (no SVG) **y no es ya WebP**:
   - Guarda el original en un temporal `uuid-origen.ext`
   - Ejecuta `cwebp` para convertir a `uuid.webp` y borra el temporal
   - Si `cwebp` no está disponible, renombra el temporal a `uuid.ext` y lo deja sin convertir
3. Si el archivo **ya es WebP** (o no es imagen), se guarda tal cual en `uuid.ext`
4. Devuelve la URL pública: `base-url/subdir/uuid.webp`

> **Lo que ya es WebP no se convierte, y el temporal lleva sufijo `-origen`.** Sin esas dos
> cosas, subir un `.webp` se rompía: el temporal y el destino caían en el mismo nombre
> (`uuid.webp`), así que `cwebp` abría el destino para escribir y con eso **vaciaba su
> propia entrada**; y si aun así devolvía éxito, el borrado del temporal se llevaba por
> delante el fichero recién escrito. Quedaba una imagen rota o un 404. Y volver a comprimir
> algo que ya viene comprimido con pérdida solo lo empeora. Arreglado en 0.9.1.

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
    allowed-types:                # SVG NO está por defecto (riesgo XSS); añádelo solo si lo necesitas
      - image/jpeg
      - image/png
      - image/gif
      - image/webp
      - image/heic
      - image/heif
    webp:
      enabled: true
      quality: 85       # 0-100. 85 es un buen equilibrio calidad/tamaño
      cwebp-path: cwebp # ruta al binario. Por defecto busca en el PATH
```

> **Seguridad de subidas:**
> - **Validación por *magic bytes***: además del `Content-Type` (falsificable), se comprueba la firma
>   binaria real (JPEG/PNG/GIF/WEBP/HEIC). Un archivo que no sea realmente una imagen se rechaza.
> - **SVG fuera de los tipos por defecto** (riesgo XSS). Si lo añades, los `/uploads` se sirven con
>   `X-Content-Type-Options: nosniff` y los `.svg` con `Content-Disposition: attachment` (nunca inline).
> - **Sin path traversal**: la extensión se sanea y el directorio se normaliza a ruta absoluta.

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

> **Reserva de X horas + calendario público (nuevo):** la reserva acepta `durationMinutes` (alta
> admin con duración personalizada; si falta, se usa la del servicio). Endpoint público de
> ocupación **sin datos del cliente**: `GET /api/busy?from=YYYY-MM-DD&to=YYYY-MM-DD` →
> `[{ date, start, end }]`. Añade `/api/busy` a `pimon.security.public-paths`. Par frontend:
> `<lib-booking-admin>` y `<lib-public-calendar>` de `@adrianmartincano/ng-reservations`.

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
- Un slot aparece como no disponible cuando reservarlo **se solaparía por intervalo** con reservas
  activas hasta alcanzar la `capacity` (no por mera coincidencia de hora de inicio): una cita de
  10:00–13:00 marca ocupados 11:00 y 12:00, y libera 13:00.

Ejemplo con `work-start: 09:00`, `work-end: 20:00`, `slot-interval-minutes: 60`,
servicio de 90 minutos:
- Slots generados: 09:00, 10:00, 11:00 ... 18:00 (el 19:00 no cabe porque acabaría a las 20:30)
- Si hay una reserva de 10:00 a 13:00 → 10:00, 11:00 y 12:00 aparecen con `available: false`

> **Alta segura ante concurrencia (`POST /api/bookings`):** la creación toma un bloqueo pesimista
> por día y comprueba solapes dentro de la transacción → dos peticiones simultáneas no pueden crear
> reservas que se pisen (ni siquiera la primera del día). Si el horario está ocupado devuelve **409**.
> La capacidad de recursos en paralelo se configura con `pimon.reservations.capacity` (por defecto 1).
>
> **H2 en local (desde 0.12.5):** la columna del cerrojo (`reservation_day_locks.day`) se mapea
> entrecomillada (`@Column(name = "\"day\"")`) porque `DAY` es palabra reservada en H2. Antes el
> `create table` fallaba y había que añadir `NON_KEYWORDS=DAY` a la URL de H2. En Postgres es la
> misma columna `day` de siempre: actualizar no requiere migración.

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
    capacity: 1                  # nº de reservas que pueden solaparse a la vez (recursos en paralelo)
```

---

## `spring-dining` — Reservas de restaurante

**ArtifactId:** `spring-dining`  
**Paquete:** `dev.pimon.dining`  
**Depende de:** `spring-common`, `spring-security`, `spring-emails`

Reservas de **mesa** para bar/restaurante. Es un dominio **distinto** al de `spring-reservations`
(citas): allí se reserva el *tiempo de un recurso* (un profesional); aquí se reserva una **mesa** y el
eje es el **nº de comensales** (pax). La asignación es un problema de encaje de grupos en mesas
(bin-packing), no de agenda.

> **¿Cuándo cada uno?** `spring-reservations` para peluquería/tatuador/clínica (servicio + duración +
> profesional). `spring-dining` para bar/restaurante (mesa + comensales + turno). Son librerías
> independientes; una app de restaurante usa `spring-dining` (+ `spring-menu` para la carta).

### Entidades y tablas

```
dining_zones                     ← zonas del local (Terraza, Salón, Barra)
  id          VARCHAR(36) PK
  name        VARCHAR NOT NULL
  sort_order  INT
  active      BOOLEAN DEFAULT true
  created_at, updated_at

dining_tables                    ← mesas, capacidad en COMENSALES (no en tiempo)
  id            VARCHAR(36) PK
  label         VARCHAR NOT NULL     — "Mesa 4", "Barra 2"
  capacity_min  INT NOT NULL         — mínimo razonable (no sentar 2 en una mesa de 8)
  capacity_max  INT NOT NULL         — tope de pax
  combinable    BOOLEAN              — se puede juntar con otras de su zona
  sort_order    INT
  active        BOOLEAN DEFAULT true
  pos_x, pos_y  INT                  — posición opcional para el plano de sala
  zone_id       FK → dining_zones.id
  created_at, updated_at

dining_seatings                  ← turnos de servicio
  id               VARCHAR(36) PK
  name             VARCHAR NOT NULL  — "Comida", "Cena 1er turno"
  start            VARCHAR(5)        — "20:00"
  end              VARCHAR(5)        — "22:00"
  duration_minutes INT              — cuánto ocupa la mesa la reserva del turno
  sort_order       INT
  active           BOOLEAN DEFAULT true
  created_at, updated_at
dining_seating_days              ← días de la semana en los que aplica cada turno
  seating_id   FK → dining_seatings.id
  day_of_week  VARCHAR  — 'MONDAY'...'SUNDAY'

dining_reservations
  id            VARCHAR(36) PK
  date          DATE NOT NULL
  seating_id    FK → dining_seatings.id
  party_size    INT NOT NULL          — nº de comensales (eje del dominio)
  client_name   VARCHAR NOT NULL
  client_email  VARCHAR NOT NULL
  client_phone  VARCHAR NOT NULL
  notes         TEXT
  status        VARCHAR              — PENDING, CONFIRMED, SEATED, NO_SHOW, CANCELLED, COMPLETED
  deposit       DECIMAL              — señal/depósito (null = sin depósito)
  created_at, updated_at
dining_reservation_tables        ← mesa(s) asignada(s); 2+ = mesas juntadas (M:N)
  reservation_id  FK → dining_reservations.id
  table_id        FK → dining_tables.id
dining_reservation_requests      ← peticiones especiales (enum)
  reservation_id  FK → dining_reservations.id
  request         VARCHAR  — TRONA, ACCESIBLE, TERRAZA, INTERIOR, TRANQUILA, CELEBRACION, ALERGIAS

dining_day_locks                 ← fila-cerrojo por día (control de concurrencia)
  day  VARCHAR(10) PK  — 'YYYY-MM-DD'
```

Estados que **ocupan** mesa y aforo: `PENDING`, `CONFIRMED`, `SEATED`. Liberan: `NO_SHOW`,
`CANCELLED`, `COMPLETED`.

### Cómo se asigna la mesa (best-fit + combinación)

`TableAvailabilityService` resuelve, dado fecha + turno + comensales:

1. **Mesa única que encaja** en `[capacity_min, capacity_max]` → la de menor `capacity_max` (best-fit,
   minimiza sillas desperdiciadas).
2. Si ninguna encaja exacta, **mesa única con `capacity_max ≥ comensales`** (ignora el mínimo, para no
   bloquear grupos pequeños cuando solo quedan mesas grandes).
3. Si tampoco, **combinación de mesas `combinable` de la misma zona** cuya capacidad sumada cubra al
   grupo (greedy: las grandes primero; entre opciones, la de menos mesas y menor sobrante).

Una mesa está **ocupada** en un turno si una reserva activa cuyo turno **se solapa** (ventanas
`[start,end)` que se intersecan) ya la usa. Si `total-capacity > 0`, se valida además el **aforo
global** del turno (Σ comensales activos + grupo ≤ aforo).

> **Alta segura ante concurrencia (`POST /api/dining/reservations`):** se toma un **bloqueo pesimista**
> sobre la fila-cerrojo del día (`dining_day_locks`) **antes** de leer ocupación y asignar, igual que
> `spring-reservations`. Así dos grupos no se quedan con la misma mesa (ni en la primera reserva del
> día). Sin mesa/aforo disponible → **409**.

### Endpoints públicos (sin autenticación)

```
GET  /api/dining/zones                                   → zonas activas (preferencia)
GET  /api/dining/seatings                                → turnos activos
GET  /api/dining/availability?date&party&zoneId          → disponibilidad por turno (sin exponer mesas)
POST /api/dining/reservations                            → crear reserva (modo público)
```

`GET /api/dining/availability?date=2026-06-20&party=4` →

```json
{
  "success": true,
  "data": [
    { "seatingId": "uuid", "seatingName": "Comida", "start": "13:00", "end": "15:00", "available": true,  "suggestedTableIds": [] },
    { "seatingId": "uuid", "seatingName": "Cena",   "start": "20:00", "end": "22:00", "available": false, "suggestedTableIds": [] }
  ]
}
```

`POST /api/dining/reservations` (el público no elige mesa; el backend asigna best-fit):

```json
{
  "date": "2026-06-20",
  "seatingId": "uuid",
  "partySize": 4,
  "clientName": "Ada Lovelace",
  "clientEmail": "ada@example.com",
  "clientPhone": "600000000",
  "notes": "Cumpleaños",
  "requests": ["TRONA", "CELEBRACION"],
  "zoneId": "uuid-terraza"
}
```

### Endpoints de administración (`ROLE_ADMIN` / `ROLE_EDITOR`)

```
GET/PUT /api/admin/dining/settings                       → aforo, antelación, días cerrados
# Zonas
GET    /api/admin/dining/zones
POST   /api/admin/dining/zones
PUT    /api/admin/dining/zones/{id}
PATCH  /api/admin/dining/zones/{id}/toggle-active
DELETE /api/admin/dining/zones/{id}                      → 409 si tiene mesas
# Mesas
GET    /api/admin/dining/tables
POST   /api/admin/dining/tables
PUT    /api/admin/dining/tables/{id}
PATCH  /api/admin/dining/tables/{id}/toggle-active
DELETE /api/admin/dining/tables/{id}                     → 409 si tiene reservas
# Turnos
GET    /api/admin/dining/seatings
POST   /api/admin/dining/seatings
PUT    /api/admin/dining/seatings/{id}
PATCH  /api/admin/dining/seatings/{id}/toggle-active
DELETE /api/admin/dining/seatings/{id}                   → 409 si tiene reservas
# Reservas
GET    /api/admin/dining/availability?date&party&zoneId  → disponibilidad CON mesas sugeridas
GET    /api/admin/dining/reservations?status&page&size   → listado paginado
GET    /api/admin/dining/reservations/day?date           → plano del día (todas)
GET    /api/admin/dining/reservations/{id}
POST   /api/admin/dining/reservations                    → alta admin (salta antelación/cierre; tableIds manual)
PUT    /api/admin/dining/reservations/{id}               → edición completa (reasigna o conserva mesas)
PATCH  /api/admin/dining/reservations/{id}/status        → cambiar estado
PATCH  /api/admin/dining/reservations/{id}/tables        → reasignar mesas a mano
```

El **modo admin** (`POST`/`PUT`) puede saltarse la antelación y los días cerrados, y **forzar mesas**
con `tableIds`. La edición completa conserva las mesas actuales si siguen siendo válidas; si cambió
turno/fecha/comensales o ya no caben, reasigna best-fit (salvo `tableIds` manuales).

### Configuración editable en caliente y `application.yml`

Ajustes vivos en `app_settings` (clave `"dining"`); `application.yml` aporta los defaults:

```yaml
pimon:
  dining:
    total-capacity: 0       # aforo total en comensales por turno; 0 = sin tope (manda la mesa)
    max-advance-days: 60    # antelación máxima para reservar
    min-advance-hours: 1    # antelación mínima antes del turno
    blocked-dates: []       # días cerrados ISO 'YYYY-MM-DD'
```

Los **turnos** no van aquí: son entidades `Seating` (con días y duración), gestionadas desde el panel.

> **`public-paths`:** añade `/api/dining/zones`, `/api/dining/seatings`, `/api/dining/availability` y
> `/api/dining/reservations` para que el flujo público funcione sin autenticación.

---

## `spring-menu` — Carta y menú del día

**ArtifactId:** `spring-menu`  
**Paquete:** `dev.pimon.menu`  
**Depende de:** `spring-common`, `spring-security`

La carta de un bar/restaurante. Dos **agregados independientes** dentro del mismo módulo:

- **Carta** (permanente): categorías → platos, estable en el tiempo.
- **Menú del día** (fechado): oferta a precio fijo para una fecha; se **publica/clona** cada día y la
  carta sigue ahí. Sus opciones **reutilizan platos de la carta** o son **líneas libres** (especial del día).

> **Sin imágenes subidas (decisión de diseño).** La carta es **tipográfica**: nada de fotos por plato
> (envejecen mal, dan aire de carta plastificada, y compiten con la tipografía cuidada). La decoración
> es solo un **icono curado** por categoría y la **presentación** (fuente/color/layout). El módulo **no
> depende de `spring-storage`**. No reutiliza `spring-products` (eso es catálogo de e-commerce con
> stock/checkout; aquí no hay ni stock ni carrito, pero sí alérgenos y raciones).

### Entidades y tablas

```
menu_categories                  ← categorías de la carta (Entrantes, Carnes, Postres, Vinos)
  id          VARCHAR(36) PK
  name        VARCHAR NOT NULL
  slug        VARCHAR UNIQUE         — generado del nombre
  description TEXT
  icon        VARCHAR(20)            — enum curado: NONE, ENTRANTES, CARNES, VINOS... (no es imagen)
  sort_order  INT
  active      BOOLEAN DEFAULT true
  created_at, updated_at

menu_items                       ← platos
  id           VARCHAR(36) PK
  name         VARCHAR NOT NULL
  description  TEXT
  price        DECIMAL              — precio base/única ración (null si solo hay raciones)
  vegan, vegetarian, gluten_free, spicy   BOOLEAN  — flags dietéticos
  badge        VARCHAR              — "Sugerencia del chef", "Nuevo" (texto, no imagen)
  available    BOOLEAN DEFAULT true — "agotado hoy" sin borrarlo
  sort_order   INT
  category_id  FK → menu_categories.id
  created_at, updated_at
menu_item_portions               ← raciones con precio (tapa/media/ración); vacío = usa price
  item_id  FK → menu_items.id
  label    VARCHAR(40)
  amount   DECIMAL
menu_item_allergens              ← 14 alérgenos UE (enum)
  item_id   FK → menu_items.id
  allergen  VARCHAR(20)  — GLUTEN, CRUSTACEOS, HUEVOS, PESCADO, LACTEOS, FRUTOS_CASCARA...

menu_daily                       ← un menú del día (clave única date+name)
  id            VARCHAR(36) PK
  date          DATE NOT NULL
  name          VARCHAR NOT NULL      — "Menú del día", "Menú ejecutivo"
  price         DECIMAL NOT NULL      — precio fijo
  description   TEXT                  — "Incluye pan, bebida y postre"
  published     BOOLEAN DEFAULT false — borrador ↔ publicado
  published_at  TIMESTAMP
  created_at, updated_at
  UNIQUE (date, name)
menu_daily_course                ← grupos de elección (Primeros, Segundos, Postres)
  id             VARCHAR(36) PK
  daily_menu_id  FK → menu_daily.id
  name           VARCHAR NOT NULL
  choose         INT  — nº de opciones que elige el comensal (normalmente 1)
  sort_order     INT  (@OrderColumn)
menu_daily_option                ← opción: plato de la carta O línea libre
  id           VARCHAR(36) PK
  course_id    FK → menu_daily_course.id
  item_id      FK → menu_items.id   — null = línea libre
  name         VARCHAR              — usado si item_id es null
  description  TEXT
  supplement   DECIMAL              — suplemento sobre el precio fijo (+3€ solomillo)
  sort_order   INT  (@OrderColumn)
menu_daily_option_allergens      ← alérgenos de la línea libre (si item_id es null)
  option_id  FK → menu_daily_option.id
  allergen   VARCHAR(20)
```

> **Reutilización vs línea libre.** Una opción que apunta a un `menu_items` **hereda** nombre,
> descripción y alérgenos del plato (un cambio en la carta se propaga). Si es línea libre, lleva sus
> propios nombre/descripción/alérgenos. La API del menú del día devuelve cada opción **ya resuelta**.

### Presentación (fuente y estilos)

Editable en caliente, en `app_settings` (clave `"menu"`). Gobierna carta **y** menú del día. Se
guardan **nombres de fuente de una whitelist**, nunca CSS libre (seguridad + consistencia); el front
los aplica vía variables CSS (`--menu-heading-font`, `--menu-body-font`, color de acento, layout).

```json
{
  "headingFont": "SERIF",        // SYSTEM | SERIF | SANS | SLAB | DISPLAY | HANDWRITTEN | MONO
  "bodyFont": "SANS",
  "accentColor": "#8a5a2b",      // validado #RRGGBB
  "layout": "LIST",              // LIST | CARDS | COLUMNS
  "showPrices": true,
  "showAllergens": true,
  "currency": "EUR"
}
```

### Endpoints públicos (sin autenticación)

```
GET /api/menu                    → carta completa: categorías con platos disponibles + presentación
GET /api/menu/categories         → solo categorías activas
GET /api/menu/daily?date         → menús del día PUBLICADOS de la fecha (por defecto, hoy)
```

`GET /api/menu` devuelve un `CartaDto` listo para pintar (categorías agrupadas con sus platos y la
presentación), sin más llamadas.

### Endpoints de administración (`ROLE_ADMIN` / `ROLE_EDITOR`)

```
GET/PUT /api/admin/menu/presentation                     → fuente/estilos
# Categorías
GET    /api/admin/menu/categories
POST   /api/admin/menu/categories
PUT    /api/admin/menu/categories/{id}
PATCH  /api/admin/menu/categories/{id}/toggle-active
DELETE /api/admin/menu/categories/{id}                   → 409 si tiene platos
# Platos
GET    /api/admin/menu/items?categoryId
POST   /api/admin/menu/items
PUT    /api/admin/menu/items/{id}
PATCH  /api/admin/menu/items/{id}/toggle-available
DELETE /api/admin/menu/items/{id}
# Menú del día
GET    /api/admin/menu/daily?date                        → menús (cualquier estado) de una fecha
GET    /api/admin/menu/daily/range?from&to               → por rango de fechas
GET    /api/admin/menu/daily/history?page&size           → histórico paginado (menús antiguos)
GET    /api/admin/menu/daily/{id}
POST   /api/admin/menu/daily
PUT    /api/admin/menu/daily/{id}
POST   /api/admin/menu/daily/{id}/publish
POST   /api/admin/menu/daily/{id}/unpublish
POST   /api/admin/menu/daily/{id}/clone?toDate           → clona a otra fecha (borrador)
DELETE /api/admin/menu/daily/{id}
```

Un plato exige **al menos un precio**: o `price` base, o alguna ración. El alta del menú del día exige
que cada opción **referencie un plato (`itemId`) o tenga nombre** (línea libre). `clone` copia grupos y
opciones a otra fecha como **borrador** (el caso real: "el de ayer con un cambio").

### `application.yml`

```yaml
pimon:
  menu:
    heading-font: SERIF
    body-font: SANS
    accent-color: "#8a5a2b"
    layout: LIST
    show-prices: true
    show-allergens: true
    currency: EUR
```

> **`public-paths`:** añade `/api/menu/**` para que la carta y el menú del día sean accesibles sin login.

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

### Recuperar la contraseña olvidada (desde 0.6.0)

Dos endpoints, los dos bajo `/api/auth/**`, que ya es público. **Tiene que serlo**: quien
los usa no puede entrar, que es justo el problema que vienen a resolver.

```
POST /api/auth/recuperar     { email }              → siempre responde lo mismo
POST /api/auth/restablecer   { token, password }    → cambia la contraseña
```

#### La librería no manda el correo

Publica `RecuperacionSolicitada(email, nombre, token)` y cada tienda lo envía con su
plantilla y su URL. Mismo criterio que `PedidoCambioEstado`: aquí no se sabe cómo se llama
la tienda ni cuál es su pantalla de restablecer. Y así `spring-users` sigue sin depender de
`spring-emails`.

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
void enviar(RecuperacionSolicitada e) {
    String enlace = "https://mitienda.com/restablecer?token=" + e.token();
    email.sendTemplate(e.email(), "Recupera tu contraseña", "recuperar",
                       Map.of("nombre", e.nombre(), "enlace", enlace));
}
```

> ⚠️ **Ese evento lleva el token EN CLARO** — es la única vez que existe fuera del correo
> del usuario. Úsalo para el enlace y nada más: **no lo registres en un log**. Un token en
> un fichero de log es un token que sigue abriendo la cuenta. Su `toString()` lo oculta
> para que un `log.info("{}", evento)` accidental no lo escriba, pero eso no cubre
> escribirlo a mano.

#### Las cuatro decisiones que sostienen esto

**1. Siempre se responde lo mismo**, exista el correo o no. Contestar «ese correo no está
registrado» convertiría la pantalla en un buscador de clientes: se prueban direcciones y se
apunta cuáles responden distinto. Por eso `solicitar()` **no lanza** cuando no encuentra la
cuenta.

**2. En la base se guarda el hash**, nunca el token. El original solo existe en el correo
del usuario. El día que alguien lea la base —una copia mal guardada, un volcado— esas filas
no valen para nada. Y por eso mismo **no se puede reenviar un enlace ya emitido**: nadie
sabe cuál era. Pedirlo otra vez emite uno nuevo.

**3. Un solo uso y una hora de vida.** Un enlace de recuperación acaba en la bandeja de
entrada y ahí se queda para siempre; sin estas dos cosas, un correo de hace dos años
seguiría abriendo la cuenta.

**4. Al usar uno mueren los demás** de esa cuenta. Quien pidió el enlace tres veces porque
el correo tardaba dejaría si no dos permisos vivos **después** de haber recuperado la
cuenta.

#### Lo que NO hace

**No limita cuántas veces se puede pedir.** Alguien puede pedir mil recuperaciones del
correo de otro y llenarle la bandeja. No se resuelve dentro porque hacerlo bien necesita
contar peticiones por IP y por cuenta, con almacenamiento y ventana temporal, y a medias es
peor que no tenerlo. Si algún día molesta, se pone delante.

#### La tabla

`tokens_recuperacion` (user_id, hash único, expira_en, usado_en). En un proyecto con
`ddl-auto: update` aparece sola; **con `validate` hay que escribir su migración**.

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
  opciones          TEXT            — JSON con lo elegido en esa línea. NULL si no se usan
```

### Opciones por línea (desde 0.3.0)

Una línea de pedido puede llevar lo que el cliente eligió para ella: el color de un
filamento, la dedicatoria de un libro, el tono de unas mechas.

**La librería no sabe qué significan.** Solo transporta pares nombre/valor y los guarda
como JSON en `order_items.opciones`. Cada tienda decide qué mete y qué es válido.

```json
POST /api/checkout/create-intent
{
  "items": [
    { "productId": "abc", "quantity": 2, "opciones": { "color": "Morado" } }
  ],
  "shippingCountry": "ES"
}
```

**Por qué esto y no variantes de producto:** una variante de verdad lleva existencias,
precio y fotos propias, y arrastra el catálogo entero. Esto es una opción del encargo, que
es lo que necesita una tienda que fabrica bajo demanda. Meter variantes completas
complicaría el checkout de todas las webs para no servir a ninguna.

**Por qué en una columna de texto y no en una tabla aparte:** hecho el pedido, esto es solo
lectura, igual que el nombre y el precio que ya se copian en la fila. Nadie consulta
pedidos filtrando por color, así que una tabla traería un join en cada consulta para no
ganar nada.

#### Validar las opciones: `ValidadorOpciones`

La comprobación **se hace siempre en el servidor**: el desplegable de la web solo enseña lo
válido, pero la petición se puede construir a mano.

Mismo patrón que `CalculadorEnvio`: la librería define el hueco y el proyecto lo rellena.
Si no se declara ninguno, las opciones se aceptan tal cual — una tienda que no las use no
tiene que implementar nada.

```java
@Bean
public ValidadorOpciones validadorOpciones() {
    return (producto, opciones) -> {
        String color = opciones.get("color");
        if (color != null && !coloresDisponibles.contains(color)) {
            throw AppException.badRequest("Ahora mismo no hay " + color);
        }
    };
}
```

**Al subir a 0.3.0:** hay una columna nueva en `order_items`. Los proyectos con
`ddl-auto: update` (lacajadelaton) la crean solos; los que usan Flyway con `validate`
(pimon3d) necesitan una migración:

```sql
ALTER TABLE order_items ADD COLUMN opciones TEXT;
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

**Desde 0.4.0 `OrderDto` incluye `shippingPostalCode` y `shippingPhone`.** Antes no salían
—la entidad sí los guardaba— y sin ellos **desde el panel no se puede rellenar una etiqueta
de envío**: había que entrar a la base de datos a buscar el código postal de cada pedido.
El teléfono tampoco es un adorno: es lo que pide el transportista para avisar antes de
entregar.

### Caducar los pedidos sin pagar (desde 0.4.0)

`create-intent` **descuenta el stock al crear el pedido**, antes de cobrar. Es lo correcto:
sin eso, dos personas podrían comprar la última pieza mientras cada una teclea su tarjeta y
una de las dos recibiría un correo de disculpa.

La contrapartida es que quien llega al pago y cierra la pestaña deja ese stock retenido.
Hasta la 0.4.0 **no lo devolvía nadie**, y una tienda pequeña acababa «sin existencias» de
algo que tenía en la mano.

```yaml
pimon:
  ecommerce:
    caducidad:
      enabled: true          # viene APAGADO: cancela pedidos y mueve existencias
      espera: 30m            # cuánto se espera antes de darlo por perdido
      intervalo-ms: 300000   # cada cuánto se revisa
```

**Viene apagado a propósito.** Que se pusiera en marcha solo al actualizar la librería sería
una sorpresa fea en una tienda en funcionamiento.

**La media hora es holgada a propósito.** Un pago con verificación del banco (3-D Secure)
tarda minutos, y quien deja la pestaña abierta mientras busca la tarjeta, más. Quedarse
corto cancela compras vivas.

#### Ante la duda, no se cancela

Antes de tocar un pedido se le pregunta a Stripe en qué estado está su pago. Si responde
`succeeded`, `processing` o `requires_capture`, **no se cancela** y se registra un `ERROR`
en el log: eso es un webhook perdido, y hay un cliente que ha pagado cuyo pedido nadie está
preparando.

Si Stripe **no contesta** —red, clave mal— tampoco se cancela. Los dos errores posibles no
cuestan lo mismo:

| Error | Coste |
|---|---|
| Dejar colgado un pedido muerto | Retiene stock. Se ve en el panel y se arregla a mano. |
| Cancelar un pedido pagado | Dinero cobrado, pedido cancelado y la pieza vendida otra vez. **El cliente se entera antes que tú.** |

Con Stripe apagado (modo simulado) sí se caduca sin preguntar: ahí no hay dinero real.

#### Cancelar siempre devuelve el stock

Hay **tres** caminos por los que un pedido se queda sin comprarse, y los tres devuelven las
unidades al catálogo:

| Camino | Quién lo dispara |
|---|---|
| El pago falla | El webhook `payment_intent.payment_failed` |
| El pedido caduca | La tarea programada |
| Alguien lo cancela | El botón del panel |

Los tres pasan por `StockDePedidos.devolver()`. Con la resta repetida en cada sitio, el día
que cambie algo se arregla en dos y se olvida el tercero — y eso **no da error**: solo deja
piezas «agotadas» que están en el almacén.

**`REFUNDED` no devuelve stock, y es deliberado:** ahí el artículo ya salió por la puerta.
Que vuelva al catálogo es una decisión del negocio —un libro se revende, una pieza impresa
a medida no— y por eso no se decide en la librería.

> ⚠️ **Sin tests todavía.** El módulo `spring-ecommerce` no tiene ninguno, y esto toca
> stock y cobros. Pruébalo en un entorno de pruebas antes de encenderlo en producción.

### Enterarse de que un pedido cambia: `PedidoCambioEstado` (desde 0.5.0)

La librería sabe cuándo entra un pago, pero **no sabe qué hacer con esa noticia**: eso
depende de la tienda. Una manda un correo, otra avisa por Telegram, otra imprime una
etiqueta. Por eso `spring-ecommerce` **no depende de `spring-emails`** y no manda ningún
correo: publica un evento y cada proyecto decide.

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
void avisar(PedidoCambioEstado e) {
    if (e.pasoA(OrderStatus.PAID)) {
        // el correo, el Telegram, lo que sea
    }
}
```

> ⚠️ **`@TransactionalEventListener` con `AFTER_COMMIT`, no `@EventListener` a secas.** El
> evento se publica dentro de la transacción que guarda el pedido. Un listener normal corre
> ahí dentro, así que si la transacción se deshace después habrías mandado un correo sobre
> un pedido que no existe.

**Se publican todos los cambios**, incluidos los automáticos —el pago que falla, el pedido
que caduca—. Filtrar en la librería sería decidir por la tienda qué merece un aviso, y hay
quien querrá avisar de que un pedido caducó. Quien escucha elige.

#### Por qué un evento y no otro bean

`CalculadorEnvio` y `ValidadorOpciones` son cosas que la librería **pregunta**: necesita la
respuesta para seguir, y por eso son interfaces con un valor por defecto. Esto lo
**anuncia**: no espera nada de vuelta, y pueden escucharlo cero o cinco sitios sin cambiar
la costura.

#### Un solo sitio cambia el estado

Los cinco caminos —pago confirmado, pago fallido, cambio manual, reembolso y caducidad—
pasan por `OrderService`, que guarda y anuncia en el mismo método privado. `CaducidadPedidos`
decide **cuáles** caducan pero no toca el pedido: llama a `cancelarPorCaducidad()`.
Repartirlo entre dos clases es como se acaba publicando el evento en cuatro caminos y
olvidándolo en el quinto.

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

## `spring-events` — Webs de eventos (RSVP)

**ArtifactId:** `spring-events`
**Paquete:** `dev.pimon.events`
**Depende de:** `spring-common`

Backend de la librería Angular `@adrianmartincano/ng-events`. Recibe las **confirmaciones de
asistencia (RSVP)** de los invitados de un evento (boda, cumpleaños…) y ofrece un **resumen
agregado** para el organizador. Se autoconfigura como los demás módulos.

### Entidad `Rsvp` (tabla `events_rsvp`)

| Campo | Tipo | Notas |
|---|---|---|
| `eventId` | `String` | Slug del evento (agrupa las respuestas); indexado |
| `name` | `String` | Nombre del invitado |
| `email` | `String` | Opcional |
| `attending` | `boolean` | ¿Asiste? |
| `guests` | `int` | Acompañantes además del invitado (solo cuentan si asiste) |
| `menuChoice` | `String` | Opción de menú elegida |
| `allergies`, `song`, `message` | `String` | Alergias, canción pedida y mensaje libre |

### Endpoints

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/events/rsvp` | Público | Registra la confirmación (`eventId`, `name` y `attending` obligatorios). Devuelve `RsvpDto`. |
| GET | `/api/events/rsvp/summary?eventId=` | Público | `confirmed`, `declined`, `totalGuests` y `byMenu`. **Solo cifras, sin datos personales.** |

```bash
curl -X POST https://api.midominio.com/api/events/rsvp \
  -H 'Content-Type: application/json' \
  -d '{ "eventId": "maria-y-juan", "name": "Ana", "attending": true, "guests": 1, "menuChoice": "Vegano" }'
```

> El listado con los datos de cada invitado todavía no tiene endpoint; hoy solo se expone el resumen.

---

## `spring-fx` — Tasas de cambio

**ArtifactId:** `spring-fx`
**Paquete:** `dev.pimon.fx`
**Depende de:** `spring-common`

Sirve **tasas de cambio** por API para que el frontend multi-divisa
(`provideLibFormat` / `provideFxSync` de `@adrianmartincano/ng-models`) convierta importes.
Por defecto las tasas son las que pongas en `application.yml` (estáticas); opcionalmente puede
**refrescarlas en automático** desde una API externa sin API key (frankfurter.app).

### Configuración

```yaml
pimon:
  fx:
    base: EUR
    rates:
      USD: 1.16
      GBP: 0.86
    refresh:
      enabled: false          # true = refresca periódicamente desde una API externa
      url: "https://api.frankfurter.app/latest?base="   # se le concatena la base
      interval-ms: 21600000   # cada 6 h (primer refresco al arrancar)
  security:
    public-paths:
      - "/api/fx"             # el endpoint público debe estar abierto
```

> Las tasas son "1 base = rate X". Con `refresh.enabled: true` el módulo consulta la API externa
> al arrancar y cada `interval-ms`; si la llamada falla, conserva las últimas tasas válidas.

### Endpoints

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/api/fx` | Público | Base + tasas actuales: `{ base, rates, updatedAt }`. |
| PUT | `/api/admin/fx` | ADMIN | Actualiza tasas: `{ rates: {...}, replace: false }` (fusiona) o `{ base, rates, replace: true }` (sustituye). |
| POST | `/api/admin/fx/refresh` | ADMIN | Fuerza un refresco desde la API externa configurada. |

```bash
curl https://api.midominio.com/api/fx
# → { "success": true, "data": { "base": "EUR",
#     "rates": { "USD": 1.16, "GBP": 0.86 }, "updatedAt": "2026-06-15T10:00:00Z" } }
```

### Enganche con el frontend

```typescript
// app.config.ts del proyecto Angular
provideLibFormat({ defaultCurrencyCode: 'EUR', currencies: [...] }),
provideFxSync({ url: '/api/fx' }),   // carga las tasas del backend al arrancar (best-effort)
```

Al arrancar, el front pide `/api/fx` y aplica las tasas al `FormatService`; todos los importes
se muestran convertidos. Si el backend no responde, se usan las tasas estáticas de `provideLibFormat`.

---

## `spring-forum` — Foro de comunidad

**ArtifactId:** `spring-forum`  
**Paquete:** `dev.pimon.forum`  
**Depende de:** `spring-common`, `spring-security`

Foro de comunidad: **categorías** (que gestiona el admin) → **hilos** que abren los usuarios →
**mensajes**. Encima: reacciones, citas, notificaciones, búsqueda y moderación. Es un dominio
distinto al de `spring-blog` (bitácora cronológica del admin con comentarios): aquí cualquier
usuario registrado abre temas y conversa.

> **Decisión de diseño:** el **mensaje de apertura** de un hilo es también un `ForumPost`
> (con `opening = true`). Así reacciones, citas, edición y borrado funcionan igual para la
> apertura y para las respuestas, sin casos especiales. Borrar el mensaje de apertura borra
> el hilo entero (en cascada con sus reacciones y denuncias).

### Entidades y tablas

```
forum_categories                 ← secciones del foro (las crea/ordena el admin)
  id          VARCHAR(36) PK
  slug        VARCHAR UNIQUE NOT NULL
  name        VARCHAR NOT NULL
  description VARCHAR(500)
  position    INT                  — orden en la portada (menor primero)
  icon, color VARCHAR              — opcionales, para la portada
  created_at, updated_at

forum_threads                    ← hilos/temas (los abre un usuario)
  id                   VARCHAR(36) PK
  category_id          FK → forum_categories.id
  slug                 VARCHAR UNIQUE NOT NULL
  title                VARCHAR NOT NULL
  author_id            VARCHAR NOT NULL
  author_name          VARCHAR NOT NULL    — nick al abrir (se resuelve en vivo al servir)
  author_is_admin      BOOLEAN             — distintivo ★ Admin
  pinned               BOOLEAN             — fijado arriba (moderación)
  locked               BOOLEAN             — cerrado: no admite respuestas
  reply_count          INT                 — denormalizado (respuestas, sin la apertura)
  view_count           BIGINT              — visitas al detalle
  last_post_at         TIMESTAMP           — ordena la lista de hilos
  last_post_author_name VARCHAR
  created_at, updated_at

forum_posts                      ← mensajes de un hilo (apertura incluida)
  id              VARCHAR(36) PK
  thread_id       FK → forum_threads.id
  author_id       VARCHAR NOT NULL
  author_name     VARCHAR NOT NULL
  author_is_admin BOOLEAN
  body            TEXT NOT NULL
  opening         BOOLEAN             — true = mensaje de apertura del hilo
  quoted_post_id  VARCHAR             — cita: id del mensaje citado (o null)
  edited_at       TIMESTAMP           — null si no se ha editado
  created_at, updated_at

forum_reactions                  ← reacciones a un mensaje
  id      VARCHAR(36) PK
  post_id VARCHAR NOT NULL
  user_id VARCHAR NOT NULL
  type    VARCHAR(20)  — LIKE, LOVE, LAUGH, THANKS
  UNIQUE (post_id, user_id, type)   — poner/quitar cada tipo una vez

forum_notifications              ← avisos para un usuario
  id            VARCHAR(36) PK
  recipient_id  VARCHAR NOT NULL
  type          VARCHAR(20)  — REPLY, QUOTE, REACTION
  actor_name    VARCHAR NOT NULL
  thread_id, thread_slug, thread_title
  post_id       VARCHAR
  read          BOOLEAN
  created_at, updated_at

forum_reports                    ← denuncias de mensajes (moderación)
  id            VARCHAR(36) PK
  post_id       VARCHAR NOT NULL
  reporter_id   VARCHAR NOT NULL
  reporter_name VARCHAR NOT NULL
  reason        VARCHAR(1000)
  status        VARCHAR(20)  — OPEN, RESOLVED
  created_at, updated_at
```

### Nick en vivo (igual que `spring-blog`)

Hilos y mensajes guardan `author_name` al escribir, pero al servir se resuelve el **nick actual
del perfil** con un bean opcional `ForumAuthorNameResolver` que provee la app (p. ej. consultando
`spring-users`). Así cambiar el nick se refleja en todo el foro y el cliente no puede falsearlo.
Si no hay bean, cae a la parte local del email.

```java
@Component
public class ForumAuthorResolver implements ForumAuthorNameResolver {
    private final UserService users;
    public ForumAuthorResolver(UserService users) { this.users = users; }
    @Override public String resolve(String userId) {
        try { return users.getById(userId).name(); } catch (Exception e) { return null; }
    }
}
```

### Endpoints públicos (`/api/forum`)

Leer es público; escribir exige usuario autenticado (`@CurrentUser`). El listado de mensajes
acepta token opcional para personalizar el flag `reacted` de cada reacción.

```
GET    /api/forum/categories                        → categorías + nº de hilos
GET    /api/forum/categories/{slug}/threads         → hilos de la categoría (fijados primero)
POST   /api/forum/categories/{slug}/threads         → abrir hilo (título + 1er mensaje)
GET    /api/forum/threads/{slug}                     → detalle del hilo (+ incrementa visitas)
GET    /api/forum/threads/{slug}/posts               → mensajes paginados
POST   /api/forum/threads/{slug}/posts               → responder (admite quotedPostId)
PUT    /api/forum/posts/{id}                          → editar el propio mensaje (ventana de edición)
DELETE /api/forum/posts/{id}                          → borrar el propio mensaje
POST   /api/forum/posts/{id}/reactions               → poner/quitar reacción {type}; devuelve resumen
POST   /api/forum/posts/{id}/report                  → denunciar {reason}
GET    /api/forum/search?q&page&size                 → busca en títulos de hilo y cuerpos de mensaje
GET    /api/forum/notifications                       → bandeja del usuario (paginada)
GET    /api/forum/notifications/unread-count          → { count }
POST   /api/forum/notifications/{id}/read             → marcar una como leída
POST   /api/forum/notifications/read-all              → marcar todas como leídas
```

Las **notificaciones** se emiten al responder (a el autor del hilo), citar (al autor citado) y
reaccionar (al autor del mensaje); nunca a uno mismo, y se deduplica si el autor del hilo coincide
con el citado.

### Endpoints de administración (`/api/admin/forum`, `ROLE_ADMIN`)

```
GET    /api/admin/forum/categories
POST   /api/admin/forum/categories                   → crear categoría
PUT    /api/admin/forum/categories/{id}              → editar
DELETE /api/admin/forum/categories/{id}?force        → 409 si tiene hilos; force=true borra en cascada
POST   /api/admin/forum/threads/{id}/move?categoryId → mover hilo de categoría
POST   /api/admin/forum/threads/{id}/pin?value       → fijar / desfijar
POST   /api/admin/forum/threads/{id}/lock?value      → cerrar / reabrir
DELETE /api/admin/forum/threads/{id}                  → borrar hilo (cascada)
DELETE /api/admin/forum/posts/{id}                    → borrar cualquier mensaje
GET    /api/admin/forum/reports?onlyOpen&page&size    → cola de denuncias
POST   /api/admin/forum/reports/{id}/resolve          → marcar resuelta
```

### Configuración y `application.yml`

```yaml
pimon:
  forum:
    edit-window-minutes: 15         # minutos para editar el propio mensaje; 0 = sin límite
    max-notifications-per-user: 0   # conserva las N notificaciones más recientes por usuario; 0 = no podar
```

El **admin no tiene límite** de edición. Un usuario normal solo puede editar su mensaje dentro de
la ventana (pasado el plazo → 403). El front debe usar el mismo valor para ocultar el botón "Editar".

**Retención de notificaciones:** con `max-notifications-per-user > 0`, al emitir un aviso se borran
los más antiguos del destinatario que excedan N (la tabla queda acotada por usuario). Con `0` (por
defecto) no se poda: la bandeja crece sin tope.

> **`public-paths`:** añade `/api/forum/**` para que leer el foro (categorías, hilos, mensajes,
> búsqueda) funcione sin autenticación. Escribir/reaccionar/denunciar/notificaciones requieren JWT
> aunque la ruta esté en `public-paths` (el servicio exige `@CurrentUser`).

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
