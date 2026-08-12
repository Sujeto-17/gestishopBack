# 📦 Core Package — Guía de uso

Este documento explica **qué es, para qué sirve y cómo usar** cada clase del paquete `core` del proyecto. La idea de este paquete es centralizar todo lo repetitivo: respuestas HTTP, manejo de errores, validaciones y utilidades comunes, para que los módulos de negocio (`infrastructure`) queden limpios y consistentes.

```
core/
├── config/
├── dto/
├── enums/
├── exception/
├── generic/
├── interfaces/
├── resolver/
├── service/
└── utils/
```

---

## 🎯 Filosofía del paquete

1. **Nunca construyes un `ResponseEntity` a mano.** Usas `BaseController` o `ApiResponseBuilder`.
2. **Nunca haces `try/catch` para errores de base de datos.** Usas `RepositoryExecutor`.
3. **Nunca lanzas una excepción genérica.** Usas `ApiResponseException` (directo o vía `ErrorFactory`/métodos de `BaseController`).
4. **Todas las respuestas de la API tienen el mismo formato**, sin importar el módulo o quién lo programó.

---

## 1. `dto/` — Formato estándar de respuestas

### `ApiBaseResponseDTO`
Clase abstracta base. Define las 3 propiedades que **toda** respuesta de la API va a tener:

| Campo | Ejemplo | Descripción |
|---|---|---|
| `codigo` | `"OK"`, `"BE_RNF"` | Código interno (viene de `ApiCodeResponse`) |
| `mensaje` | `"Petición satisfactoria"` | Mensaje legible |
| `tipo` | `"SUCCESS"`, `"BUSINESS_ERROR"` | Categoría del resultado |

No se usa directamente, la extienden `ApiDataResponseDTO` y `ApiPageResponseDTO`.

### `ApiDataResponseDTO<T>`
Respuesta estándar **sin paginación**. Agrega el campo `datos` con el contenido real.

```json
{
  "codigo": "OK",
  "mensaje": "Petición satisfactoria",
  "tipo": "SUCCESS",
  "datos": { "id": "...", "nombre": "Categoría A" }
}
```

### `ApiPageResponseDTO<T>`
Respuesta estándar **con paginación**. Agrega `datos` (una lista) y `page` (metadatos).

```json
{
  "codigo": "OK",
  "mensaje": "Petición satisfactoria",
  "tipo": "SUCCESS",
  "datos": [ {...}, {...} ],
  "page": {
    "number": 0,
    "size": 10,
    "totalElements": 57,
    "totalPages": 6,
    "first": true,
    "last": false,
    "sort": "nombre,asc"
  }
}
```

### `PageMetadataDTO`
Metadatos de paginación. Se construye automáticamente a partir de un `Page<?>` de Spring Data con `PageMetadataDTO.from(page)`. **No se usa manualmente casi nunca** — lo llama `ApiResponseBuilder.buildPage(...)` por ti.

### `StoredFileDTO`
Representa un archivo ya guardado en disco (nombre generado, nombre original, tipo, tamaño, ruta). Lo devuelve `FileStorageUtil.save(...)`.

### `SearchFilterDTO` / `SearchRequestDTO`
Para armar búsquedas dinámicas/avanzadas desde el frontend:

```json
{
  "filters": [
    { "field": "nombre", "operation": "LIKE", "value": "area" },
    { "field": "activo", "operation": "EQUAL", "value": true }
  ],
  "page": 0,
  "size": 10,
  "sortField": "nombre",
  "sortDirection": "ASC"
}
```
> ⚠️ Estas clases **solo transportan el filtro**. Necesitas una clase adicional (típicamente una `Specification<T>` builder en `infrastructure/specification`) que traduzca esto a una consulta JPA.

---

## 2. `enums/` — Catálogos y códigos

### `ApiCodeResponse` ⭐ (la más importante)
Enum con **todos los códigos de respuesta posibles** de la API, agrupados en 3 categorías (`ApiCodeType`):

| Grupo | Códigos | HTTP |
|---|---|---|
| ✅ Éxito | `SUCCESS`, `RESOURCE_FOUND`, `RESOURCE_CREATED`, `RESOURCE_UPDATED`, `RESOURCE_DELETED`, `NOT_FOUND` | 200/201 |
| ⚠️ Negocio | `REQUIRED_DATA`, `RESOURCE_NOT_FOUND`, `CONFLICT` | 400/404/409 |
| 🔧 Técnico | `INTERNAL_ERROR`, `DATABASE_ERROR`, `DATABASE_UNAVAILABLE`, `BAD_REQUEST`, `RESOURCE_UNAVAILABLE`, `TIMEOUT` | 500/503/504 |

**¿Cuándo agregar un código nuevo?** Solo si ninguno de los existentes describe bien tu caso. Antes de crear uno nuevo, revisa si `CONFLICT` o `REQUIRED_DATA` ya cubren el escenario — la mayoría de los errores de negocio caben en los que ya existen.

### `ApiCodeType`
Solo el enum de las 3 categorías (`SUCCESS`, `BUSINESS_ERROR`, `TECHNICAL_ERROR`). Se usa dentro de `ApiCodeResponse`.

### `SearchOperation`
Operadores disponibles para filtros dinámicos: `EQUAL`, `NOT_EQUAL`, `GREATER_THAN`, `LESS_THAN`, `LIKE`, `GREATER_THAN_EQUAL`, `LESS_THAN_EQUAL`, `IN`.

### `TextAlternation`
Usado por `GeneralUtil.homotext(...)`: `UPPERCASE`, `LOWERCASE`, `NORMAL`.

### `AlgorithmHash`
Algoritmos de hash soportados por `HashUtil`: `MD5`, `SHA1`, `SHA256`, `SHA384`, `SHA512`. Cada valor sabe crear su propio `MessageDigest` con `.createDigest()`.

---

## 3. `exception/` — Manejo centralizado de errores

### `ApiResponseException` ⭐
La **única** excepción que deberías lanzar manualmente en tus services/controllers para errores controlados. Encapsula:
- un `ApiCodeResponse` (código + mensaje + HTTP status)
- datos adicionales (`Object data`) — normalmente un `Map` con contexto (`entidad`, `operacion`, `detalle`, etc.)

```java
throw new ApiResponseException(ApiCodeResponse.CONFLICT, Map.of("detalle", "Ya existe"));
```

No necesitas capturarla en ningún lado — la atrapa `GlobalExceptionHandler` automáticamente.

### `GlobalExceptionHandler`
`@RestControllerAdvice` que intercepta **toda** `ApiResponseException` lanzada en cualquier controller/service de la app y construye la respuesta HTTP con `ApiResponseBuilder`. Además registra un log (`log.warn`) con el código y los datos del error.

> 💡 Si necesitas capturar otros tipos de excepción globalmente (por ejemplo `MethodArgumentNotValidException` de `@Valid`), este es el lugar para agregar más métodos `@ExceptionHandler`.

### `RepositoryExecutor` ⭐⭐ (la pieza más potente)
Envuelve las llamadas a tus repositorios JPA para **traducir automáticamente** las excepciones técnicas (Postgres, Spring Data, transacciones) a `ApiResponseException` con el código correcto.

**Métodos disponibles:**

| Método | Uso |
|---|---|
| `execute(Supplier<T>, entidad, operacion)` | Para cualquier operación que devuelve un valor (`save`, `findById`, `findAll`, etc.) |
| `executeVoid(Runnable, entidad, operacion)` | Para operaciones sin retorno (`deleteById`, etc.) |
| `executeList(Supplier<List<T>>, entidad, operacion)` | Como `execute`, pero además lanza `NOT_FOUND` automáticamente si la lista viene vacía o nula |

**Ejemplo:**
```java
Categoria guardada = RepositoryExecutor.execute(
        () -> repository.save(categoria),
        "Categoria",
        "crear"
);
```

**¿Qué traduce automáticamente?**

| Excepción original | Se convierte en |
|---|---|
| `PSQLException` con `sqlState` que empieza en `23` (constraint UNIQUE, FK, CHECK, NOT NULL) | `CONFLICT` con el mensaje resuelto por `ConstraintMessageResolver` |
| `PSQLException` con `sqlState` que empieza en `P` (RAISE EXCEPTION en función de Postgres) | `CONFLICT` con el mensaje/detalle/hint de Postgres |
| `PSQLException` con otro `sqlState` | `DATABASE_ERROR` |
| `CannotGetJdbcConnectionException` | `DATABASE_UNAVAILABLE` |
| `CannotCreateTransactionException` | `DATABASE_UNAVAILABLE` |
| `QueryTimeoutException` | `TIMEOUT` |
| `CannotAcquireLockException`, `DeadlockLoserDataAccessException`, `OptimisticLockingFailureException` | `CONFLICT` (concurrencia) |
| `DataIntegrityViolationException` | `CONFLICT` |
| `InvalidDataAccessApiUsageException`, `IllegalArgumentException` | `BAD_REQUEST` |
| `DataAccessResourceFailureException` | `DATABASE_UNAVAILABLE` |
| `DataAccessException` (genérico) | `DATABASE_ERROR` |
| Cualquier otra excepción | `INTERNAL_ERROR` |

**Regla de oro:** *toda* llamada al repositorio dentro de un service debería pasar por `RepositoryExecutor.execute(...)` o `.executeVoid(...)`. Así nunca tienes que escribir un `try/catch` de JPA/Postgres en tu código de negocio.

---

## 4. `resolver/` — Mensajes amigables

### `ConstraintMessageResolver`
Traduce el **nombre técnico de un constraint de Postgres** (ej. `cat_ops_stk_uuid_uk`) a un mensaje legible para el usuario final.

```java
private static final Map<String, String> MENSAJES = Map.ofEntries(
    Map.entry("cat_ops_stk_uuid_uk", "Ya existe un registro con el UUID especificado.")
);
```

**¿Cuándo lo tocas?** Cada vez que crees un constraint nuevo en la base de datos (UNIQUE, CHECK, etc.) y quieras que, si se viola, el usuario vea un mensaje claro en vez de un error técnico de Postgres. Simplemente agrega una entrada al `Map`:

```java
Map.entry("nombre_del_constraint_en_bd", "Mensaje amigable para el usuario.")
```

Si el constraint no está en el mapa, usa `DEFAULT_MESSAGE` automáticamente. No necesitas usarlo directamente — `RepositoryExecutor` ya lo invoca por ti.

---

## 5. `generic/` — Lo que usan tus controllers y services

### `ApiCode` (interfaz)
Contrato que implementa `ApiCodeResponse`. Rara vez la tocas directamente; existe para que en el futuro puedas tener más de un enum de códigos si el proyecto crece.

### `ApiResponseBuilder`
Construye el `ResponseEntity` final a partir de un `ApiCodeResponse` y los datos. Tiene 2 métodos:
- `build(codigo, data)` → respuesta simple (`ApiDataResponseDTO`)
- `buildPage(codigo, page)` → respuesta paginada (`ApiPageResponseDTO`)

**Normalmente no lo llamas directo** — lo usa `BaseController` y `GlobalExceptionHandler` por ti.

### `BaseController` ⭐ (la clase que extiendes en cada controller)
Dale `extends BaseController` a todos tus `@RestController` y obtienes gratis:

**Métodos de respuesta exitosa:**

| Método | Código usado | Cuándo usarlo |
|---|---|---|
| `ok(data)` / `ok()` | `SUCCESS` | Operación genérica exitosa |
| `okEncontrado(data)` | `RESOURCE_FOUND` | GET de un recurso específico |
| `okCreado(data)` | `RESOURCE_CREATED` | POST exitoso (201) |
| `okActualizado(data)` | `RESOURCE_UPDATED` | PUT/PATCH exitoso |
| `okEliminado(data)` / `okEliminado()` | `RESOURCE_DELETED` | DELETE exitoso |
| `pkPaginado(page)` | `SUCCESS` + paginación | GET de listados |

**Métodos que lanzan error directamente (atajos):**

| Método | Código | Equivale a |
|---|---|---|
| `exDatoRequerido(data)` | `REQUIRED_DATA` | Falta un campo obligatorio |
| `exRecursoNoEncontrado(data)` | `RESOURCE_NOT_FOUND` | El recurso buscado no existe |
| `exConflicto(data)` | `CONFLICT` | Ya existe / hay un choque de datos |
| `exErrorInterno(data)` | `INTERNAL_ERROR` | Error inesperado |
| `exErrorBaseDeDatos(data)` | `DATABASE_ERROR` | Error de BD manual |
| `exRecursoNoDisponible(data)` | `RESOURCE_UNAVAILABLE` | Servicio externo caído |
| `exTimeout(data)` | `TIMEOUT` | Se agotó el tiempo de espera |
| `error(codigo, data)` | El que tú definas | Caso genérico |

> En la práctica, muchas de estas validaciones (`exDatoRequerido`, `exRecursoNoEncontrado`) las vas a lanzar mejor desde el **service**, no desde el controller, porque el controller ya delega la lógica al service. Ahí puedes usar directamente `throw new ApiResponseException(...)` o `ErrorFactory`.

### `ErrorFactory`
Atajos para las excepciones más comunes, para no repetir `Map.of(...)` en cada service:

```java
ErrorFactory.notFound("Categoria");           // sin id
ErrorFactory.notFound("Categoria", id);       // con id
ErrorFactory.unavailable("Servicio de correo");
```

---

## 6. `interfaces/` — Contratos

### `FileStorageService`
Interfaz para el manejo de archivos. Solo define `save(MultipartFile file, String basePath)`. La implementa `FileStorageUtil`. Si un día cambias de almacenamiento local a S3/Azure, solo creas otra clase que implemente esta interfaz.

---

## 7. `utils/` — Utilidades transversales

### `GeneralUtil`
Caja de herramientas general:

| Método | Para qué |
|---|---|
| `homotext(texto)` / `homotext(texto, alternation)` | Normaliza texto: quita acentos, cambia `ñ`→`n`, espacios→`_`. Útil para generar una columna "buscable" sin tildes (ej. guardar `AREA_VERDE` junto con `Área Verde`) |
| `generarHomotext(objeto, propiedad, htPropiedad)` | Usa reflexión para llenar automáticamente el campo homotexto de un objeto a partir de otro campo |
| `generarUUIDv4()` / `generarUUIDv1()` | Generar UUIDs |
| `toUuidV4(String)` | Parsear y validar que un string sea un UUID v4 válido |
| `toCamelCase(input)` / `toCamelCaseExceptFirst(input)` | Convertir texto a CamelCase / camelCase |
| `getCurrentDateTime()` | `LocalDateTime.now()` |
| `generateDatePath()` | Genera ruta tipo `2026/08` (útil para organizar archivos por fecha) |
| `getUsername(username)` | Devuelve el username o un valor por defecto si viene vacío |

### `HashUtil`
Genera hashes en Hex o Base64, desde `String`, `byte[]` o `Path` (archivo), usando cualquier `AlgorithmHash`:

```java
String hash = HashUtil.hashHex("mi-texto", AlgorithmHash.SHA256);
String hashArchivo = HashUtil.hashBase64(pathAlArchivo, AlgorithmHash.SHA256);
```
Útil para verificar integridad de archivos subidos o generar identificadores únicos deterministas.

### `PageableUtil`
Construye un `Pageable` de Spring **de forma segura**, evitando que el frontend rompa el backend con valores inválidos:

```java
Pageable pageable = PageableUtil.build(page, size, "nombre", "ASC");
```
- `page` nunca puede ser negativo (se fuerza a 0 como mínimo).
- `size` se limita entre 1 y 100 (evita que alguien pida 1,000,000 de registros).
- `direction` acepta solo `"ASC"`/`"DESC"` (cualquier otra cosa cae a `ASC`).

### `ValidatorUtils`
Un solo método, `isEmpty(Object value)`, que funciona para:
- `String` (vacío o solo espacios)
- `UUID`
- `Collection` (lista/set vacío)
- `Optional` (vacío)
- `null` en general

```java
if (ValidatorUtils.isEmpty(request.getNombre())) {
    throw new ApiResponseException(ApiCodeResponse.REQUIRED_DATA, List.of("nombre"));
}
```

### `FileStorageUtil` (implementa `FileStorageService`)
Guarda archivos subidos (`MultipartFile`) en disco de forma segura:
1. Valida que el archivo no venga vacío.
2. Genera un nombre único (`UUID` + extensión original) para evitar colisiones.
3. Crea el directorio si no existe (`basePath` dentro de `properties.getUploadDir()`).
4. Copia el archivo físico.
5. Devuelve un `StoredFileDTO` con toda la metadata.

También tiene `getUrlResource(basePathFile)` para recuperar un archivo guardado como `Resource` (para servirlo en un endpoint de descarga), lanzando `ApiResponseException` si no existe o no es legible.

```java
@Service
@RequiredArgsConstructor
public class MiService {
    private final FileStorageUtil fileStorageUtil;

    public StoredFileDTO subirImagen(MultipartFile file) {
        return fileStorageUtil.save(file, "categorias/imagenes");
    }
}
```

---

## 🔄 Flujo típico end-to-end

```
Controller (extends BaseController)
        │
        ▼
   Service (usa RepositoryExecutor + ErrorFactory + ValidatorUtils)
        │
        ▼
   Repository (JpaRepository)
        │
   ¿Falla? ──► Excepción técnica (Postgres/Spring Data)
        │
        ▼
RepositoryExecutor.translate(...) ──► ApiResponseException
        │
        ▼
GlobalExceptionHandler (@RestControllerAdvice)
        │
        ▼
ApiResponseBuilder.build(...) ──► ResponseEntity<ApiDataResponseDTO<T>>
        │
        ▼
   Respuesta JSON estándar al cliente (Angular)
```

Si **todo sale bien**, el flujo es más corto: `Service` devuelve el dato → `Controller` llama a `ok(...)`/`okCreado(...)`/etc. → `ApiResponseBuilder` construye la respuesta.

---

## ✅ Checklist rápido al crear un módulo nuevo (ej. `Clientes`, `Pedidos`)

1. Entidad (`infrastructure/model`)
2. Repository extendiendo `JpaRepository` (`infrastructure/repository`)
3. DTOs de request/response (`infrastructure/dto`)
4. Mapper simple entidad ↔ DTO (`infrastructure/mapper`)
5. Service:
   - Todas las llamadas al repository van dentro de `RepositoryExecutor.execute(...)` / `.executeVoid(...)`
   - Usa `ErrorFactory.notFound(...)` cuando no se encuentre el recurso
   - Usa `ValidatorUtils.isEmpty(...)` para validaciones simples
   - Usa `PageableUtil.build(...)` para listados paginados
6. Controller extendiendo `BaseController`:
   - GET listado → `pkPaginado(...)`
   - GET por id → `okEncontrado(...)`
   - POST → `okCreado(...)`
   - PUT → `okActualizado(...)`
   - DELETE → `okEliminado(...)`
7. Si hay constraints nuevos en la BD → agrégalos a `ConstraintMessageResolver.MENSAJES`
8. Si hay un código de error que no existe en `ApiCodeResponse` → evalúa primero si `CONFLICT`/`REQUIRED_DATA`/`RESOURCE_NOT_FOUND` ya lo cubren antes de crear uno nuevo.

---

## 📝 Notas finales

- **No repitas try/catch de JPA/Postgres nunca.** Si te encuentras escribiendo uno, seguramente falta pasar esa llamada por `RepositoryExecutor`.
- **No construyas `Map.of(...)` de error a mano en cada service** si el caso ya es cubierto por `ErrorFactory`. Si se repite mucho un patrón nuevo, agrégalo como método a `ErrorFactory`.
- **Todo el formato de respuesta (JSON) es idéntico en toda la API** — esto es clave para que tu frontend en Angular pueda tener un solo interceptor/servicio genérico que interprete `codigo`, `tipo` y `datos` sin importar el endpoint.