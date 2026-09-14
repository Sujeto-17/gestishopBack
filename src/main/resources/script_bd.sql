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
    sistema_type    VARCHAR(20)  NOT NULL CHECK (sistema_type IN ('servicio','tienda')),
    precio          NUMERIC(10,2) NOT NULL CHECK (precio >= 0),
    descripcion     VARCHAR(160) NOT NULL,
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
    id_plan         BIGINT       NOT NULL REFERENCES planes(id_plan),  -- plan ACTUAL vigente (denormalizado para lectura rápida)
    estatus         VARCHAR(20)  NOT NULL DEFAULT 'prueba'
                    CHECK (estatus IN ('activo','prueba','inactivo')),
    fecha_alta      DATE         NOT NULL DEFAULT CURRENT_DATE,  -- fecha de negocio, se muestra al usuario
    vencimiento     DATE         NOT NULL,                       -- vencimiento del plan/prueba actual
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),         -- metadatos técnicos de auditoría
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ                                  -- soft delete: NULL = activo, fecha = "borrado"
);
COMMENT ON TABLE negocios IS
  'Cada negocio que renta el sistema. id_plan apunta al plan actual vigente; el historial completo de pagos/cambios de plan vive en la tabla suscripciones.';

CREATE UNIQUE INDEX ux_negocios_uuid   ON negocios(uuid_negocio);
-- Índice único PARCIAL: el correo debe ser único solo entre negocios NO borrados,
-- así se puede reutilizar un correo si el negocio original fue dado de baja.
CREATE UNIQUE INDEX ux_negocios_correo ON negocios(correo) WHERE deleted_at IS NULL;
CREATE INDEX ix_negocios_plan  ON negocios(id_plan);
CREATE INDEX ix_negocios_giro  ON negocios(id_giro);


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
    fecha_pago          DATE,                     -- cuándo se registró el pago real (NULL si sigue pendiente)
    metodo_pago         VARCHAR(20)  CHECK (metodo_pago IN ('transferencia','tarjeta','efectivo','otro')),
    estatus             VARCHAR(20)  NOT NULL DEFAULT 'pendiente'
                        CHECK (estatus IN ('pagada','pendiente','vencida','cancelada')),
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
    direccion                   VARCHAR(200),
    fecha_nacimiento            DATE,
    id_genero                   BIGINT REFERENCES cat_generos(id_genero),
    id_estado_civil             BIGINT REFERENCES cat_estados_civiles(id_estado_civil),
    id_municipio                BIGINT REFERENCES cat_municipios(id_municipio),

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
CREATE UNIQUE INDEX ux_usuarios_correo ON usuarios(correo) WHERE deleted_at IS NULL;
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

    -- Datos personales adicionales (específicos de trabajador, no de usuarios en general)
    curp            VARCHAR(18),
    nss             VARCHAR(11),

    -- Puesto: qué hace el trabajador (vocacional, no controla permisos)
    puesto          VARCHAR(100) NOT NULL,
    rol             VARCHAR(20)  NOT NULL
                    CHECK (rol IN ('operador','diseñador','cajero','vendedor','otro')),

    -- nivel_acceso controla qué ve en el sistema, independiente del puesto:
        -- 'gerente'  → Dashboard, Pedidos, Ventas, Clientes, Asistencia, Vacaciones,
        --              Justificantes, Solicitudes, Citas, Perfil.
        --              NUNCA ve Productos, Proveedores, Categorías, Trabajadores,
        --              Configuración, Reportes (protege costos/contactos del dueño).
        -- 'estandar' → Dashboard, Asistencia, Vacaciones, Justificantes,
        --              Solicitudes, Perfil (solo autoservicio personal).
        nivel_acceso    VARCHAR(20)  NOT NULL DEFAULT 'estandar'
                    CHECK (nivel_acceso IN ('gerente','estandar')),

    turno           VARCHAR(20)  NOT NULL CHECK (turno IN ('mañana','tarde','completo')),
    tipo_contrato   VARCHAR(20)  NOT NULL CHECK (tipo_contrato IN ('planta','temporal','honorarios')),
    salario         NUMERIC(10,2) NOT NULL CHECK (salario >= 0),
    fecha_ingreso   DATE         NOT NULL DEFAULT CURRENT_DATE,
    notas           VARCHAR(300)
);
COMMENT ON TABLE trabajadores IS
  'rol = puesto vocacional (qué hace). nivel_acceso = qué módulos puede ver (permisos). Son conceptos independientes.';

CREATE INDEX ix_trabajadores_negocio ON trabajadores(id_negocio);


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
-- SEED (datos iniciales de referencia)
-- ============================================================

-- Módulos base ya existentes en el sistema
INSERT INTO cat_modulos (nombre, descripcion, icono, ruta, orden) VALUES
  ('Dashboard',      'Panel principal con resumen, distinto según el rol que lo consulta', 'chart-pie',        '/dashboard',      1),
  ('Asistencia',     'Registro y consulta de asistencia de trabajadores',                  'user-check',       '/asistencia',     2),
  ('Pedidos',        'Gestión de pedidos con seguimiento de proceso (negocios servicio)',  'clipboard-list',   '/pedidos',        3),
  ('Ventas',         'Venta directa de productos (negocios tienda)',                        'cash-register',    '/ventas',         4),
  ('Clientes',       'Catálogo de clientes frecuentes, factura y mayoristas',               'users',            '/clientes',       5),
  ('Cotizaciones',   'Elaboración de cotizaciones previas a un pedido',                     'file-invoice',     '/cotizaciones',   6),
  ('Productos',      'Catálogo de productos con precios e inventario',                      'box-open',         '/productos',      7),
  ('Proveedores',    'Contactos y condiciones de compra a proveedores',                     'truck',            '/proveedores',    8),
  ('Categorías',     'Clasificación de productos o servicios',                              'tag',              '/categorias',     9),
  ('Trabajadores',   'Alta y gestión del personal del negocio',                             'user-tie',         '/trabajadores',  10),
  ('Configuración',  'Ajustes generales del negocio',                                       'sliders',          '/configuracion', 11),
  ('Justificantes',  'Solicitud y autorización de justificantes de falta',                  'file-signature',   '/justificantes', 12),
  ('Vacaciones',     'Solicitud y control de días de vacaciones',                           'umbrella-beach',   '/vacaciones',    13),
  ('Solicitudes',    'Permisos especiales: home office, licencia médica, etc.',             'inbox',            '/solicitudes',   14),
  ('Citas',          'Agenda de citas con clientes (negocios tipo servicio)',               'calendar-check',   '/citas',         15),
  ('Reportes',       'Reportes y estadísticas del negocio',                                 'chart-line',       '/reportes',      16),
  ('Perfil',         'Datos personales y configuración de la propia cuenta',                'user',             '/perfil',        17);

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
WHERE nombre IN (
  'Dashboard','Asistencia','Pedidos','Ventas','Clientes',
  'Vacaciones','Justificantes','Solicitudes','Citas','Perfil'
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