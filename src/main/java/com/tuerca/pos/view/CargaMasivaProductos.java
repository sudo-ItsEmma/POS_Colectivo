package com.tuerca.pos.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;
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
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Pantalla de "Carga masiva de productos" (FN.3, importación desde Excel).
 * Sin `.form`: layout a mano con MigLayout, mismo estilo visual que
 * {@link NuevoProducto}. Toda la lógica vive en
 * {@link com.tuerca.pos.controller.ProductoController}.
 */
public class CargaMasivaProductos extends JPanel {

    private final JLabel lblTitulo = new JLabel("Carga masiva de productos");
    private final JButton btnBack = new JButton("Volver");
    private final JLabel lblUsuario = new JLabel("Usuario: ");

    private final JComboBox<Object> cbEmprendedor = new JComboBox<>();
    private final JButton btnSeleccionarArchivo = new JButton("Seleccionar archivo");
    private final JLabel lblNombreArchivo = new JLabel("Ningún archivo seleccionado");
    private final JButton btnVisualizar = new JButton("Visualizar productos");
    private final JTable vistaProductos = new JTable();

    private final JButton btnRegistrar = new JButton("Cargar productos");
    private final JButton btnCancelar = new JButton("Cancelar");

    public CargaMasivaProductos() {
        initComponents();
        limpiarFormulario();
    }

    private void initComponents() {
        setLayout(new MigLayout("insets 20, fill, wrap 1", "[grow]", "[][grow][]"));

        lblTitulo.setFont(new Font("SF Pro Rounded", Font.BOLD, 28));
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);

        btnBack.putClientProperty("FlatLaf.style", "arc: 13; iconTextGap: 10; focusWidth: 0");
        btnBack.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/back.svg", 24, 24));
        btnBack.addActionListener(e -> volver());

        JPanel panelSuperior = new JPanel(new MigLayout("insets 0, fillx", "[][grow][]"));
        panelSuperior.add(btnBack);
        panelSuperior.add(lblTitulo, "growx, align center");
        add(panelSuperior, "growx");

        JPanel formulario = new JPanel(new MigLayout("insets 20, fill, wrap 1", "[grow]"));
        formulario.putClientProperty("FlatLaf.style", "arc: 20");
        formulario.setBorder(BorderFactory.createTitledBorder(
                null, "Datos del producto", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION,
                new Font("SF Pro Rounded", Font.BOLD, 18)));

        JLabel lbl2 = new JLabel("Selecciona el emprendimiento:");
        lbl2.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));

        cbEmprendedor.putClientProperty("FlatLaf.style", "arc: 20");
        cbEmprendedor.setFont(new Font("SF Pro Rounded", Font.PLAIN, 18));

        btnSeleccionarArchivo.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 10; focusWidth: 0");
        btnSeleccionarArchivo.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Blue"));
        btnSeleccionarArchivo.setForeground(Color.WHITE);
        btnSeleccionarArchivo.setFont(new Font("SF Compact Rounded", Font.PLAIN, 18));
        btnSeleccionarArchivo.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/upload.svg", 24, 24));

        lblNombreArchivo.setFont(new Font("SF Pro Rounded", Font.PLAIN, 24));

        btnVisualizar.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 10; focusWidth: 0");
        btnVisualizar.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Yellow"));
        btnVisualizar.setForeground(Color.WHITE);
        btnVisualizar.setFont(new Font("SF Compact Rounded", Font.PLAIN, 18));
        btnVisualizar.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/create.svg", 24, 24));

        JPanel panelArchivo = new JPanel(new MigLayout("insets 0, fillx", "[240!][grow][240!]"));
        panelArchivo.add(btnSeleccionarArchivo, "h 40!");
        panelArchivo.add(lblNombreArchivo);
        panelArchivo.add(btnVisualizar, "h 40!");

        vistaProductos.setFont(new Font("SF Pro Rounded", Font.PLAIN, 18));
        vistaProductos.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"Código", "Descripción", "Precio", "Stock", "Departamento"}
        ));
        JScrollPane jScrollPane1 = new JScrollPane(vistaProductos);

        btnRegistrar.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 10; focusWidth: 0");
        btnRegistrar.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Green"));
        btnRegistrar.setForeground(Color.WHITE);
        btnRegistrar.setFont(new Font("SF Pro Rounded", Font.BOLD, 18));
        btnRegistrar.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/check.svg", 24, 24));

        btnCancelar.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 10; focusWidth: 0");
        btnCancelar.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Red"));
        btnCancelar.setForeground(Color.WHITE);
        btnCancelar.setFont(new Font("SF Pro Rounded", Font.BOLD, 18));
        btnCancelar.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/cancel.svg", 24, 24));
        btnCancelar.addActionListener(e -> volver());

        formulario.add(lbl2, "growx");
        formulario.add(cbEmprendedor, "growx, h 30!");
        formulario.add(panelArchivo, "growx, gaptop 10");
        formulario.add(jScrollPane1, "grow, push, gaptop 10");
        JPanel panelBotones = new JPanel(new MigLayout("insets 0, fillx", "[240!][grow][240!]"));
        panelBotones.add(btnCancelar, "h 40!");
        panelBotones.add(new JLabel());
        panelBotones.add(btnRegistrar, "h 40!");
        formulario.add(panelBotones, "growx, gaptop 10");

        add(formulario, "grow");

        lblUsuario.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));
        add(lblUsuario, "growx");
    }

    private void volver() {
        java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(this);
        if (window instanceof MainView main) {
            main.showView("products");
        }
    }

    // Exponemos los datos
    public JComboBox<Object> getCbEmprendedor() { return cbEmprendedor; }

    public JButton getBtnRegistrar() { return btnRegistrar; }
    public JButton getBtnSeleccionarArchivo() { return btnSeleccionarArchivo; }
    public JButton getBtnVisualizar() { return btnVisualizar; }
    public JLabel getLblNombreArchivo() { return lblNombreArchivo; }
    public JButton getBtnCancelar() { return btnCancelar; }
    public JButton getBtnBack() { return btnBack; }

    public void limpiarFormulario() {
        if (cbEmprendedor.getItemCount() > 0) {
            cbEmprendedor.setSelectedIndex(0);
        }
        cbEmprendedor.requestFocus();
    }

    public DefaultTableModel getTableModel() {
        return (DefaultTableModel) vistaProductos.getModel();
    }

    public JTable getVistaTablaProductos() {
        return vistaProductos;
    }
}
