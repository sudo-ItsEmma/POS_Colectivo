package com.tuerca.pos.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.view.components.RelojEnVivo;
import java.awt.Font;
import javax.swing.JButton;
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
 * Pantalla de Gestión de Empleados. Sin `.form`: layout a mano con MigLayout,
 * mismo estilo visual que {@link GestionProductos}/{@link GestionEmprendedores}.
 * Toda la lógica de datos vive en {@link com.tuerca.pos.controller.EmpleadoController}.
 */
public class GestionEmpleados extends JPanel {

    private final JLabel lblTitulo = new JLabel("Gestión de Empleados");
    private final JButton btnBack = new JButton("Volver");
    private final JTextField txtBuscar = new JTextField();
    private final JButton btnNuevoEmpleado = new JButton("Nuevo Empleado");
    private final JTable tablaEmpleados = new JTable();
    private final JLabel lblUsuario = new JLabel("Usuario: ");
    private final JLabel lblFechaHora = new JLabel(" ");
    private final JRadioButton rbVerInactivos = new JRadioButton("Ver empleados desactivados");

    public GestionEmpleados() {
        initComponents();
        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar Empleado...");
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

        btnNuevoEmpleado.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 10; focusWidth: 0");
        btnNuevoEmpleado.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Blue"));
        btnNuevoEmpleado.setForeground(java.awt.Color.WHITE);
        btnNuevoEmpleado.setFont(new Font("SF Pro Rounded", Font.BOLD, 18));
        btnNuevoEmpleado.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/new.svg", 24, 24));
        btnNuevoEmpleado.addActionListener(e -> btnNuevoEmpleadoActionPerformed());

        JPanel panelFiltros = new JPanel(new MigLayout("insets 0, fillx", "[grow][240!]"));
        panelFiltros.add(txtBuscar, "h 40!, growx");
        panelFiltros.add(btnNuevoEmpleado, "h 40!");
        add(panelFiltros, "growx");

        tablaEmpleados.setFont(new Font("SF Compact Rounded", Font.PLAIN, 13));
        tablaEmpleados.setRowHeight(35);
        tablaEmpleados.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"ID", "Nombre", "Ap. Paterno", "Ap. Materno", "Contacto", "Usuario", "Tipo", "Acciones"}
        ) {
            final boolean[] canEdit = {false, false, false, false, false, false, false, true};

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        JScrollPane jScrollPane1 = new JScrollPane(tablaEmpleados);
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

    private void btnNuevoEmpleadoActionPerformed() {
        java.awt.Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof MainView main) {
            main.showView("nuevoEmpleado");
        }
    }

    public JTable getTablaEmpleados() { return tablaEmpleados; }
    public JTextField getTxtBuscar() { return txtBuscar; }
    public JRadioButton getRbVerInactivos() { return rbVerInactivos; }

    public DefaultTableModel getTableModel() {
        return (DefaultTableModel) tablaEmpleados.getModel();
    }

    public void limpiarFiltro() {
        txtBuscar.setText("");
    }

    public void setNombreUsuarioActivo(String texto) {
        lblUsuario.setText(texto);
    }
}
