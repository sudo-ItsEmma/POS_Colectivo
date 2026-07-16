package com.tuerca.pos.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import net.miginfocom.swing.MigLayout;

/**
 * Formulario "Nuevo Empleado". Sin `.form`: layout a mano con MigLayout,
 * mismo estilo visual que {@link NuevoProducto}/{@link NuevoEmprendedor}. El
 * username lo genera el sistema (ver {@code EmpleadoController.generarUsername()}),
 * por eso no hay un campo para capturarlo aquí — se muestra al usuario en el
 * mensaje de éxito tras registrar.
 */
public class NuevoEmpleado extends JPanel {

    private final JLabel lblTitulo = new JLabel("Crear nuevo empleado");
    private final JButton btnBack = new JButton("Volver");
    private final JLabel lblUsuario = new JLabel("Usuario: ");

    private final JTextField nombreField = new JTextField();
    private final JTextField paternoField = new JTextField();
    private final JTextField maternoField = new JTextField();
    private final JTextField numeroField = new JTextField();
    private final JComboBox<String> rolComboBox = new JComboBox<>(new String[]{"Administrador", "Vendedor"});
    private final JPasswordField contraField = new JPasswordField();
    private final JPasswordField confirmarContraField = new JPasswordField();

    private final JButton btnRegistrar = new JButton("Registrar");
    private final JButton btnCancelar = new JButton("Cancelar");

    public NuevoEmpleado() {
        initComponents();

        nombreField.putClientProperty("JTextField.placeholderText", "Introduce el nombre");
        nombreField.putClientProperty("JTextField.showClearButton", true);

        paternoField.putClientProperty("JTextField.placeholderText", "Introduce el apellido paterno");
        paternoField.putClientProperty("JTextField.showClearButton", true);

        maternoField.putClientProperty("JTextField.placeholderText", "Introduce el apellido materno");
        maternoField.putClientProperty("JTextField.showClearButton", true);

        numeroField.putClientProperty("JTextField.placeholderText", "Introduce el número de contacto a 10 dígitos");
        numeroField.putClientProperty("JTextField.showClearButton", true);

        contraField.putClientProperty("JPasswordField.showRevealButton", true);
        confirmarContraField.putClientProperty("JPasswordField.showRevealButton", true);

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
                null, "Datos Personales", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION,
                new Font("SF Pro Rounded", Font.BOLD, 18)));

        Font lblFont = new Font("SF Pro Rounded", Font.BOLD, 14);
        Font fieldFont = new Font("SF Pro Rounded", Font.PLAIN, 18);

        nombreField.putClientProperty("FlatLaf.style", "arc: 20");
        paternoField.putClientProperty("FlatLaf.style", "arc: 20");
        maternoField.putClientProperty("FlatLaf.style", "arc: 20");
        numeroField.putClientProperty("FlatLaf.style", "arc: 20");
        rolComboBox.putClientProperty("FlatLaf.style", "arc: 20");
        contraField.putClientProperty("FlatLaf.style", "arc: 13");
        confirmarContraField.putClientProperty("FlatLaf.style", "arc: 13");

        for (JTextField f : new JTextField[]{nombreField, paternoField, maternoField, numeroField}) {
            f.setFont(fieldFont);
        }
        rolComboBox.setFont(fieldFont);
        contraField.setFont(fieldFont);
        confirmarContraField.setFont(fieldFont);

        JLabel lbl1 = new JLabel("Nombre:");
        lbl1.setFont(lblFont);
        JLabel lbl2 = new JLabel("Apellido Paterno:");
        lbl2.setFont(lblFont);
        JLabel lbl3 = new JLabel("Apellido Materno:");
        lbl3.setFont(lblFont);
        JLabel lbl4 = new JLabel("Número de teléfono:");
        lbl4.setFont(lblFont);
        JLabel lbl5 = new JLabel("Rol:");
        lbl5.setFont(lblFont);
        JLabel lbl6 = new JLabel("Contraseña:");
        lbl6.setFont(lblFont);
        JLabel lbl7 = new JLabel("Confirma tu contraseña:");
        lbl7.setFont(lblFont);

        formulario.add(lbl1, "span 2");
        formulario.add(nombreField, "span 2, growx, h 30!");
        formulario.add(lbl2, "span 2");
        formulario.add(paternoField, "span 2, growx, h 30!");
        formulario.add(lbl3, "span 2");
        formulario.add(maternoField, "span 2, growx, h 30!");
        formulario.add(lbl4, "span 2");
        formulario.add(numeroField, "span 2, growx, h 30!");
        formulario.add(lbl5, "span 2");
        formulario.add(rolComboBox, "span 2, growx, h 30!");
        formulario.add(lbl6, "span 2");
        formulario.add(contraField, "span 2, growx, h 30!");
        formulario.add(lbl7, "span 2");
        formulario.add(confirmarContraField, "span 2, growx, h 30!");

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
    }

    private void volver() {
        java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(this);
        if (window instanceof MainView main) {
            main.showView("empleados");
        }
    }

    // Exponemos los datos
    public String getNombre() { return nombreField.getText().trim(); }
    public String getPaterno() { return paternoField.getText().trim(); }
    public String getMaterno() { return maternoField.getText().trim(); }
    public String getTelefono() { return numeroField.getText().trim(); }
    public String getRol() { return rolComboBox.getSelectedItem().toString(); }
    public String getContra() { return new String(contraField.getPassword()); }
    public String getConfirmarContra() { return new String(confirmarContraField.getPassword()); }

    public JButton getBtnRegistrar() { return btnRegistrar; }
    public JButton getBtnCancelar() { return btnCancelar; }
    public JButton getBtnBack() { return btnBack; }

    public void limpiarFormulario() {
        nombreField.setText("");
        paternoField.setText("");
        maternoField.setText("");
        numeroField.setText("");
        contraField.setText("");
        confirmarContraField.setText("");
        rolComboBox.setSelectedIndex(0);
        nombreField.requestFocus();
    }
}
