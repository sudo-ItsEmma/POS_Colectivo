package com.tuerca.pos.view.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.math.BigDecimal;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Un solo diálogo con los datos del cliente (nombre, teléfono) y el monto de
 * abono inicial para crear un Apartado — reemplaza la cadena de
 * {@code JOptionPane} (confirm de datos + input de abono por separado) que
 * había antes en {@code ApartadoController.procesarApartado()}. Mismo patrón
 * de {@code JDialog} a medida ya usado en {@code DevolucionController}.
 */
public class DatosApartadoDialog extends JDialog {

    public static class Resultado {
        public final String nombreCliente;
        public final String telefonoCliente;
        public final BigDecimal montoAbono;
        public final String metodoPago;

        Resultado(String nombreCliente, String telefonoCliente, BigDecimal montoAbono, String metodoPago) {
            this.nombreCliente = nombreCliente;
            this.telefonoCliente = telefonoCliente;
            this.montoAbono = montoAbono;
            this.metodoPago = metodoPago;
        }
    }

    private final JTextField txtNombre = new JTextField(20);
    private final JTextField txtTelefono = new JTextField(15);
    private final JTextField txtMonto = new JTextField(15);
    private final JComboBox<String> cbMetodoPago = new JComboBox<>(new String[]{"Efectivo", "Transferencia"});
    private final BigDecimal totalCarrito;

    private Resultado resultado; // null si el usuario cancela

    private DatosApartadoDialog(Component parent, BigDecimal totalCarrito, BigDecimal sugerido) {
        super(SwingUtilities.getWindowAncestor(parent), "Datos del Apartado - Aura POS", ModalityType.APPLICATION_MODAL);
        this.totalCarrito = totalCarrito;
        initComponents(totalCarrito, sugerido);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    // Devuelve los datos capturados, o null si el usuario canceló el diálogo.
    public static Resultado solicitar(Component parent, BigDecimal totalCarrito, BigDecimal sugerido) {
        DatosApartadoDialog dialogo = new DatosApartadoDialog(parent, totalCarrito, sugerido);
        dialogo.setVisible(true);
        return dialogo.resultado;
    }

    private void initComponents(BigDecimal totalCarrito, BigDecimal sugerido) {
        txtNombre.putClientProperty("JTextField.placeholderText", "Nombre y Apellido");
        txtTelefono.putClientProperty("JTextField.placeholderText", "Ej. 7771234567");
        txtMonto.setText(sugerido.toPlainString());

        JPanel panelCampos = new JPanel(new GridLayout(0, 1, 5, 5));
        panelCampos.add(new JLabel("TOTAL A APARTAR: $" + String.format("%.2f", totalCarrito)));
        panelCampos.add(new JLabel("Nombre del Cliente:"));
        panelCampos.add(txtNombre);
        panelCampos.add(new JLabel("Teléfono de contacto:"));
        panelCampos.add(txtTelefono);
        panelCampos.add(new JLabel("Monto de abono inicial (sugerido 10%: $" + String.format("%.2f", sugerido) + "):"));
        panelCampos.add(txtMonto);
        panelCampos.add(new JLabel("Método de pago del abono:"));
        panelCampos.add(cbMetodoPago);

        JButton btnAceptar = new JButton("Registrar Apartado");
        JButton btnCancelar = new JButton("Cancelar");
        btnAceptar.addActionListener(e -> validarYAceptar());
        btnCancelar.addActionListener(e -> { resultado = null; dispose(); });

        JPanel panelBotones = new JPanel();
        panelBotones.add(btnCancelar);
        panelBotones.add(btnAceptar);

        JPanel contenido = new JPanel(new BorderLayout(10, 10));
        contenido.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contenido.add(panelCampos, BorderLayout.CENTER);
        contenido.add(panelBotones, BorderLayout.SOUTH);

        setContentPane(contenido);
        getRootPane().setDefaultButton(btnAceptar);
    }

    private void validarYAceptar() {
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "El nombre del cliente es obligatorio para registrar el apartado.",
                "Datos Incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            BigDecimal monto = new BigDecimal(txtMonto.getText().trim());
            if (monto.compareTo(BigDecimal.ZERO) <= 0 || monto.compareTo(totalCarrito) > 0) {
                JOptionPane.showMessageDialog(this, "Monto de abono inválido.", "Datos Incompletos", JOptionPane.WARNING_MESSAGE);
                return;
            }
            resultado = new Resultado(nombre, txtTelefono.getText().trim(), monto, (String) cbMetodoPago.getSelectedItem());
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Por favor, ingrese un monto numérico válido.", "Datos Incompletos", JOptionPane.WARNING_MESSAGE);
        }
    }
}
