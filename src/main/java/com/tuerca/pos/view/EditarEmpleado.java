package com.tuerca.pos.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;
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
 * Formulario "Editar Empleado". Sin `.form`: layout a mano con MigLayout,
 * mismo estilo visual que {@link EditarProducto}/{@link EditarEmprendimiento}.
 * Los getters devuelven {@code String} (antes devolvían el {@code JTextField})
 * para ser consistentes con {@link NuevoEmpleado} y con el resto de pantallas
 * ya migradas — ver {@code prepararEdicion}/{@code actualizarEmpleado} en
 * {@link com.tuerca.pos.controller.EmpleadoController}.
 *
 * El username es de solo lectura una vez creado (decisión confirmada del
 * Paso 14): se muestra como {@link JLabel}, no como campo editable.
 */
public class EditarEmpleado extends JPanel {

    private final JLabel lblTitulo = new JLabel("Editar empleado");
    private final JButton btnBack = new JButton("Volver");
    private final JLabel lblUsuario = new JLabel("Usuario: ");

    private final JTextField nombreField = new JTextField();
    private final JTextField paternoField = new JTextField();
    private final JTextField maternoField = new JTextField();
    private final JTextField numeroField = new JTextField();
    private final JComboBox<String> rolComboBox = new JComboBox<>(new String[]{"Administrador", "Vendedor"});
    private final JLabel lblUsuarioAsignado = new JLabel("Usuario asignado: —");

    private final JButton btnActualizar = new JButton("Actualizar");
    private final JButton btnCancelar = new JButton("Cancelar");

    public EditarEmpleado() {
        initComponents();

        nombreField.putClientProperty("JTextField.placeholderText", "Introduce el nombre");
        nombreField.putClientProperty("JTextField.showClearButton", true);

        paternoField.putClientProperty("JTextField.placeholderText", "Introduce el apellido paterno");
        paternoField.putClientProperty("JTextField.showClearButton", true);

        maternoField.putClientProperty("JTextField.placeholderText", "Introduce el apellido materno");
        maternoField.putClientProperty("JTextField.showClearButton", true);

        numeroField.putClientProperty("JTextField.placeholderText", "Introduce el número de contacto a 10 dígitos");
        numeroField.putClientProperty("JTextField.showClearButton", true);
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

        for (JTextField f : new JTextField[]{nombreField, paternoField, maternoField, numeroField}) {
            f.setFont(fieldFont);
        }
        rolComboBox.setFont(fieldFont);
        lblUsuarioAsignado.setFont(fieldFont);

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
        // Solo lectura: el username no se puede modificar una vez creado (decisión
        // confirmada del Paso 14, evita romper sesiones activas con ese username).
        formulario.add(lblUsuarioAsignado, "span 2, gaptop 10");

        btnActualizar.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 10; focusWidth: 0");
        btnActualizar.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Blue"));
        btnActualizar.setForeground(Color.WHITE);
        btnActualizar.setFont(new Font("SF Pro Rounded", Font.BOLD, 18));
        btnActualizar.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/check.svg", 24, 24));

        btnCancelar.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 10; focusWidth: 0");
        btnCancelar.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Red"));
        btnCancelar.setForeground(Color.WHITE);
        btnCancelar.setFont(new Font("SF Pro Rounded", Font.BOLD, 18));
        btnCancelar.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/cancel.svg", 24, 24));
        btnCancelar.addActionListener(e -> volver());

        formulario.add(btnCancelar, "split 2, span 2, growx, h 40!");
        formulario.add(btnActualizar, "growx, h 40!");

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

    // Exponemos los datos (String, consistente con NuevoEmpleado)
    public String getNombreField() { return nombreField.getText().trim(); }
    public String getPaternoField() { return paternoField.getText().trim(); }
    public String getMaternoField() { return maternoField.getText().trim(); }
    public String getNumeroField() { return numeroField.getText().trim(); }
    public JComboBox<String> getRolComboBox() { return rolComboBox; }

    public void setNombreField(String texto) { nombreField.setText(texto); }
    public void setPaternoField(String texto) { paternoField.setText(texto); }
    public void setMaternoField(String texto) { maternoField.setText(texto); }
    public void setNumeroField(String texto) { numeroField.setText(texto); }
    public void setUsuarioAsignado(String username) { lblUsuarioAsignado.setText("Usuario asignado: " + username); }

    public JButton getBtnActualizar() { return btnActualizar; }
    public JButton getBtnCancelar() { return btnCancelar; }
    public JButton getBtnBack() { return btnBack; }

    public void limpiarFormulario() {
        nombreField.setText("");
        paternoField.setText("");
        maternoField.setText("");
        numeroField.setText("");
        rolComboBox.setSelectedIndex(0);
        lblUsuarioAsignado.setText("Usuario asignado: —");
        nombreField.requestFocus();
    }
}
