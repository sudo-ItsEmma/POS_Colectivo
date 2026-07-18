package com.tuerca.pos.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.view.components.RelojEnVivo;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Pantalla de Gestión de Productos (FN.3). Sin `.form`: layout a mano con
 * MigLayout, mismo estilo visual que {@link GestionEmprendedores}. Toda la
 * lógica de datos vive en {@link com.tuerca.pos.controller.ProductoController}.
 */
public class GestionProductos extends JPanel {

    private final JLabel lblTitulo = new JLabel("Gestión de Productos");
    private final JButton btnBack = new JButton("Volver");
    private final JTextField txtBuscar = new JTextField();
    private final JComboBox<Object> cbFiltroEmprendedor = new JComboBox<>();
    private final JButton btnNuevoProducto = new JButton("Nuevo Producto");
    private final JButton btnCargaMasiva = new JButton("Carga Masiva");
    private final JTable tablaProductos = new JTable();
    private final JLabel lblUsuario = new JLabel("Usuario: ");
    private final JLabel lblFechaHora = new JLabel(" ");
    private final JRadioButton rbVerInactivos = new JRadioButton("Ver Inactivos");

    public GestionProductos() {
        initComponents();
        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar producto...");
        txtBuscar.putClientProperty("JTextField.showClearButton", true);
        limpiarFiltro();
    }

    private void initComponents() {
        setLayout(new MigLayout("insets 20, fill, wrap 1", "[grow]", "[][][grow][]"));

        lblTitulo.setFont(new Font("SF Pro Rounded", Font.BOLD, 28));
        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);

        btnBack.putClientProperty("FlatLaf.style", "arc: 13; iconTextGap: 10; focusWidth: 0");
        btnBack.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/back.svg", 24, 24));
        btnBack.addActionListener(e -> btnBackActionPerformed());

        JPanel panelSuperior = new JPanel(new MigLayout("insets 0, fillx", "[][grow][]"));
        panelSuperior.add(btnBack);
        panelSuperior.add(lblTitulo, "growx, align center");
        add(panelSuperior, "growx");

        txtBuscar.putClientProperty("FlatLaf.style", "arc: 20");
        txtBuscar.setFont(new Font("SF Pro Rounded", Font.PLAIN, 13));

        cbFiltroEmprendedor.putClientProperty("FlatLaf.style", "arc: 20");
        cbFiltroEmprendedor.setFont(new Font("SF Pro Rounded", Font.PLAIN, 16));

        btnNuevoProducto.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 10; focusWidth: 0");
        btnNuevoProducto.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Blue"));
        btnNuevoProducto.setForeground(Color.WHITE);
        btnNuevoProducto.setFont(new Font("SF Pro Rounded", Font.BOLD, 16));
        btnNuevoProducto.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/new.svg", 24, 24));
        btnNuevoProducto.addActionListener(e -> btnNuevoProductoActionPerformed());

        btnCargaMasiva.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 10; focusWidth: 0");
        btnCargaMasiva.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Yellow"));
        btnCargaMasiva.setForeground(Color.WHITE);
        btnCargaMasiva.setFont(new Font("SF Pro Rounded", Font.BOLD, 16));
        btnCargaMasiva.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/upload.svg", 24, 24));
        btnCargaMasiva.addActionListener(e -> btnCargaMasivaActionPerformed());

        JPanel panelFiltros = new JPanel(new MigLayout("insets 0, fillx", "[grow][220!][220!][220!]"));
        panelFiltros.add(txtBuscar, "h 40!, growx");
        panelFiltros.add(cbFiltroEmprendedor, "h 40!");
        panelFiltros.add(btnNuevoProducto, "h 40!");
        panelFiltros.add(btnCargaMasiva, "h 40!");
        add(panelFiltros, "growx");

        tablaProductos.setFont(new Font("SF Compact Rounded", Font.PLAIN, 13));
        tablaProductos.setRowHeight(35);
        tablaProductos.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"ID", "Código", "Descripción", "Emprendimiento", "Precio", "Stock", "Acciones"}
        ) {
            final boolean[] canEdit = {false, false, false, false, false, false, true};

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        JScrollPane jScrollPane1 = new JScrollPane(tablaProductos);
        jScrollPane1.putClientProperty("FlatLaf.style", "arc: 20");
        add(jScrollPane1, "grow");

        JPanel panelPie = new JPanel(new MigLayout("insets 0, fillx", "[grow][]"));
        lblUsuario.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));
        lblFechaHora.setFont(new Font("SF Pro Rounded", Font.PLAIN, 12));
        JPanel panelUsuarioInfo = new JPanel(new MigLayout("insets 0, wrap 1", "[grow]"));
        panelUsuarioInfo.add(lblUsuario, "growx");
        panelUsuarioInfo.add(lblFechaHora, "growx");
        RelojEnVivo.iniciar(lblFechaHora);
        panelPie.add(panelUsuarioInfo, "growx");
        panelPie.add(rbVerInactivos);
        add(panelPie, "growx");
    }

    private void btnBackActionPerformed() {
        limpiarFiltro();
        java.awt.Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof MainView main) {
            main.showView(Sesion.getInstancia().isAdmin() ? "admin" : "employee");
        }
    }

    private void btnNuevoProductoActionPerformed() {
        java.awt.Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof MainView main) {
            main.showView("nuevoProducto");
        }
    }

    private void btnCargaMasivaActionPerformed() {
        java.awt.Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof MainView main) {
            main.showView("cargaMasiva");
        }
    }

    public JTable getTablaProductos() { return tablaProductos; }
    public JComboBox<Object> getCbFiltroEmprendedor() { return cbFiltroEmprendedor; }
    public JTextField getTxtBuscar() { return txtBuscar; }
    public JButton getBtnNuevoProducto() { return btnNuevoProducto; }
    public JButton getBtnCargaMasiva() { return btnCargaMasiva; }
    public JRadioButton getRbVerInactivos() { return rbVerInactivos; }

    public void limpiarFiltro() {
        txtBuscar.setText("");
        if (cbFiltroEmprendedor.getItemCount() > 0) {
            cbFiltroEmprendedor.setSelectedIndex(0);
        }
    }

    public void setNombreUsuarioActivo(String texto) {
        lblUsuario.setText(texto);
    }
}
