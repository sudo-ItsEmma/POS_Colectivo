package com.tuerca.pos.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.tuerca.pos.view.components.RelojEnVivo;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import net.miginfocom.swing.MigLayout;

/**
 * Dashboard del rol Administrador. Sin `.form`: layout a mano con MigLayout
 * (la cuadrícula interna de botones se conserva como {@link GridLayout},
 * igual que en el `.form` original). La navegación y el cierre de sesión
 * viven en {@link com.tuerca.pos.controller.AdminDashboardController}.
 */
public class AdminPanel extends JPanel {

    private final JLabel lblTitulo = new JLabel("POS de Venta");
    private final JLabel lblSubtitulo = new JLabel("Aura Tienda Colectiva");
    private final JPanel panelBotones = new JPanel(new GridLayout(4, 3, 20, 20));

    private final JButton btnEmpleados = new JButton("Empleados");
    private final JButton btnProductos = new JButton("Gestión Productos");
    private final JButton btnVentas = new JButton("Venta");
    private final JButton btnEmprendedores = new JButton("Gestión Emprendedores");
    private final JButton btnApartados = new JButton("Gestión Apartados");
    private final JButton btnArqueoCaja = new JButton("Arqueo de Caja");
    private final JButton btnPagoEmprendedores = new JButton("Pago Emprendedores");
    private final JButton btnDevolucion = new JButton("Devolución");
    private final JButton btnCorteCaja = new JButton("Corte de caja");
    private final JButton btnReportes = new JButton("Generar Reportes");

    private final JPanel panelEstado = new JPanel();
    private final JLabel lblUsuarioActivo = new JLabel(" ");
    private final JLabel lblFechaHora = new JLabel(" ");
    private final JButton btnCerrarSesion = new JButton("Cerrar sesión");

    public AdminPanel() {
        initComponents();
    }

    private void initComponents() {
        setPreferredSize(new java.awt.Dimension(1280, 720));
        setLayout(new MigLayout("insets 20, fill, wrap 1"));

        lblTitulo.setFont(new Font("SF Pro Rounded", Font.BOLD, 28));
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);

        lblSubtitulo.setFont(new Font("SF Pro Rounded", Font.BOLD, 28));
        lblSubtitulo.setHorizontalAlignment(SwingConstants.CENTER);

        add(lblTitulo, "growx");
        add(lblSubtitulo, "growx");

        estilizarBoton(btnEmpleados, "Actions.Blue", "empleados.svg");
        estilizarBoton(btnProductos, "Actions.Blue", "productos.svg");
        estilizarBoton(btnVentas, "Actions.Green", "ventas.svg");
        estilizarBoton(btnEmprendedores, "Actions.Blue", "emprendedores.svg");
        estilizarBoton(btnApartados, "Actions.Blue", "apartados.svg");
        estilizarBoton(btnArqueoCaja, "Actions.Yellow", "arqueocaja.svg");
        estilizarBoton(btnPagoEmprendedores, "Actions.Blue", "pagoemprendedores.svg");
        estilizarBoton(btnDevolucion, "Button.borderColor", "devolucion.svg");
        estilizarBoton(btnCorteCaja, "Actions.Yellow", "corte.svg");
        estilizarBoton(btnReportes, "Button.borderColor", "reportes.svg");

        panelBotones.add(btnEmpleados);
        panelBotones.add(btnProductos);
        panelBotones.add(btnVentas);
        panelBotones.add(btnEmprendedores);
        panelBotones.add(btnApartados);
        panelBotones.add(btnArqueoCaja);
        panelBotones.add(btnPagoEmprendedores);
        panelBotones.add(btnDevolucion);
        panelBotones.add(btnCorteCaja);
        panelBotones.add(btnReportes);

        add(panelBotones, "grow, push");

        lblUsuarioActivo.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));

        btnCerrarSesion.putClientProperty("FlatLaf.style", "arc: 20");
        btnCerrarSesion.setBackground(UIManager.getDefaults().getColor("Actions.Red"));
        btnCerrarSesion.setForeground(Color.WHITE);
        btnCerrarSesion.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));

        lblFechaHora.setFont(new Font("SF Pro Rounded", Font.PLAIN, 12));
        JPanel panelUsuarioInfo = new JPanel(new MigLayout("insets 0, wrap 1", "[grow]"));
        panelUsuarioInfo.add(lblUsuarioActivo, "growx");
        panelUsuarioInfo.add(lblFechaHora, "growx");
        RelojEnVivo.iniciar(lblFechaHora);

        panelEstado.setLayout(new MigLayout("insets 0, fillx", "[grow][]"));
        panelEstado.add(panelUsuarioInfo, "growx");
        panelEstado.add(btnCerrarSesion);

        add(panelEstado, "growx");
    }

    private void estilizarBoton(JButton btn, String colorKey, String icono) {
        btn.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 15; focusWidth: 0");
        btn.setBackground(UIManager.getDefaults().getColor(colorKey));
        btn.setFont(new Font("SF Pro Rounded", Font.BOLD, 24));
        btn.setForeground(Color.WHITE);
        btn.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/" + icono, 48, 48));
    }

    public void setNombreUsuarioActivo(String texto) {
        lblUsuarioActivo.setText(texto);
    }

    public JButton getBtnEmpleados() {
        return btnEmpleados;
    }

    public JButton getBtnProductos() {
        return btnProductos;
    }

    public JButton getBtnVentas() {
        return btnVentas;
    }

    public JButton getBtnEmprendedores() {
        return btnEmprendedores;
    }

    public JButton getBtnApartados() {
        return btnApartados;
    }

    public JButton getBtnArqueoCaja() {
        return btnArqueoCaja;
    }

    public JButton getBtnPagoEmprendedores() {
        return btnPagoEmprendedores;
    }

    public JButton getBtnDevolucion() {
        return btnDevolucion;
    }

    public JButton getBtnCorteCaja() {
        return btnCorteCaja;
    }

    public JButton getBtnReportes() {
        return btnReportes;
    }

    public JButton getBtnCerrarSesion() {
        return btnCerrarSesion;
    }
}
