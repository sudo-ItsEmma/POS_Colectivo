package com.tuerca.pos.view;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import net.miginfocom.swing.MigLayout;

/**
 * Pantalla de apertura de caja. Se muestra tras el login cuando no hay una
 * {@code CashSession} abierta el día de hoy; obliga a confirmar el fondo
 * fijo antes de dejar operar el POS. Sin `.form`: layout a mano con MigLayout.
 */
public class AperturaCajaPanel extends JPanel {

    private final JLabel lblTitulo = new JLabel("Apertura de Caja");
    private final JLabel lblBienvenida = new JLabel(" ");
    private final JPanel cardApertura = new JPanel();
    private final JLabel lblMensaje = new JLabel("Aún no se ha abierto la caja el día de hoy.");
    private final JLabel lblFondoFijo = new JLabel("$600.00");
    private final JButton btnConfirmarApertura = new JButton("Confirmar apertura de caja");

    public AperturaCajaPanel() {
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

        cardApertura.putClientProperty("FlatLaf.style", "arc: 20");
        cardApertura.setBorder(BorderFactory.createTitledBorder(
                null, "Fondo fijo del día", TitledBorder.CENTER, TitledBorder.TOP,
                new Font("SF Pro Rounded", Font.BOLD, 18)));
        cardApertura.setLayout(new MigLayout("insets 25 30 25 30, fillx, wrap 1", "[grow, center]"));

        lblMensaje.setFont(new Font("SF Pro Rounded", Font.PLAIN, 14));
        lblMensaje.setHorizontalAlignment(SwingConstants.CENTER);

        lblFondoFijo.setFont(new Font("SF Pro Rounded", Font.BOLD, 40));
        lblFondoFijo.setHorizontalAlignment(SwingConstants.CENTER);
        lblFondoFijo.setForeground(UIManager.getDefaults().getColor("Actions.Green"));

        btnConfirmarApertura.putClientProperty("FlatLaf.style", "arc: 20");
        btnConfirmarApertura.setBackground(UIManager.getDefaults().getColor("Actions.Green"));
        btnConfirmarApertura.setForeground(Color.WHITE);
        btnConfirmarApertura.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));

        cardApertura.add(lblMensaje, "growx");
        cardApertura.add(lblFondoFijo, "growx, gaptop 15, gapbottom 15");
        cardApertura.add(btnConfirmarApertura, "growx, gaptop 10, h 40!");

        add(cardApertura, "growx, width 500:500:600");
    }

    public void setNombreUsuario(String nombre) {
        lblBienvenida.setText("Bienvenido(a), " + nombre);
    }

    public JButton getBtnConfirmarApertura() {
        return btnConfirmarApertura;
    }
}
