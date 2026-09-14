package mx.com.gestishop.infrastructure.enums;

/**
 * Representa los 3 tipos de usuario que pueden autenticarse en el sistema.
 * <p>
 * A diferencia de CatalogoModulo (que SÍ debe eliminarse por ser dinámico),
 * este enum es válido porque los 3 tipos de usuario son una decisión de
 * ARQUITECTURA que no cambia sin una reestructuración completa del sistema
 * de permisos — no es un catálogo de negocio que el superadmin edite.
 * <p>
 * Se usa para lógica interna (switch en AuthServiceImpl, validaciones,
 * asignación de authorities de Spring Security). El valor se persiste como
 * texto plano en la columna usuarios.tipo_usuario (VARCHAR + CHECK en BD).
 */
public enum TipoUsuario {

    /** Administrador de la plataforma completa. Da de alta negocios y admins. No pertenece a ningún negocio. */
    SUPERADMIN,

    /** Dueño (o co-dueño) de un negocio. Control total sobre su propio negocio. */
    ADMIN,

    /** Empleado de un negocio. Su nivel de acceso (gerente/estandar) determina qué módulos ve. */
    TRABAJADOR;

    /**
     * Convierte el valor de BD (texto en minúsculas) al enum correspondiente.
     * Se usa al leer el campo tipo_usuario desde la entidad Usuario.
     */
    public static TipoUsuario fromValor(String valor) {
        return TipoUsuario.valueOf(valor.toUpperCase());
    }

    /**
     * Devuelve el valor tal como se guarda en base de datos (minúsculas),
     * consistente con el CHECK constraint de la tabla usuarios.
     */
    public String toValor() {
        return this.name().toLowerCase();
    }
}
