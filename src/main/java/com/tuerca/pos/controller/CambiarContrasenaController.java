package com.tuerca.pos.controller;

import com.tuerca.pos.dao.EmpleadoDAO;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.view.CambiarContrasenaPanel;
import java.util.Arrays;
import javax.swing.JOptionPane;

/**
 * Controla la pantalla forzada de "Cambiar Contraseña" (Paso 18): se muestra
 * cuando {@link com.tuerca.pos.model.Empleado#isMustChangePassword()} viene
 * en true tras el login (un Administrador restableció la contraseña desde
 * Editar Empleado). Al completarse, delega en
 * {@link LoginController#continuarFlujoPostLogin()} para seguir exactamente
 * el mismo flujo que un login normal (caja abierta → dashboard, si no →
 * apertura de caja).
 */
public class CambiarContrasenaController {

    private final CambiarContrasenaPanel vista;
    private final LoginController loginController;
    private final EmpleadoDAO dao;

    public CambiarContrasenaController(CambiarContrasenaPanel vista, LoginController loginController) {
        this.vista = vista;
        this.loginController = loginController;
        this.dao = new EmpleadoDAO();

        vista.getBtnCambiar().addActionListener(e -> cambiarContrasena());
    }

    private void cambiarContrasena() {
        vista.getBtnCambiar().setEnabled(false);

        char[] nueva = vista.getNuevaContrasena();
        char[] confirmar = vista.getConfirmarContrasena();

        if (nueva.length < 8) {
            JOptionPane.showMessageDialog(vista, "La contraseña debe tener al menos 8 caracteres.",
                    "Contraseña inválida", JOptionPane.WARNING_MESSAGE);
            Arrays.fill(nueva, ' ');
            Arrays.fill(confirmar, ' ');
            vista.getBtnCambiar().setEnabled(true);
            return;
        }

        if (!Arrays.equals(nueva, confirmar)) {
            JOptionPane.showMessageDialog(vista, "Las contraseñas no coinciden.",
                    "Contraseña inválida", JOptionPane.WARNING_MESSAGE);
            Arrays.fill(nueva, ' ');
            Arrays.fill(confirmar, ' ');
            vista.getBtnCambiar().setEnabled(true);
            return;
        }

        boolean ok = dao.cambiarContrasena(Sesion.getInstancia().getIdUserAccount(), new String(nueva));
        Arrays.fill(nueva, ' ');
        Arrays.fill(confirmar, ' ');

        if (!ok) {
            JOptionPane.showMessageDialog(vista, "No se pudo cambiar la contraseña. Intenta de nuevo.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            vista.getBtnCambiar().setEnabled(true);
            return;
        }

        JOptionPane.showMessageDialog(vista, "Contraseña actualizada con éxito.");
        loginController.continuarFlujoPostLogin();
    }
}
