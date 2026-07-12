package com.tuerca.pos.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Pantalla de Ventas (POS). Sin `.form`: layout a mano con MigLayout,
 * siguiendo el boceto de `POS.excalidraw` (buscador + carrito a la
 * izquierda, total/método de pago/botones a la derecha). Toda la lógica
 * de negocio vive en {@link com.tuerca.pos.controller.VentaController}
 * (y, para "Apartar productos", en {@link com.tuerca.pos.controller.ApartadoController},
 * que reutiliza esta misma vista).
 */
public class Ventas extends JPanel {

    private final JLabel lblTitulo = new JLabel("POS de Venta");
    private final JButton btnBack = new JButton("Volver");
    private final JLabel lblUsuario = new JLabel("Usuario: ");
    private final JButton btnApartarProductos = new JButton("Apartar Productos");

    private final JTextField txtBusqueda = new JTextField();
    private final JTable tablaVenta = new JTable();
    private final JScrollPane scrollTabla = new JScrollPane(tablaVenta);

    private final JLabel lblTituloTotal = new JLabel("Total");
    private final JLabel lblTotal = new JLabel("$0.00");
    private final JLabel lblTituloMetodoPago = new JLabel("Selecciona método de pago");
    private final JComboBox<String> cbMetodoPago = new JComboBox<>(new String[]{"Efectivo", "Transferencia", "Mixto"});
    private final JButton btnPagar = new JButton("Pagar");
    private final JButton btnCancelar = new JButton("Cancelar");

    public Ventas() {
        initComponents();

        txtBusqueda.putClientProperty("JTextField.placeholderText", "Buscar Producto...");
        txtBusqueda.putClientProperty("JTextField.showClearButton", true);
    }

    private void initComponents() {
        setLayout(new MigLayout("insets 20, fill, wrap 1", "[grow]", "[][grow]"));

        lblTitulo.setFont(new Font("SF Pro Rounded", Font.BOLD, 28));
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);

        btnBack.putClientProperty("FlatLaf.style", "arc: 13; iconTextGap: 10; focusWidth: 0");
        btnBack.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/back.svg", 24, 24));

        JPanel panelSuperior = new JPanel(new MigLayout("insets 0, fillx", "[][grow][]"));
        panelSuperior.add(btnBack);
        panelSuperior.add(lblTitulo, "growx, align center");
        add(panelSuperior, "growx");

        JPanel panelCuerpo = new JPanel(new MigLayout("insets 0, fill", "[grow][350!, fill]"));
        panelCuerpo.add(construirPanelIzquierdo(), "grow");
        panelCuerpo.add(construirPanelDerecho(), "grow");
        add(panelCuerpo, "grow");
    }

    private JPanel construirPanelIzquierdo() {
        JPanel panel = new JPanel(new MigLayout("insets 0 10 0 0, fill, wrap 1", "[grow]", "[][grow][]"));

        txtBusqueda.putClientProperty("FlatLaf.style", "arc: 20");
        panel.add(txtBusqueda, "growx, h 40!");

        scrollTabla.putClientProperty("FlatLaf.style", "arc: 20");
        prepararTablaInicial();
        panel.add(scrollTabla, "grow");

        JPanel panelPie = new JPanel(new MigLayout("insets 0, fillx", "[grow][]"));
        lblUsuario.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));
        panelPie.add(lblUsuario, "growx");

        btnApartarProductos.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 10; focusWidth: 0");
        btnApartarProductos.setBackground(UIManager.getDefaults().getColor("Actions.Blue"));
        btnApartarProductos.setForeground(Color.WHITE);
        btnApartarProductos.setFont(new Font("SF Pro Rounded", Font.BOLD, 12));
        btnApartarProductos.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/apartados.svg", 24, 24));
        panelPie.add(btnApartarProductos);

        panel.add(panelPie, "growx");
        return panel;
    }

    private JPanel construirPanelDerecho() {
        JPanel panel = new JPanel(new MigLayout("insets 0 0 0 10, fill, wrap 1", "[grow]"));

        lblTituloTotal.setFont(new Font("SF Pro Rounded", Font.BOLD, 28));
        lblTituloTotal.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblTituloTotal, "growx");

        lblTotal.setFont(new Font("SF Pro Rounded", Font.BOLD, 48));
        lblTotal.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblTotal, "growx, gapbottom 30");

        lblTituloMetodoPago.setFont(new Font("SF Pro Rounded", Font.BOLD, 18));
        lblTituloMetodoPago.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblTituloMetodoPago, "growx, push");

        cbMetodoPago.putClientProperty("FlatLaf.style", "arc: 20");
        panel.add(cbMetodoPago, "growx, h 40!, gaptop 10");

        btnPagar.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 10; focusWidth: 0");
        btnPagar.setBackground(UIManager.getDefaults().getColor("Actions.Green"));
        btnPagar.setForeground(Color.WHITE);
        btnPagar.setFont(new Font("SF Pro Rounded", Font.BOLD, 24));
        btnPagar.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/money.svg", 36, 36));
        panel.add(btnPagar, "growx, h 74!, gaptop 20");

        btnCancelar.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 10; focusWidth: 0");
        btnCancelar.setBackground(UIManager.getDefaults().getColor("Actions.Red"));
        btnCancelar.setForeground(Color.WHITE);
        btnCancelar.setFont(new Font("SF Pro Rounded", Font.BOLD, 24));
        btnCancelar.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/cancel.svg", 36, 36));
        panel.add(btnCancelar, "growx, h 74!, gaptop 18");

        return panel;
    }

    // El modelo real (columnas, editabilidad, tipos) lo define VentaController.prepararModeloTabla();
    // este modelo inicial solo evita que la tabla arranque sin columnas antes de que exista el controller.
    private void prepararTablaInicial() {
        tablaVenta.setFont(new Font("SF Compact Rounded", Font.PLAIN, 18));
        tablaVenta.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"Cantidad", "Código", "Descripción", "Precio Unitario", "Descuento", "Subtotal", "Acción"}
        ));
    }

    // --- GETTERS PARA ELEMENTOS DE VENTA ---

    public JTextField getTxtBusqueda() {
        return txtBusqueda;
    }

    public JTable getTablaVenta() {
        return tablaVenta;
    }

    public JLabel getLblTotal() {
        return lblTotal;
    }

    public JComboBox<String> getCbMetodoPago() {
        return cbMetodoPago;
    }

    public JButton getBtnCobrar() {
        return btnPagar;
    }

    public JButton getBtnApartarProductos() {
        return btnApartarProductos;
    }

    public JButton getBtnCancelar() {
        return btnCancelar;
    }

    public JButton getBtnBack() {
        return btnBack;
    }

    public void setNombreUsuarioActivo(String texto) {
        lblUsuario.setText(texto);
    }
}
