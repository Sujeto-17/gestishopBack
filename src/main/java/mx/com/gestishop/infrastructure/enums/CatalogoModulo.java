package mx.com.gestishop.infrastructure.enums;

public enum CatalogoModulo {

    ASISTENCIA("Asistencia"),
    NEGOCIOS("Negocios"),
    ADMINS("Admins"),
    SUSCRIPCIONES("Suscripciones"),
    CATEGORIAS("Categorias");

    private final String nombre;

    CatalogoModulo(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
