package com.tuerca.pos.view;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import net.miginfocom.swing.MigLayout;

/**
 * Pantalla de arranque (FN.11): se muestra mientras {@link
 * com.tuerca.pos.controller.SplashController} levanta el motor MariaDB
 * portable y verifica/crea el esquema. Sin `.form`: layout a mano con
 * MigLayout, con el mismo branding "Aura Tienda Colectiva" que
 * {@link LoginPanel} — antes era solo una barra de progreso sin marca.
 */
public class SplashView extends JFrame {

    private final JLabel lblMarca = new JLabel("Aura");
    private final JLabel lblSubtituloMarca = new JLabel("Tienda Colectiva");
    public final JProgressBar progressBar = new JProgressBar();

    public SplashView() {
        initComponents();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel panel = new JPanel(new MigLayout("insets 50, fill, wrap 1", "[grow, center]"));

        lblMarca.setFont(new Font("SF Pro Rounded", Font.BOLD, 40));
        lblMarca.setHorizontalAlignment(SwingConstants.CENTER);

        lblSubtituloMarca.setFont(new Font("SF Pro Rounded", Font.PLAIN, 15));
        lblSubtituloMarca.setForeground(Color.GRAY);
        lblSubtituloMarca.setHorizontalAlignment(SwingConstants.CENTER);

        progressBar.setStringPainted(true);
        progressBar.setString("Iniciando...");
        progressBar.putClientProperty("FlatLaf.style", "arc: 10");

        panel.add(lblMarca, "growx, gapbottom 2");
        panel.add(lblSubtituloMarca, "growx, gapbottom 40");
        panel.add(progressBar, "growx, h 24!");

        setContentPane(panel);
        setSize(420, 260);
    }
}
