package com.tuerca.pos.controller;

import com.tuerca.pos.view.EmployeePanel;
import com.tuerca.pos.view.MainView;

/**
 * Navegación del dashboard de Empleado y cierre de sesión. Reemplaza el
 * patrón anterior de {@code .form} donde cada botón buscaba el
 * {@code MainView} en tiempo de ejecución vía {@code getWindowAncestor}.
 */
public class EmployeeDashboardController {

    public EmployeeDashboardController(EmployeePanel vista, MainView mainView) {
        vista.getBtnEmprendedores().addActionListener(e -> mainView.showView("entrepreneur"));
        vista.getBtnProductos().addActionListener(e -> mainView.showView("products"));
        vista.getBtnVentas().addActionListener(e -> mainView.showView("ventas"));
        vista.getBtnPagoEmprendedores().addActionListener(e -> mainView.showView("pagoEmprendedores"));
        vista.getBtnApartados().addActionListener(e -> mainView.showView("apartados"));
        vista.getBtnArqueoCaja().addActionListener(e -> mainView.showView("arqueo"));
        vista.getBtnReportes().addActionListener(e -> mainView.showView("reportes"));
        vista.getBtnDevolucion().addActionListener(e -> mainView.showView("devoluciones"));
        vista.getBtnCorteCaja().addActionListener(e -> mainView.showView("corte"));
        vista.getBtnCerrarSesion().addActionListener(e -> mainView.cerrarSesion());
    }
}
