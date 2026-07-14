package com.tuerca.pos.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.toedter.calendar.JDateChooser;
import com.tuerca.pos.model.Sesion;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Pantalla de Pago a Emprendedores (FN.9). Sin `.form`: layout a mano con
 * MigLayout, mismo estilo visual que {@link GestionDevoluciones}. Toda la
 * lógica vive en {@link com.tuerca.pos.controller.PagoEmprendedoresController}.
 */
public class PagoEmprendedores extends JPanel {

    private final JLabel lblTitulo = new JLabel("Pago a Emprendedores");
    private final JButton btnBack = new JButton("Volver");
    private final JLabel lblUsuario = new JLabel("Usuario: ");

    private final JComboBox<Object> cbEmprendedor = new JComboBox<>();
    private final JDateChooser fechaInicio = new JDateChooser();
    private final JDateChooser fechaFin = new JDateChooser();
    private final JButton btnCalcular = new JButton("Calcular");

    private final JTable tablaVentas = new JTable();

    private final JLabel lblBruto = new JLabel("Ventas Brutas: $0.00");
    private final JLabel lblDescuentos = new JLabel("Descuentos: $0.00");
    private final JCheckBox cbIncluirRenta = new JCheckBox("Incluir renta mensual", true);
    private final JLabel lblRenta = new JLabel("Renta mensual: $0.00");
    private final JLabel lblAvisoRenta = new JLabel(" ");
    private final JLabel lblTotalNeto = new JLabel("Total Neto a Pagar: $0.00");
    private final JButton btnRegistrarPago = new JButton("Registrar Pago");

    public PagoEmprendedores() {
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

        JPanel panelFiltros = new JPanel(new MigLayout("insets 15, fillx", "[][grow][][200!][][200!][160!]"));
        panelFiltros.putClientProperty("FlatLaf.style", "arc: 20");
        panelFiltros.setBorder(BorderFactory.createTitledBorder(
                null, "Selecciona un Emprendedor y Periodo", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
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

        btnCalcular.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 10; focusWidth: 0");
        btnCalcular.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Green"));
        btnCalcular.setForeground(Color.WHITE);
        btnCalcular.setFont(new Font("SF Pro Rounded", Font.BOLD, 18));
        btnCalcular.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/calculate.svg", 24, 24));

        panelFiltros.add(lblEmprendedor);
        panelFiltros.add(cbEmprendedor, "growx, h 36!");
        panelFiltros.add(lblInicio);
        panelFiltros.add(fechaInicio, "h 36!");
        panelFiltros.add(lblFin);
        panelFiltros.add(fechaFin, "h 36!");
        panelFiltros.add(btnCalcular, "h 36!");
        add(panelFiltros, "growx");

        tablaVentas.setFont(new Font("SF Compact Rounded", Font.PLAIN, 13));
        tablaVentas.setRowHeight(35);
        tablaVentas.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"Incluir", "Ticket", "Fecha", "Bruto"}
        ) {
            final Class<?>[] tipos = {Boolean.class, Integer.class, Object.class, String.class};
            final boolean[] canEdit = {true, false, false, false};

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return tipos[columnIndex];
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        JScrollPane jScrollPane1 = new JScrollPane(tablaVentas);
        jScrollPane1.putClientProperty("FlatLaf.style", "arc: 20");
        add(jScrollPane1, "grow");

        JPanel panelResultado = new JPanel(new MigLayout("insets 15, fillx, wrap 1", "[grow]"));
        panelResultado.putClientProperty("FlatLaf.style", "arc: 20");
        panelResultado.setBorder(BorderFactory.createTitledBorder(
                null, "Cálculo de Pago", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                new Font("SF Pro Rounded", Font.BOLD, 18)));

        for (JLabel lbl : new JLabel[]{lblBruto, lblDescuentos}) {
            lbl.setFont(new Font("SF Compact Rounded", Font.BOLD, 18));
            panelResultado.add(lbl, "gaptop 5");
        }

        JPanel panelRenta = new JPanel(new MigLayout("insets 0, fillx", "[][grow]"));
        cbIncluirRenta.setFont(new Font("SF Compact Rounded", Font.PLAIN, 14));
        lblRenta.setFont(new Font("SF Compact Rounded", Font.BOLD, 18));
        panelRenta.add(cbIncluirRenta);
        panelRenta.add(lblRenta);
        panelResultado.add(panelRenta, "gaptop 5, growx");

        lblAvisoRenta.setFont(new Font("SF Compact Rounded", Font.ITALIC, 13));
        lblAvisoRenta.setForeground(new Color(200, 120, 0));
        panelResultado.add(lblAvisoRenta, "growx");

        lblTotalNeto.setFont(new Font("SF Compact Rounded", Font.BOLD, 20));
        panelResultado.add(lblTotalNeto, "gaptop 10");
        add(panelResultado, "growx");

        JPanel panelPie = new JPanel(new MigLayout("insets 0, fillx", "[grow][240!]"));
        lblUsuario.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));
        panelPie.add(lblUsuario, "growx");

        btnRegistrarPago.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 10; focusWidth: 0");
        btnRegistrarPago.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Blue"));
        btnRegistrarPago.setForeground(Color.WHITE);
        btnRegistrarPago.setFont(new Font("SF Pro Rounded", Font.BOLD, 18));
        btnRegistrarPago.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/check.svg", 24, 24));
        panelPie.add(btnRegistrarPago, "h 44!");

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

    public JButton getBtnCalcular() {
        return btnCalcular;
    }

    public JTable getTablaVentas() {
        return tablaVentas;
    }

    public JButton getBtnRegistrarPago() {
        return btnRegistrarPago;
    }

    public JButton getBtnBack() {
        return btnBack;
    }

    public JCheckBox getCbIncluirRenta() {
        return cbIncluirRenta;
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

    public void setAvisoRenta(String texto) {
        lblAvisoRenta.setText(texto == null ? " " : texto);
    }

    public void setTotalNeto(String texto) {
        lblTotalNeto.setText("Total Neto a Pagar: " + texto);
    }
}
