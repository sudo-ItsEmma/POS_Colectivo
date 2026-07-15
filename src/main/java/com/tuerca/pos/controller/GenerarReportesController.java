package com.tuerca.pos.controller;

import com.lowagie.text.DocumentException;
import com.tuerca.pos.dao.EmprendedorDAO;
import com.tuerca.pos.dao.SettlementDAO;
import com.tuerca.pos.model.Emprendedor;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.pdf.ReporteVentasPDF;
import com.tuerca.pos.pdf.dto.LineaReporteVenta;
import com.tuerca.pos.pdf.dto.ReporteEstadoVentas;
import com.tuerca.pos.view.GenerarReportes;
import com.tuerca.pos.view.MainView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

/**
 * Controlador de Generar Reportes (FN.10). El "Estado de Ventas del
 * Emprendedor" es de solo lectura — a diferencia de Pago a Emprendedores,
 * no marca nada como liquidado ni requiere autorización de Administrador,
 * por eso muestra TODAS las ventas del periodo (pagadas o no).
 */
public class GenerarReportesController {

    private final GenerarReportes vista;
    private final SettlementDAO settlementDao;
    private final EmprendedorDAO emprendedorDao;

    private List<LineaReporteVenta> lineasCargadas = new ArrayList<>();
    private java.sql.Date periodoInicioCargado;
    private java.sql.Date periodoFinCargado;
    private double brutoCargado, descuentosCargados, rentaCargada, netoCargado;
    private java.sql.Date fechaUltimaRentaCargada;

    public GenerarReportesController(GenerarReportes vista, MainView mainView) {
        this.vista = vista;
        this.settlementDao = new SettlementDAO();
        this.emprendedorDao = new EmprendedorDAO();

        vista.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                cargarCombo();
                limpiarFormulario();
            }

            @Override
            public void componentHidden(java.awt.event.ComponentEvent e) {
                limpiarFormulario();
            }
        });

        vista.getBtnGenerarReporte().addActionListener(e -> generarPreview());
        vista.getBtnDescargarPDF().addActionListener(e -> descargarPDF());

        cargarCombo();
    }

    private void limpiarFormulario() {
        if (vista.getCbEmprendedor().getItemCount() > 0) {
            vista.getCbEmprendedor().setSelectedIndex(0);
        }
        vista.getFechaInicio().setDate(null);
        vista.getFechaFin().setDate(null);

        lineasCargadas = new ArrayList<>();
        fechaUltimaRentaCargada = null;
        ((DefaultTableModel) vista.getTablaPreview().getModel()).setRowCount(0);

        vista.setBruto("$0.00");
        vista.setDescuentos("$0.00");
        vista.setRenta("$0.00");
        vista.setTotalNeto("$0.00");
        vista.setEstadoPago(null);
        vista.setAvisoRenta(null);
    }

    private void cargarCombo() {
        vista.getCbEmprendedor().removeAllItems();
        vista.getCbEmprendedor().addItem("Selecciona un emprendedor...");
        for (Emprendedor emp : emprendedorDao.listar()) {
            vista.getCbEmprendedor().addItem(emp);
        }
    }

    private void generarPreview() {
        Emprendedor emp = emprendedorSeleccionado();
        if (emp == null) {
            JOptionPane.showMessageDialog(vista, "Selecciona un emprendedor.");
            return;
        }

        java.util.Date inicio = vista.getFechaInicio().getDate();
        java.util.Date fin = vista.getFechaFin().getDate();
        if (inicio == null || fin == null) {
            JOptionPane.showMessageDialog(vista, "Selecciona el rango de fechas.");
            return;
        }
        if (fin.before(inicio)) {
            JOptionPane.showMessageDialog(vista, "La fecha de fin no puede ser anterior a la fecha de inicio.");
            return;
        }

        periodoInicioCargado = new java.sql.Date(inicio.getTime());
        periodoFinCargado = new java.sql.Date(fin.getTime());
        lineasCargadas = settlementDao.obtenerDetalleVentasDelPeriodo(emp.getId(), periodoInicioCargado, periodoFinCargado);

        DefaultTableModel modelo = (DefaultTableModel) vista.getTablaPreview().getModel();
        modelo.setRowCount(0);
        SimpleDateFormat fmtFecha = new SimpleDateFormat("dd/MM/yyyy");

        brutoCargado = 0;
        descuentosCargados = 0;
        double montoPagado = 0;
        double montoPendiente = 0;
        for (LineaReporteVenta l : lineasCargadas) {
            modelo.addRow(new Object[]{
                l.getIdSale(),
                fmtFecha.format(l.getFecha()),
                l.getCodigo(),
                l.getDescripcion(),
                l.getCantidad(),
                "$" + String.format("%.2f", l.getPrecioUnitario()),
                l.getDescuento() > 0 ? "$" + String.format("%.2f", l.getDescuento()) : "—",
                "$" + String.format("%.2f", l.getSubtotal()),
                l.isPagado() ? "Pagado" : "Pendiente"
            });
            brutoCargado += l.getSubtotal();
            descuentosCargados += l.getDescuento();
            if (l.isPagado()) {
                montoPagado += l.getSubtotal();
            } else {
                montoPendiente += l.getSubtotal();
            }
        }

        rentaCargada = emp.getRentaMensual();
        netoCargado = brutoCargado - descuentosCargados - rentaCargada;
        fechaUltimaRentaCargada = settlementDao.obtenerFechaUltimaRentaCobradaEsteMes(emp.getId());

        vista.setBruto("$" + String.format("%.2f", brutoCargado));
        vista.setDescuentos("$" + String.format("%.2f", descuentosCargados));
        vista.setRenta("$" + String.format("%.2f", rentaCargada));
        vista.setTotalNeto("$" + String.format("%.2f", netoCargado));
        vista.setEstadoPago("Ya pagado: $" + String.format("%.2f", montoPagado)
                + "   |   Pendiente de pagar: $" + String.format("%.2f", montoPendiente));
        vista.setAvisoRenta(fechaUltimaRentaCargada != null
                ? "✓ Renta ya cobrada este mes el " + fmtFecha.format(fechaUltimaRentaCargada)
                : "⚠ Renta pendiente de cobrar este mes");

        if (lineasCargadas.isEmpty()) {
            JOptionPane.showMessageDialog(vista,
                    "No hay ventas para este emprendedor en el rango seleccionado.");
        }
    }

    private void descargarPDF() {
        Emprendedor emp = emprendedorSeleccionado();
        if (emp == null) {
            JOptionPane.showMessageDialog(vista, "Selecciona un emprendedor.");
            return;
        }
        if (lineasCargadas.isEmpty() || periodoInicioCargado == null || periodoFinCargado == null) {
            JOptionPane.showMessageDialog(vista, "Primero genera el reporte para poder descargarlo.");
            return;
        }

        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar Estado de Ventas");
        selector.setFileFilter(new FileNameExtensionFilter("Documento PDF (.pdf)", "pdf"));
        selector.setSelectedFile(new File(ReporteVentasPDF.nombreSugerido("EstadoVentas", emp.getMarca())));

        if (selector.showSaveDialog(vista) != JFileChooser.APPROVE_OPTION) return;

        File destino = selector.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".pdf")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".pdf");
        }

        ReporteEstadoVentas datos = new ReporteEstadoVentas(
                emp.getMarca(), periodoInicioCargado, periodoFinCargado, lineasCargadas,
                brutoCargado, descuentosCargados, rentaCargada, netoCargado, Sesion.getInstancia().getNombreCompleto(),
                fechaUltimaRentaCargada != null, fechaUltimaRentaCargada);

        try {
            ReporteVentasPDF.generar(destino, "Estado de Ventas del Emprendedor", datos);
            JOptionPane.showMessageDialog(vista, "Reporte guardado en:\n" + destino.getAbsolutePath());
        } catch (DocumentException | IOException e) {
            JOptionPane.showMessageDialog(vista,
                    "No se pudo generar el PDF:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Emprendedor emprendedorSeleccionado() {
        Object seleccion = vista.getCbEmprendedor().getSelectedItem();
        return (seleccion instanceof Emprendedor emp) ? emp : null;
    }
}
