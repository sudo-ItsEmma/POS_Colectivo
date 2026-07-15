package com.tuerca.pos.controller;

import com.lowagie.text.DocumentException;
import com.tuerca.pos.dao.EmpleadoDAO;
import com.tuerca.pos.dao.EmprendedorDAO;
import com.tuerca.pos.dao.SettlementDAO;
import com.tuerca.pos.model.Emprendedor;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.model.Settlement;
import com.tuerca.pos.pdf.ReporteVentasPDF;
import com.tuerca.pos.pdf.dto.LineaReporteVenta;
import com.tuerca.pos.pdf.dto.ReporteEstadoVentas;
import com.tuerca.pos.view.MainView;
import com.tuerca.pos.view.PagoEmprendedores;
import com.tuerca.pos.view.components.AutorizacionAdminDialog;

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
 * Controlador de Pago a Emprendedores (FN.9). Igual que Devoluciones: si la
 * sesión activa no es de Admin, se pide usuario/contraseña de un
 * Administrador antes de registrar el pago.
 */
public class PagoEmprendedoresController {

    private final PagoEmprendedores vista;
    private final SettlementDAO settlementDao;
    private final EmprendedorDAO emprendedorDao;
    private final EmpleadoDAO empleadoDao;

    // Mismo orden que las filas de la tabla: idSale, saleDateTime, bruto, descuentos
    private List<Object[]> ventasCargadas = new ArrayList<>();

    // mainView no se usa directamente aquí (la navegación de "Volver" vive en la propia vista,
    // igual que GestionApartados/GestionEmprendedores), pero se mantiene en la firma del
    // constructor por consistencia con el resto de los controllers.
    public PagoEmprendedoresController(PagoEmprendedores vista, MainView mainView) {
        this.vista = vista;
        this.settlementDao = new SettlementDAO();
        this.emprendedorDao = new EmprendedorDAO();
        this.empleadoDao = new EmpleadoDAO();

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

        vista.getBtnCalcular().addActionListener(e -> calcular());
        vista.getBtnRegistrarPago().addActionListener(e -> registrarPago());
        vista.getTablaVentas().getModel().addTableModelListener(evt -> {
            if (evt.getColumn() == 0) {
                recalcularTotales();
            }
        });
        vista.getCbIncluirRenta().addItemListener(e -> recalcularTotales());

        cargarCombo();
    }

    // Limpia la última consulta calculada: sin esto, al volver a esta pantalla se seguía
    // viendo la tabla/totales de la consulta anterior en vez de un formulario en blanco.
    private void limpiarFormulario() {
        if (vista.getCbEmprendedor().getItemCount() > 0) {
            vista.getCbEmprendedor().setSelectedIndex(0);
        }
        vista.getFechaInicio().setDate(null);
        vista.getFechaFin().setDate(null);

        ventasCargadas = new ArrayList<>();
        ((DefaultTableModel) vista.getTablaVentas().getModel()).setRowCount(0);

        vista.getCbIncluirRenta().setSelected(true);
        vista.setAvisoRenta(null);
        vista.setBruto("$0.00");
        vista.setDescuentos("$0.00");
        vista.setRenta("$0.00");
        vista.setTotalNeto("$0.00");
    }

    private void cargarCombo() {
        vista.getCbEmprendedor().removeAllItems();
        vista.getCbEmprendedor().addItem("Selecciona un emprendedor...");
        for (Emprendedor emp : emprendedorDao.listar()) {
            vista.getCbEmprendedor().addItem(emp);
        }
    }

    private void calcular() {
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

        ventasCargadas = settlementDao.listarVentasPendientes(
                emp.getId(), new java.sql.Date(inicio.getTime()), new java.sql.Date(fin.getTime()));

        DefaultTableModel modelo = (DefaultTableModel) vista.getTablaVentas().getModel();
        modelo.setRowCount(0);
        for (Object[] v : ventasCargadas) {
            modelo.addRow(new Object[]{
                Boolean.TRUE,
                v[0],
                v[1],
                "$" + String.format("%.2f", (double) v[2])
            });
        }

        // Si ya se le cobró la renta este mes calendario, la casilla arranca desmarcada (el
        // usuario puede forzarla manualmente si de verdad quiere volver a cobrarla).
        java.sql.Date ultimaRenta = settlementDao.obtenerFechaUltimaRentaCobradaEsteMes(emp.getId());
        if (ultimaRenta != null) {
            vista.getCbIncluirRenta().setSelected(false);
            vista.setAvisoRenta("⚠ Ya se cobró la renta de este mes el "
                    + new SimpleDateFormat("dd/MM/yyyy").format(ultimaRenta) + ".");
        } else {
            vista.getCbIncluirRenta().setSelected(true);
            vista.setAvisoRenta(null);
        }

        recalcularTotales();

        if (ventasCargadas.isEmpty()) {
            JOptionPane.showMessageDialog(vista,
                    "No hay ventas pendientes de pago para este emprendedor en el rango seleccionado.");
        }
    }

    // Recalcula los 4 renglones en vivo según los tickets que el usuario deje marcados.
    private void recalcularTotales() {
        DefaultTableModel modelo = (DefaultTableModel) vista.getTablaVentas().getModel();
        double bruto = 0, descuentos = 0;

        for (int i = 0; i < modelo.getRowCount() && i < ventasCargadas.size(); i++) {
            if (Boolean.TRUE.equals(modelo.getValueAt(i, 0))) {
                bruto += (double) ventasCargadas.get(i)[2];
                descuentos += (double) ventasCargadas.get(i)[3];
            }
        }

        Emprendedor emp = emprendedorSeleccionado();
        double renta = (emp != null && vista.getCbIncluirRenta().isSelected()) ? emp.getRentaMensual() : 0;
        double neto = bruto - descuentos - renta;

        vista.setBruto("$" + String.format("%.2f", bruto));
        vista.setDescuentos("$" + String.format("%.2f", descuentos));
        vista.setRenta("$" + String.format("%.2f", renta));
        vista.setTotalNeto("$" + String.format("%.2f", neto));
    }

    private void registrarPago() {
        Emprendedor emp = emprendedorSeleccionado();
        if (emp == null) {
            JOptionPane.showMessageDialog(vista, "Selecciona un emprendedor.");
            return;
        }

        DefaultTableModel modelo = (DefaultTableModel) vista.getTablaVentas().getModel();
        List<Integer> seleccionados = new ArrayList<>();
        for (int i = 0; i < modelo.getRowCount(); i++) {
            if (Boolean.TRUE.equals(modelo.getValueAt(i, 0))) {
                seleccionados.add((int) modelo.getValueAt(i, 1));
            }
        }

        if (seleccionados.isEmpty()) {
            JOptionPane.showMessageDialog(vista, "Selecciona al menos un ticket para registrar el pago.");
            return;
        }

        java.util.Date inicio = vista.getFechaInicio().getDate();
        java.util.Date fin = vista.getFechaFin().getDate();
        if (inicio == null || fin == null) {
            JOptionPane.showMessageDialog(vista, "Selecciona el rango de fechas.");
            return;
        }

        int confirmar = JOptionPane.showConfirmDialog(
            vista,
            "¿Registrar el pago a " + emp.getMarca() + " por los " + seleccionados.size() + " ticket(s) seleccionados?\n" +
            "Esta acción no se puede deshacer.",
            "Confirmar Pago", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
        );
        if (confirmar != JOptionPane.YES_OPTION) return;

        int idUserAccountAutoriza;
        if (Sesion.getInstancia().isAdmin()) {
            idUserAccountAutoriza = Sesion.getInstancia().getIdUserAccount();
        } else {
            Integer idAdmin = AutorizacionAdminDialog.solicitar(vista, empleadoDao);
            if (idAdmin == null) return;
            idUserAccountAutoriza = idAdmin;
        }

        Settlement settlement = new Settlement();
        settlement.setIdEntrepreneur(emp.getId());
        settlement.setIdUserAccount(idUserAccountAutoriza);
        settlement.setPeriodStartDate(new java.sql.Date(inicio.getTime()));
        settlement.setPeriodEndDate(new java.sql.Date(fin.getTime()));
        settlement.setRentDiscount(vista.getCbIncluirRenta().isSelected() ? emp.getRentaMensual() : 0);
        settlement.setOtherDiscounts(0);

        if (settlementDao.registrarPago(settlement, seleccionados)) {
            JOptionPane.showMessageDialog(vista,
                    "Pago registrado con éxito.\nTotal Neto Pagado: $" + String.format("%.2f", settlement.getNetAmountPaid()));

            // Se consulta por idSettlement (no por la selección en pantalla): es la fuente de
            // verdad de qué líneas quedaron marcadas exactamente por este pago.
            List<LineaReporteVenta> detallesProductos = settlementDao.obtenerDetallesDelPago(settlement.getIdSettlement());
            generarComprobantePDF(emp, settlement, detallesProductos);

            calcular(); // refresca: los tickets ya pagados no deberían volver a aparecer
        } else {
            JOptionPane.showMessageDialog(vista, "No se pudo registrar el pago.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Comprobante de pago en PDF para entregarle al emprendedor (fechas, productos incluidos,
    // desglose y total neto). Es opcional — el pago ya quedó registrado aunque el usuario
    // cancele el diálogo de guardado.
    private void generarComprobantePDF(Emprendedor emp, Settlement settlement, List<LineaReporteVenta> detallesProductos) {
        int confirmar = JOptionPane.showConfirmDialog(
            vista,
            "¿Deseas generar el comprobante en PDF para entregárselo al emprendedor?",
            "Comprobante de Pago", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE
        );
        if (confirmar != JOptionPane.YES_OPTION) return;

        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar comprobante de pago");
        selector.setFileFilter(new FileNameExtensionFilter("Documento PDF (.pdf)", "pdf"));
        selector.setSelectedFile(new File(ReporteVentasPDF.nombreSugerido("Liquidacion", emp.getMarca())));

        if (selector.showSaveDialog(vista) != JFileChooser.APPROVE_OPTION) return;

        File destino = selector.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".pdf")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".pdf");
        }

        // Ya se registró el pago justo antes de llegar aquí, así que si esta liquidación
        // incluyó renta, la consulta ya la encuentra (es la fuente de verdad, no se asume).
        java.sql.Date fechaUltimaRenta = settlementDao.obtenerFechaUltimaRentaCobradaEsteMes(emp.getId());
        ReporteEstadoVentas datos = new ReporteEstadoVentas(
                emp.getMarca(), settlement.getPeriodStartDate(), settlement.getPeriodEndDate(), detallesProductos,
                settlement.getGrossAmount(), settlement.getTotalDiscounts(), settlement.getRentDiscount(),
                settlement.getNetAmountPaid(), Sesion.getInstancia().getNombreCompleto(),
                fechaUltimaRenta != null, fechaUltimaRenta);

        try {
            ReporteVentasPDF.generar(destino, "Comprobante de Pago a Emprendedor", datos);
            JOptionPane.showMessageDialog(vista, "Comprobante guardado en:\n" + destino.getAbsolutePath());
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
