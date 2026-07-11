package com.tuerca.pos.controller;

import com.tuerca.pos.view.AdminPanel;
import com.tuerca.pos.view.MainView;

/**
 * Navegación del dashboard de Administrador y cierre de sesión. Reemplaza
 * el patrón anterior de {@code .form} donde cada botón buscaba el
 * {@code MainView} en tiempo de ejecución vía {@code getWindowAncestor}.
 *
 * Nota: igual que en el {@code .form} original, no todos los botones tienen
 * listener todavía — Pago Emprendedores, Devolución y Reportes son módulos
 * "sin iniciar" (ver ESTADO_PROYECTO.md), se conectan cuando les toque su
 * propio paso del roadmap. Arqueo de Caja (Paso 4) y Corte de Caja (Paso 5)
 * ya se conectaron.
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
        vista.getBtnCerrarSesion().addActionListener(e -> mainView.cerrarSesion());
    }
}
