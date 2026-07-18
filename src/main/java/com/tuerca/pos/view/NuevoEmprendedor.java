package com.tuerca.pos.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.toedter.calendar.JDateChooser;
import com.tuerca.pos.view.components.RelojEnVivo;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import net.miginfocom.swing.MigLayout;

/**
 * Formulario "Nuevo Emprendedor" (FN.2). Sin `.form`: layout a mano con
 * MigLayout, mismo estilo visual que las demás pantallas migradas.
 * Toda la lógica vive en {@link com.tuerca.pos.controller.EmprendedorController}.
 */
public class NuevoEmprendedor extends JPanel {

    private final JLabel lblTitulo = new JLabel("Crear un nuevo emprendimiento");
    private final JButton btnBack = new JButton("Volver");
    private final JLabel lblUsuario = new JLabel("Usuario: ");
    private final JLabel lblFechaHora = new JLabel(" ");

    private final JTextField brandNameField = new JTextField();
    private final JTextField contactNameField = new JTextField();
    private final JTextField contactPhoneField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField rentField = new JTextField();
    private final JDateChooser datePicker = new JDateChooser();

    private final JButton btnRegistrar = new JButton("Registrar");
    private final JButton btnCancelar = new JButton("Cancelar");

    public NuevoEmprendedor() {
        initComponents();

        brandNameField.putClientProperty("JTextField.placeholderText", "Introduce el nombre del emprendimiento");
        brandNameField.putClientProperty("JTextField.showClearButton", true);

        contactNameField.putClientProperty("JTextField.placeholderText", "Introduce el nombre del emprendedor");
        contactNameField.putClientProperty("JTextField.showClearButton", true);

        contactPhoneField.putClientProperty("JTextField.placeholderText", "Introduce el número de contacto a 10 digitos");
        contactPhoneField.putClientProperty("JTextField.showClearButton", true);

        emailField.putClientProperty("JTextField.placeholderText", "Introduce el correo electronico del emprendedor");
        emailField.putClientProperty("JTextField.showClearButton", true);

        rentField.putClientProperty("JTextField.placeholderText", "Introduce la renta mensual (Ej: 400)");
        rentField.putClientProperty("JTextField.showClearButton", true);

        JTextField dateEditor = (JTextField) datePicker.getDateEditor().getUiComponent();
        dateEditor.putClientProperty("JTextField.placeholderText", "Selecciona la fecha de contrato");
        dateEditor.putClientProperty("JTextField.showClearButton", true);
        dateEditor.setEditable(false);

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
                null, "Datos del Emprendimiento", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION,
                new Font("SF Pro Rounded", Font.BOLD, 18)));

        Font lblFont = new Font("SF Pro Rounded", Font.BOLD, 14);
        Font fieldFont = new Font("SF Pro Rounded", Font.PLAIN, 18);

        brandNameField.putClientProperty("FlatLaf.style", "arc: 20");
        contactNameField.putClientProperty("FlatLaf.style", "arc: 20");
        contactPhoneField.putClientProperty("FlatLaf.style", "arc: 20");
        emailField.putClientProperty("FlatLaf.style", "arc: 20");
        rentField.putClientProperty("FlatLaf.style", "arc: 20");
        datePicker.putClientProperty("FlatLaf.style", "arc: 20");

        for (JTextField f : new JTextField[]{brandNameField, contactNameField, contactPhoneField, emailField, rentField}) {
            f.setFont(fieldFont);
        }

        JLabel lbl1 = new JLabel("Nombre del emprendimiento:");
        lbl1.setFont(lblFont);
        JLabel lbl2 = new JLabel("Emprendedor:");
        lbl2.setFont(lblFont);
        JLabel lbl6 = new JLabel("Número de teléfono:");
        lbl6.setFont(lblFont);
        JLabel lbl12 = new JLabel("Correo electronico:");
        lbl12.setFont(lblFont);
        JLabel lbl10 = new JLabel("Costo de renta mensual:");
        lbl10.setFont(lblFont);
        JLabel lbl11 = new JLabel("Fecha de inicio");
        lbl11.setFont(lblFont);

        formulario.add(lbl1, "span 2");
        formulario.add(brandNameField, "span 2, growx, h 30!");
        formulario.add(lbl2, "span 2");
        formulario.add(contactNameField, "span 2, growx, h 30!");
        formulario.add(lbl6, "span 2");
        formulario.add(contactPhoneField, "span 2, growx, h 30!");
        formulario.add(lbl12, "span 2");
        formulario.add(emailField, "span 2, growx, h 30!");
        formulario.add(lbl10, "span 2");
        formulario.add(rentField, "span 2, growx, h 30!");
        formulario.add(lbl11, "span 2");
        formulario.add(datePicker, "span 2, growx, h 30!");

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
            main.showView("entrepreneur");
        }
    }

    // Exponemos los datos
    public String getBrandName() { return brandNameField.getText().trim(); }
    public String getContactName() { return contactNameField.getText().trim(); }
    public String getContactPhone() { return contactPhoneField.getText().trim(); }
    public String getEmail() { return emailField.getText().trim(); }
    public String getRent() { return rentField.getText().trim(); }
    public java.util.Date getFechaSeleccionada() { return datePicker.getDate(); }

    public JButton getBtnRegistrar() { return btnRegistrar; }
    public JButton getBtnCancelar() { return btnCancelar; }
    public JButton getBtnBack() { return btnBack; }

    public void limpiarFormulario() {
        brandNameField.setText("");
        contactPhoneField.setText("");
        contactNameField.setText("");
        emailField.setText("");
        rentField.setText("");
        datePicker.setDate(null);
        brandNameField.requestFocus();
    }
}
