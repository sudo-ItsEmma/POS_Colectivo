package com.tuerca.pos.controller;

import com.tuerca.pos.dao.CashSessionDAO;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.view.AperturaCajaPanel;
import com.tuerca.pos.view.MainView;
import java.math.BigDecimal;
import javax.swing.JOptionPane;

/**
 * Controla la apertura de caja con el fondo fijo de $600.00. Se muestra
 * tras el login cuando {@link CashSessionDAO#obtenerSesionAbierta()} no
 * encuentra una sesión abierta el día de hoy.
 */
public class AperturaCajaController {

    private static final BigDecimal FONDO_FIJO = new BigDecimal("600.00");

    private final AperturaCajaPanel vista;
    private final MainView mainView;
    private final CashSessionDAO dao;

    public AperturaCajaController(AperturaCajaPanel vista, MainView mainView) {
        this.vista = vista;
        this.mainView = mainView;
        this.dao = new CashSessionDAO();

        vista.getBtnConfirmarApertura().addActionListener(e -> confirmarApertura());
    }

    private void confirmarApertura() {
        boolean abierta = dao.abrirCaja(Sesion.getInstancia().getIdUserAccount(), FONDO_FIJO) != null;

        if (!abierta) {
            JOptionPane.showMessageDialog(mainView,
                    "No se pudo abrir la caja. Es posible que ya exista una sesión abierta.",
                    "Error al abrir caja", JOptionPane.ERROR_MESSAGE);
            return;
        }

        mainView.showView(Sesion.getInstancia().isAdmin() ? "admin" : "employee");
    }
}
