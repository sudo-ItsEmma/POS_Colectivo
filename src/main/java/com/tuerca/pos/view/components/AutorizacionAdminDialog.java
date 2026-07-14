package com.tuerca.pos.view.components;

import com.tuerca.pos.dao.EmpleadoDAO;
import com.tuerca.pos.model.Empleado;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.Arrays;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Pide usuario/contraseña de un Administrador (patrón "autorización de
 * gerente"), reutilizado por operaciones que solo un Admin puede autorizar
 * (Devoluciones FN.5, Pago a Emprendedores FN.9) cuando la sesión activa es
 * de Empleado.
 */
public class AutorizacionAdminDialog {

    private AutorizacionAdminDialog() {
    }

    // Devuelve el idUserAccount del Administrador que autorizó, o null si canceló / no era Admin.
    public static Integer solicitar(Component parent, EmpleadoDAO empleadoDao) {
        JTextField txtUsuario = new JTextField(15);
        JPasswordField txtContrasena = new JPasswordField(15);

        JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
        panel.add(new JLabel("Se requiere autorización de un Administrador:"));
        panel.add(txtUsuario);
        panel.add(new JLabel("Contraseña:"));
        panel.add(txtContrasena);

        int resultado = JOptionPane.showConfirmDialog(
                parent, panel, "Autorización requerida",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );
        if (resultado != JOptionPane.OK_OPTION) return null;

        String usuario = txtUsuario.getText().trim();
        char[] contrasena = txtContrasena.getPassword();

        try {
            Empleado admin = empleadoDao.autenticar(usuario, new String(contrasena));
            if (admin == null || !"Admin".equalsIgnoreCase(admin.getRoleName())) {
                JOptionPane.showMessageDialog(parent,
                        "Credenciales inválidas o el usuario no es Administrador.",
                        "Autorización denegada", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            return admin.getIdUserAccount();
        } finally {
            Arrays.fill(contrasena, ' ');
        }
    }
}
