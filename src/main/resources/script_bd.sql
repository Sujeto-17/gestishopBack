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
  'Catálogo de planes de suscripción que el superadmin ofrece. sistema_type define el flujo operativo: "servicio" (trabajo con proceso/seguimiento, ej. imprenta, barbería) o "tienda" (venta directa, ej. farmacia, papelería). No es el giro del negocio, es el tipo de flujo técnico.';

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
    rol             VARCHAR(20)  NOT NULL,                  -- texto libre: qué hace en ese negocio
    nivel_acceso    VARCHAR(20)  NOT NULL DEFAULT 'estandar'
                    CHECK (nivel_acceso IN ('gerente','estandar')),   -- controla qué módulos ve
    turno           VARCHAR(20)  NOT NULL CHECK (turno IN ('mañana','tarde','completo')),
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

COMMENT ON TABLE trabajadores IS
  'rol = qué hace (texto libre). nivel_acceso = qué módulos ve. regimen = con o sin nómina formal. Son conceptos independientes.';

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
  ('configuracion', 'Configuración',  'Ajustes generales del negocio',                                       'sliders',          '/configuracion', 11),
  ('justificantes', 'Justificantes',  'Solicitud y autorización de justificantes de falta',                  'file-signature',   '/justificantes', 12),
  ('vacaciones', 'Vacaciones',     'Solicitud y control de días de vacaciones',                           'umbrella-beach',   '/vacaciones',    13),
  ('solicitudes', 'Solicitudes',    'Permisos especiales: home office, licencia médica, etc.',             'inbox',            '/solicitudes',   14),
  ('citas', 'Citas',          'Agenda de citas con clientes (negocios tipo servicio)',               'calendar-check',   '/citas',         15),
  ('reportes', 'Reportes',       'Reportes y estadísticas del negocio',                                 'chart-line',       '/reportes',      16),
  ('perfil', 'Perfil',         'Datos personales y configuración de la propia cuenta',                'user',             '/perfil',        17);

-- Giros de negocio de ejemplo
INSERT INTO cat_giros_negocio (nombre, icono, orden) VALUES
  ('Restaurantes / comida',   'utensils',        1),
  ('Tiendas de ropa',         'shirt',           2),
  ('Imprentas',               'print',           3),
  ('Tiendas de tecnología',   'laptop',          4),
  ('Abarrotes / minisuper',   'basket-shopping', 5),
  ('Barberías / estéticas',   'scissors',        6),
  ('Farmacias',               'pills',           7),
  ('Papelerías',              'pencil',          8),
  ('Negocios de servicios',   'briefcase',       9),
  ('Otro comercio',           'store',          99);

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