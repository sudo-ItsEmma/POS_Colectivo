package com.tuerca.pos.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import net.miginfocom.swing.MigLayout;

/**
 * Pantalla de Corte de Caja (FN.8). Sin `.form`: layout a mano con
 * MigLayout, mismo estilo visual que {@link Ventas}/{@link ArqueoDeCaja}.
 * Consolida el día y cierra la {@code CashSession}. Toda la lógica vive
 * en {@link com.tuerca.pos.controller.CorteCajaController}.
 */
public class CorteDeCaja extends JPanel {

    private final JLabel lblTitulo = new JLabel("Corte de Caja");
    private final JButton btnBack = new JButton("Volver");
    private final JLabel lblUsuario = new JLabel("Usuario: ");
    private final JButton btnFinalizarJornada = new JButton("Finalizar Jornada");

    private final JLabel lblVentasEfectivo = new JLabel("Ventas en efectivo: $0.00");
    private final JLabel lblAbonosEfectivo = new JLabel("Abonos en efectivo: $0.00");
    private final JLabel lblFondoInicial = new JLabel("Fondo inicial: $0.00");
    private final JLabel lblTotalEfectivo = new JLabel("Total efectivo: $0.00");

    private final JLabel lblVentasTransferencia = new JLabel("Ventas por transferencia: $0.00");
    private final JLabel lblCantidadTransferencias = new JLabel("Cantidad de transacciones: 0");

    private final JLabel lblApartadosNuevos = new JLabel("Nuevos: $0.00");
    private final JLabel lblApartadosAbonos = new JLabel("Abonos: $0.00");
    private final JLabel lblApartadosTotal = new JLabel("Total apartados: $0.00");

    private final JLabel lblDebeHaberEnCaja = new JLabel("Debes tener en caja: $0.00");
    private final JLabel lblEfectivoContado = new JLabel("Efectivo contado: —");
    private final JLabel lblMontoARetirar = new JLabel("Monto a retirar: —");

    public CorteDeCaja() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new MigLayout("insets 20, fill, wrap 1", "[grow]", "[][grow][]"));

        lblTitulo.setFont(new Font("SF Pro Rounded", Font.BOLD, 28));
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);

        btnBack.putClientProperty("FlatLaf.style", "arc: 13; iconTextGap: 10; focusWidth: 0");
        btnBack.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/back.svg", 24, 24));

        JPanel panelSuperior = new JPanel(new MigLayout("insets 0, fillx", "[][grow][]"));
        panelSuperior.add(btnBack);
        panelSuperior.add(lblTitulo, "growx, align center");
        add(panelSuperior, "growx");

        JPanel panelCuadrantes = new JPanel(new GridLayout(2, 2, 20, 20));
        panelCuadrantes.add(construirCuadrante("Resumen de Efectivo",
                lblVentasEfectivo, lblAbonosEfectivo, lblFondoInicial, lblTotalEfectivo));
        panelCuadrantes.add(construirCuadrante("Resumen de Transferencias",
                lblVentasTransferencia, lblCantidadTransferencias));
        panelCuadrantes.add(construirCuadrante("Resumen de Apartados",
                lblApartadosNuevos, lblApartadosAbonos, lblApartadosTotal));
        JPanel panelTotalDia = construirCuadrante("Total del Día",
                lblDebeHaberEnCaja, lblEfectivoContado, lblMontoARetirar);
        lblDebeHaberEnCaja.setFont(new Font("SF Pro Rounded", Font.BOLD, 18));
        panelCuadrantes.add(panelTotalDia);
        add(panelCuadrantes, "grow");

        JPanel panelPie = new JPanel(new MigLayout("insets 0, fillx", "[grow][]"));
        lblUsuario.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));
        panelPie.add(lblUsuario, "growx");

        btnFinalizarJornada.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 10; focusWidth: 0");
        btnFinalizarJornada.setBackground(UIManager.getDefaults().getColor("Actions.Blue"));
        btnFinalizarJornada.setForeground(Color.WHITE);
        btnFinalizarJornada.setFont(new Font("SF Pro Rounded", Font.BOLD, 18));
        btnFinalizarJornada.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/close.svg", 36, 36));
        panelPie.add(btnFinalizarJornada, "h 50!");

        add(panelPie, "growx");
    }

    private JPanel construirCuadrante(String titulo, JLabel... valores) {
        JPanel panel = new JPanel(new MigLayout("insets 15, fillx, wrap 1", "[grow]"));
        panel.putClientProperty("FlatLaf.style", "arc: 20");
        panel.setBorder(BorderFactory.createTitledBorder(
                null, titulo, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                new Font("SF Pro Rounded", Font.BOLD, 20)));

        for (int i = 0; i < valores.length; i++) {
            JLabel lbl = valores[i];
            lbl.setFont(new Font("SF Pro Rounded", i == valores.length - 1 ? Font.BOLD : Font.PLAIN, 18));
            panel.add(lbl, "growx, gaptop 8");
        }

        return panel;
    }

    public JButton getBtnBack() {
        return btnBack;
    }

    public JButton getBtnFinalizarJornada() {
        return btnFinalizarJornada;
    }

    public void setNombreUsuarioActivo(String texto) {
        lblUsuario.setText(texto);
    }

    public void setVentasEfectivo(String texto) {
        lblVentasEfectivo.setText("Ventas en efectivo: " + texto);
    }

    public void setAbonosEfectivo(String texto) {
        lblAbonosEfectivo.setText("Abonos en efectivo: " + texto);
    }

    public void setFondoInicial(String texto) {
        lblFondoInicial.setText("Fondo inicial: " + texto);
    }

    public void setTotalEfectivo(String texto) {
        lblTotalEfectivo.setText("Total efectivo: " + texto);
    }

    public void setVentasTransferencia(String texto) {
        lblVentasTransferencia.setText("Ventas por transferencia: " + texto);
    }

    public void setCantidadTransferencias(int cantidad) {
        lblCantidadTransferencias.setText("Cantidad de transacciones: " + cantidad);
    }

    public void setApartadosNuevos(String texto) {
        lblApartadosNuevos.setText("Nuevos: " + texto);
    }

    public void setApartadosAbonos(String texto) {
        lblApartadosAbonos.setText("Abonos: " + texto);
    }

    public void setApartadosTotal(String texto) {
        lblApartadosTotal.setText("Total apartados: " + texto);
    }

    public void setDebeHaberEnCaja(String texto) {
        lblDebeHaberEnCaja.setText("Debes tener en caja: " + texto);
    }

    public void setEfectivoContado(String texto) {
        lblEfectivoContado.setText("Efectivo contado: " + texto);
    }

    public void setMontoARetirar(String texto) {
        lblMontoARetirar.setText("Monto a retirar: " + texto);
    }
}
