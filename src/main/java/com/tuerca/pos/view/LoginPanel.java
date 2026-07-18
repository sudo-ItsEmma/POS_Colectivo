package com.tuerca.pos.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.UIManager;
import net.miginfocom.swing.MigLayout;

/**
 * Pantalla de inicio de sesión. Layout dividido estilo login moderno:
 * 2/5 formulario (izquierda) + 3/5 panel decorativo con degradado
 * (derecha). Sin `.form`: layout a mano con MigLayout. La autenticación
 * real vive en {@link com.tuerca.pos.controller.LoginController}.
 */
public class LoginPanel extends JPanel {

    private final JPanel panelFormulario = new JPanel();
    private final GradientPanel panelDecorativo = new GradientPanel(
            new Color(0x2A, 0x18, 0x52), new Color(0x8A, 0x2F, 0x9E), new Color(0xE0, 0x66, 0x9A));

    private final JLabel lblBienvenida = new JLabel("Bienvenido de vuelta");
    private final JLabel lblSubtitulo = new JLabel("Inicia sesión para continuar");
    private final JLabel lblUsuario = new JLabel("Usuario");
    private final JTextField userField = new JTextField();
    private final JLabel lblContrasena = new JLabel("Contraseña");
    private final JPasswordField contraField = new JPasswordField();
    private final JButton btnIniciarSesion = new JButton("Iniciar sesión");
    private final JLabel lblOlvideContrasena = new JLabel(
            "<html><div style='width: 140px; text-align: center;'>¿Olvidaste tu contraseña? "
            + "Pídele a un Administrador que te la restablezca desde Gestión de Empleados.</div></html>");

    private final JLabel lblMarca = new JLabel("Aura");
    private final JLabel lblSubtituloMarca = new JLabel("Tienda Colectiva");

    public LoginPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new MigLayout("insets 0, fill, gap 0", "[grow 20, fill][grow 80, fill]", "[fill]"));

        construirFormulario();
        construirPanelDecorativo();

        add(panelFormulario, "grow");
        add(panelDecorativo, "grow");
    }

    private void construirFormulario() {
        panelFormulario.setLayout(new MigLayout("insets 60, fillx, wrap 1", "[grow, center]"));

        lblBienvenida.setFont(new Font("SF Pro Rounded", Font.BOLD, 30));

        lblSubtitulo.setFont(new Font("SF Pro Rounded", Font.PLAIN, 15));
        lblSubtitulo.setForeground(UIManager.getColor("Label.disabledForeground"));

        panelFormulario.add(lblBienvenida, "growx, gapbottom 4");
        panelFormulario.add(lblSubtitulo, "growx, gapbottom 40");

        lblUsuario.setFont(new Font("SF Pro Rounded", Font.BOLD, 13));
        userField.putClientProperty("FlatLaf.style", "arc: 13");

        lblContrasena.setFont(new Font("SF Pro Rounded", Font.BOLD, 13));
        // "showRevealButton" es una propiedad "styleable" de FlatPasswordFieldUI: solo
        // funciona dentro del string de "FlatLaf.style", no como client property suelta.
        contraField.putClientProperty("FlatLaf.style", "arc: 13; showRevealButton: true");

        btnIniciarSesion.putClientProperty("FlatLaf.style", "arc: 13");
        btnIniciarSesion.setBackground(UIManager.getDefaults().getColor("Actions.Green"));
        btnIniciarSesion.setForeground(Color.WHITE);
        btnIniciarSesion.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));

        panelFormulario.add(lblUsuario, "growx");
        panelFormulario.add(userField, "growx, h 42!, gapbottom 20");
        panelFormulario.add(lblContrasena, "growx");
        panelFormulario.add(contraField, "growx, h 42!, gapbottom 30");
        panelFormulario.add(btnIniciarSesion, "growx, h 42!");

        lblOlvideContrasena.setFont(new Font("SF Pro Rounded", Font.PLAIN, 12));
        lblOlvideContrasena.setForeground(UIManager.getColor("Label.disabledForeground"));
        panelFormulario.add(lblOlvideContrasena, "growx, gaptop 15");
    }

    private void construirPanelDecorativo() {
        panelDecorativo.setLayout(new BorderLayout());

        JPanel panelTexto = new JPanel(new MigLayout("insets 50, wrap 1", "[grow]"));
        panelTexto.setOpaque(false);

        lblMarca.setFont(new Font("SF Pro Rounded", Font.BOLD, 44));
        lblMarca.setForeground(Color.WHITE);

        lblSubtituloMarca.setFont(new Font("SF Pro Rounded", Font.PLAIN, 17));
        lblSubtituloMarca.setForeground(new Color(255, 255, 255, 210));

        panelTexto.add(lblMarca, "growx");
        panelTexto.add(lblSubtituloMarca, "growx, gaptop 4");

        panelDecorativo.add(panelTexto, BorderLayout.SOUTH);
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

    /**
     * Panel con fondo en degradado diagonal de 3 tonos (índigo → morado → rosa,
     * evocando un "aura") más un par de resplandores de color translúcidos.
     */
    private static class GradientPanel extends JPanel {
        private final Color colorInicio;
        private final Color colorMedio;
        private final Color colorFin;

        GradientPanel(Color colorInicio, Color colorMedio, Color colorFin) {
            this.colorInicio = colorInicio;
            this.colorMedio = colorMedio;
            this.colorFin = colorFin;
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth();
            int h = getHeight();

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setPaint(new LinearGradientPaint(
                    0, 0, w, h,
                    new float[]{0f, 0.55f, 1f},
                    new Color[]{colorInicio, colorMedio, colorFin}));
            g2.fillRect(0, 0, w, h);

            // Resplandores translúcidos en tonos cálido/frío, como un aura difuminada.
            g2.setColor(new Color(255, 200, 230, 40));
            g2.fillOval(w - w / 3, -h / 6, w / 2, w / 2);
            g2.setColor(new Color(160, 200, 255, 28));
            g2.fillOval(-w / 6, h - h / 3, w * 2 / 5, w * 2 / 5);

            g2.dispose();
        }
    }
}
