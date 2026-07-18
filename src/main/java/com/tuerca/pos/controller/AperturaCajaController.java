package com.tuerca.pos.controller;

import com.tuerca.pos.dao.ApartadoDAO;
import com.tuerca.pos.dao.CashSessionDAO;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.view.AperturaCajaPanel;
import com.tuerca.pos.view.MainView;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.swing.JOptionPane;

/**
 * Controla la apertura de caja. Se muestra tras el login cuando
 * {@link CashSessionDAO#obtenerSesionAbierta()} no encuentra una sesión
 * abierta el día de hoy. El monto de apertura lo escribe el usuario (viene
 * pre-cargado con el fondo fijo habitual de $600.00, pero es editable).
 */
public class AperturaCajaController {

    private final AperturaCajaPanel vista;
    private final MainView mainView;
    private final CashSessionDAO dao;
    private final ApartadoDAO apartadoDao;

    public AperturaCajaController(AperturaCajaPanel vista, MainView mainView) {
        this.vista = vista;
        this.mainView = mainView;
        this.dao = new CashSessionDAO();
        this.apartadoDao = new ApartadoDAO();

        vista.getBtnConfirmarApertura().addActionListener(e -> confirmarApertura());
    }

    private void confirmarApertura() {
        BigDecimal monto;
        try {
            monto = new BigDecimal(vista.getMontoInicialTexto()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(mainView, "Ingresa un monto numérico válido.",
                    "Monto inválido", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(mainView, "El monto de apertura debe ser mayor a $0.00.",
                    "Monto inválido", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Se deshabilita para evitar doble-submit (doble clic / Enter repetido)
        // mientras se espera la respuesta de la BD.
        vista.getBtnConfirmarApertura().setEnabled(false);

        boolean abierta = dao.abrirCaja(Sesion.getInstancia().getIdUserAccount(), monto) != null;

        // Si abrirCaja() falló, puede ser una carrera real (otro clic ya la abrió
        // mientras esperábamos la respuesta): en ese caso ya existe una sesión
        // abierta y no hay nada mal, simplemente entramos como si hubiera sido la
        // nuestra. Solo se considera error real si tampoco hay ninguna abierta.
        if (!abierta && dao.obtenerSesionAbierta() == null) {
            JOptionPane.showMessageDialog(mainView,
                    "No se pudo abrir la caja. Intenta de nuevo.",
                    "Error al abrir caja", JOptionPane.ERROR_MESSAGE);
            vista.getBtnConfirmarApertura().setEnabled(true);
            return;
        }

        // Auto-marcado de apartados vencidos (FN.6: "alerta visual al iniciar la jornada").
        // Solo cambia el estado — el stock se queda reservado hasta que alguien cancele el
        // apartado explícitamente desde Gestión de Apartados.
        int vencidos = apartadoDao.marcarVencidosAutomaticamente();
        if (vencidos > 0) {
            JOptionPane.showMessageDialog(mainView,
                    "Hay " + vencidos + " apartado(s) vencido(s).\n\n"
                    + "Revísalos en Gestión de Apartados.",
                    "Apartados vencidos", JOptionPane.WARNING_MESSAGE);
        }

        mainView.showView(Sesion.getInstancia().isAdmin() ? "admin" : "employee");
    }
}
