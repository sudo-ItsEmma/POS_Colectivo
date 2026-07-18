package com.tuerca.pos.controller;

import com.toedter.calendar.JDateChooser;
import com.tuerca.pos.dao.EmprendedorDAO;
import com.tuerca.pos.dao.SettlementDAO;
import com.tuerca.pos.model.Emprendedor;
import com.tuerca.pos.pdf.ReporteVentasPDF;
import com.tuerca.pos.pdf.dto.LineaReporteVenta;
import com.tuerca.pos.view.GenerarReportes;
import com.tuerca.pos.view.MainView;

import java.io.File;
import java.sql.Date;
import java.util.Calendar;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GenerarReportesControllerTest {

    @Mock
    private GenerarReportes vista;
    @Mock
    private MainView mainView;

    private final JComboBox<Object> cbEmprendedor = new JComboBox<>();
    private final JDateChooser fechaInicio = new JDateChooser();
    private final JDateChooser fechaFin = new JDateChooser();
    private final JButton btnGenerarReporte = new JButton();
    private final JButton btnDescargarPDF = new JButton();
    private final JTable tablaPreview = new JTable(new DefaultTableModel(
            new Object[][]{}, new String[]{"Folio", "Fecha", "Código", "Descripción", "Cant", "Precio", "Desc", "Subtotal", "Estado"}));

    private MockedConstruction<EmprendedorDAO> construccionEmprendedorDao;
    private MockedConstruction<SettlementDAO> construccionSettlementDao;
    private SettlementDAO settlementDao;

    @BeforeEach
    void construirController() {
        when(vista.getCbEmprendedor()).thenReturn(cbEmprendedor);
        when(vista.getFechaInicio()).thenReturn(fechaInicio);
        when(vista.getFechaFin()).thenReturn(fechaFin);
        when(vista.getBtnGenerarReporte()).thenReturn(btnGenerarReporte);
        when(vista.getBtnDescargarPDF()).thenReturn(btnDescargarPDF);
        when(vista.getTablaPreview()).thenReturn(tablaPreview);

        construccionEmprendedorDao = mockConstruction(EmprendedorDAO.class,
                (mock, context) -> when(mock.listar()).thenReturn(List.of()));
        construccionSettlementDao = mockConstruction(SettlementDAO.class);

        new GenerarReportesController(vista, mainView);

        settlementDao = construccionSettlementDao.constructed().get(0);
    }

    @AfterEach
    void cerrarMocks() {
        construccionEmprendedorDao.close();
        construccionSettlementDao.close();
    }

    private Emprendedor emprendedorDePrueba(int id, double renta) {
        Emprendedor emp = new Emprendedor();
        emp.setId(id);
        emp.setMarca("JUNIT MARCA");
        emp.setRentaMensual(renta);
        return emp;
    }

    private java.util.Date fecha(int anio, int mes, int dia) {
        Calendar cal = Calendar.getInstance();
        cal.set(anio, mes - 1, dia, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    @Test
    void generarPreview_sinEmprendedorSeleccionado_muestraAviso() {
        cbEmprendedor.setSelectedItem(null);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnGenerarReporte.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vista), eq("Selecciona un emprendedor.")));
        }
        verify(settlementDao, never()).obtenerDetalleVentasDelPeriodo(org.mockito.ArgumentMatchers.anyInt(), any(), any());
    }

    @Test
    void generarPreview_sinFechas_muestraAviso() {
        Emprendedor emp = emprendedorDePrueba(1, 500);
        cbEmprendedor.addItem(emp);
        cbEmprendedor.setSelectedItem(emp);
        fechaInicio.setDate(null);
        fechaFin.setDate(null);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnGenerarReporte.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vista), eq("Selecciona el rango de fechas.")));
        }
    }

    @Test
    void generarPreview_finAntesDeInicio_muestraAviso() {
        Emprendedor emp = emprendedorDePrueba(1, 500);
        cbEmprendedor.addItem(emp);
        cbEmprendedor.setSelectedItem(emp);
        fechaInicio.setDate(fecha(2026, 7, 10));
        fechaFin.setDate(fecha(2026, 7, 1));

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnGenerarReporte.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), eq("La fecha de fin no puede ser anterior a la fecha de inicio.")));
        }
    }

    @Test
    void generarPreview_exitoso_calculaTotalesYLlenaLaTabla() {
        Emprendedor emp = emprendedorDePrueba(1, 100.0);
        cbEmprendedor.addItem(emp);
        cbEmprendedor.setSelectedItem(emp);
        fechaInicio.setDate(fecha(2026, 7, 1));
        fechaFin.setDate(fecha(2026, 7, 31));

        LineaReporteVenta linea = new LineaReporteVenta(1, fecha(2026, 7, 10), "JT01", "Producto",
                2, 20.0, 5.0, 35.0, true);
        when(settlementDao.obtenerDetalleVentasDelPeriodo(eq(1), any(Date.class), any(Date.class)))
                .thenReturn(List.of(linea));
        when(settlementDao.obtenerFechaUltimaRentaCobradaEsteMes(1)).thenReturn(null);

        btnGenerarReporte.doClick();

        verify(vista).setBruto("$35.00");
        verify(vista).setDescuentos("$5.00");
        verify(vista).setRenta("$100.00");
        verify(vista).setTotalNeto("$-70.00");
        assertEquals(1, tablaPreview.getModel().getRowCount());
    }

    @Test
    void generarPreview_sinVentasEnElRango_muestraAviso() {
        Emprendedor emp = emprendedorDePrueba(1, 100.0);
        cbEmprendedor.addItem(emp);
        cbEmprendedor.setSelectedItem(emp);
        fechaInicio.setDate(fecha(2026, 7, 1));
        fechaFin.setDate(fecha(2026, 7, 31));
        when(settlementDao.obtenerDetalleVentasDelPeriodo(eq(1), any(Date.class), any(Date.class)))
                .thenReturn(List.of());

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnGenerarReporte.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), eq("No hay ventas para este emprendedor en el rango seleccionado.")));
        }
    }

    @Test
    void descargarPDF_sinReportePrevioGenerado_muestraAviso() {
        Emprendedor emp = emprendedorDePrueba(1, 100.0);
        cbEmprendedor.addItem(emp);
        cbEmprendedor.setSelectedItem(emp);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnDescargarPDF.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), eq("Primero genera el reporte para poder descargarlo.")));
        }
    }

    @Test
    void descargarPDF_usuarioCancelaElSelectorDeArchivo_noGeneraNada() {
        generarReportePrevioValido();

        try (MockedConstruction<JFileChooser> construccionChooser = mockConstruction(JFileChooser.class,
                (mock, context) -> when(mock.showSaveDialog(any())).thenReturn(JFileChooser.CANCEL_OPTION));
             MockedStatic<ReporteVentasPDF> reportePdf = mockStatic(ReporteVentasPDF.class)) {
            reportePdf.when(() -> ReporteVentasPDF.nombreSugerido(any(), any())).thenReturn("sugerido.pdf");

            btnDescargarPDF.doClick();

            reportePdf.verify(() -> ReporteVentasPDF.generar(any(), any(), any()), never());
        }
    }

    @Test
    void descargarPDF_exitoso_generaElPdfYMuestraLaRuta() {
        generarReportePrevioValido();
        File archivoDestino = new File("estado_ventas_prueba.pdf");

        try (MockedConstruction<JFileChooser> construccionChooser = mockConstruction(JFileChooser.class,
                (mock, context) -> {
                    when(mock.showSaveDialog(any())).thenReturn(JFileChooser.APPROVE_OPTION);
                    when(mock.getSelectedFile()).thenReturn(archivoDestino);
                });
             MockedStatic<ReporteVentasPDF> reportePdf = mockStatic(ReporteVentasPDF.class);
             MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            reportePdf.when(() -> ReporteVentasPDF.nombreSugerido(any(), any())).thenReturn("sugerido.pdf");

            btnDescargarPDF.doClick();

            reportePdf.verify(() -> ReporteVentasPDF.generar(eq(archivoDestino), eq("Estado de Ventas del Emprendedor"), any()));
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vista), anyString()));
        }
    }

    @Test
    void descargarPDF_fallaLaGeneracion_muestraError() throws Exception {
        generarReportePrevioValido();
        File archivoDestino = new File("estado_ventas_prueba.pdf");

        try (MockedConstruction<JFileChooser> construccionChooser = mockConstruction(JFileChooser.class,
                (mock, context) -> {
                    when(mock.showSaveDialog(any())).thenReturn(JFileChooser.APPROVE_OPTION);
                    when(mock.getSelectedFile()).thenReturn(archivoDestino);
                });
             MockedStatic<ReporteVentasPDF> reportePdf = mockStatic(ReporteVentasPDF.class);
             MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            reportePdf.when(() -> ReporteVentasPDF.nombreSugerido(any(), any())).thenReturn("sugerido.pdf");
            reportePdf.when(() -> ReporteVentasPDF.generar(any(), any(), any()))
                    .thenThrow(new java.io.IOException("disco lleno"));

            btnDescargarPDF.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), anyString(), eq("Error"), eq(JOptionPane.ERROR_MESSAGE)));
        }
    }

    private void generarReportePrevioValido() {
        Emprendedor emp = emprendedorDePrueba(1, 100.0);
        cbEmprendedor.addItem(emp);
        cbEmprendedor.setSelectedItem(emp);
        fechaInicio.setDate(fecha(2026, 7, 1));
        fechaFin.setDate(fecha(2026, 7, 31));
        LineaReporteVenta linea = new LineaReporteVenta(1, fecha(2026, 7, 10), "JT01", "Producto",
                1, 20.0, 0.0, 20.0, true);
        when(settlementDao.obtenerDetalleVentasDelPeriodo(eq(1), any(Date.class), any(Date.class)))
                .thenReturn(List.of(linea));
        btnGenerarReporte.doClick();
    }
}
