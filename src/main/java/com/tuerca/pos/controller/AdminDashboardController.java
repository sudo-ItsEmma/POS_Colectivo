package com.tuerca.pos.controller;

import com.tuerca.pos.view.AdminPanel;
import com.tuerca.pos.view.MainView;

/**
 * Navegación del dashboard de Administrador y cierre de sesión. Reemplaza
 * el patrón anterior de {@code .form} donde cada botón buscaba el
 * {@code MainView} en tiempo de ejecución vía {@code getWindowAncestor}.
 *
 * Nota: igual que en el {@code .form} original, no todos los botones tienen
 * listener todavía — Reportes es el único módulo "sin iniciar" que queda
 * (ver ESTADO_PROYECTO.md). Arqueo de Caja (Paso 4), Corte de Caja (Paso 5),
 * Devolución (Paso 8) y Pago Emprendedores (Paso 9) ya se conectaron.
 */
public class AdminDashboardController {

    public AdminDashboardController(AdminPanel vista, MainView mainView) {
        vista.getBtnEmpleados().addActionListener(e -> mainView.showView("empleados"));
        vista.getBtnEmprendedores().addActionListener(e -> mainView.showView("entrepreneur"));
        vista.getBtnProductos().addActionListener(e -> mainView.showView("products"));
        vista.getBtnVentas().addActionListener(e -> mainView.showView("ventas"));
        vista.getBtnApartados().addActionListener(e -> mainView.showView("apartados"));
        vista.getBtnArqueoCaja().addActionListener(e -> mainView.showView("arqueo"));
        vista.getBtnCorteCaja().addActionListener(e -> mainView.showView("corte"));
        vista.getBtnDevolucion().addActionListener(e -> mainView.showView("devoluciones"));
        vista.getBtnPagoEmprendedores().addActionListener(e -> mainView.showView("pagoEmprendedores"));
        vista.getBtnCerrarSesion().addActionListener(e -> mainView.cerrarSesion());
    }
}
