package com.tuerca.pos.view;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import net.miginfocom.swing.MigLayout;

/**
 * Pantalla de inicio de sesión. Sin `.form`: layout hecho a mano con
 * MigLayout. La autenticación real vive en {@link com.tuerca.pos.controller.LoginController}.
 */
public class LoginPanel extends JPanel {

    private final JLabel lblTitulo = new JLabel("POS de Venta");
    private final JLabel lblSubtitulo = new JLabel("Aura Tienda Colectiva");
    private final JPanel cardInicio = new JPanel();
    private final JLabel lblUsuario = new JLabel("Usuario:");
    private final JTextField userField = new JTextField();
    private final JLabel lblContrasena = new JLabel("Contraseña:");
    private final JPasswordField contraField = new JPasswordField();
    private final JButton btnIniciarSesion = new JButton("Iniciar sesión");

    public LoginPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new MigLayout("insets 60, fillx, wrap 1", "[grow, center]"));

        lblTitulo.setFont(new Font("SF Pro Rounded", Font.BOLD, 28));
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);

        lblSubtitulo.setFont(new Font("SF Pro Rounded", Font.BOLD, 28));
        lblSubtitulo.setHorizontalAlignment(SwingConstants.CENTER);

        add(lblTitulo, "growx");
        add(lblSubtitulo, "growx, gapbottom 40");

        cardInicio.putClientProperty("FlatLaf.style", "arc: 20");
        cardInicio.setBorder(BorderFactory.createTitledBorder(
                null, "Inicio de sesión", TitledBorder.CENTER, TitledBorder.TOP,
                new Font("SF Pro Rounded", Font.BOLD, 18)));
        cardInicio.setLayout(new MigLayout("insets 25 30 25 30, fillx, wrap 1", "[grow]"));

        lblUsuario.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));
        lblContrasena.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));

        userField.putClientProperty("FlatLaf.style", "arc: 13");

        contraField.putClientProperty("FlatLaf.style", "arc: 13");
        contraField.putClientProperty("JPasswordField.showRevealButton", true);

        btnIniciarSesion.putClientProperty("FlatLaf.style", "arc: 20");
        btnIniciarSesion.setBackground(UIManager.getDefaults().getColor("Actions.Green"));
        btnIniciarSesion.setForeground(Color.WHITE);
        btnIniciarSesion.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));

        cardInicio.add(lblUsuario);
        cardInicio.add(userField, "growx, h 30!");
        cardInicio.add(lblContrasena, "gaptop 20");
        cardInicio.add(contraField, "growx, h 30!");
        cardInicio.add(btnIniciarSesion, "align right, gaptop 25");

        add(cardInicio, "growx, width 500:500:600");
    }

    public String getUsuario() {
        return userField.getText().trim();
    }

    public char[] getContrasena() {
        return contraField.getPassword();
    }

    public void limpiarContrasena() {
        contraField.setText("");
    }

    public void limpiarFormulario() {
        userField.setText("");
        contraField.setText("");
    }

    public JTextField getUserField() {
        return userField;
    }

    public JPasswordField getContraField() {
        return contraField;
    }

    public JButton getBtnIniciarSesion() {
        return btnIniciarSesion;
    }
}
