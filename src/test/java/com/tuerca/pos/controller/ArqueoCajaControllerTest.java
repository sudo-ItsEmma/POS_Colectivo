package com.tuerca.pos.controller;

import com.tuerca.pos.dao.ArqueoDAO;
import com.tuerca.pos.dao.CashSessionDAO;
import com.tuerca.pos.model.CashCount;
import com.tuerca.pos.model.CashSession;
import com.tuerca.pos.model.Empleado;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.view.ArqueoDeCaja;
import com.tuerca.pos.view.MainView;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mock;
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

/**
 * Segundo controller con Mockito (Paso 20, parte 2), ahora con los dos
 * elementos que faltaban por probar en el patrón:
 * <ul>
 *   <li>{@code mockConstruction} para interceptar los {@code new ArqueoDAO()}/
 *       {@code new CashSessionDAO()} que el controller crea internamente en su
 *       constructor — no se modifica producción para inyectarlos.</li>
 *   <li>{@code mockStatic(JOptionPane.class)} para que los diálogos nunca se
 *       muestren de verdad (colgarían el test esperando un clic humano) y
 *       poder simular lo que el usuario respondería.</li>
 * </ul>
 * {@link Sesion} es un singleton real y liviano (sin BD) — se usa tal cual,
 * iniciando/cerrando una sesión de prueba en cada test.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArqueoCajaControllerTest {

    @Mock
    private ArqueoDeCaja vista;
    @Mock
    private MainView mainView;

    private final JButton btnBack = new JButton();
    private final JButton btnIntroducirCantidad = new JButton();
    private final JButton btnCancelar = new JButton();
    private final JLabel lblSaldoTeorico = new JLabel();
    private final JLabel lblTransferencias = new JLabel();
    private final JLabel lblEfectivoContado = new JLabel();
    private final JLabel lblDiferencia = new JLabel();
    private final JTable tablaVentas = new JTable(new DefaultTableModel(
            new Object[][]{}, new String[]{"Hora", "Método", "Total", "Efectivo", "Transferencia"}));

    private MockedConstruction<ArqueoDAO> construccionArqueoDao;
    private MockedConstruction<CashSessionDAO> construccionCashSessionDao;
    private ArqueoDAO arqueoDao;
    private CashSessionDAO cashSessionDao;
    private ArqueoCajaController controller;

    @BeforeEach
    void construirController() {
        when(vista.getBtnBack()).thenReturn(btnBack);
        when(vista.getBtnIntroducirCantidad()).thenReturn(btnIntroducirCantidad);
        when(vista.getBtnCancelar()).thenReturn(btnCancelar);
        when(vista.getLblSaldoTeorico()).thenReturn(lblSaldoTeorico);
        when(vista.getLblTransferencias()).thenReturn(lblTransferencias);
        when(vista.getLblEfectivoContado()).thenReturn(lblEfectivoContado);
        when(vista.getLblDiferencia()).thenReturn(lblDiferencia);
        when(vista.getTablaVentas()).thenReturn(tablaVentas);

        construccionArqueoDao = mockConstruction(ArqueoDAO.class);
        construccionCashSessionDao = mockConstruction(CashSessionDAO.class);

        controller = new ArqueoCajaController(vista, mainView);

        arqueoDao = construccionArqueoDao.constructed().get(0);
        cashSessionDao = construccionCashSessionDao.constructed().get(0);

        Empleado empleado = new Empleado();
        empleado.setIdUserAccount(42);
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
        construccionCashSessionDao.close();
        Sesion.getInstancia().cerrarSesion();
    }

    private CashSession sesionAbiertaDePrueba() {
        CashSession cs = new CashSession();
        cs.setIdCashSession(99);
        cs.setOpeningDateTime(LocalDateTime.now().minusHours(2));
        cs.setInitialCashAmount(new BigDecimal("600.00"));
        return cs;
    }

    @Test
    void actualizarDatos_sinCajaAbierta_muestraAvisoYLimpiaLosLabels() {
        when(cashSessionDao.obtenerSesionAbierta()).thenReturn(null);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            controller.actualizarDatos();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), anyString(), eq("Arqueo no disponible"), eq(JOptionPane.WARNING_MESSAGE)));
        }

        org.junit.jupiter.api.Assertions.assertEquals("$0.00", lblSaldoTeorico.getText());
        org.junit.jupiter.api.Assertions.assertEquals("$0.00", lblTransferencias.getText());
    }

    @Test
    void actualizarDatos_conCajaAbierta_calculaSaldoTeoricoYTransferencias() {
        CashSession sesion = sesionAbiertaDePrueba();
        when(cashSessionDao.obtenerSesionAbierta()).thenReturn(sesion);
        when(arqueoDao.calcularSaldoTeorico(sesion)).thenReturn(new BigDecimal("724.00"));
        when(arqueoDao.calcularVentasTransferencia(sesion.getOpeningDateTime())).thenReturn(new BigDecimal("150.00"));
        when(arqueoDao.obtenerVentasDesde(sesion.getOpeningDateTime())).thenReturn(List.<Object[]>of(
                new Object[]{"10:00", "Efectivo", new BigDecimal("100.00"), new BigDecimal("100.00"), BigDecimal.ZERO}
        ));

        controller.actualizarDatos();

        org.junit.jupiter.api.Assertions.assertEquals("$724.00", lblSaldoTeorico.getText());
        org.junit.jupiter.api.Assertions.assertEquals("$150.00", lblTransferencias.getText());
        org.junit.jupiter.api.Assertions.assertEquals(2, tablaVentas.getModel().getRowCount(),
                "1 venta + 1 fila de TOTAL");
    }

    @Test
    void procesarArqueo_sinCajaAbierta_soloMuestraElAviso() {
        // sesionActual sigue null porque nunca se llamó actualizarDatos()
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnIntroducirCantidad.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), anyString(), eq("Arqueo no disponible"), eq(JOptionPane.WARNING_MESSAGE)));
            jOptionPane.verify(() -> JOptionPane.showInputDialog(any(), any(), any(), anyInt()), never());
        }
    }

    @Test
    void procesarArqueo_montoInvalido_muestraErrorYNoRegistra() {
        CashSession sesion = sesionAbiertaDePrueba();
        when(cashSessionDao.obtenerSesionAbierta()).thenReturn(sesion);
        controller.actualizarDatos();

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(
                    eq(vista), anyString(), eq("Arqueo de Caja"), eq(JOptionPane.QUESTION_MESSAGE)))
                    .thenReturn("no-es-un-numero");

            btnIntroducirCantidad.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), anyString(), eq("Monto inválido"), eq(JOptionPane.ERROR_MESSAGE)));
        }
        verify(arqueoDao, never()).registrarArqueo(any());
    }

    @Test
    void procesarArqueo_sinDiferencia_seGuardaSinPedirComentario() {
        CashSession sesion = sesionAbiertaDePrueba();
        when(cashSessionDao.obtenerSesionAbierta()).thenReturn(sesion);
        when(arqueoDao.calcularSaldoTeorico(sesion)).thenReturn(new BigDecimal("600.00"));
        when(arqueoDao.calcularVentasTransferencia(any())).thenReturn(BigDecimal.ZERO);
        when(arqueoDao.obtenerVentasDesde(any())).thenReturn(List.of());
        controller.actualizarDatos();
        when(arqueoDao.registrarArqueo(any())).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(
                    eq(vista), anyString(), eq("Arqueo de Caja"), eq(JOptionPane.QUESTION_MESSAGE)))
                    .thenReturn("600.00");

            btnIntroducirCantidad.doClick();

            jOptionPane.verify(() -> JOptionPane.showInputDialog(
                    any(), any(), eq("Justificación requerida"), anyInt()), never());
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vista), eq("Arqueo registrado correctamente.")));
        }

        verify(arqueoDao).registrarArqueo(any());
        org.junit.jupiter.api.Assertions.assertEquals("$0.00", lblDiferencia.getText());
    }

    @Test
    void procesarArqueo_conDiferenciaYComentarioValido_seGuardaConElComentario() {
        CashSession sesion = sesionAbiertaDePrueba();
        when(cashSessionDao.obtenerSesionAbierta()).thenReturn(sesion);
        when(arqueoDao.calcularSaldoTeorico(sesion)).thenReturn(new BigDecimal("600.00"));
        when(arqueoDao.calcularVentasTransferencia(any())).thenReturn(BigDecimal.ZERO);
        when(arqueoDao.obtenerVentasDesde(any())).thenReturn(List.of());
        controller.actualizarDatos();
        when(arqueoDao.registrarArqueo(any())).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(
                    eq(vista), anyString(), eq("Arqueo de Caja"), eq(JOptionPane.QUESTION_MESSAGE)))
                    .thenReturn("590.00");
            jOptionPane.when(() -> JOptionPane.showInputDialog(
                    eq(vista), anyString(), eq("Justificación requerida"), eq(JOptionPane.WARNING_MESSAGE)))
                    .thenReturn("Faltó un billete, ya se justificó con el cliente.");

            btnIntroducirCantidad.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vista), eq("Arqueo registrado correctamente.")));
        }

        verify(arqueoDao).registrarArqueo(argThatJustificationEquals("Faltó un billete, ya se justificó con el cliente."));
    }

    @Test
    void procesarArqueo_conDiferenciaYSinComentario_noRegistraNada() {
        CashSession sesion = sesionAbiertaDePrueba();
        when(cashSessionDao.obtenerSesionAbierta()).thenReturn(sesion);
        when(arqueoDao.calcularSaldoTeorico(sesion)).thenReturn(new BigDecimal("600.00"));
        when(arqueoDao.calcularVentasTransferencia(any())).thenReturn(BigDecimal.ZERO);
        when(arqueoDao.obtenerVentasDesde(any())).thenReturn(List.of());
        controller.actualizarDatos();

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(
                    eq(vista), anyString(), eq("Arqueo de Caja"), eq(JOptionPane.QUESTION_MESSAGE)))
                    .thenReturn("590.00");
            jOptionPane.when(() -> JOptionPane.showInputDialog(
                    eq(vista), anyString(), eq("Justificación requerida"), eq(JOptionPane.WARNING_MESSAGE)))
                    .thenReturn(null); // el usuario cancela el comentario

            btnIntroducirCantidad.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), anyString(), eq("Arqueo cancelado"), eq(JOptionPane.WARNING_MESSAGE)));
        }

        verify(arqueoDao, never()).registrarArqueo(any());
    }

    @Test
    void btnCancelar_limpiaAvisaYNavegaSegunElRol() {
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnCancelar.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vista), eq("Comparación de arqueo cancelada.")));
        }

        verify(mainView).showView("employee"); // roleName = "Sales" en el fixture de esta clase
        org.junit.jupiter.api.Assertions.assertEquals("$0.00", lblEfectivoContado.getText());
        org.junit.jupiter.api.Assertions.assertEquals("$0.00", lblDiferencia.getText());
    }

    @Test
    void btnBack_navegaSegunElRol() {
        btnBack.doClick();
        verify(mainView).showView("employee");
    }

    private CashCount argThatJustificationEquals(String comentarioEsperado) {
        return org.mockito.ArgumentMatchers.argThat(cc -> comentarioEsperado.equals(cc.getJustificationComment()));
    }
}
