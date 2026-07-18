package com.tuerca.pos.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.toedter.calendar.JDateChooser;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.view.components.RelojEnVivo;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Pantalla de Generar Reportes (FN.10). Sin `.form`: layout a mano con
 * MigLayout, mismo estilo visual que {@link PagoEmprendedores}. Toda la
 * lógica vive en {@link com.tuerca.pos.controller.GenerarReportesController}.
 */
public class GenerarReportes extends JPanel {

    private final JLabel lblTitulo = new JLabel("Generar Reportes");
    private final JButton btnBack = new JButton("Volver");
    private final JLabel lblUsuario = new JLabel("Usuario: ");
    private final JLabel lblFechaHora = new JLabel(" ");

    private final JComboBox<Object> cbEmprendedor = new JComboBox<>();
    private final JDateChooser fechaInicio = new JDateChooser();
    private final JDateChooser fechaFin = new JDateChooser();
    private final JButton btnGenerarReporte = new JButton("Generar Reporte");

    private final JTable tablaPreview = new JTable();

    private final JLabel lblBruto = new JLabel("Ventas Brutas: $0.00");
    private final JLabel lblDescuentos = new JLabel("Descuentos: $0.00");
    private final JLabel lblRenta = new JLabel("Renta mensual: $0.00");
    private final JLabel lblTotalNeto = new JLabel("Total Neto: $0.00");
    private final JLabel lblEstadoPago = new JLabel(" ");
    private final JLabel lblAvisoRenta = new JLabel(" ");
    private final JButton btnDescargarPDF = new JButton("Descargar PDF");

    public GenerarReportes() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new MigLayout("insets 20, fill, wrap 1", "[grow]", "[][][grow][][]"));

        lblTitulo.setFont(new Font("SF Pro Rounded", Font.BOLD, 28));
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);

        btnBack.putClientProperty("FlatLaf.style", "arc: 13; iconTextGap: 10; focusWidth: 0");
        btnBack.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/back.svg", 24, 24));
        btnBack.addActionListener(e -> btnBackActionPerformed());

        JPanel panelSuperior = new JPanel(new MigLayout("insets 0, fillx", "[][grow][]"));
        panelSuperior.add(btnBack);
        panelSuperior.add(lblTitulo, "growx, align center");
        add(panelSuperior, "growx");

        JPanel panelFiltros = new JPanel(new MigLayout("insets 15, fillx", "[][grow][][200!][][200!][220!]"));
        panelFiltros.putClientProperty("FlatLaf.style", "arc: 20");
        panelFiltros.setBorder(BorderFactory.createTitledBorder(
                null, "Configuración de Reporte", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                new Font("SF Pro Rounded", Font.BOLD, 18)));

        Font lblFont = new Font("SF Pro Rounded", Font.BOLD, 14);
        JLabel lblEmprendedor = new JLabel("Emprendedor:");
        lblEmprendedor.setFont(lblFont);
        JLabel lblInicio = new JLabel("Inicio:");
        lblInicio.setFont(lblFont);
        JLabel lblFin = new JLabel("Fin:");
        lblFin.setFont(lblFont);

        cbEmprendedor.putClientProperty("FlatLaf.style", "arc: 20");
        cbEmprendedor.setFont(new Font("SF Pro Rounded", Font.PLAIN, 16));
        fechaInicio.putClientProperty("FlatLaf.style", "arc: 20");
        fechaFin.putClientProperty("FlatLaf.style", "arc: 20");

        btnGenerarReporte.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 10; focusWidth: 0");
        btnGenerarReporte.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Green"));
        btnGenerarReporte.setForeground(Color.WHITE);
        btnGenerarReporte.setFont(new Font("SF Pro Rounded", Font.BOLD, 18));
        btnGenerarReporte.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/create.svg", 24, 24));

        panelFiltros.add(lblEmprendedor);
        panelFiltros.add(cbEmprendedor, "growx, h 36!");
        panelFiltros.add(lblInicio);
        panelFiltros.add(fechaInicio, "h 36!");
        panelFiltros.add(lblFin);
        panelFiltros.add(fechaFin, "h 36!");
        panelFiltros.add(btnGenerarReporte, "h 36!");
        add(panelFiltros, "growx");

        tablaPreview.setFont(new Font("SF Compact Rounded", Font.PLAIN, 12));
        tablaPreview.setRowHeight(30);
        tablaPreview.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"Ticket", "Fecha", "Código", "Descripción", "Cant.", "Precio U.", "Descuento", "Subtotal", "Estado"}
        ) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        });
        // Ticket/Fecha/Código/Cant./Precio U./Descuento/Subtotal/Estado se ven mejor centrados
        // que a la izquierda — Descripción se deja alineada a la izquierda por ser texto largo.
        DefaultTableCellRenderer renderCentrado = new DefaultTableCellRenderer();
        renderCentrado.setHorizontalAlignment(SwingConstants.CENTER);
        for (int col = 0; col < tablaPreview.getColumnCount(); col++) {
            if (col != 3) {
                tablaPreview.getColumnModel().getColumn(col).setCellRenderer(renderCentrado);
            }
        }
        JScrollPane jScrollPane1 = new JScrollPane(tablaPreview);
        jScrollPane1.putClientProperty("FlatLaf.style", "arc: 20");
        jScrollPane1.setBorder(BorderFactory.createTitledBorder(
                null, "Vista Previa del Documento", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                new Font("SF Pro Rounded", Font.BOLD, 18)));
        add(jScrollPane1, "grow");

        JPanel panelResultado = new JPanel(new MigLayout("insets 15, fillx, wrap 4", "[][][][grow]"));
        panelResultado.putClientProperty("FlatLaf.style", "arc: 20");

        Font fuenteResultado = new Font("SF Compact Rounded", Font.BOLD, 16);
        for (JLabel lbl : new JLabel[]{lblBruto, lblDescuentos, lblRenta}) {
            lbl.setFont(fuenteResultado);
            panelResultado.add(lbl);
        }
        lblTotalNeto.setFont(new Font("SF Compact Rounded", Font.BOLD, 18));
        panelResultado.add(lblTotalNeto, "growx");

        Color colorAviso = new Color(200, 120, 0);
        lblEstadoPago.setFont(new Font("SF Compact Rounded", Font.PLAIN, 13));
        panelResultado.add(lblEstadoPago, "span 4, growx, gaptop 8");
        lblAvisoRenta.setFont(new Font("SF Compact Rounded", Font.ITALIC, 13));
        lblAvisoRenta.setForeground(colorAviso);
        panelResultado.add(lblAvisoRenta, "span 4, growx");

        add(panelResultado, "growx");

        JPanel panelPie = new JPanel(new MigLayout("insets 0, fillx", "[grow][240!]"));
        lblUsuario.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));
        lblFechaHora.setFont(new Font("SF Pro Rounded", Font.PLAIN, 12));
        JPanel panelUsuarioInfo = new JPanel(new MigLayout("insets 0, wrap 1", "[grow]"));
        panelUsuarioInfo.add(lblUsuario, "growx");
        panelUsuarioInfo.add(lblFechaHora, "growx");
        RelojEnVivo.iniciar(lblFechaHora);
        panelPie.add(panelUsuarioInfo, "growx");

        btnDescargarPDF.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 10; focusWidth: 0");
        btnDescargarPDF.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Blue"));
        btnDescargarPDF.setForeground(Color.WHITE);
        btnDescargarPDF.setFont(new Font("SF Pro Rounded", Font.BOLD, 18));
        btnDescargarPDF.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/download.svg", 24, 24));
        panelPie.add(btnDescargarPDF, "h 44!");

        add(panelPie, "growx");
    }

    private void btnBackActionPerformed() {
        java.awt.Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof MainView main) {
            main.showView(Sesion.getInstancia().isAdmin() ? "admin" : "employee");
        }
    }

    public JComboBox<Object> getCbEmprendedor() {
        return cbEmprendedor;
    }

    public JDateChooser getFechaInicio() {
        return fechaInicio;
    }

    public JDateChooser getFechaFin() {
        return fechaFin;
    }

    public JButton getBtnGenerarReporte() {
        return btnGenerarReporte;
    }

    public JTable getTablaPreview() {
        return tablaPreview;
    }

    public JButton getBtnDescargarPDF() {
        return btnDescargarPDF;
    }

    public JButton getBtnBack() {
        return btnBack;
    }

    public void setNombreUsuarioActivo(String texto) {
        lblUsuario.setText(texto);
    }

    public void setBruto(String texto) {
        lblBruto.setText("Ventas Brutas: " + texto);
    }

    public void setDescuentos(String texto) {
        lblDescuentos.setText("Descuentos: " + texto);
    }

    public void setRenta(String texto) {
        lblRenta.setText("Renta mensual: " + texto);
    }

    public void setTotalNeto(String texto) {
        lblTotalNeto.setText("Total Neto: " + texto);
    }

    public void setEstadoPago(String texto) {
        lblEstadoPago.setText(texto == null ? " " : texto);
    }

    public void setAvisoRenta(String texto) {
        lblAvisoRenta.setText(texto == null ? " " : texto);
    }
}
