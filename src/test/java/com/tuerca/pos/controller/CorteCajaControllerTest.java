package com.tuerca.pos.controller;

import com.tuerca.pos.dao.ArqueoDAO;
import com.tuerca.pos.dao.CashSessionDAO;
import com.tuerca.pos.dao.CorteDAO;
import com.tuerca.pos.model.CashSession;
import com.tuerca.pos.model.Empleado;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.view.CorteDeCaja;
import com.tuerca.pos.view.MainView;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CorteCajaControllerTest {

    @Mock
    private CorteDeCaja vista;
    @Mock
    private MainView mainView;

    private final JButton btnBack = new JButton();
    private final JButton btnFinalizarJornada = new JButton();

    private MockedConstruction<ArqueoDAO> construccionArqueoDao;
    private MockedConstruction<CorteDAO> construccionCorteDao;
    private MockedConstruction<CashSessionDAO> construccionCashSessionDao;
    private ArqueoDAO arqueoDao;
    private CorteDAO corteDao;
    private CashSessionDAO cashSessionDao;
    private CorteCajaController controller;

    @BeforeEach
    void construirController() {
        when(vista.getBtnBack()).thenReturn(btnBack);
        when(vista.getBtnFinalizarJornada()).thenReturn(btnFinalizarJornada);

        construccionArqueoDao = mockConstruction(ArqueoDAO.class);
        construccionCorteDao = mockConstruction(CorteDAO.class);
        construccionCashSessionDao = mockConstruction(CashSessionDAO.class);

        controller = new CorteCajaController(vista, mainView);

        arqueoDao = construccionArqueoDao.constructed().get(0);
        corteDao = construccionCorteDao.constructed().get(0);
        cashSessionDao = construccionCashSessionDao.constructed().get(0);

        Empleado empleado = new Empleado();
        empleado.setIdUserAccount(11);
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
        construccionArqueoDao.close();
        construccionCorteDao.close();
        construccionCashSessionDao.close();
        Sesion.getInstancia().cerrarSesion();
    }

    private CashSession sesionAbiertaDePrueba() {
        CashSession cs = new CashSession();
        cs.setIdCashSession(55);
        cs.setOpeningDateTime(LocalDateTime.now().minusHours(3));
        cs.setInitialCashAmount(new BigDecimal("600.00"));
        return cs;
    }

    private void configurarCalculosBase(CashSession sesion, BigDecimal saldoTeorico) {
        when(arqueoDao.calcularVentasEfectivo(sesion.getOpeningDateTime())).thenReturn(new BigDecimal("100.00"));
        when(arqueoDao.calcularAbonosEfectivo(sesion.getOpeningDateTime())).thenReturn(new BigDecimal("50.00"));
        when(arqueoDao.calcularVentasTransferencia(sesion.getOpeningDateTime())).thenReturn(new BigDecimal("200.00"));
        when(arqueoDao.contarVentasConTransferencia(sesion.getOpeningDateTime())).thenReturn(2);
        when(corteDao.calcularApartadosNuevos(sesion.getOpeningDateTime())).thenReturn(new BigDecimal("40.00"));
        when(corteDao.calcularAbonosApartados(any(), any())).thenReturn(new BigDecimal("25.00"));
        when(corteDao.calcularApartadosNuevosPorMetodo(any(), any())).thenReturn(BigDecimal.ZERO);
        when(corteDao.calcularAbonosApartadosPorMetodo(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(arqueoDao.calcularSaldoTeorico(sesion)).thenReturn(saldoTeorico);
    }

    @Test
    void actualizarDatos_sinCajaAbierta_muestraAviso() {
        when(cashSessionDao.obtenerSesionAbierta()).thenReturn(null);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            controller.actualizarDatos();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), anyString(), eq("Corte no disponible"), eq(JOptionPane.WARNING_MESSAGE)));
        }
        verify(vista, never()).setVentasEfectivo(any());
    }

    @Test
    void actualizarDatos_conCajaAbierta_calculaYMuestraElResumenCompleto() {
        CashSession sesion = sesionAbiertaDePrueba();
        when(cashSessionDao.obtenerSesionAbierta()).thenReturn(sesion);
        configurarCalculosBase(sesion, new BigDecimal("750.00"));

        controller.actualizarDatos();

        verify(vista).setVentasEfectivo("$100.00");
        verify(vista).setAbonosEfectivo("$50.00");
        verify(vista).setFondoInicial("$600.00");
        verify(vista).setTotalEfectivo("$750.00"); // 600 + 100 + 50
        verify(vista).setVentasTransferencia("$200.00");
        verify(vista).setCantidadTransferencias(2);
        verify(vista).setApartadosTotal("$65.00"); // 40 + 25
        verify(vista).setDebeHaberEnCaja("$750.00");
        verify(vista).setEfectivoContado("—");
        verify(vista).setMontoARetirar("—");
    }

    @Test
    void procesarCierre_sinCajaAbierta_muestraAviso() {
        when(cashSessionDao.obtenerSesionAbierta()).thenReturn(null);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            controller.actualizarDatos(); // deja sesionActual en null
            btnFinalizarJornada.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), anyString(), eq("Corte no disponible"), eq(JOptionPane.WARNING_MESSAGE)), org.mockito.Mockito.times(2));
        }
        verify(corteDao, never()).cerrarCaja(any());
    }

    @Test
    void procesarCierre_usuarioCancelaLaConfirmacion_noPideNadaMas() {
        CashSession sesion = sesionAbiertaDePrueba();
        when(cashSessionDao.obtenerSesionAbierta()).thenReturn(sesion);
        configurarCalculosBase(sesion, new BigDecimal("750.00"));
        controller.actualizarDatos();

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(
                    eq(vista), anyString(), eq("Confirmar cierre de jornada"), eq(JOptionPane.YES_NO_OPTION), eq(JOptionPane.WARNING_MESSAGE)))
                    .thenReturn(JOptionPane.NO_OPTION);

            btnFinalizarJornada.doClick();

            jOptionPane.verify(() -> JOptionPane.showInputDialog(any(), any(), any(), anyInt()), never());
        }
        verify(corteDao, never()).cerrarCaja(any());
    }

    @Test
    void procesarCierre_sinDiferencia_cierraSinComentarioYFuerzaLogout() {
        CashSession sesion = sesionAbiertaDePrueba();
        when(cashSessionDao.obtenerSesionAbierta()).thenReturn(sesion);
        configurarCalculosBase(sesion, new BigDecimal("750.00"));
        controller.actualizarDatos();
        when(corteDao.cerrarCaja(any())).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(JOptionPane.YES_OPTION);
            jOptionPane.when(() -> JOptionPane.showInputDialog(
                    eq(vista), anyString(), eq("Corte de Caja"), eq(JOptionPane.QUESTION_MESSAGE)))
                    .thenReturn("750.00");

            btnFinalizarJornada.doClick();

            jOptionPane.verify(() -> JOptionPane.showInputDialog(
                    any(), any(), eq("Justificación requerida"), anyInt()), never());
        }
        verify(corteDao).cerrarCaja(any());
        verify(mainView).cerrarSesion();
    }

    @Test
    void procesarCierre_conDiferenciaSinComentario_noCierraLaCaja() {
        CashSession sesion = sesionAbiertaDePrueba();
        when(cashSessionDao.obtenerSesionAbierta()).thenReturn(sesion);
        configurarCalculosBase(sesion, new BigDecimal("750.00"));
        controller.actualizarDatos();

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(JOptionPane.YES_OPTION);
            jOptionPane.when(() -> JOptionPane.showInputDialog(
                    eq(vista), anyString(), eq("Corte de Caja"), eq(JOptionPane.QUESTION_MESSAGE)))
                    .thenReturn("700.00"); // distinto de 750 -> hay diferencia
            jOptionPane.when(() -> JOptionPane.showInputDialog(
                    any(), any(), eq("Justificación requerida"), anyInt()))
                    .thenReturn(null);

            btnFinalizarJornada.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), anyString(), eq("Corte cancelado"), eq(JOptionPane.WARNING_MESSAGE)));
        }
        verify(corteDao, never()).cerrarCaja(any());
        verify(mainView, never()).cerrarSesion();
    }

    @Test
    void procesarCierre_elCorteDaoFalla_muestraErrorYNoFuerzaLogout() {
        CashSession sesion = sesionAbiertaDePrueba();
        when(cashSessionDao.obtenerSesionAbierta()).thenReturn(sesion);
        configurarCalculosBase(sesion, new BigDecimal("750.00"));
        controller.actualizarDatos();
        when(corteDao.cerrarCaja(any())).thenReturn(false);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(JOptionPane.YES_OPTION);
            jOptionPane.when(() -> JOptionPane.showInputDialog(
                    eq(vista), anyString(), eq("Corte de Caja"), eq(JOptionPane.QUESTION_MESSAGE)))
                    .thenReturn("750.00");

            btnFinalizarJornada.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vista), eq("No se pudo cerrar la caja."), eq("Error"), eq(JOptionPane.ERROR_MESSAGE)));
        }
        verify(mainView, never()).cerrarSesion();
    }

    @Test
    void btnBack_navegaSegunElRol() {
        btnBack.doClick();
        verify(mainView).showView("employee");
    }
}
