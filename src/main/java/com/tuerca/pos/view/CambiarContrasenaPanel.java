package com.tuerca.pos.view;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import net.miginfocom.swing.MigLayout;

/**
 * Pantalla forzada de "Cambiar Contraseña" (Paso 18). Se muestra tras el
 * login cuando la cuenta tiene {@code mustChangePassword} activo (un
 * Administrador le restableció la contraseña desde Editar Empleado). Sin
 * `.form`: layout a mano con MigLayout, mismo estilo que
 * {@link AperturaCajaPanel}. No tiene botón de "Volver" ni se puede saltar —
 * hay que completarla para continuar con el flujo normal de login.
 */
public class CambiarContrasenaPanel extends JPanel {

    private final JLabel lblTitulo = new JLabel("Cambiar Contraseña");
    private final JLabel lblBienvenida = new JLabel(" ");
    private final JPanel cardCambio = new JPanel();
    private final JLabel lblMensaje = new JLabel(
            "<html><div style='text-align: center;'>Por seguridad, debes establecer una nueva "
            + "contraseña antes de continuar.</div></html>");
    private final JLabel lblNuevaTitulo = new JLabel("Nueva contraseña:");
    private final JPasswordField campoNueva = new JPasswordField();
    private final JLabel lblConfirmarTitulo = new JLabel("Confirma la nueva contraseña:");
    private final JPasswordField campoConfirmar = new JPasswordField();
    private final JButton btnCambiar = new JButton("Cambiar Contraseña");

    public CambiarContrasenaPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new MigLayout("insets 60, fillx, wrap 1", "[grow, center]"));

        lblTitulo.setFont(new Font("SF Pro Rounded", Font.BOLD, 28));
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);

        lblBienvenida.setFont(new Font("SF Pro Rounded", Font.PLAIN, 16));
        lblBienvenida.setHorizontalAlignment(SwingConstants.CENTER);

        add(lblTitulo, "growx");
        add(lblBienvenida, "growx, gapbottom 40");

        cardCambio.putClientProperty("FlatLaf.style", "arc: 20");
        cardCambio.setBorder(BorderFactory.createTitledBorder(
                null, "Nueva contraseña", TitledBorder.CENTER, TitledBorder.TOP,
                new Font("SF Pro Rounded", Font.BOLD, 18)));
        cardCambio.setLayout(new MigLayout("insets 25 30 25 30, fillx, wrap 1", "[grow, center]"));

        lblMensaje.setFont(new Font("SF Pro Rounded", Font.PLAIN, 14));
        lblMensaje.setHorizontalAlignment(SwingConstants.CENTER);

        lblNuevaTitulo.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));
        lblConfirmarTitulo.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));

        campoNueva.putClientProperty("FlatLaf.style", "arc: 13");
        campoNueva.putClientProperty("JPasswordField.showRevealButton", true);
        campoNueva.setFont(new Font("SF Pro Rounded", Font.PLAIN, 18));

        campoConfirmar.putClientProperty("FlatLaf.style", "arc: 13");
        campoConfirmar.putClientProperty("JPasswordField.showRevealButton", true);
        campoConfirmar.setFont(new Font("SF Pro Rounded", Font.PLAIN, 18));

        btnCambiar.putClientProperty("FlatLaf.style", "arc: 20");
        btnCambiar.setBackground(UIManager.getDefaults().getColor("Actions.Green"));
        btnCambiar.setForeground(Color.WHITE);
        btnCambiar.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));

        cardCambio.add(lblMensaje, "growx");
        cardCambio.add(lblNuevaTitulo, "growx, gaptop 15");
        cardCambio.add(campoNueva, "growx, h 40!");
        cardCambio.add(lblConfirmarTitulo, "growx, gaptop 15");
        cardCambio.add(campoConfirmar, "growx, h 40!");
        cardCambio.add(btnCambiar, "growx, gaptop 20, h 40!");

        add(cardCambio, "growx, width 500:500:600");
    }

    public void setNombreUsuario(String nombre) {
        lblBienvenida.setText("Hola, " + nombre);
    }

    public char[] getNuevaContrasena() {
        return campoNueva.getPassword();
    }

    public char[] getConfirmarContrasena() {
        return campoConfirmar.getPassword();
    }

    /** Reinicia la pantalla al estado inicial (campos vacíos, botón habilitado). */
    public void resetear() {
        campoNueva.setText("");
        campoConfirmar.setText("");
        btnCambiar.setEnabled(true);
    }

    public JButton getBtnCambiar() {
        return btnCambiar;
    }
}
