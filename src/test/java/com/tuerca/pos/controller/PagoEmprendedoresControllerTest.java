package com.tuerca.pos.controller;

import com.toedter.calendar.JDateChooser;
import com.tuerca.pos.dao.EmpleadoDAO;
import com.tuerca.pos.dao.EmprendedorDAO;
import com.tuerca.pos.dao.SettlementDAO;
import com.tuerca.pos.model.Emprendedor;
import com.tuerca.pos.model.Empleado;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.model.Settlement;
import com.tuerca.pos.pdf.ReporteVentasPDF;
import com.tuerca.pos.pdf.dto.LineaReporteVenta;
import com.tuerca.pos.view.MainView;
import com.tuerca.pos.view.PagoEmprendedores;
import com.tuerca.pos.view.components.AutorizacionAdminDialog;

import java.util.Calendar;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PagoEmprendedoresControllerTest {

    @Mock
    private PagoEmprendedores vista;
    @Mock
    private MainView mainView;

    private final JComboBox<Object> cbEmprendedor = new JComboBox<>();
    private final JDateChooser fechaInicio = new JDateChooser();
    private final JDateChooser fechaFin = new JDateChooser();
    private final JButton btnCalcular = new JButton();
    private final JButton btnRegistrarPago = new JButton();
    private final JCheckBox cbIncluirRenta = new JCheckBox();
    private final JTable tablaVentas = new JTable(new DefaultTableModel(
            new Object[][]{}, new String[]{"Sel", "Folio", "Fecha", "Bruto"}) {
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Boolean.class : Object.class;
        }
    });

    private MockedConstruction<EmprendedorDAO> construccionEmprendedorDao;
    private MockedConstruction<SettlementDAO> construccionSettlementDao;
    private MockedConstruction<EmpleadoDAO> construccionEmpleadoDao;
    private SettlementDAO settlementDao;

    @BeforeEach
    void construirController() {
        when(vista.getCbEmprendedor()).thenReturn(cbEmprendedor);
        when(vista.getFechaInicio()).thenReturn(fechaInicio);
        when(vista.getFechaFin()).thenReturn(fechaFin);
        when(vista.getBtnCalcular()).thenReturn(btnCalcular);
        when(vista.getBtnRegistrarPago()).thenReturn(btnRegistrarPago);
        when(vista.getCbIncluirRenta()).thenReturn(cbIncluirRenta);
        when(vista.getTablaVentas()).thenReturn(tablaVentas);

        construccionEmprendedorDao = mockConstruction(EmprendedorDAO.class,
                (mock, context) -> when(mock.listar()).thenReturn(List.of()));
        construccionSettlementDao = mockConstruction(SettlementDAO.class);
        construccionEmpleadoDao = mockConstruction(EmpleadoDAO.class);

        new PagoEmprendedoresController(vista, mainView);

        settlementDao = construccionSettlementDao.constructed().get(0);

        Empleado empleado = new Empleado();
        empleado.setIdUserAccount(13);
        empleado.setId(1);
        empleado.setNombre("Test");
        empleado.setPaterno("User");
        empleado.setUsername("testuser");
        empleado.setIdRole(1);
        empleado.setRoleName("Admin");
        Sesion.getInstancia().iniciarSesion(empleado);
    }

    @AfterEach
    void cerrarMocksYSesion() {
        construccionEmprendedorDao.close();
        construccionSettlementDao.close();
        construccionEmpleadoDao.close();
        Sesion.getInstancia().cerrarSesion();
    }

    private Emprendedor emprendedorDePrueba(int id, double renta) {
        Emprendedor emp = new Emprendedor();
        emp.setId(id);
        emp.setMarca("JUNIT MARCA");
        emp.setRentaMensual(renta);
        return emp;
    }

    private void seleccionarEmprendedor(Emprendedor emp) {
        cbEmprendedor.addItem(emp);
        cbEmprendedor.setSelectedItem(emp);
    }

    private java.util.Date fecha(int anio, int mes, int dia) {
        Calendar cal = Calendar.getInstance();
        cal.set(anio, mes - 1, dia, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    @Test
    void calcular_sinEmprendedor_muestraAviso() {
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnCalcular.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vista), eq("Selecciona un emprendedor.")));
        }
    }

    @Test
    void calcular_sinFechas_muestraAviso() {
        seleccionarEmprendedor(emprendedorDePrueba(1, 500));
        fechaInicio.setDate(null);
        fechaFin.setDate(null);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnCalcular.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vista), eq("Selecciona el rango de fechas.")));
        }
    }

    @Test
    void calcular_conVentasPendientes_llenaTablaYDesmarcaRentaSiYaSeCobro() {
        seleccionarEmprendedor(emprendedorDePrueba(1, 500));
        fechaInicio.setDate(fecha(2026, 7, 1));
        fechaFin.setDate(fecha(2026, 7, 31));
        when(settlementDao.listarVentasPendientes(eq(1), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{10, "10/07/2026", 100.0, 0.0}
        ));
        when(settlementDao.obtenerFechaUltimaRentaCobradaEsteMes(1)).thenReturn(new java.sql.Date(fecha(2026, 7, 5).getTime()));

        btnCalcular.doClick();

        org.junit.jupiter.api.Assertions.assertEquals(1, tablaVentas.getModel().getRowCount());
        org.junit.jupiter.api.Assertions.assertFalse(cbIncluirRenta.isSelected(),
                "si ya se cobró la renta este mes, la casilla debe arrancar desmarcada");
        verify(vista).setAvisoRenta(anyString());
    }

    @Test
    void calcular_sinVentasPendientes_muestraAvisoYMarcaRentaPorDefecto() {
        seleccionarEmprendedor(emprendedorDePrueba(1, 500));
        fechaInicio.setDate(fecha(2026, 7, 1));
        fechaFin.setDate(fecha(2026, 7, 31));
        when(settlementDao.listarVentasPendientes(eq(1), any(), any())).thenReturn(List.of());
        when(settlementDao.obtenerFechaUltimaRentaCobradaEsteMes(1)).thenReturn(null);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnCalcular.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), eq("No hay ventas pendientes de pago para este emprendedor en el rango seleccionado.")));
        }
        org.junit.jupiter.api.Assertions.assertTrue(cbIncluirRenta.isSelected());
    }

    @Test
    void registrarPago_sinTicketsSeleccionados_muestraAviso() {
        seleccionarEmprendedor(emprendedorDePrueba(1, 500));

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnRegistrarPago.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), eq("Selecciona al menos un ticket para registrar el pago.")));
        }
    }

    @Test
    void registrarPago_usuarioCancelaLaConfirmacion_noRegistraNada() {
        seleccionarEmprendedor(emprendedorDePrueba(1, 500));
        fechaInicio.setDate(fecha(2026, 7, 1));
        fechaFin.setDate(fecha(2026, 7, 31));
        when(settlementDao.listarVentasPendientes(eq(1), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{10, "10/07/2026", 100.0, 0.0}
        ));
        btnCalcular.doClick();

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), eq("Confirmar Pago"), eq(JOptionPane.YES_NO_OPTION), eq(JOptionPane.WARNING_MESSAGE)))
                    .thenReturn(JOptionPane.NO_OPTION);

            btnRegistrarPago.doClick();
        }
        verify(settlementDao, never()).registrarPago(any(), anyList());
    }

    @Test
    void registrarPago_comoAdmin_exitosoYRechazaGenerarComprobante() {
        seleccionarEmprendedor(emprendedorDePrueba(1, 500));
        fechaInicio.setDate(fecha(2026, 7, 1));
        fechaFin.setDate(fecha(2026, 7, 31));
        when(settlementDao.listarVentasPendientes(eq(1), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{10, "10/07/2026", 100.0, 0.0}
        ));
        btnCalcular.doClick();

        when(settlementDao.registrarPago(any(Settlement.class), anyList())).thenAnswer(inv -> {
            Settlement s = inv.getArgument(0);
            s.setIdSettlement(900);
            s.setGrossAmount(100.0);
            s.setNetAmountPaid(100.0 - s.getRentDiscount());
            return true;
        });
        when(settlementDao.obtenerDetallesDelPago(900)).thenReturn(List.<LineaReporteVenta>of());

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), eq("Confirmar Pago"), eq(JOptionPane.YES_NO_OPTION), eq(JOptionPane.WARNING_MESSAGE)))
                    .thenReturn(JOptionPane.YES_OPTION);
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), eq("Comprobante de Pago"), eq(JOptionPane.YES_NO_OPTION), eq(JOptionPane.QUESTION_MESSAGE)))
                    .thenReturn(JOptionPane.NO_OPTION); // no quiere generar el PDF

            btnRegistrarPago.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vista), anyString()));
        }
        verify(settlementDao).registrarPago(any(Settlement.class), anyList());
    }

    @Test
    void registrarPago_comoEmpleado_siAdminCancelaLaAutorizacion_noRegistraNada() {
        Empleado empleadoSinAdmin = new Empleado();
        empleadoSinAdmin.setIdUserAccount(5);
        empleadoSinAdmin.setId(2);
        empleadoSinAdmin.setNombre("Cajero");
        empleadoSinAdmin.setPaterno("Prueba");
        empleadoSinAdmin.setUsername("cajero");
        empleadoSinAdmin.setIdRole(2);
        empleadoSinAdmin.setRoleName("Sales");
        Sesion.getInstancia().iniciarSesion(empleadoSinAdmin);

        seleccionarEmprendedor(emprendedorDePrueba(1, 500));
        fechaInicio.setDate(fecha(2026, 7, 1));
        fechaFin.setDate(fecha(2026, 7, 31));
        when(settlementDao.listarVentasPendientes(eq(1), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{10, "10/07/2026", 100.0, 0.0}
        ));
        btnCalcular.doClick();

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class);
             MockedStatic<AutorizacionAdminDialog> autorizacion = mockStatic(AutorizacionAdminDialog.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), eq("Confirmar Pago"), eq(JOptionPane.YES_NO_OPTION), eq(JOptionPane.WARNING_MESSAGE)))
                    .thenReturn(JOptionPane.YES_OPTION);
            autorizacion.when(() -> AutorizacionAdminDialog.solicitar(any(), any())).thenReturn(null);

            btnRegistrarPago.doClick();
        }
        verify(settlementDao, never()).registrarPago(any(), anyList());
    }

    @Test
    void registrarPago_elDaoFalla_muestraError() {
        seleccionarEmprendedor(emprendedorDePrueba(1, 500));
        fechaInicio.setDate(fecha(2026, 7, 1));
        fechaFin.setDate(fecha(2026, 7, 31));
        when(settlementDao.listarVentasPendientes(eq(1), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{10, "10/07/2026", 100.0, 0.0}
        ));
        btnCalcular.doClick();
        when(settlementDao.registrarPago(any(), anyList())).thenReturn(false);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), eq("Confirmar Pago"), eq(JOptionPane.YES_NO_OPTION), eq(JOptionPane.WARNING_MESSAGE)))
                    .thenReturn(JOptionPane.YES_OPTION);

            btnRegistrarPago.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), eq("No se pudo registrar el pago."), eq("Error"), eq(JOptionPane.ERROR_MESSAGE)));
        }
    }
}
