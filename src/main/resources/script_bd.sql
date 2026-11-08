-- ============================================================
-- EXTENSIONES REQUERIDAS
-- ============================================================
-- pgcrypto habilita gen_random_uuid(), usado para generar los
-- identificadores públicos (uuid_x) que se exponen en las URLs
-- de la API, evitando exponer los IDs internos autoincrementales.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- BLOQUE 1: CATÁLOGOS GENERALES DE PERSONA
-- ============================================================
-- Estas tablas casi no cambian con el tiempo. Se usan para
-- normalizar datos demográficos de usuarios (trabajadores,
-- admins) y así poder hacer reportes/segmentaciones a futuro.

-- Catálogo de géneros
CREATE TABLE cat_generos (
    id_genero   BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(30) NOT NULL UNIQUE,  -- 'Hombre', 'Mujer', 'Otro'
    estatus     VARCHAR(20) NOT NULL DEFAULT 'activo'
                CHECK (estatus IN ('activo','inactivo'))
);
COMMENT ON TABLE cat_generos IS 'Catálogo de géneros para datos personales de usuarios.';

-- Catálogo de estados civiles
CREATE TABLE cat_estados_civiles (
    id_estado_civil BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(30) NOT NULL UNIQUE,  -- 'Soltero(a)', 'Casado(a)'...
    estatus         VARCHAR(20) NOT NULL DEFAULT 'activo'
                    CHECK (estatus IN ('activo','inactivo'))
);
COMMENT ON TABLE cat_estados_civiles IS 'Catálogo de estados civiles.';

-- Catálogo de estados de México (los 32, catálogo completo)
CREATE TABLE cat_estados (
    id_estado   BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(60) NOT NULL UNIQUE,  -- 'Tabasco', 'Yucatán'...
    clave_inegi CHAR(2)                       -- clave oficial INEGI
);
COMMENT ON TABLE cat_estados IS 'Los 32 estados de la República Mexicana, catálogo completo.';

-- Municipios: inician solo con Tabasco, tabla lista para escalar a todo el país después
CREATE TABLE cat_municipios (
    id_municipio BIGSERIAL PRIMARY KEY,
    id_estado    BIGINT NOT NULL REFERENCES cat_estados(id_estado),
    nombre       VARCHAR(80) NOT NULL          -- 'Villahermosa', 'Macuspana'...
);
COMMENT ON TABLE cat_municipios IS
  'Municipios por estado. Se cargan inicialmente solo los 17 municipios de Tabasco; el resto del país se puede agregar después sin cambiar la estructura.';

CREATE INDEX ix_municipios_estado ON cat_municipios(id_estado);

-- ============================================================
-- BLOQUE 2: CATÁLOGOS DEL SISTEMA (giros y módulos)
-- ============================================================

-- Giro comercial del negocio (para categorizar, no para lógica de sistema)
CREATE TABLE cat_giros_negocio (
    id_giro     BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(60) NOT NULL UNIQUE,  -- 'Restaurantes / comida', 'Barberías / estéticas'...
    icono       VARCHAR(30),                  -- nombre de ícono FontAwesome, opcional, solo visual
    orden       SMALLINT NOT NULL DEFAULT 0,  -- controla el orden de aparición en selects
    estatus     VARCHAR(20) NOT NULL DEFAULT 'activo'
                CHECK (estatus IN ('activo','inactivo'))
);
COMMENT ON TABLE cat_giros_negocio IS
  'Catálogo abierto de giros comerciales (restaurante, barbería, imprenta, etc). Es solo categorización/etiqueta visual, no afecta la lógica del sistema.';

-- Módulos del sistema — el superadmin los administra desde su propia pantalla CRUD.
-- Cada fila representa una feature ya construida en código; agregar una fila
-- aquí la hace SELECCIONABLE en planes y permisos, pero no la crea por sí sola.
CREATE TABLE cat_modulos (
    id_modulo   BIGSERIAL PRIMARY KEY,
    clave       VARCHAR(40) NOT NULL
                CONSTRAINT uq_cat_modulos_clave UNIQUE
                CONSTRAINT ck_cat_modulos_clave CHECK (clave ~ '^[a-z][a-z0-9_]{1,39}$'),  -- identificador ESTABLE que usa el código (token, guards, permisos); el nombre se puede editar, la clave NO
    nombre      VARCHAR(60) NOT NULL UNIQUE,       -- 'Dashboard', 'Pedidos', 'Ventas'...
    descripcion VARCHAR(150),
    icono       VARCHAR(30),                       -- ícono FA para el sidebar
    ruta        VARCHAR(80),                       -- ej. '/admin/pedidos', para armar el menú dinámicamente
    orden       SMALLINT NOT NULL DEFAULT 0,        -- orden de aparición en el sidebar
    estatus     VARCHAR(20) NOT NULL DEFAULT 'activo'
                CHECK (estatus IN ('activo','inactivo')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMENT ON TABLE cat_modulos IS
  'Catálogo de módulos funcionales, administrado por el superadmin en su propia pantalla CRUD. El desarrollo real del módulo (pantallas, endpoints) siempre requiere programación; esta tabla solo controla su disponibilidad en planes y permisos.';

-- ============================================================
-- BLOQUE 3: PLANES DE SUSCRIPCIÓN (catálogo global del superadmin)
-- ============================================================

CREATE TABLE planes (
    id_plan         BIGSERIAL PRIMARY KEY,
    uuid_plan       UUID NOT NULL DEFAULT gen_random_uuid(),  -- ID público para exponer en API/URLs
    nivel           VARCHAR(20)  NOT NULL CHECK (nivel IN ('basico','pro','elite')),
    nombre          VARCHAR(50)  NOT NULL,                     -- 'Básico', 'Pro', 'Elite'
    precio          NUMERIC(10,2) NOT NULL CHECK (precio >= 0),
    descripcion     VARCHAR(160) NOT NULL,
    -- Límites del plan (NULL = sin límite). Se validan al registrar trabajadores y administradores del negocio
    max_trabajadores    INTEGER CONSTRAINT ck_planes_max_trabajadores    CHECK (max_trabajadores IS NULL OR max_trabajadores > 0),
    max_administradores INTEGER CONSTRAINT ck_planes_max_administradores CHECK (max_administradores IS NULL OR max_administradores > 0),
    estatus         VARCHAR(20)  NOT NULL DEFAULT 'activo'
                    CHECK (estatus IN ('activo','inactivo')),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
COMMENT ON TABLE planes IS
  'Catálogo FIJO de planes (basico / pro / elite) que el superadmin ofrece, con precio, módulos incluidos y límites de trabajadores y administradores. El tipo de flujo (servicio / tienda) es del negocio (negocios.sistema_type), no del plan.';

CREATE UNIQUE INDEX ux_planes_uuid ON planes(uuid_plan);

-- Relación N:M — qué módulos incluye cada plan
CREATE TABLE plan_modulos (
    id_plan     BIGINT NOT NULL REFERENCES planes(id_plan)   ON DELETE CASCADE,
    id_modulo   BIGINT NOT NULL REFERENCES cat_modulos(id_modulo),
    PRIMARY KEY (id_plan, id_modulo)
);
COMMENT ON TABLE plan_modulos IS
  'Tabla intermedia: cada fila dice "este plan incluye este módulo". Un plan puede tener varios módulos y un módulo puede estar en varios planes. Si un módulo cambia de nombre, se actualiza solo en cat_modulos y todos los planes lo reflejan automáticamente (no se duplica texto).';


-- ============================================================
-- BLOQUE 4: NEGOCIOS
-- ============================================================

CREATE TABLE negocios (
    id_negocio      BIGSERIAL PRIMARY KEY,
    uuid_negocio    UUID NOT NULL DEFAULT gen_random_uuid(),   -- ID público, se usa en URLs de API
    nombre          VARCHAR(80)  NOT NULL,
    logo_url        VARCHAR(255),
    sistema_type    VARCHAR(20)  NOT NULL CHECK (sistema_type IN ('servicio','tienda')),
    id_giro         BIGINT       NOT NULL REFERENCES cat_giros_negocio(id_giro),
    correo          VARCHAR(150) NOT NULL,
    telefono        VARCHAR(15),
    direccion       VARCHAR(200),
    id_municipio    BIGINT       REFERENCES cat_municipios(id_municipio),  -- dónde se ubica (obligatorio al registrar; NULL solo en negocios anteriores)
    -- Datos de facturación: opcionales, pero van los 4 primeros completos o ninguno
    razon_social         VARCHAR(150),
    rfc                  VARCHAR(13),
    regimen_fiscal       VARCHAR(3),     -- clave SAT de 3 dígitos (601, 612, 626...)
    codigo_postal_fiscal VARCHAR(5),
    correo_facturacion   VARCHAR(150),
    notas_internas       VARCHAR(300),   -- privadas del superadmin, el negocio nunca las ve
    id_plan         BIGINT       NOT NULL REFERENCES planes(id_plan),  -- plan ACTUAL vigente (denormalizado para lectura rápida)
    estatus         VARCHAR(20)  NOT NULL DEFAULT 'prueba'
                    CHECK (estatus IN ('activo','prueba','inactivo')),
    fecha_alta      DATE         NOT NULL DEFAULT CURRENT_DATE,  -- fecha de negocio, se muestra al usuario
    vencimiento     DATE         NOT NULL,                       -- vencimiento del plan/prueba actual
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),         -- metadatos técnicos de auditoría
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,                                  -- soft delete: NULL = activo, fecha = "borrado"
    CONSTRAINT ck_negocios_rfc          CHECK (rfc IS NULL OR rfc ~ '^[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3}$'),
    CONSTRAINT ck_negocios_regimen      CHECK (regimen_fiscal IS NULL OR regimen_fiscal ~ '^[0-9]{3}$'),
    CONSTRAINT ck_negocios_cp_fiscal    CHECK (codigo_postal_fiscal IS NULL OR codigo_postal_fiscal ~ '^[0-9]{5}$'),
    CONSTRAINT ck_negocios_fiscal_completo CHECK (
        (rfc IS NULL AND razon_social IS NULL AND regimen_fiscal IS NULL AND codigo_postal_fiscal IS NULL)
     OR (rfc IS NOT NULL AND razon_social IS NOT NULL AND regimen_fiscal IS NOT NULL AND codigo_postal_fiscal IS NOT NULL))
);
COMMENT ON TABLE negocios IS
  'Cada negocio que renta el sistema. id_plan apunta al plan actual vigente; el historial completo de pagos/cambios de plan vive en la tabla suscripciones.';

CREATE UNIQUE INDEX ux_negocios_uuid   ON negocios(uuid_negocio);
-- Índice único PARCIAL: el correo debe ser único solo entre negocios NO borrados,
-- así se puede reutilizar un correo si el negocio original fue dado de baja.
CREATE UNIQUE INDEX ux_negocios_correo ON negocios(correo) WHERE deleted_at IS NULL;
CREATE INDEX ix_negocios_plan  ON negocios(id_plan);
CREATE INDEX ix_negocios_giro  ON negocios(id_giro);
CREATE INDEX ix_negocios_municipio ON negocios(id_municipio);


-- ============================================================
-- BLOQUE 5: SUSCRIPCIONES (historial real de pagos por negocio)
-- ============================================================

CREATE TABLE suscripciones (
    id_suscripcion      BIGSERIAL PRIMARY KEY,
    id_negocio          BIGINT       NOT NULL REFERENCES negocios(id_negocio),
    id_plan             BIGINT       NOT NULL REFERENCES planes(id_plan),
    precio              NUMERIC(10,2) NOT NULL,  -- precio pagado en ESE periodo (histórico; si el plan sube de precio después, este registro no cambia)
    fecha_inicio        DATE         NOT NULL,
    fecha_vencimiento   DATE         NOT NULL,
    fecha_pago          DATE,                     -- cuándo se registró el pago real (NULL en el periodo de prueba)
    metodo_pago         VARCHAR(20)  CHECK (metodo_pago IN ('transferencia','tarjeta','efectivo','otro')),
    estatus             VARCHAR(20)  NOT NULL
                        CHECK (estatus IN ('prueba','pagada','cancelada')),  -- prueba = 15 días sin costo | pagada = 30 días cobrados | cancelada = pago registrado por error (no da acceso)
    notas               VARCHAR(255),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
COMMENT ON TABLE suscripciones IS
  'Historial de cada periodo de suscripción pagado (o pendiente) por un negocio. Una fila por mes/periodo. Permite ver cuándo pagó, cuánto, y detectar negocios vencidos.';

CREATE INDEX ix_suscripciones_negocio ON suscripciones(id_negocio);

-- ============================================================
-- BLOQUE 6: USUARIOS (tabla central de identidad y acceso)
-- ============================================================
-- Unifica superadmin, admin y trabajador en una sola tabla de
-- login. Los datos específicos de cada rol viven en tablas de
-- extensión (admin_negocio, trabajadores), evitando duplicar
-- campos comunes (nombre, correo, password) tres veces.

CREATE TABLE usuarios (
    id_usuario                  BIGSERIAL PRIMARY KEY,
    uuid_usuario                UUID NOT NULL DEFAULT gen_random_uuid(),
    tipo_usuario                VARCHAR(20)  NOT NULL
                                 CHECK (tipo_usuario IN ('superadmin','admin','trabajador')),

    -- Datos personales
    nombre                      VARCHAR(100) NOT NULL,
    correo                      VARCHAR(150) NOT NULL,
    telefono                    VARCHAR(15),

    -- Acceso / seguridad
    password_hash               VARCHAR(255) NOT NULL,          -- SIEMPRE hash (bcrypt/argon2), nunca texto plano
    debe_actualizar_password    BOOLEAN      NOT NULL DEFAULT true,  -- true al crear cuenta o al resetear password
    ultimo_acceso               TIMESTAMPTZ,

    -- Control
    estatus                     VARCHAR(20)  NOT NULL DEFAULT 'activo'
                                 CHECK (estatus IN ('activo','inactivo')),
    fecha_alta                  DATE         NOT NULL DEFAULT CURRENT_DATE,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at                  TIMESTAMPTZ
);
COMMENT ON TABLE usuarios IS
  'Tabla central de identidad para superadmin, admin y trabajador. Datos específicos de rol viven en tablas de extensión.';

CREATE UNIQUE INDEX ux_usuarios_uuid   ON usuarios(uuid_usuario);
CREATE UNIQUE INDEX ux_usuarios_correo ON usuarios(lower(correo)) WHERE deleted_at IS NULL;
CREATE INDEX ix_usuarios_tipo ON usuarios(tipo_usuario);

-- ============================================================
-- BLOQUE 7: ADMIN_NEGOCIO
-- ============================================================
-- Relación N:M entre usuarios (tipo_usuario='admin') y negocios.
-- Permite que un mismo negocio tenga varios administradores
-- (el dueño, su esposa, un familiar de confianza), todos con
-- control total sobre ese negocio.

CREATE TABLE admin_negocio (
    id_admin_negocio BIGSERIAL PRIMARY KEY,
    id_usuario       BIGINT NOT NULL REFERENCES usuarios(id_usuario),
    id_negocio       BIGINT NOT NULL REFERENCES negocios(id_negocio),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (id_usuario, id_negocio)  -- evita registrar al mismo admin dos veces en el mismo negocio
);
COMMENT ON TABLE admin_negocio IS
  'Conecta usuarios de tipo admin con el/los negocio(s) que administran. Soporta varios admins por negocio (dueño, esposa, familiar de confianza, etc).';

CREATE INDEX ix_admin_negocio_negocio ON admin_negocio(id_negocio);
CREATE INDEX ix_admin_negocio_usuario ON admin_negocio(id_usuario);

-- ============================================================
-- BLOQUE 8B: HORARIOS (módulo Horarios)
-- ============================================================

-- btree_gist permite la restricción de "sin traslape de fechas" por trabajador
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Tolerancia de retardo (minutos). Catálogo general y oficial: no lo edita nadie desde la app.
CREATE TABLE cat_tolerancias (
    id_tolerancia BIGSERIAL PRIMARY KEY,
    minutos       SMALLINT    NOT NULL UNIQUE CHECK (minutos BETWEEN 0 AND 120),
    nombre        VARCHAR(30) NOT NULL UNIQUE      -- 'Sin tolerancia', '15 minutos'...
);
COMMENT ON TABLE cat_tolerancias IS
  'Minutos de gracia antes de contar un retardo. 0 = sin tolerancia. Se asigna a cada trabajador.';

CREATE TABLE horarios (
    id_horario    BIGSERIAL PRIMARY KEY,
    uuid_horario  UUID         NOT NULL DEFAULT gen_random_uuid(),   -- ID público para la API
    id_negocio    BIGINT REFERENCES negocios(id_negocio),            -- NULL = horario global
    nombre        VARCHAR(80)  NOT NULL,
    estatus       VARCHAR(20)  NOT NULL DEFAULT 'activo'
                  CHECK (estatus IN ('activo','inactivo')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_horarios_uuid   ON horarios(uuid_horario);
CREATE UNIQUE INDEX ux_horarios_nombre ON horarios (COALESCE(id_negocio, 0), lower(nombre));
CREATE INDEX ix_horarios_negocio ON horarios(id_negocio);
COMMENT ON TABLE horarios IS
  'id_negocio NULL = horario global (inmutable). Con valor = horario propio del negocio. Evitar horarios repetidos se valida en el backend.';

CREATE TABLE horario_dias (
    id_horario_dia BIGSERIAL PRIMARY KEY,
    id_horario     BIGINT   NOT NULL REFERENCES horarios(id_horario) ON DELETE CASCADE,
    dia_semana     SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),   -- ISO: 1 = lunes ... 7 = domingo
    hora_entrada   TIME     NOT NULL,
    hora_salida    TIME     NOT NULL,
    CONSTRAINT uq_horario_dias UNIQUE (id_horario, dia_semana),
    CONSTRAINT ck_horario_dias_horas CHECK (hora_entrada <> hora_salida)
);
COMMENT ON TABLE horario_dias IS
  'Un día sin fila es día de descanso. Si hora_salida <= hora_entrada el turno cruza medianoche y cuenta para el día en que empieza.';

-- ============================================================
-- BLOQUE 8: TRABAJADORES
-- ============================================================
-- Extiende usuarios con los datos laborales. Cada trabajador
-- pertenece a exactamente un negocio.

CREATE TABLE trabajadores (
    id_trabajador   BIGSERIAL PRIMARY KEY,
    id_usuario      BIGINT NOT NULL UNIQUE REFERENCES usuarios(id_usuario),  -- 1 a 1 con usuarios
    id_negocio      BIGINT NOT NULL REFERENCES negocios(id_negocio),

    -- Datos personales
    fecha_nacimiento DATE,
    id_genero        BIGINT REFERENCES cat_generos(id_genero),
    id_estado_civil  BIGINT REFERENCES cat_estados_civiles(id_estado_civil),
    direccion        VARCHAR(200),
    id_municipio     BIGINT REFERENCES cat_municipios(id_municipio),
    curp             VARCHAR(18),
    nss              VARCHAR(11),

    -- Contacto de emergencia
    contacto_emergencia_nombre     VARCHAR(100),
    contacto_emergencia_telefono   VARCHAR(10),
    contacto_emergencia_parentesco VARCHAR(30),

    -- Datos laborales
    puesto          VARCHAR(100) NOT NULL,
    id_tolerancia   BIGINT NOT NULL REFERENCES cat_tolerancias(id_tolerancia),  -- minutos de gracia para retardo; propia de cada trabajador
    nivel_acceso    VARCHAR(20)  NOT NULL DEFAULT 'estandar'
                    CHECK (nivel_acceso IN ('gerente','estandar')),   -- controla qué módulos ve
    tipo_contrato   VARCHAR(20)  NOT NULL,                  -- texto libre
    fecha_ingreso   DATE         NOT NULL DEFAULT CURRENT_DATE,
    notas           VARCHAR(300),

    -- Pago
    salario            NUMERIC(10,2) NOT NULL CHECK (salario >= 0),   -- monto por periodo de pago
    periodicidad_pago  VARCHAR(20)   NOT NULL DEFAULT 'mensual'
                       CHECK (periodicidad_pago IN ('diario','semanal','quincenal','mensual')),
    metodo_pago        VARCHAR(20)   NOT NULL DEFAULT 'efectivo'
                       CHECK (metodo_pago IN ('efectivo','transferencia','cheque')),
    banco              VARCHAR(50),
    clabe              VARCHAR(18),

    -- Régimen
    regimen                  VARCHAR(10) NOT NULL DEFAULT 'informal'
                             CHECK (regimen IN ('informal','formal')),
    rfc                      VARCHAR(13),
    numero_empleado          VARCHAR(20),
    salario_diario_integrado NUMERIC(10,2) CHECK (salario_diario_integrado IS NULL OR salario_diario_integrado >= 0),
    fecha_alta_imss          DATE,
    dias_aguinaldo           SMALLINT CHECK (dias_aguinaldo IS NULL OR dias_aguinaldo >= 15),
    prima_vacacional_pct     SMALLINT CHECK (prima_vacacional_pct IS NULL OR prima_vacacional_pct BETWEEN 25 AND 100),

    -- Formatos
    CONSTRAINT ck_trabajadores_nacimiento    CHECK (fecha_nacimiento IS NULL OR fecha_nacimiento <= CURRENT_DATE),
    CONSTRAINT ck_trabajadores_curp          CHECK (curp IS NULL OR curp ~ '^[A-Z0-9]{18}$'),
    CONSTRAINT ck_trabajadores_nss           CHECK (nss  IS NULL OR nss  ~ '^[0-9]{11}$'),
    CONSTRAINT ck_trabajadores_rfc           CHECK (rfc  IS NULL OR rfc  ~ '^[A-ZÑ&]{4}[0-9]{6}[A-Z0-9]{3}$'),
    CONSTRAINT ck_trabajadores_clabe         CHECK (clabe IS NULL OR clabe ~ '^[0-9]{18}$'),
    CONSTRAINT ck_trabajadores_emergencia_tel CHECK (contacto_emergencia_telefono IS NULL
                                                     OR contacto_emergencia_telefono ~ '^[0-9]{10}$'),

    -- Reglas entre campos
    CONSTRAINT ck_trabajador_formal CHECK (
        regimen = 'informal'
        OR (curp IS NOT NULL AND nss IS NOT NULL AND rfc IS NOT NULL
            AND salario_diario_integrado IS NOT NULL AND fecha_alta_imss IS NOT NULL)),
    CONSTRAINT ck_trabajador_transferencia CHECK (
        metodo_pago <> 'transferencia' OR (banco IS NOT NULL AND clabe IS NOT NULL))
);

CREATE INDEX ix_trabajadores_negocio ON trabajadores(id_negocio);
CREATE INDEX ix_trabajadores_tolerancia ON trabajadores(id_tolerancia);

COMMENT ON TABLE trabajadores IS
  'nivel_acceso = qué módulos ve. regimen = con o sin nómina formal. El horario vive en trabajador_horarios; la tolerancia de retardo es del propio trabajador. Son conceptos independientes.';

CREATE TABLE trabajador_horarios (
    id_trabajador_horario BIGSERIAL PRIMARY KEY,
    id_trabajador   BIGINT NOT NULL REFERENCES trabajadores(id_trabajador),
    id_horario      BIGINT NOT NULL REFERENCES horarios(id_horario),
    vigente_desde   DATE   NOT NULL,
    vigente_hasta   DATE,                                   -- NULL = sigue vigente
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_trabajador_horarios_fechas CHECK (vigente_hasta IS NULL OR vigente_hasta >= vigente_desde),
    -- un trabajador no puede tener dos horarios en la misma fecha (cubre también "solo uno abierto")
    CONSTRAINT ex_trabajador_horarios_sin_traslape EXCLUDE USING gist (
        id_trabajador WITH =,
        daterange(vigente_desde, vigente_hasta, '[]') WITH &&)
);
CREATE INDEX ix_trabajador_horarios_horario ON trabajador_horarios(id_horario);
COMMENT ON TABLE trabajador_horarios IS
  'Historial de horarios por trabajador. Para cambiar: cerrar el vigente (vigente_hasta = día anterior) y abrir uno nuevo. El horario de una fecha es el que la contiene. La tolerancia NO va aquí: es del trabajador.';

-- ============================================================
-- BLOQUE 9: ROL_MODULOS (permisos data-driven)
-- ============================================================
-- Define qué módulos puede ver cada combinación de tipo_usuario
-- + nivel_acceso. Se consulta al armar el menú lateral y al
-- validar cada request en el backend. Se actualiza con datos,
-- sin necesidad de tocar código cuando cambian las reglas.

CREATE TABLE rol_modulos (
    id_rol_modulo   BIGSERIAL PRIMARY KEY,
    tipo_usuario    VARCHAR(20) NOT NULL CHECK (tipo_usuario IN ('admin','trabajador')),
    nivel_acceso    VARCHAR(20) CHECK (nivel_acceso IN ('gerente','estandar')),  -- NULL cuando tipo_usuario='admin'
    id_modulo       BIGINT NOT NULL REFERENCES cat_modulos(id_modulo),
    UNIQUE (tipo_usuario, nivel_acceso, id_modulo)
);
COMMENT ON TABLE rol_modulos IS
  'admin no necesita filas aquí: siempre ve todos los módulos de su plan sin restricción. Esta tabla solo restringe explícitamente a gerente/estandar.';

-- ============================================================
-- REFRESH TOKENS (sesiones activas, revocables)
-- ============================================================
CREATE TABLE refresh_tokens (
    id_refresh_token    BIGSERIAL PRIMARY KEY,
    id_usuario          BIGINT NOT NULL REFERENCES usuarios(id_usuario),
    token_hash          VARCHAR(255) NOT NULL,   -- SHA-256 del token real, nunca el token en claro
    ip_origen           VARCHAR(45),              -- IPv4/IPv6 de origen, para auditoría
    user_agent          VARCHAR(255),
    id_negocio_activo   BIGINT REFERENCES negocios(id_negocio),  -- negocio con el que trabaja el ADMIN en esta sesión (NULL para los demás)
    fecha_expiracion    TIMESTAMPTZ NOT NULL,
    revocado            BOOLEAN NOT NULL DEFAULT false,
    fecha_revocado       TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMENT ON TABLE refresh_tokens IS
  'Sesiones de refresh token activas. Cada login genera una fila. Permite revocar sesiones individuales (logout) o todas (robo detectado) sin esperar expiración.';

CREATE INDEX ix_refresh_tokens_usuario ON refresh_tokens(id_usuario);
CREATE UNIQUE INDEX ux_refresh_tokens_hash ON refresh_tokens(token_hash);

-- ============================================================
-- INTENTOS DE LOGIN FALLIDOS (rate limiting / bloqueo temporal)
-- ============================================================
CREATE TABLE intentos_login (
    id_intento      BIGSERIAL PRIMARY KEY,
    correo          VARCHAR(150) NOT NULL,
    ip_origen       VARCHAR(45),
    exitoso         BOOLEAN NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMENT ON TABLE intentos_login IS
  'Bitácora de intentos de login (exitosos y fallidos) usada para bloqueo temporal por fuerza bruta.';

CREATE INDEX ix_intentos_login_correo_fecha ON intentos_login(correo, created_at);

-- ============================================================
-- CATÁLOGO: a qué tipo de entidad pertenece un archivo
-- ============================================================
-- Abierto (no CHECK fijo) porque, como ya anticipaste, crecerá:
-- hoy 'negocio', luego 'producto', 'trabajador', 'cliente', etc.
CREATE TABLE cat_tipos_entidad_archivo (
    id_tipo_entidad BIGSERIAL PRIMARY KEY,
    clave           VARCHAR(30) NOT NULL UNIQUE,  -- 'negocio', 'producto', 'trabajador'...
    nombre          VARCHAR(60) NOT NULL,          -- 'Negocios', 'Productos', 'Trabajadores'
    estatus         VARCHAR(20) NOT NULL DEFAULT 'activo' CHECK (estatus IN ('activo','inactivo'))
);
COMMENT ON TABLE cat_tipos_entidad_archivo IS
  'Catálogo de módulos que pueden tener archivos adjuntos (negocio, producto, trabajador...). Se amplía insertando filas nuevas, sin migraciones.';

  -- ============================================================
  -- ARCHIVOS: tabla genérica para fotos/documentos de cualquier módulo
  -- ============================================================
  CREATE TABLE archivos (
      id_archivo        BIGSERIAL PRIMARY KEY,
      uuid_archivo      UUID NOT NULL DEFAULT gen_random_uuid(),

      -- Relación polimórfica: a qué tipo de entidad y a qué fila específica pertenece.
      -- NO es una FK real de Postgres (no puede apuntar a varias tablas distintas);
      -- la integridad se controla desde el código del service.
      id_tipo_entidad   BIGINT NOT NULL REFERENCES cat_tipos_entidad_archivo(id_tipo_entidad),
      id_entidad        BIGINT NOT NULL,              -- ej. el id_negocio, id_producto, id_trabajador

      -- Clasificación dentro de la misma entidad (una entidad puede tener varios "tipos" de archivo)
      categoria         VARCHAR(30) NOT NULL,          -- 'logo', 'foto_producto', 'documento_curp', 'foto_perfil'...

      -- Metadata del archivo físico (viene directo de StoredFileDTO)
      nombre_archivo    VARCHAR(150) NOT NULL,          -- nombre generado (UUID + extensión)
      nombre_original   VARCHAR(255),                   -- nombre con el que lo subió el usuario
      content_type      VARCHAR(100),
      tamanio_bytes     BIGINT,
      ruta_relativa     VARCHAR(255) NOT NULL,          -- subcarpeta donde vive, ej. 'negocios/14'

      -- Para entidades con varias fotos (producto, trabajador): cuál es la principal/portada
      es_principal      BOOLEAN NOT NULL DEFAULT false,
      orden             SMALLINT NOT NULL DEFAULT 0,

      estatus           VARCHAR(20) NOT NULL DEFAULT 'activo' CHECK (estatus IN ('activo','inactivo')),
      created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
      deleted_at        TIMESTAMPTZ                      -- soft delete: el archivo físico se puede limpiar en batch después
  );
  COMMENT ON TABLE archivos IS
    'Tabla genérica de archivos adjuntos (fotos, documentos) para cualquier módulo del sistema. id_tipo_entidad + id_entidad identifican a qué registro pertenece.';

  CREATE INDEX ix_archivos_entidad ON archivos(id_tipo_entidad, id_entidad);
  CREATE UNIQUE INDEX ux_archivos_uuid ON archivos(uuid_archivo);

  -- Seed inicial
  INSERT INTO cat_tipos_entidad_archivo (clave, nombre) VALUES
    ('negocio', 'Negocios');
    -- más adelante: ('producto', 'Productos'), ('trabajador', 'Trabajadores'), ...

-- ============================================================
-- SEED (datos iniciales de referencia)
-- ============================================================

-- Módulos base ya existentes en el sistema
INSERT INTO cat_modulos (clave, nombre, descripcion, icono, ruta, orden) VALUES
  ('dashboard', 'Dashboard',      'Panel principal con resumen, distinto según el rol que lo consulta', 'chart-pie',        '/dashboard',      1),
  ('asistencia', 'Asistencia',     'Registro y consulta de asistencia de trabajadores',                  'user-check',       '/asistencia',     2),
  ('pedidos', 'Pedidos',        'Gestión de pedidos con seguimiento de proceso (negocios servicio)',  'clipboard-list',   '/pedidos',        3),
  ('ventas', 'Ventas',         'Venta directa de productos (negocios tienda)',                        'cash-register',    '/ventas',         4),
  ('clientes', 'Clientes',       'Catálogo de clientes frecuentes, factura y mayoristas',               'users',            '/clientes',       5),
  ('cotizaciones', 'Cotizaciones',   'Elaboración de cotizaciones previas a un pedido',                     'file-invoice',     '/cotizaciones',   6),
  ('productos', 'Productos',      'Catálogo de productos con precios e inventario',                      'box-open',         '/productos',      7),
  ('proveedores', 'Proveedores',    'Contactos y condiciones de compra a proveedores',                     'truck',            '/proveedores',    8),
  ('categorias', 'Categorías',     'Clasificación de productos o servicios',                              'tag',              '/categorias',     9),
  ('trabajadores', 'Trabajadores',   'Alta y gestión del personal del negocio',                             'user-tie',         '/trabajadores',  10),
  ('horarios', 'Horarios',        'Horarios de trabajo del personal: días, entradas, salidas y tolerancia', 'clock',            '/horarios',      11),
  ('configuracion', 'Configuración',  'Ajustes generales del negocio',                                       'sliders',          '/configuracion', 12),
  ('justificantes', 'Justificantes',  'Solicitud y autorización de justificantes de falta',                  'file-signature',   '/justificantes', 13),
  ('vacaciones', 'Vacaciones',     'Solicitud y control de días de vacaciones',                           'umbrella-beach',   '/vacaciones',    14),
  ('solicitudes', 'Solicitudes',    'Permisos especiales: home office, licencia médica, etc.',             'inbox',            '/solicitudes',   15),
  ('citas', 'Citas',          'Agenda de citas con clientes (negocios tipo servicio)',               'calendar-check',   '/citas',         16),
  ('reportes', 'Reportes',       'Reportes y estadísticas del negocio',                                 'chart-line',       '/reportes',      17),
  ('perfil', 'Perfil',         'Datos personales y configuración de la propia cuenta',                'user',             '/perfil',        18);

-- Giros de negocio de ejemplo
INSERT INTO cat_giros_negocio (nombre, icono, orden) VALUES
  ('Distribuidor de Materiales',   'dolly',           1),
  ('Restaurantes / comida',        'utensils',        2),
  ('Tiendas de ropa',              'shirt',           3),
  ('Imprentas',                    'print',           4),
  ('Tiendas de tecnología',        'laptop',          5),
  ('Abarrotes / minisuper',        'basket-shopping', 6),
  ('Barberías / estéticas',        'scissors',        7),
  ('Farmacias',                    'pills',           8),
  ('Papelerías',                   'pencil',          9),
  ('Negocios de servicios',        'briefcase',       10),
  ('Otro comercio',                'store',          99);

-- GERENTE: control operativo amplio, sin ver costos/proveedores/config
INSERT INTO rol_modulos (tipo_usuario, nivel_acceso, id_modulo)
SELECT 'trabajador', 'gerente', id_modulo FROM cat_modulos
-- (Los módulos del gerente se definirán más adelante; por ahora se conserva su lista original.)
WHERE clave IN (
  'dashboard','asistencia','pedidos','ventas','clientes',
  'vacaciones','justificantes','solicitudes','citas','perfil'
);

-- ESTANDAR: solo autoservicio personal
INSERT INTO rol_modulos (tipo_usuario, nivel_acceso, id_modulo)
SELECT 'trabajador', 'estandar', id_modulo FROM cat_modulos
WHERE nombre IN (
  'Dashboard','Asistencia','Vacaciones','Justificantes','Solicitudes','Perfil'
);

-- Ejemplo de municipios iniciales (Tabasco)
INSERT INTO cat_estados (nombre, clave_inegi) VALUES
  ('Aguascalientes', '01'), ('Baja California', '02'), ('Baja California Sur', '03'),
  ('Campeche', '04'), ('Coahuila', '05'), ('Colima', '06'), ('Chiapas', '07'),
  ('Chihuahua', '08'), ('Ciudad de México', '09'), ('Durango', '10'),
  ('Guanajuato', '11'), ('Guerrero', '12'), ('Hidalgo', '13'), ('Jalisco', '14'),
  ('México', '15'), ('Michoacán', '16'), ('Morelos', '17'), ('Nayarit', '18'),
  ('Nuevo León', '19'), ('Oaxaca', '20'), ('Puebla', '21'), ('Querétaro', '22'),
  ('Quintana Roo', '23'), ('San Luis Potosí', '24'), ('Sinaloa', '25'),
  ('Sonora', '26'), ('Tabasco', '27'), ('Tamaulipas', '28'), ('Tlaxcala', '29'),
  ('Veracruz', '30'), ('Yucatán', '31'), ('Zacatecas', '32');

INSERT INTO cat_municipios (id_estado, nombre)
SELECT id_estado, m.nombre
FROM cat_estados,
     (VALUES
        ('Balancán'), ('Cárdenas'), ('Centla'), ('Centro'),
        ('Comalcalco'), ('Cunduacán'), ('Emiliano Zapata'), ('Huimanguillo'),
        ('Jalapa'), ('Jalpa de Méndez'), ('Jonuta'), ('Macuspana'),
        ('Nacajuca'), ('Paraíso'), ('Tacotalpa'), ('Teapa'), ('Tenosique')
     ) AS m(nombre)
WHERE cat_estados.nombre = 'Tabasco';

-- Géneros y estados civiles base
INSERT INTO cat_generos (nombre) VALUES ('Hombre'), ('Mujer'), ('Otro');

INSERT INTO cat_estados_civiles (nombre) VALUES
  ('Soltero(a)'), ('Casado(a)'), ('Unión libre'), ('Divorciado(a)'), ('Viudo(a)');

  -- ------------------------------------------------------------
  -- SEED: tolerancias generales y horarios globales (lunes a viernes)
  -- ------------------------------------------------------------
  INSERT INTO cat_tolerancias (minutos, nombre) VALUES
    (0,  'Sin tolerancia'),
    (5,  '5 minutos'),
    (10, '10 minutos'),
    (15, '15 minutos'),
    (20, '20 minutos'),
    (25, '25 minutos'),
    (30, '30 minutos');

  INSERT INTO horarios (nombre) VALUES
    ('Lunes a viernes 08:00 - 16:00'),
    ('Lunes a viernes 09:00 - 17:00'),
    ('Lunes a viernes 08:00 - 17:00'),
    ('Lunes a viernes 09:00 - 18:00'),
    ('Nocturno lunes a viernes 22:00 - 06:00');

  INSERT INTO horario_dias (id_horario, dia_semana, hora_entrada, hora_salida)
  SELECT h.id_horario, d.dia, v.entrada, v.salida
  FROM horarios h
  JOIN (VALUES
    ('Lunes a viernes 08:00 - 16:00',          TIME '08:00', TIME '16:00'),
    ('Lunes a viernes 09:00 - 17:00',          TIME '09:00', TIME '17:00'),
    ('Lunes a viernes 08:00 - 17:00',          TIME '08:00', TIME '17:00'),
    ('Lunes a viernes 09:00 - 18:00',          TIME '09:00', TIME '18:00'),
    ('Nocturno lunes a viernes 22:00 - 06:00', TIME '22:00', TIME '06:00')
  ) AS v(nombre, entrada, salida) ON v.nombre = h.nombre
  CROSS JOIN generate_series(1, 5) AS d(dia);

-- ------------------------------------------------------------
-- TRIGGERS (van al FINAL, después de los seeds: los horarios globales se insertan antes de protegerlos)
-- Protegen el historial y el aislamiento entre negocios
-- ------------------------------------------------------------

-- Horarios globales intocables; los propios se bloquean al primer uso; cancelar solo sin trabajadores vigentes
CREATE FUNCTION fn_horarios_proteger() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.id_negocio IS NULL THEN
        RAISE EXCEPTION 'Los horarios globales no se pueden modificar ni eliminar';
    END IF;

    IF TG_OP = 'DELETE' THEN
        IF EXISTS (SELECT 1 FROM trabajador_horarios WHERE id_horario = OLD.id_horario) THEN
            RAISE EXCEPTION 'El horario ya fue usado: solo se puede cancelar, no eliminar';
        END IF;
        RETURN OLD;
    END IF;

    IF (NEW.nombre IS DISTINCT FROM OLD.nombre OR NEW.id_negocio IS DISTINCT FROM OLD.id_negocio)
       AND EXISTS (SELECT 1 FROM trabajador_horarios WHERE id_horario = OLD.id_horario) THEN
        RAISE EXCEPTION 'El horario ya fue usado: no se puede modificar, solo cancelar';
    END IF;

    IF NEW.estatus = 'inactivo' AND OLD.estatus <> 'inactivo'
       AND EXISTS (SELECT 1 FROM trabajador_horarios
                   WHERE id_horario = OLD.id_horario
                     AND (vigente_hasta IS NULL OR vigente_hasta >= CURRENT_DATE)) THEN
        RAISE EXCEPTION 'No se puede cancelar: hay trabajadores con este horario vigente';
    END IF;

    RETURN NEW;
END $$;

CREATE TRIGGER tg_horarios_proteger
    BEFORE UPDATE OR DELETE ON horarios
    FOR EACH ROW EXECUTE FUNCTION fn_horarios_proteger();

-- Los días de un horario global o ya usado no cambian
CREATE FUNCTION fn_horario_dias_proteger() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_id_horario BIGINT := CASE WHEN TG_OP = 'DELETE' THEN OLD.id_horario ELSE NEW.id_horario END;
    v_negocio    BIGINT;
BEGIN
    SELECT id_negocio INTO v_negocio FROM horarios WHERE id_horario = v_id_horario;
    IF NOT FOUND THEN
        RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;      -- borrado en cascada: el horario ya se eliminó
    END IF;

    IF v_negocio IS NULL THEN
        RAISE EXCEPTION 'Los horarios globales no se pueden modificar';
    END IF;
    IF EXISTS (SELECT 1 FROM trabajador_horarios WHERE id_horario = v_id_horario) THEN
        RAISE EXCEPTION 'El horario ya fue usado: no se pueden cambiar sus días';
    END IF;

    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END $$;

CREATE TRIGGER tg_horario_dias_proteger
    BEFORE INSERT OR UPDATE OR DELETE ON horario_dias
    FOR EACH ROW EXECUTE FUNCTION fn_horario_dias_proteger();

-- Un trabajador solo puede usar horarios globales o de su propio negocio, y activos al asignarlos
CREATE FUNCTION fn_trabajador_horarios_validar() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_negocio_horario BIGINT;
    v_estatus         VARCHAR(20);
    v_negocio_trab    BIGINT;
BEGIN
    SELECT id_negocio, estatus INTO v_negocio_horario, v_estatus FROM horarios WHERE id_horario = NEW.id_horario;
    SELECT id_negocio INTO v_negocio_trab FROM trabajadores WHERE id_trabajador = NEW.id_trabajador;

    IF v_negocio_horario IS NOT NULL AND v_negocio_horario <> v_negocio_trab THEN
        RAISE EXCEPTION 'El horario pertenece a otro negocio';
    END IF;
    IF TG_OP = 'INSERT' AND v_estatus <> 'activo' THEN
        RAISE EXCEPTION 'El horario está cancelado y no se puede asignar';
    END IF;

    RETURN NEW;
END $$;

CREATE TRIGGER tg_trabajador_horarios_validar
    BEFORE INSERT OR UPDATE ON trabajador_horarios
    FOR EACH ROW EXECUTE FUNCTION fn_trabajador_horarios_validar();

-- ============================================================
-- MIGRACIÓN: INVENTARIO (categorías, atributos, productos con variantes,
--            proveedores, movimientos de inventario y resurtido)
-- ============================================================
-- Se ejecuta UNA sola vez sobre la BD que ya tiene el módulo de horarios.
--
-- Decisiones de diseño:
--   * Todo es POR NEGOCIO: cada negocio tiene sus propias categorías, atributos, productos y proveedores.
--     El aislamiento entre negocios lo garantizan llaves foráneas COMPUESTAS (id, id_negocio):
--     es imposible enlazar, por ejemplo, un producto de un negocio con una categoría de otro.
--   * Producto = ficha general. Variante = lo que realmente se vende y se cuenta (precio, stock, SKU).
--     Todo producto tiene al menos una variante; el producto simple tiene una sola, sin atributos.
--   * El stock solo cambia a través de movimientos_inventario (un trigger lo aplica): nadie edita el número.
--   * El costo y el proveedor viven en variante_proveedores y movimientos: son datos que el gerente NO ve.
--     Eso se aplica en el backend con DTOs distintos por rol.

BEGIN;

-- ------------------------------------------------------------
-- 1. Configuración de IVA del negocio
-- ------------------------------------------------------------
ALTER TABLE negocios
    ADD COLUMN emite_factura        BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN precios_incluyen_iva BOOLEAN NOT NULL DEFAULT true;
COMMENT ON COLUMN negocios.emite_factura IS
  'false = el negocio no factura y el IVA no existe para él. true = factura.';
COMMENT ON COLUMN negocios.precios_incluyen_iva IS
  'Solo aplica si emite_factura. true = el precio público ya trae el IVA incluido; false = precio sin IVA, se suma al facturar.';

-- ------------------------------------------------------------
-- 2. Catálogos globales
-- ------------------------------------------------------------
CREATE TABLE cat_unidades_medida (
    id_unidad     BIGSERIAL PRIMARY KEY,
    clave         VARCHAR(10) NOT NULL UNIQUE,   -- 'pza', 'm', 'kg'
    nombre        VARCHAR(40) NOT NULL UNIQUE,   -- 'Pieza', 'Metro'
    permite_decimales BOOLEAN NOT NULL DEFAULT false,  -- metros, litros y kilos sí; piezas no
    orden         SMALLINT NOT NULL DEFAULT 0,
    estatus       VARCHAR(20) NOT NULL DEFAULT 'activo' CHECK (estatus IN ('activo','inactivo'))
);
COMMENT ON TABLE cat_unidades_medida IS 'Unidad en la que se compra, cuenta y vende un producto. Catálogo general.';

INSERT INTO cat_unidades_medida (clave, nombre, permite_decimales, orden) VALUES
  ('pza',   'Pieza',    false, 1),
  ('par',   'Par',      false, 2),
  ('caja',  'Caja',     false, 3),
  ('paq',   'Paquete',  false, 4),
  ('rollo', 'Rollo',    false, 5),
  ('juego', 'Juego',    false, 6),
  ('m',     'Metro',    true,  7),
  ('m2',    'Metro cuadrado', true, 8),
  ('kg',    'Kilogramo', true, 9),
  ('g',     'Gramo',    true, 10),
  ('l',     'Litro',    true, 11),
  ('ml',    'Mililitro', true, 12);

-- Categorías y atributos que se OFRECEN al negocio según su giro (el admin las copia y edita; no quedan enlazadas)
CREATE TABLE cat_categorias_sugeridas (
    id_sugerida   BIGSERIAL PRIMARY KEY,
    id_giro       BIGINT NOT NULL REFERENCES cat_giros_negocio(id_giro),
    nombre        VARCHAR(80) NOT NULL,
    id_padre      BIGINT REFERENCES cat_categorias_sugeridas(id_sugerida),   -- NULL = familia; con valor = subfamilia
    orden         SMALLINT NOT NULL DEFAULT 0,
    UNIQUE (id_giro, id_padre, nombre)
);
COMMENT ON TABLE cat_categorias_sugeridas IS 'Plantillas de categorías por giro. Dos niveles: familia → subfamilia.';

CREATE TABLE cat_atributos_sugeridos (
    id_sugerido   BIGSERIAL PRIMARY KEY,
    id_giro       BIGINT NOT NULL REFERENCES cat_giros_negocio(id_giro),
    nombre        VARCHAR(40) NOT NULL,
    valores       VARCHAR(500) NOT NULL,   -- valores separados por '|' (ej. 'CH|M|G|XL')
    UNIQUE (id_giro, nombre)
);
COMMENT ON TABLE cat_atributos_sugeridos IS 'Atributos típicos por giro (Color, Talla, Presentación...) con sus valores, para no empezar de cero.';

-- ------------------------------------------------------------
-- 3. Categorías del negocio (dos niveles)
-- ------------------------------------------------------------
CREATE TABLE categorias (
    id_categoria   BIGSERIAL PRIMARY KEY,
    uuid_categoria UUID NOT NULL DEFAULT gen_random_uuid(),
    id_negocio     BIGINT NOT NULL REFERENCES negocios(id_negocio),
    id_padre       BIGINT,
    nombre         VARCHAR(80) NOT NULL,
    descripcion    VARCHAR(200),
    orden          SMALLINT NOT NULL DEFAULT 0,
    estatus        VARCHAR(20) NOT NULL DEFAULT 'activo' CHECK (estatus IN ('activo','inactivo')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_categorias_id_negocio UNIQUE (id_categoria, id_negocio),
    -- el padre debe ser del MISMO negocio
    CONSTRAINT fk_categorias_padre FOREIGN KEY (id_padre, id_negocio) REFERENCES categorias (id_categoria, id_negocio)
);
CREATE UNIQUE INDEX ux_categorias_uuid   ON categorias(uuid_categoria);
-- mismo nombre no se repite dentro del mismo nivel y negocio (sin distinguir mayúsculas)
CREATE UNIQUE INDEX ux_categorias_nombre ON categorias (id_negocio, COALESCE(id_padre, 0), lower(nombre));
CREATE INDEX ix_categorias_padre ON categorias(id_padre);
COMMENT ON TABLE categorias IS 'Clasificación de productos del negocio: familia (sin padre) y subfamilia (con padre). Máximo dos niveles.';

-- Máximo dos niveles: el padre no puede tener padre, y una categoría con hijas no puede volverse hija
CREATE FUNCTION fn_categorias_validar() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.id_padre IS NOT NULL THEN
        IF NEW.id_padre = NEW.id_categoria THEN
            RAISE EXCEPTION 'Una categoría no puede ser su propio padre';
        END IF;
        IF EXISTS (SELECT 1 FROM categorias WHERE id_categoria = NEW.id_padre AND id_padre IS NOT NULL) THEN
            RAISE EXCEPTION 'Las categorías solo tienen dos niveles: familia y subfamilia';
        END IF;
        IF TG_OP = 'UPDATE' AND EXISTS (SELECT 1 FROM categorias WHERE id_padre = NEW.id_categoria) THEN
            RAISE EXCEPTION 'La categoría tiene subcategorías, no puede quedar dentro de otra';
        END IF;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tg_categorias_validar BEFORE INSERT OR UPDATE ON categorias
    FOR EACH ROW EXECUTE FUNCTION fn_categorias_validar();

-- ------------------------------------------------------------
-- 4. Atributos de variante (definidos por cada negocio)
-- ------------------------------------------------------------
CREATE TABLE atributos (
    id_atributo    BIGSERIAL PRIMARY KEY,
    uuid_atributo  UUID NOT NULL DEFAULT gen_random_uuid(),
    id_negocio     BIGINT NOT NULL REFERENCES negocios(id_negocio),
    nombre         VARCHAR(40) NOT NULL,       -- 'Color', 'Talla', 'Presentación'
    estatus        VARCHAR(20) NOT NULL DEFAULT 'activo' CHECK (estatus IN ('activo','inactivo')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_atributos_id_negocio UNIQUE (id_atributo, id_negocio)
);
CREATE UNIQUE INDEX ux_atributos_uuid   ON atributos(uuid_atributo);
CREATE UNIQUE INDEX ux_atributos_nombre ON atributos(id_negocio, lower(nombre));
COMMENT ON TABLE atributos IS 'Cada negocio define los suyos: una barbería usa "Presentación", una mueblería "Material". El sistema no impone ninguno.';

CREATE TABLE atributo_valores (
    id_valor       BIGSERIAL PRIMARY KEY,
    id_atributo    BIGINT NOT NULL REFERENCES atributos(id_atributo) ON DELETE CASCADE,
    valor          VARCHAR(40) NOT NULL,       -- 'Rojo', 'CH', '500 ml'
    orden          SMALLINT NOT NULL DEFAULT 0,
    estatus        VARCHAR(20) NOT NULL DEFAULT 'activo' CHECK (estatus IN ('activo','inactivo')),
    CONSTRAINT uq_atributo_valores_atributo UNIQUE (id_valor, id_atributo)
);
CREATE UNIQUE INDEX ux_atributo_valores_valor ON atributo_valores(id_atributo, lower(valor));

-- ------------------------------------------------------------
-- 5. Proveedores
-- ------------------------------------------------------------
CREATE TABLE proveedores (
    id_proveedor      BIGSERIAL PRIMARY KEY,
    uuid_proveedor    UUID NOT NULL DEFAULT gen_random_uuid(),
    id_negocio        BIGINT NOT NULL REFERENCES negocios(id_negocio),
    nombre            VARCHAR(100) NOT NULL,
    razon_social      VARCHAR(150),
    rfc               VARCHAR(13),
    resumen_surtido   VARCHAR(200),            -- qué vende, de un vistazo: 'papelería, vinil, sublimación'
    -- Facturación
    factura           BOOLEAN NOT NULL DEFAULT false,         -- ¿puede facturar?
    precios_incluyen_iva BOOLEAN NOT NULL DEFAULT true,       -- true = sus precios ya traen IVA; false = se suma al facturar
    -- Envíos
    hace_envios       BOOLEAN NOT NULL DEFAULT false,
    costo_envio       NUMERIC(10,2) CHECK (costo_envio IS NULL OR costo_envio >= 0),
    envio_gratis_desde NUMERIC(10,2) CHECK (envio_gratis_desde IS NULL OR envio_gratis_desde >= 0),
    dias_entrega      SMALLINT CHECK (dias_entrega IS NULL OR dias_entrega >= 0),
    -- Condiciones
    pedido_minimo     NUMERIC(10,2) CHECK (pedido_minimo IS NULL OR pedido_minimo >= 0),
    dias_credito      SMALLINT NOT NULL DEFAULT 0 CHECK (dias_credito >= 0),
    formas_pago       VARCHAR(100),
    calificacion      SMALLINT CHECK (calificacion IS NULL OR calificacion BETWEEN 1 AND 5),   -- apreciación interna
    -- Ubicación
    direccion         VARCHAR(200),
    id_municipio      BIGINT REFERENCES cat_municipios(id_municipio),
    sitio_web         VARCHAR(150),
    notas             VARCHAR(500),
    estatus           VARCHAR(20) NOT NULL DEFAULT 'activo' CHECK (estatus IN ('activo','inactivo')),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_proveedores_id_negocio UNIQUE (id_proveedor, id_negocio),
    CONSTRAINT ck_proveedores_rfc CHECK (rfc IS NULL OR rfc ~ '^[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3}$')
);
CREATE UNIQUE INDEX ux_proveedores_uuid   ON proveedores(uuid_proveedor);
CREATE UNIQUE INDEX ux_proveedores_nombre ON proveedores(id_negocio, lower(nombre));
CREATE INDEX ix_proveedores_municipio ON proveedores(id_municipio);
COMMENT ON TABLE proveedores IS 'Proveedores del negocio. Solo el admin los ve: el gerente nunca accede a esta información.';

CREATE TABLE proveedor_contactos (
    id_contacto    BIGSERIAL PRIMARY KEY,
    id_proveedor   BIGINT NOT NULL REFERENCES proveedores(id_proveedor) ON DELETE CASCADE,
    nombre         VARCHAR(100) NOT NULL,
    puesto         VARCHAR(60),               -- 'Vendedor', 'Cobranza'
    telefono       VARCHAR(15),
    whatsapp       VARCHAR(15),
    correo         VARCHAR(150),
    es_principal   BOOLEAN NOT NULL DEFAULT false,
    notas          VARCHAR(200),
    CONSTRAINT ck_proveedor_contactos_medio CHECK (telefono IS NOT NULL OR whatsapp IS NOT NULL OR correo IS NOT NULL)
);
CREATE INDEX ix_proveedor_contactos_proveedor ON proveedor_contactos(id_proveedor);
-- un solo contacto principal por proveedor
CREATE UNIQUE INDEX ux_proveedor_contactos_principal ON proveedor_contactos(id_proveedor) WHERE es_principal;

CREATE TABLE proveedor_descuentos (
    id_descuento   BIGSERIAL PRIMARY KEY,
    id_proveedor   BIGINT NOT NULL REFERENCES proveedores(id_proveedor) ON DELETE CASCADE,
    monto_minimo_mensual NUMERIC(12,2) NOT NULL CHECK (monto_minimo_mensual > 0),   -- cuánto hay que comprar al mes
    porcentaje     NUMERIC(5,2) NOT NULL CHECK (porcentaje > 0 AND porcentaje <= 100),
    descripcion    VARCHAR(150),
    UNIQUE (id_proveedor, monto_minimo_mensual)
);
COMMENT ON TABLE proveedor_descuentos IS 'Escalones de descuento por volumen: "comprando $10,000 al mes, 5%".';

-- ------------------------------------------------------------
-- 6. Productos y variantes
-- ------------------------------------------------------------
CREATE TABLE productos (
    id_producto    BIGSERIAL PRIMARY KEY,
    uuid_producto  UUID NOT NULL DEFAULT gen_random_uuid(),
    id_negocio     BIGINT NOT NULL REFERENCES negocios(id_negocio),
    id_categoria   BIGINT NOT NULL,
    nombre         VARCHAR(120) NOT NULL,
    marca          VARCHAR(60),
    modelo         VARCHAR(60),
    descripcion    VARCHAR(500),
    id_unidad      BIGINT NOT NULL REFERENCES cat_unidades_medida(id_unidad),
    tasa_iva       SMALLINT NOT NULL DEFAULT 16 CHECK (tasa_iva IN (0, 8, 16)),
    es_insumo      BOOLEAN NOT NULL DEFAULT false,   -- se usa para dar un servicio (tijeras, gel): tiene stock y costo pero no se vende
    estatus        VARCHAR(20) NOT NULL DEFAULT 'activo' CHECK (estatus IN ('activo','inactivo')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_productos_id_negocio UNIQUE (id_producto, id_negocio),
    -- la categoría debe ser del MISMO negocio
    CONSTRAINT fk_productos_categoria FOREIGN KEY (id_categoria, id_negocio) REFERENCES categorias (id_categoria, id_negocio)
);
CREATE UNIQUE INDEX ux_productos_uuid ON productos(uuid_producto);
CREATE INDEX ix_productos_negocio_categoria ON productos(id_negocio, id_categoria);
CREATE INDEX ix_productos_nombre ON productos(id_negocio, lower(nombre));
COMMENT ON TABLE productos IS 'Ficha general del producto. Lo que se vende y se cuenta son sus variantes.';

-- Qué atributos aplican a cada producto (los valores concretos van en cada variante)
CREATE TABLE producto_atributos (
    id_producto    BIGINT NOT NULL,
    id_atributo    BIGINT NOT NULL,
    id_negocio     BIGINT NOT NULL,
    orden          SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id_producto, id_atributo),
    FOREIGN KEY (id_producto, id_negocio) REFERENCES productos (id_producto, id_negocio) ON DELETE CASCADE,
    FOREIGN KEY (id_atributo, id_negocio) REFERENCES atributos (id_atributo, id_negocio)
);

CREATE TABLE variantes (
    id_variante     BIGSERIAL PRIMARY KEY,
    uuid_variante   UUID NOT NULL DEFAULT gen_random_uuid(),
    id_producto     BIGINT NOT NULL,
    id_negocio      BIGINT NOT NULL,
    sku             VARCHAR(40),
    codigo_barras   VARCHAR(30),
    -- Combinación de valores en forma canónica ('3:12|5:40' = atributo:valor ordenado por atributo).
    -- '' = variante única de un producto simple. Evita variantes duplicadas dentro del mismo producto.
    firma           VARCHAR(200) NOT NULL DEFAULT '',
    precio_publico  NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (precio_publico >= 0),
    stock_actual    NUMERIC(12,3) NOT NULL DEFAULT 0 CHECK (stock_actual >= 0),   -- lo mueve SOLO el trigger de movimientos
    stock_minimo    NUMERIC(12,3) NOT NULL DEFAULT 0 CHECK (stock_minimo >= 0),
    estatus         VARCHAR(20) NOT NULL DEFAULT 'activo' CHECK (estatus IN ('activo','inactivo')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_variantes_id_negocio UNIQUE (id_variante, id_negocio),
    CONSTRAINT uq_variantes_id_producto UNIQUE (id_variante, id_producto),
    CONSTRAINT uq_variantes_firma UNIQUE (id_producto, firma),
    FOREIGN KEY (id_producto, id_negocio) REFERENCES productos (id_producto, id_negocio)
);
CREATE UNIQUE INDEX ux_variantes_uuid ON variantes(uuid_variante);
CREATE UNIQUE INDEX ux_variantes_sku  ON variantes(id_negocio, lower(sku)) WHERE sku IS NOT NULL;
CREATE UNIQUE INDEX ux_variantes_barras ON variantes(id_negocio, codigo_barras) WHERE codigo_barras IS NOT NULL;
CREATE INDEX ix_variantes_producto ON variantes(id_producto);
COMMENT ON TABLE variantes IS
  'Unidad real de venta e inventario. precio_publico es el precio de la variante (la taza blanca $22 y la de color $30). stock 0 = no se puede vender.';

CREATE TABLE variante_valores (
    id_variante    BIGINT NOT NULL,
    id_producto    BIGINT NOT NULL,
    id_atributo    BIGINT NOT NULL,
    id_valor       BIGINT NOT NULL,
    PRIMARY KEY (id_variante, id_atributo),
    FOREIGN KEY (id_variante, id_producto) REFERENCES variantes (id_variante, id_producto) ON DELETE CASCADE,
    -- el atributo debe ser uno de los que aplican al producto
    FOREIGN KEY (id_producto, id_atributo) REFERENCES producto_atributos (id_producto, id_atributo),
    -- y el valor debe pertenecer a ese atributo
    FOREIGN KEY (id_valor, id_atributo) REFERENCES atributo_valores (id_valor, id_atributo)
);

-- Costo por proveedor de cada variante (privado del admin)
CREATE TABLE variante_proveedores (
    id_variante_proveedor BIGSERIAL PRIMARY KEY,
    id_variante    BIGINT NOT NULL,
    id_proveedor   BIGINT NOT NULL,
    id_negocio     BIGINT NOT NULL,
    costo          NUMERIC(12,2) NOT NULL CHECK (costo >= 0),   -- tal como lo cotiza el proveedor (ver proveedores.precios_incluyen_iva)
    codigo_proveedor VARCHAR(40),                                -- clave que el proveedor usa para este artículo
    es_preferido   BOOLEAN NOT NULL DEFAULT false,
    notas          VARCHAR(200),                                 -- ej. calidad: 'mejor para rotular'
    ultima_compra  DATE,
    estatus        VARCHAR(20) NOT NULL DEFAULT 'activo' CHECK (estatus IN ('activo','inactivo')),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (id_variante, id_proveedor),
    FOREIGN KEY (id_variante, id_negocio)  REFERENCES variantes (id_variante, id_negocio) ON DELETE CASCADE,
    FOREIGN KEY (id_proveedor, id_negocio) REFERENCES proveedores (id_proveedor, id_negocio)
);
-- un solo proveedor preferido por variante
CREATE UNIQUE INDEX ux_variante_proveedores_preferido ON variante_proveedores(id_variante) WHERE es_preferido;
CREATE INDEX ix_variante_proveedores_proveedor ON variante_proveedores(id_proveedor);
COMMENT ON TABLE variante_proveedores IS 'Quién vende cada variante y a qué costo. Permite comparar proveedores. El gerente nunca recibe estos datos.';

-- ------------------------------------------------------------
-- 7. Solicitudes de compra (resurtido)
-- ------------------------------------------------------------
CREATE TABLE solicitudes_compra (
    id_solicitud    BIGSERIAL PRIMARY KEY,
    uuid_solicitud  UUID NOT NULL DEFAULT gen_random_uuid(),
    id_negocio      BIGINT NOT NULL REFERENCES negocios(id_negocio),
    id_proveedor    BIGINT NOT NULL,
    estatus         VARCHAR(20) NOT NULL DEFAULT 'pendiente'
                    CHECK (estatus IN ('pendiente','parcial','recibida','cancelada')),
    notas           VARCHAR(300),
    id_usuario_crea BIGINT NOT NULL REFERENCES usuarios(id_usuario),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_solicitudes_id_negocio UNIQUE (id_solicitud, id_negocio),
    FOREIGN KEY (id_proveedor, id_negocio) REFERENCES proveedores (id_proveedor, id_negocio)
);
CREATE UNIQUE INDEX ux_solicitudes_uuid ON solicitudes_compra(uuid_solicitud);
CREATE INDEX ix_solicitudes_negocio_estatus ON solicitudes_compra(id_negocio, estatus);
COMMENT ON TABLE solicitudes_compra IS
  'Lo que el admin pide a un proveedor. El gerente confirma la recepción capturando solo cantidades: no ve proveedor ni costos.';

CREATE TABLE solicitud_compra_detalles (
    id_detalle      BIGSERIAL PRIMARY KEY,
    id_solicitud    BIGINT NOT NULL,
    id_variante     BIGINT NOT NULL,
    id_negocio      BIGINT NOT NULL,
    cantidad_solicitada NUMERIC(12,3) NOT NULL CHECK (cantidad_solicitada > 0),
    cantidad_recibida   NUMERIC(12,3) NOT NULL DEFAULT 0 CHECK (cantidad_recibida >= 0),
    costo_unitario  NUMERIC(12,2) CHECK (costo_unitario IS NULL OR costo_unitario >= 0),   -- lo captura el admin
    UNIQUE (id_solicitud, id_variante),
    CONSTRAINT uq_solicitud_detalles_id_negocio UNIQUE (id_detalle, id_negocio),
    FOREIGN KEY (id_solicitud, id_negocio) REFERENCES solicitudes_compra (id_solicitud, id_negocio) ON DELETE CASCADE,
    FOREIGN KEY (id_variante, id_negocio)  REFERENCES variantes (id_variante, id_negocio)
);

-- ------------------------------------------------------------
-- 8. Movimientos de inventario (única forma de cambiar el stock)
-- ------------------------------------------------------------
CREATE TABLE movimientos_inventario (
    id_movimiento   BIGSERIAL PRIMARY KEY,
    uuid_movimiento UUID NOT NULL DEFAULT gen_random_uuid(),
    id_negocio      BIGINT NOT NULL,
    id_variante     BIGINT NOT NULL,
    tipo            VARCHAR(10) NOT NULL CHECK (tipo IN ('entrada','salida','ajuste')),
    motivo          VARCHAR(25) NOT NULL
                    CHECK (motivo IN ('compra','recepcion_solicitud','inventario_inicial','venta','consumo','merma','correccion','devolucion','otro')),
    cantidad        NUMERIC(12,3) NOT NULL,           -- con signo: entrada > 0, salida < 0, ajuste cualquiera distinto de 0
    stock_anterior  NUMERIC(12,3) NOT NULL DEFAULT 0, -- lo llena el trigger
    stock_resultante NUMERIC(12,3) NOT NULL DEFAULT 0,-- lo llena el trigger
    id_usuario      BIGINT NOT NULL REFERENCES usuarios(id_usuario),   -- quién lo hizo
    id_proveedor    BIGINT,
    id_detalle_solicitud BIGINT,
    costo_unitario  NUMERIC(12,2) CHECK (costo_unitario IS NULL OR costo_unitario >= 0),   -- privado del admin; puede completarse después
    notas           VARCHAR(300),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_movimientos_signo CHECK (
        (tipo = 'entrada' AND cantidad > 0) OR (tipo = 'salida' AND cantidad < 0) OR (tipo = 'ajuste' AND cantidad <> 0)),
    -- un ajuste siempre exige explicar por qué
    CONSTRAINT ck_movimientos_ajuste_notas CHECK (tipo <> 'ajuste' OR (notas IS NOT NULL AND length(trim(notas)) > 0)),
    FOREIGN KEY (id_variante, id_negocio)  REFERENCES variantes (id_variante, id_negocio),
    FOREIGN KEY (id_proveedor, id_negocio) REFERENCES proveedores (id_proveedor, id_negocio),
    FOREIGN KEY (id_detalle_solicitud, id_negocio) REFERENCES solicitud_compra_detalles (id_detalle, id_negocio)
);
CREATE UNIQUE INDEX ux_movimientos_uuid ON movimientos_inventario(uuid_movimiento);
CREATE INDEX ix_movimientos_variante_fecha ON movimientos_inventario(id_variante, created_at DESC);
CREATE INDEX ix_movimientos_negocio_fecha  ON movimientos_inventario(id_negocio, created_at DESC);
COMMENT ON TABLE movimientos_inventario IS
  'Bitácora inmutable: quién, cuándo, cuánto y por qué cambió el stock. variantes.stock_actual siempre es el resultado de estos movimientos.';

-- Aplica el movimiento al stock de la variante (con bloqueo de fila) y registra stock anterior/resultante.
-- Rechaza dejar el stock en negativo.
CREATE FUNCTION fn_movimientos_aplicar() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    v_stock NUMERIC(12,3);
BEGIN
    SELECT stock_actual INTO v_stock FROM variantes WHERE id_variante = NEW.id_variante FOR UPDATE;

    NEW.stock_anterior   := v_stock;
    NEW.stock_resultante := v_stock + NEW.cantidad;

    IF NEW.stock_resultante < 0 THEN
        RAISE EXCEPTION 'Stock insuficiente: hay % y se intenta restar %', v_stock, abs(NEW.cantidad);
    END IF;

    UPDATE variantes SET stock_actual = NEW.stock_resultante, updated_at = now()
    WHERE id_variante = NEW.id_variante;

    -- Si viene de una solicitud de compra, se acumula lo recibido y se actualiza su estatus
    IF NEW.id_detalle_solicitud IS NOT NULL THEN
        UPDATE solicitud_compra_detalles
        SET cantidad_recibida = cantidad_recibida + NEW.cantidad
        WHERE id_detalle = NEW.id_detalle_solicitud;

        UPDATE solicitudes_compra s
        SET estatus = CASE
                WHEN NOT EXISTS (SELECT 1 FROM solicitud_compra_detalles d
                                 WHERE d.id_solicitud = s.id_solicitud AND d.cantidad_recibida < d.cantidad_solicitada) THEN 'recibida'
                ELSE 'parcial' END,
            updated_at = now()
        WHERE s.id_solicitud = (SELECT id_solicitud FROM solicitud_compra_detalles WHERE id_detalle = NEW.id_detalle_solicitud)
          AND s.estatus IN ('pendiente','parcial');
    END IF;

    RETURN NEW;
END $$;
CREATE TRIGGER tg_movimientos_aplicar BEFORE INSERT ON movimientos_inventario
    FOR EACH ROW EXECUTE FUNCTION fn_movimientos_aplicar();

-- Los movimientos no se editan ni se borran. Única excepción: el admin puede capturar el costo
-- (y el proveedor) de una entrada que quedó pendiente, una sola vez.
CREATE FUNCTION fn_movimientos_proteger() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Los movimientos de inventario no se pueden eliminar';
    END IF;

    IF OLD.costo_unitario IS NULL AND NEW.costo_unitario IS NOT NULL
       AND (to_jsonb(NEW) - 'costo_unitario' - 'id_proveedor') = (to_jsonb(OLD) - 'costo_unitario' - 'id_proveedor') THEN
        RETURN NEW;
    END IF;

    RAISE EXCEPTION 'Los movimientos de inventario no se pueden modificar; registra un ajuste';
END $$;
CREATE TRIGGER tg_movimientos_proteger BEFORE UPDATE OR DELETE ON movimientos_inventario
    FOR EACH ROW EXECUTE FUNCTION fn_movimientos_proteger();

-- El stock de una variante solo puede cambiar desde el trigger de movimientos: si alguien lo edita a mano
-- (o un UPDATE de la aplicación lo sobrescribe) la BD lo rechaza.
CREATE FUNCTION fn_variantes_stock_proteger() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.stock_actual IS DISTINCT FROM OLD.stock_actual AND pg_trigger_depth() < 2 THEN
        RAISE EXCEPTION 'El stock no se edita directamente: registra un movimiento de inventario';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tg_variantes_stock_proteger BEFORE UPDATE ON variantes
    FOR EACH ROW EXECUTE FUNCTION fn_variantes_stock_proteger();

-- ------------------------------------------------------------
-- 9. Seeds: archivos, módulos y sugerencias por giro
-- ------------------------------------------------------------
INSERT INTO cat_tipos_entidad_archivo (clave, nombre) VALUES
  ('producto', 'Productos'),
  ('variante', 'Variantes de producto');

-- Resurtido: solicitudes de compra y recepción de mercancía (el gerente lo usa; admin siempre)
INSERT INTO cat_modulos (clave, nombre, descripcion, icono, ruta, orden) VALUES
  ('resurtido', 'Resurtido', 'Solicitudes de compra y recepción de mercancía', 'boxes-packing', '/resurtido', 19);

-- El gerente consulta productos (sin costos ni proveedores) y recibe mercancía
INSERT INTO rol_modulos (tipo_usuario, nivel_acceso, id_modulo)
SELECT 'trabajador', 'gerente', id_modulo FROM cat_modulos WHERE clave IN ('productos', 'resurtido');

-- Categorías sugeridas (familia y subfamilia) para algunos giros
INSERT INTO cat_categorias_sugeridas (id_giro, nombre, orden)
SELECT g.id_giro, v.nombre, v.orden
FROM cat_giros_negocio g
JOIN (VALUES
  ('Distribuidor de Materiales', 'Vinil', 1), ('Distribuidor de Materiales', 'Sublimación', 2),
  ('Distribuidor de Materiales', 'Playeras', 3), ('Distribuidor de Materiales', 'Promocionales', 4),
  ('Imprentas', 'Papel', 1), ('Imprentas', 'Tintas y consumibles', 2), ('Imprentas', 'Vinil', 3),
  ('Tiendas de ropa', 'Playeras', 1), ('Tiendas de ropa', 'Pantalones', 2), ('Tiendas de ropa', 'Accesorios', 3),
  ('Abarrotes / minisuper', 'Bebidas', 1), ('Abarrotes / minisuper', 'Botanas', 2), ('Abarrotes / minisuper', 'Lácteos', 3),
  ('Barberías / estéticas', 'Insumos de corte', 1), ('Barberías / estéticas', 'Productos de cuidado', 2),
  ('Papelerías', 'Cuadernos', 1), ('Papelerías', 'Escritura', 2), ('Papelerías', 'Oficina', 3)
) AS v(giro, nombre, orden) ON v.giro = g.nombre;

INSERT INTO cat_categorias_sugeridas (id_giro, nombre, id_padre, orden)
SELECT p.id_giro, v.nombre, p.id_sugerida, v.orden
FROM cat_categorias_sugeridas p
JOIN cat_giros_negocio g ON g.id_giro = p.id_giro
JOIN (VALUES
  ('Distribuidor de Materiales', 'Vinil', 'Rotulación', 1), ('Distribuidor de Materiales', 'Vinil', 'Textil', 2),
  ('Distribuidor de Materiales', 'Sublimación', 'Tazas', 1), ('Distribuidor de Materiales', 'Sublimación', 'Llaveros', 2),
  ('Abarrotes / minisuper', 'Bebidas', 'Refrescos', 1), ('Abarrotes / minisuper', 'Bebidas', 'Aguas y electrolitos', 2)
) AS v(giro, padre, nombre, orden) ON v.giro = g.nombre AND v.padre = p.nombre AND p.id_padre IS NULL;

-- Atributos sugeridos
INSERT INTO cat_atributos_sugeridos (id_giro, nombre, valores)
SELECT g.id_giro, v.nombre, v.valores
FROM cat_giros_negocio g
JOIN (VALUES
  ('Distribuidor de Materiales', 'Color',  'Blanco|Negro|Rojo|Azul|Verde|Amarillo'),
  ('Distribuidor de Materiales', 'Talla',  'CH|M|G|XL|XXL'),
  ('Tiendas de ropa', 'Color',   'Blanco|Negro|Azul|Rojo|Verde'),
  ('Tiendas de ropa', 'Talla',   'CH|M|G|XL|XXL'),
  ('Tiendas de ropa', 'Género',  'Caballero|Dama|Niño|Unisex'),
  ('Abarrotes / minisuper', 'Presentación', 'Lata|600 ml|1 L|1.5 L|2.5 L'),
  ('Barberías / estéticas', 'Presentación', '100 ml|250 ml|500 ml|1 L'),
  ('Papelerías', 'Color', 'Negro|Azul|Rojo|Verde')
) AS v(giro, nombre, valores) ON v.giro = g.nombre;

COMMIT;


-- ============================================================
-- MIGRACIÓN: HISTORIAL DE PRECIOS (venta y costo)
-- ============================================================
-- El precio vigente vive en variantes.precio_publico y variante_proveedores.costo.
-- Cada cambio queda registrado automáticamente por trigger (no depende de que el código se acuerde).
-- Las ventas/servicios futuros guardarán además el precio y el costo con que se vendieron (copia),
-- de modo que ningún cambio posterior altera el pasado.
BEGIN;

CREATE TABLE variante_precios_historial (
    id_historial   BIGSERIAL PRIMARY KEY,
    id_variante    BIGINT NOT NULL,
    id_negocio     BIGINT NOT NULL,
    precio_anterior NUMERIC(12,2),             -- NULL en el alta de la variante
    precio_nuevo   NUMERIC(12,2) NOT NULL,
    id_usuario     BIGINT REFERENCES usuarios(id_usuario),   -- quién lo cambió (si la app lo informa)
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    FOREIGN KEY (id_variante, id_negocio) REFERENCES variantes (id_variante, id_negocio)
);
CREATE INDEX ix_precios_hist_variante ON variante_precios_historial(id_variante, created_at DESC);

CREATE TABLE variante_costos_historial (
    id_historial   BIGSERIAL PRIMARY KEY,
    id_variante    BIGINT NOT NULL,
    id_proveedor   BIGINT NOT NULL,
    id_negocio     BIGINT NOT NULL,
    costo_anterior NUMERIC(12,2),
    costo_nuevo    NUMERIC(12,2) NOT NULL,
    id_usuario     BIGINT REFERENCES usuarios(id_usuario),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    FOREIGN KEY (id_variante, id_negocio)  REFERENCES variantes (id_variante, id_negocio),
    FOREIGN KEY (id_proveedor, id_negocio) REFERENCES proveedores (id_proveedor, id_negocio)
);
CREATE INDEX ix_costos_hist_variante ON variante_costos_historial(id_variante, created_at DESC);
COMMENT ON TABLE variante_costos_historial IS 'Privado del admin: el gerente nunca recibe costos.';

-- Usuario opcional: la aplicación puede ejecutar SELECT set_config('app.id_usuario', '<id>', true) en la transacción.
CREATE FUNCTION fn_usuario_app() RETURNS BIGINT LANGUAGE sql STABLE AS $$
    SELECT NULLIF(current_setting('app.id_usuario', true), '')::bigint
$$;

CREATE FUNCTION fn_variantes_precio_historial() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' OR NEW.precio_publico IS DISTINCT FROM OLD.precio_publico THEN
        INSERT INTO variante_precios_historial (id_variante, id_negocio, precio_anterior, precio_nuevo, id_usuario)
        VALUES (NEW.id_variante, NEW.id_negocio,
                CASE WHEN TG_OP = 'UPDATE' THEN OLD.precio_publico END, NEW.precio_publico, fn_usuario_app());
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER tg_variantes_precio_historial AFTER INSERT OR UPDATE OF precio_publico ON variantes
    FOR EACH ROW EXECUTE FUNCTION fn_variantes_precio_historial();

CREATE FUNCTION fn_variante_proveedores_costo_historial() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' OR NEW.costo IS DISTINCT FROM OLD.costo THEN
        INSERT INTO variante_costos_historial (id_variante, id_proveedor, id_negocio, costo_anterior, costo_nuevo, id_usuario)
        VALUES (NEW.id_variante, NEW.id_proveedor, NEW.id_negocio,
                CASE WHEN TG_OP = 'UPDATE' THEN OLD.costo END, NEW.costo, fn_usuario_app());
        NEW.updated_at := now();
    END IF;
    RETURN NEW;
END $$;
-- BEFORE para poder tocar updated_at; el historial referencia filas ya existentes (variante y proveedor), no la propia
CREATE TRIGGER tg_variante_proveedores_costo_historial BEFORE INSERT OR UPDATE OF costo ON variante_proveedores
    FOR EACH ROW EXECUTE FUNCTION fn_variante_proveedores_costo_historial();

-- Variantes ya existentes: se registra su precio actual como punto de partida
INSERT INTO variante_precios_historial (id_variante, id_negocio, precio_nuevo)
SELECT id_variante, id_negocio, precio_publico FROM variantes;
INSERT INTO variante_costos_historial (id_variante, id_proveedor, id_negocio, costo_nuevo)
SELECT id_variante, id_proveedor, id_negocio, costo FROM variante_proveedores;

COMMIT;