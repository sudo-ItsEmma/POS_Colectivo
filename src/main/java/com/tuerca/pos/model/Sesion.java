package com.tuerca.pos.model;

/**
 * Sesión del usuario activo durante la ejecución de la aplicación.
 * Singleton: vive mientras la app está abierta, se resetea al cerrar sesión.
 */
public class Sesion {

    private static final Sesion INSTANCIA = new Sesion();

    private boolean autenticado = false;
    private int idUserAccount;
    private int idEmployee;
    private String nombreCompleto;
    private String username;
    private int idRole;
    private String roleName;

    private Sesion() {
    }

    public static Sesion getInstancia() {
        return INSTANCIA;
    }

    public void iniciarSesion(Empleado usuario) {
        this.idUserAccount = usuario.getIdUserAccount();
        this.idEmployee = usuario.getId();
        this.nombreCompleto = (usuario.getNombre() + " " + usuario.getPaterno()).trim();
        this.username = usuario.getUsername();
        this.idRole = usuario.getIdRole();
        this.roleName = usuario.getRoleName();
        this.autenticado = true;
    }

    public void cerrarSesion() {
        this.autenticado = false;
        this.idUserAccount = 0;
        this.idEmployee = 0;
        this.nombreCompleto = null;
        this.username = null;
        this.idRole = 0;
        this.roleName = null;
    }

    public boolean isAutenticado() {
        return autenticado;
    }

    public boolean isAdmin() {
        return "Admin".equalsIgnoreCase(roleName);
    }

    public int getIdUserAccount() {
        return idUserAccount;
    }

    public int getIdEmployee() {
        return idEmployee;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getUsername() {
        return username;
    }

    public int getIdRole() {
        return idRole;
    }

    public String getRoleName() {
        return roleName;
    }
}
