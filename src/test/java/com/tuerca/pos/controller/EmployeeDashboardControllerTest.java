package com.tuerca.pos.controller;

import com.tuerca.pos.view.EmployeePanel;
import com.tuerca.pos.view.MainView;

import javax.swing.JButton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeDashboardControllerTest {

    @Mock
    private EmployeePanel vista;
    @Mock
    private MainView mainView;

    private final JButton btnEmprendedores = new JButton();
    private final JButton btnProductos = new JButton();
    private final JButton btnVentas = new JButton();
    private final JButton btnPagoEmprendedores = new JButton();
    private final JButton btnApartados = new JButton();
    private final JButton btnArqueoCaja = new JButton();
    private final JButton btnReportes = new JButton();
    private final JButton btnDevolucion = new JButton();
    private final JButton btnCorteCaja = new JButton();
    private final JButton btnCerrarSesion = new JButton();

    @BeforeEach
    void construirController() {
        when(vista.getBtnEmprendedores()).thenReturn(btnEmprendedores);
        when(vista.getBtnProductos()).thenReturn(btnProductos);
        when(vista.getBtnVentas()).thenReturn(btnVentas);
        when(vista.getBtnPagoEmprendedores()).thenReturn(btnPagoEmprendedores);
        when(vista.getBtnApartados()).thenReturn(btnApartados);
        when(vista.getBtnArqueoCaja()).thenReturn(btnArqueoCaja);
        when(vista.getBtnReportes()).thenReturn(btnReportes);
        when(vista.getBtnDevolucion()).thenReturn(btnDevolucion);
        when(vista.getBtnCorteCaja()).thenReturn(btnCorteCaja);
        when(vista.getBtnCerrarSesion()).thenReturn(btnCerrarSesion);

        new EmployeeDashboardController(vista, mainView);
    }

    @Test
    void btnEmprendedores_navegaAGestionDeEmprendedores() {
        btnEmprendedores.doClick();
        verify(mainView).showView("entrepreneur");
    }

    @Test
    void btnProductos_navegaAGestionDeProductos() {
        btnProductos.doClick();
        verify(mainView).showView("products");
    }

    @Test
    void btnVentas_navegaAVentas() {
        btnVentas.doClick();
        verify(mainView).showView("ventas");
    }

    @Test
    void btnPagoEmprendedores_navegaAPagoEmprendedores() {
        btnPagoEmprendedores.doClick();
        verify(mainView).showView("pagoEmprendedores");
    }

    @Test
    void btnApartados_navegaAGestionDeApartados() {
        btnApartados.doClick();
        verify(mainView).showView("apartados");
    }

    @Test
    void btnArqueoCaja_navegaAArqueo() {
        btnArqueoCaja.doClick();
        verify(mainView).showView("arqueo");
    }

    @Test
    void btnReportes_navegaAReportes() {
        btnReportes.doClick();
        verify(mainView).showView("reportes");
    }

    @Test
    void btnDevolucion_navegaADevoluciones() {
        btnDevolucion.doClick();
        verify(mainView).showView("devoluciones");
    }

    @Test
    void btnCorteCaja_navegaACorte() {
        btnCorteCaja.doClick();
        verify(mainView).showView("corte");
    }

    @Test
    void btnCerrarSesion_llamaACerrarSesionDeMainView() {
        btnCerrarSesion.doClick();
        verify(mainView).cerrarSesion();
    }
}
