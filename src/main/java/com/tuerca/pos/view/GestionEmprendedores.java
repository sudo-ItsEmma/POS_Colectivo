package com.tuerca.pos.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.view.components.RelojEnVivo;
import java.awt.Color;
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
 * Pantalla de Gestión de Emprendedores (FN.2). Sin `.form`: layout a mano
 * con MigLayout, mismo estilo visual que {@link ArqueoDeCaja}/{@link CorteDeCaja}.
 * Toda la lógica de datos vive en {@link com.tuerca.pos.controller.EmprendedorController}.
 */
public class GestionEmprendedores extends JPanel {

    private final JLabel lblTitulo = new JLabel("Gestión de Emprendedores");
    private final JButton btnBack = new JButton("Volver");
    private final JTextField txtBuscar = new JTextField();
    private final JButton btnNuevoEmprendedor = new JButton("Nuevo Emprendedor");
    private final JTable tablaEmprendedores = new JTable();
    private final JLabel lblUsuario = new JLabel("Usuario: ");
    private final JLabel lblFechaHora = new JLabel(" ");
    private final JRadioButton rbVerInactivos = new JRadioButton("Ver emprendimientos desactivados");

    public GestionEmprendedores() {
        initComponents();
        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar Emprendedor...");
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

        btnNuevoEmprendedor.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 10; focusWidth: 0");
        btnNuevoEmprendedor.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Blue"));
        btnNuevoEmprendedor.setForeground(Color.WHITE);
        btnNuevoEmprendedor.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));
        btnNuevoEmprendedor.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/new.svg", 24, 24));
        btnNuevoEmprendedor.addActionListener(e -> btnNuevoEmprendedorActionPerformed());

        JPanel panelFiltros = new JPanel(new MigLayout("insets 0, fillx", "[grow][220!]"));
        panelFiltros.add(txtBuscar, "h 40!, growx");
        panelFiltros.add(btnNuevoEmprendedor, "h 40!");
        add(panelFiltros, "growx");

        tablaEmprendedores.setFont(new Font("SF Compact Rounded", Font.PLAIN, 13));
        tablaEmprendedores.setRowHeight(35);
        tablaEmprendedores.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"ID", "Emprendimiento", "Dueño", "Número de telefono", "Correo", "Fecha de contrato", "Costo de renta", "Acciones"}
        ) {
            final boolean[] canEdit = {false, false, false, false, false, false, false, true};

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        JScrollPane jScrollPane1 = new JScrollPane(tablaEmprendedores);
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

    private void btnNuevoEmprendedorActionPerformed() {
        java.awt.Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof MainView main) {
            main.showView("nuevoEmprendedor");
        }
    }

    public JRadioButton getRbVerInactivos() {
        return rbVerInactivos;
    }

    public void limpiarFiltro() {
        txtBuscar.setText("");
    }

    public JTextField getTxtBuscar() {
        return txtBuscar;
    }

    public DefaultTableModel getTableModel() {
        return (DefaultTableModel) tablaEmprendedores.getModel();
    }

    public JTable getTablaEmprendedores() {
        return tablaEmprendedores;
    }

    public void setNombreUsuarioActivo(String texto) {
        lblUsuario.setText(texto);
    }
}
