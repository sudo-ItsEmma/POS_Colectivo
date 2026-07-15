package com.tuerca.pos.controller;

import com.tuerca.pos.view.AdminPanel;
import com.tuerca.pos.view.MainView;

/**
 * Navegación del dashboard de Administrador y cierre de sesión. Reemplaza
 * el patrón anterior de {@code .form} donde cada botón buscaba el
 * {@code MainView} en tiempo de ejecución vía {@code getWindowAncestor}.
 *
 * Todos los botones del dashboard ya están conectados a su paso
 * correspondiente del roadmap (Arqueo de Caja, Corte de Caja, Devolución,
 * Pago Emprendedores, Reportes).
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
        vista.getBtnReportes().addActionListener(e -> mainView.showView("reportes"));
        vista.getBtnCerrarSesion().addActionListener(e -> mainView.cerrarSesion());
    }
}
