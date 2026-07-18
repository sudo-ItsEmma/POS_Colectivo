package com.tuerca.pos.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.tuerca.pos.view.components.RelojEnVivo;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import net.miginfocom.swing.MigLayout;

/**
 * Formulario "Nuevo Producto" (FN.3). Sin `.form`: layout a mano con
 * MigLayout, mismo estilo visual que {@link NuevoEmprendedor}. Toda la
 * lógica vive en {@link com.tuerca.pos.controller.ProductoController}.
 */
public class NuevoProducto extends JPanel {

    private final JLabel lblTitulo = new JLabel("Crear un nuevo producto");
    private final JButton btnBack = new JButton("Volver");
    private final JLabel lblUsuario = new JLabel("Usuario: ");
    private final JLabel lblFechaHora = new JLabel(" ");

    private final JComboBox<Object> cbEmprendedor = new JComboBox<>();
    private final JTextField codigoField = new JTextField();
    private final JTextField descripcionField = new JTextField();
    private final JTextField departamentoField = new JTextField();
    private final JTextField precioField = new JTextField();
    private final JTextField stockField = new JTextField();

    private final JButton btnRegistrar = new JButton("Registrar");
    private final JButton btnCancelar = new JButton("Cancelar");

    public NuevoProducto() {
        initComponents();

        codigoField.putClientProperty("JTextField.placeholderText", "Introduce el código de producto (EJ. AA00)");
        codigoField.putClientProperty("JTextField.showClearButton", true);

        descripcionField.putClientProperty("JTextField.placeholderText", "Introduce la descripción del producto");
        descripcionField.putClientProperty("JTextField.showClearButton", true);

        departamentoField.putClientProperty("JTextField.placeholderText", "Introduce el departamento al que pertenece el producto");
        departamentoField.putClientProperty("JTextField.showClearButton", true);

        precioField.putClientProperty("JTextField.placeholderText", "Introduce el precio del producto (EJ. 35)");
        precioField.putClientProperty("JTextField.showClearButton", true);

        stockField.putClientProperty("JTextField.placeholderText", "Introduce el stock del producto (EJ. 10)");
        stockField.putClientProperty("JTextField.showClearButton", true);

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

        JPanel formulario = new JPanel(new MigLayout("insets 20 60 20 60, fill, wrap 2", "[grow][grow]"));
        formulario.putClientProperty("FlatLaf.style", "arc: 20");
        formulario.setBorder(BorderFactory.createTitledBorder(
                null, "Datos del producto", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION,
                new Font("SF Pro Rounded", Font.BOLD, 18)));

        Font lblFont = new Font("SF Pro Rounded", Font.BOLD, 14);
        Font fieldFont = new Font("SF Pro Rounded", Font.PLAIN, 18);

        cbEmprendedor.putClientProperty("FlatLaf.style", "arc: 20");
        cbEmprendedor.setFont(new Font("SF Pro Rounded", Font.PLAIN, 18));
        codigoField.putClientProperty("FlatLaf.style", "arc: 20");
        descripcionField.putClientProperty("FlatLaf.style", "arc: 20");
        departamentoField.putClientProperty("FlatLaf.style", "arc: 20");
        precioField.putClientProperty("FlatLaf.style", "arc: 20");
        stockField.putClientProperty("FlatLaf.style", "arc: 20");

        for (JTextField f : new JTextField[]{codigoField, descripcionField, departamentoField, precioField, stockField}) {
            f.setFont(fieldFont);
        }

        JLabel lbl1 = new JLabel("Selecciona el emprendimiento:");
        lbl1.setFont(lblFont);
        JLabel lbl2 = new JLabel("Código del producto:");
        lbl2.setFont(lblFont);
        JLabel lbl4 = new JLabel("Descripción del producto:");
        lbl4.setFont(lblFont);
        JLabel lbl6 = new JLabel("Departamento del producto:");
        lbl6.setFont(lblFont);
        JLabel lbl7 = new JLabel("Precio del producto:");
        lbl7.setFont(lblFont);
        JLabel lbl8 = new JLabel("Stock del producto");
        lbl8.setFont(lblFont);

        formulario.add(lbl1, "span 2");
        formulario.add(cbEmprendedor, "span 2, growx, h 30!");
        formulario.add(lbl2, "span 2");
        formulario.add(codigoField, "span 2, growx, h 30!");
        formulario.add(lbl4, "span 2");
        formulario.add(descripcionField, "span 2, growx, h 30!");
        formulario.add(lbl6, "span 2");
        formulario.add(departamentoField, "span 2, growx, h 30!");
        formulario.add(lbl7, "span 2");
        formulario.add(precioField, "span 2, growx, h 30!");
        formulario.add(lbl8, "span 2");
        formulario.add(stockField, "span 2, growx, h 30!");

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

        formulario.add(btnCancelar, "split 2, span 2, growx, h 40!");
        formulario.add(btnRegistrar, "growx, h 40!");

        add(formulario, "grow");

        lblUsuario.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));
        add(lblUsuario, "growx");

        lblFechaHora.setFont(new Font("SF Pro Rounded", Font.PLAIN, 12));
        add(lblFechaHora, "growx");
        RelojEnVivo.iniciar(lblFechaHora);
    }

    private void volver() {
        java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(this);
        if (window instanceof MainView main) {
            main.showView("products");
        }
    }

    // Exponemos los datos
    public JComboBox<Object> getCbEmprendedor() { return cbEmprendedor; }
    public String getCodigoField() { return codigoField.getText().trim(); }
    public String getDescripcionField() { return descripcionField.getText().trim(); }
    public String getDepartamentoField() { return departamentoField.getText().trim(); }
    public String getPrecioField() { return precioField.getText().trim(); }
    public String getStockField() { return stockField.getText().trim(); }

    public JButton getBtnRegistrar() { return btnRegistrar; }
    public JButton getBtnCancelar() { return btnCancelar; }
    public JButton getBtnBack() { return btnBack; }

    public void limpiarFormulario() {
        codigoField.setText("");
        descripcionField.setText("");
        departamentoField.setText("");
        precioField.setText("");
        stockField.setText("");
        if (cbEmprendedor.getItemCount() > 0) {
            cbEmprendedor.setSelectedIndex(0);
        }
        codigoField.requestFocus();
    }
}
