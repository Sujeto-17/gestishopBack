# GestiShop Backend — Guía de uso del paquete `core`

El paquete `core` contiene código transversal reutilizable por TODOS los módulos
de negocio (negocios, admins, trabajadores, productos, etc). No contiene lógica
de negocio específica — solo el "andamiaje" común: respuestas estándar, manejo
de errores, paginación/búsqueda genérica, y utilidades.

Regla general: si vas a escribir código que se repetiría igual en más de un
módulo (formatear una respuesta, traducir una excepción de BD, paginar una
lista), probablemente ya existe en `core` — revisa aquí antes de reinventarlo.

---

## 1. Respuestas de la API (`core/dto`, `core/generic`)

Toda respuesta HTTP de la API sigue el mismo sobre (envelope):

```json
{
  "codigo": "OK",
  "mensaje": "Petición satisfactoria",
  "tipo": "SUCCESS",
  "datos": { ... }
}
```

Esto lo arma automáticamente `ApiResponseBuilder`, y tu controlador nunca lo
llama directo — en su lugar, tu controlador **extiende `BaseController`** y usa
sus métodos de ayuda:

```java
@RestController
@RequestMapping("/api/v1/negocios")
@RequiredArgsConstructor
public class NegocioController extends BaseController {

    private final NegocioService negocioService;

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiDataResponseDTO<NegocioResponseDTO>> obtener(@PathVariable UUID uuid) {
        NegocioResponseDTO negocio = negocioService.obtenerPorUuid(uuid);
        return ok(negocio);                    // 200, codigo=OK
    }

    @PostMapping
    public ResponseEntity<ApiDataResponseDTO<NegocioResponseDTO>> crear(@RequestBody @Valid NegocioRequestDTO dto) {
        NegocioResponseDTO creado = negocioService.crear(dto);
        return okCreado(creado);               // 201, codigo=OK_RC
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ApiDataResponseDTO<NegocioResponseDTO>> actualizar(@PathVariable UUID uuid, @RequestBody @Valid NegocioRequestDTO dto) {
        return okActualizado(negocioService.actualizar(uuid, dto));  // 200, codigo=OK_RU
    }

    @GetMapping
    public ResponseEntity<ApiPageResponseDTO<NegocioResponseDTO>> listar(SearchRequestDTO filtro) {
        return pkPaginado(negocioService.listar(filtro));  // 200, con page metadata
    }
}
```

**Cuándo usar cada método de `BaseController`:**

| Método | Cuándo | HTTP | Código |
|---|---|---|---|
| `ok(data)` | Consulta exitosa genérica | 200 | `OK` |
| `okEncontrado(data)` | Búsqueda puntual (GET por ID) | 200 | `OK_RF` |
| `okCreado(data)` | Después de un POST exitoso | 201 | `OK_RC` |
| `okActualizado(data)` | Después de un PUT/PATCH exitoso | 200 | `OK_RU` |
| `okEliminado(data)` | Después de un DELETE exitoso | 200 | `OK_RD` |
| `pkPaginado(page)` | Listados con paginación | 200 | `OK` + metadata |

No necesitas construir `ApiDataResponseDTO` a mano nunca — siempre a través de
estos métodos.

---

## 2. Manejo de errores (`core/exception`)

**Nunca captures excepciones de base de datos manualmente en el service.**
El flujo correcto es:

1. Tu `Repository` (Spring Data JPA) lanza la excepción nativa que sea
   (`DataIntegrityViolationException`, `PSQLException` envuelta, etc).
2. Envuelves la llamada al repositorio con `RepositoryExecutor`, que la traduce
   a una `ApiResponseException` con el código correcto automáticamente.
3. `GlobalExceptionHandler` intercepta cualquier `ApiResponseException` en
   cualquier parte de la app y arma la respuesta HTTP final.

Ejemplo real dentro de un `ServiceImpl`:

```java
@Service
@RequiredArgsConstructor
public class NegocioServiceImpl implements NegocioService {

    private final NegocioRepository negocioRepository;
    private final NegocioMapper negocioMapper;

    @Override
    public NegocioResponseDTO crear(NegocioRequestDTO dto) {
        Negocio entidad = negocioMapper.toEntity(dto);

        Negocio guardado = RepositoryExecutor.execute(
                () -> negocioRepository.save(entidad),
                "Negocio",       // entidad (para el mensaje de error)
                "crear"          // operación (para el mensaje de error)
        );

        return negocioMapper.toResponse(guardado);
    }

    @Override
    public NegocioResponseDTO obtenerPorUuid(UUID uuid) {
        Negocio negocio = RepositoryExecutor.execute(
                () -> negocioRepository.findByUuidNegocio(uuid)
                        .orElseThrow(() -> ErrorFactory.notFound("Negocio", uuid)),
                "Negocio",
                "consultar"
        );

        return negocioMapper.toResponse(negocio);
    }
}
```

Si, por ejemplo, intentas crear un negocio con un correo duplicado, Postgres
lanza `23505` (unique violation), `RepositoryExecutor` lo detecta como código
`23xxx`, arma automáticamente un `ApiResponseException` con
`ApiCodeResponse.CONFLICT` y usa `ConstraintMessageResolver` para dar un
mensaje humano legible en vez del texto crudo de Postgres.

**`ErrorFactory`** es un atajo para los casos de "no encontrado", que es el más
común en cualquier CRUD:
```java
.orElseThrow(() -> ErrorFactory.notFound("Trabajador", id))
```

**Cuándo lanzar una excepción manualmente (no de BD):** cuando es una regla de
negocio, no algo que la base de datos detecte. Ejemplo: "un negocio en periodo
de prueba no puede tener más de 2 trabajadores":

```java
if (negocio.getEstatus().equals("prueba") && trabajadoresActuales >= 2) {
    throw new ApiResponseException(
        ApiCodeResponse.CONFLICT,
        Map.of("detalle", "El plan de prueba permite máximo 2 trabajadores.")
    );
}
```

---

## 3. Códigos de respuesta (`ApiCodeResponse`)

Ya tienes un catálogo amplio de códigos (`SUCCESS`, `RESOURCE_NOT_FOUND`,
`CONFLICT`, `DATABASE_ERROR`, etc). **No agregues códigos nuevos a la ligera**
— la mayoría de tus casos ya caben en los existentes. Solo agrega uno nuevo si
representa un **tipo de situación genuinamente distinto** que se repetirá en
varios módulos (por ejemplo, si más adelante necesitas un código específico
para "plan vencido, acceso denegado", eso sí ameritaría uno nuevo).

---

## 4. Búsqueda y paginación genérica (`SearchRequestDTO`, `SearchFilterDTO`, `PageableUtil`)

Este mecanismo te permite construir un único endpoint de listado que soporte
filtros dinámicos, sin escribir un método de repositorio distinto por cada
combinación de filtros de búsqueda que tu Angular necesite (recuerda tus
tablas con buscador + filtro de estatus).

`SearchRequestDTO` viaja desde el frontend con esta forma:
```json
{
  "filters": [
    { "field": "estatus", "value": "activo", "operation": "EQUAL" },
    { "field": "nombre", "value": "julian", "operation": "LIKE" }
  ],
  "page": 0,
  "size": 10,
  "shortField": "nombre",
  "shortDirection": "ASC"
}
```

Para convertir `page/size/shortField/shortDirection` en un `Pageable` de
Spring, usas `PageableUtil.build(...)`. Para convertir la lista de `filters`
en una consulta dinámica, se recomienda una **Specification** de JPA (no la
tienes todavía en `core`, mi sugerencia es agregarla como
`GenericSpecificationBuilder` cuando armemos el primer módulo real de listado
con filtros — te lo muestro en el bloque de Negocios más abajo).

**Cuándo usar `SearchRequestDTO` vs. parámetros de query simples:** si el
listado tiene 1-2 filtros fijos (ej. solo estatus), usa `@RequestParam`
directo — es más simple. Si el listado necesita búsqueda libre + múltiples
filtros combinables (como tus tablas de Angular con buscador + filtro de
estatus + búsqueda por texto en varios campos), usa `SearchRequestDTO`.

---

## 5. `CatalogoRefDTO` — para selects/dropdowns

Este DTO es exactamente lo que necesitas para poblar los `<select>` de tu
Angular sin mandar el objeto completo. Ejemplo: cuando el frontend pide la
lista de giros de negocio para el formulario de alta:

```java
@GetMapping("/catalogos/giros")
public ResponseEntity<ApiDataResponseDTO<List<CatalogoRefDTO>>> listarGiros() {
    List<CatalogoRefDTO> giros = giroNegocioService.listarParaSelect();
    return ok(giros);
}
```

```json
{ "datos": [ { "id": 1, "nombre": "Restaurantes / comida" }, { "id": 2, "nombre": "Imprentas" } ] }
```

Úsalo para **todos** tus catálogos simples (`cat_generos`, `cat_giros_negocio`,
`cat_modulos`, `cat_estados`, `cat_municipios`) — es el mismo patrón, mismo DTO,
solo cambia el servicio que lo llena.

---

## 6. Utilidades (`core/utils`)

- **`GeneralUtil`**: normalización de texto (para búsquedas insensibles a
  acentos/mayúsculas — el "homotext"), generación de UUIDs, capitalización.
  Úsalo, por ejemplo, al guardar un campo "buscable" auxiliar en BD para
  búsquedas rápidas sin `LOWER()`/`UNACCENT()` en cada query.
- **`HashUtil`**: hashing genérico (MD5/SHA) — **NO es para contraseñas**.
  Para contraseñas usa **BCrypt** vía `PasswordEncoder` de Spring Security
  (lo configuraremos en `SecurityConfig`), nunca `HashUtil` directamente.
  `HashUtil` es útil para, por ejemplo, generar un hash de verificación de
  integridad de un archivo subido.
- **`DateUtils`**: formateo de fechas al estilo mexicano/español para PDFs o
  reportes con Jasper.
- **`PageableUtil`**: como ya vimos, arma el `Pageable` desde parámetros planos.
- **`ValidatorUtils`**: `isEmpty()` genérico que funciona igual para
  `String`, `UUID`, `Collection`, `Optional` — útil en validaciones dentro
  de un service sin repetir `if (x == null || x.isEmpty())` distinto por tipo.

---

## 7. Generación de reportes PDF (`FileStorageService`)

Ya tienes el servicio armado con JasperReports. Se usa cuando necesites
exportar algo a PDF (ej. corte de caja, reporte de asistencia mensual,
comprobante de pedido). El flujo es: diseñas el `.jrxml` con Jaspersoft
Studio, lo colocas en `resources/static/programa/...`, y llamas:

```java
byte[] pdf = fileStorageService.generarReportePdfMultiple(
    "programa/2026",
    List.of("asistencia_mensual"),
    Map.of("negocio", "The Julian's", "mes", "Julio 2026"),
    listaDeRegistrosDeAsistencia
);
```

No lo necesitas para el CRUD básico — solo cuando llegues a los módulos de
reportes/comprobantes imprimibles.

---

## 8. `CatalogoModulo` — este enum hay que reconsiderarlo

Detecté algo importante: tienes un enum `CatalogoModulo` con valores fijos
(`ASISTENCIA`, `NEGOCIOS`, `ADMINS`...). Esto **contradice** el diseño que
armamos en la base de datos, donde `cat_modulos` es una **tabla dinámica**
que el superadmin administra desde su panel (puede agregar módulos nuevos sin
tocar código). Si usas un enum Java fijo para los módulos, cada vez que el
superadmin quiera un módulo nuevo, tendrías que recompilar y desplegar. Mi
recomendación: **elimina `CatalogoModulo` como enum** y en su lugar, cuando
necesites verificar "¿este usuario tiene acceso al módulo X?", consulta contra
la tabla `cat_modulos`/`rol_modulos` en base de datos (o cachéala en memoria al
iniciar sesión, dentro del JWT, como ya diseñamos con la interfaz `Sesion`).

Los enums Java (`CHECK` constraints reflejados como enums) sí tienen sentido
para cosas que **nunca** deben cambiar sin una decisión de arquitectura, como
`ApiCodeType` (SUCCESS/BUSINESS_ERROR/TECHNICAL_ERROR) o `estatus`
(activo/inactivo) — pero no para catálogos de negocio que el propio superadmin
va a estar ampliando.