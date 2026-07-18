package com.tuerca.pos.controller;

import com.tuerca.pos.dao.ApartadoDAO;
import com.tuerca.pos.dao.CashSessionDAO;
import com.tuerca.pos.model.CashSession;
import com.tuerca.pos.model.Empleado;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.view.AperturaCajaPanel;
import com.tuerca.pos.view.MainView;

import java.math.BigDecimal;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AperturaCajaControllerTest {

    @Mock
    private AperturaCajaPanel vista;
    @Mock
    private MainView mainView;

    private final JButton btnConfirmarApertura = new JButton();

    private MockedConstruction<CashSessionDAO> construccionCashSessionDao;
    private MockedConstruction<ApartadoDAO> construccionApartadoDao;
    private CashSessionDAO cashSessionDao;
    private ApartadoDAO apartadoDao;

    @BeforeEach
    void construirController() {
        when(vista.getBtnConfirmarApertura()).thenReturn(btnConfirmarApertura);

        construccionCashSessionDao = mockConstruction(CashSessionDAO.class);
        construccionApartadoDao = mockConstruction(ApartadoDAO.class);

        new AperturaCajaController(vista, mainView);

        cashSessionDao = construccionCashSessionDao.constructed().get(0);
        apartadoDao = construccionApartadoDao.constructed().get(0);

        Empleado empleado = new Empleado();
        empleado.setIdUserAccount(7);
        empleado.setId(1);
        empleado.setNombre("Test");
        empleado.setPaterno("User");
        empleado.setUsername("testuser");
        empleado.setIdRole(2);
        empleado.setRoleName("Sales");
        Sesion.getInstancia().iniciarSesion(empleado);
    }

    @AfterEach
    void cerrarMocksYSesion() {
        construccionCashSessionDao.close();
        construccionApartadoDao.close();
        Sesion.getInstancia().cerrarSesion();
    }

    @Test
    void confirmarApertura_textoNoNumerico_muestraErrorYNoAbre() {
        when(vista.getMontoInicialTexto()).thenReturn("no-es-numero");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnConfirmarApertura.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(mainView), any(), eq("Monto inválido"), eq(JOptionPane.ERROR_MESSAGE)));
        }
        verify(cashSessionDao, never()).abrirCaja(anyInt(), any());
    }

    @Test
    void confirmarApertura_montoCero_muestraErrorYNoAbre() {
        when(vista.getMontoInicialTexto()).thenReturn("0.00");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnConfirmarApertura.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(mainView), any(), eq("Monto inválido"), eq(JOptionPane.ERROR_MESSAGE)));
        }
        verify(cashSessionDao, never()).abrirCaja(anyInt(), any());
    }

    @Test
    void confirmarApertura_exitosaSinVencidos_navegaSegunRolSinAvisoDeVencidos() {
        when(vista.getMontoInicialTexto()).thenReturn("600.00");
        when(cashSessionDao.abrirCaja(eq(7), eq(new BigDecimal("600.00")))).thenReturn(new CashSession());
        when(apartadoDao.marcarVencidosAutomaticamente()).thenReturn(0);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnConfirmarApertura.doClick();
            jOptionPane.verifyNoInteractions();
        }
        verify(mainView).showView("employee");
    }

    @Test
    void confirmarApertura_conApartadosVencidos_muestraElAvisoYNavegaIgual() {
        when(vista.getMontoInicialTexto()).thenReturn("600.00");
        when(cashSessionDao.abrirCaja(anyInt(), any())).thenReturn(new CashSession());
        when(apartadoDao.marcarVencidosAutomaticamente()).thenReturn(3);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnConfirmarApertura.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(mainView), any(), eq("Apartados vencidos"), eq(JOptionPane.WARNING_MESSAGE)));
        }
        verify(mainView).showView("employee");
    }

    @Test
    void confirmarApertura_abrirCajaFallaPeroYaHayUnaAbierta_continuaSinErrorNiSegundoIntento() {
        when(vista.getMontoInicialTexto()).thenReturn("600.00");
        when(cashSessionDao.abrirCaja(anyInt(), any())).thenReturn(null); // carrera: alguien más ya la abrió
        when(cashSessionDao.obtenerSesionAbierta()).thenReturn(new CashSession()); // ...y en efecto ya hay una
        when(apartadoDao.marcarVencidosAutomaticamente()).thenReturn(0);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnConfirmarApertura.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    any(), any(), eq("Error al abrir caja"), anyInt()), never());
        }
        verify(mainView).showView("employee");
    }

    @Test
    void confirmarApertura_fallaRealSinNingunaAbierta_muestraErrorYReactivaElBoton() {
        when(vista.getMontoInicialTexto()).thenReturn("600.00");
        when(cashSessionDao.abrirCaja(anyInt(), any())).thenReturn(null);
        when(cashSessionDao.obtenerSesionAbierta()).thenReturn(null);
        btnConfirmarApertura.setEnabled(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnConfirmarApertura.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(mainView), any(), eq("Error al abrir caja"), eq(JOptionPane.ERROR_MESSAGE)));
        }
        verify(mainView, never()).showView(any());
        assertTrue(btnConfirmarApertura.isEnabled(), "el botón se debe reactivar tras un error real");
    }
}
