package com.tuerca.pos.controller;

import com.tuerca.pos.dao.CashSessionDAO;
import com.tuerca.pos.dao.EmpleadoDAO;
import com.tuerca.pos.model.Empleado;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.view.AperturaCajaPanel;
import com.tuerca.pos.view.LoginPanel;
import com.tuerca.pos.view.MainView;
import java.util.Arrays;
import javax.swing.JOptionPane;

/**
 *
 * @author mannycalderon
 */
public class LoginController {

    private final LoginPanel vista;
    private final AperturaCajaPanel aperturaCajaPanel;
    private final MainView mainView;
    private final EmpleadoDAO dao;
    private final CashSessionDAO cashSessionDAO;

    public LoginController(LoginPanel vista, AperturaCajaPanel aperturaCajaPanel, MainView mainView) {
        this.vista = vista;
        this.aperturaCajaPanel = aperturaCajaPanel;
        this.mainView = mainView;
        this.dao = new EmpleadoDAO();
        this.cashSessionDAO = new CashSessionDAO();

        vista.getBtnIniciarSesion().addActionListener(e -> iniciarSesion());
        vista.getUserField().addActionListener(e -> iniciarSesion());
        vista.getContraField().addActionListener(e -> iniciarSesion());
    }

    private void iniciarSesion() {
        String username = vista.getUsuario();
        char[] contrasena = vista.getContrasena();

        if (username.isEmpty() || contrasena.length == 0) {
            JOptionPane.showMessageDialog(mainView, "Ingresa tu usuario y contraseña.");
            Arrays.fill(contrasena, ' ');
            return;
        }

        Empleado usuario = dao.autenticar(username, new String(contrasena));
        Arrays.fill(contrasena, ' ');

        if (usuario == null) {
            // Mensaje genérico: no revela si falló el usuario o la contraseña
            JOptionPane.showMessageDialog(mainView, "Usuario o contraseña incorrectos.",
                    "Error de acceso", JOptionPane.ERROR_MESSAGE);
            vista.limpiarContrasena();
            return;
        }

        Sesion.getInstancia().iniciarSesion(usuario);
        vista.limpiarFormulario();

        if (cashSessionDAO.obtenerSesionAbierta() != null) {
            mainView.showView(Sesion.getInstancia().isAdmin() ? "admin" : "employee");
        } else {
            aperturaCajaPanel.setNombreUsuario(Sesion.getInstancia().getNombreCompleto());
            mainView.showView("aperturaCaja");
        }
    }
}
