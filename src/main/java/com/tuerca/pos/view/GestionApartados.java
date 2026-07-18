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
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Pantalla de Gestión de Apartados (FN.6). Sin `.form`: layout a mano con
 * MigLayout, mismo estilo visual que {@link GestionEmprendedores}/
 * {@link GestionProductos}. Toda la lógica vive en
 * {@link com.tuerca.pos.controller.ApartadoController}.
 */
public class GestionApartados extends JPanel {

    private final JLabel lblTitulo = new JLabel("Gestión de Apartados");
    private final JButton btnBack = new JButton("Volver");
    private final JTextField txtBuscar = new JTextField();
    private final JComboBox<String> cbEstado = new JComboBox<>(new String[]{"Pendientes", "Liquidados", "Cancelados", "Vencidos"});
    private final JButton btnNuevoApartado = new JButton("Nuevo Apartado");
    private final JLabel lblLeyendaVencidos = new JLabel("🟧 Por vencer (≤ 3 días)     🟥 Vencido");
    private final JTable tablaApartados = new JTable();
    private final JLabel lblUsuario = new JLabel("Usuario: ");
    private final JLabel lblFechaHora = new JLabel(" ");

    public GestionApartados() {
        initComponents();
        txtBuscar.putClientProperty("JTextField.placeholderText", "Nombre del cliente...");
        txtBuscar.putClientProperty("JTextField.showClearButton", true);
    }

    private void initComponents() {
        setLayout(new MigLayout("insets 20, fill, wrap 1", "[grow]", "[][][][grow][]"));

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

        cbEstado.putClientProperty("FlatLaf.style", "arc: 20");
        cbEstado.setFont(new Font("SF Pro Rounded", Font.PLAIN, 16));

        btnNuevoApartado.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 10; focusWidth: 0");
        btnNuevoApartado.setBackground(javax.swing.UIManager.getDefaults().getColor("Actions.Blue"));
        btnNuevoApartado.setForeground(Color.WHITE);
        btnNuevoApartado.setFont(new Font("SF Pro Rounded", Font.BOLD, 18));
        btnNuevoApartado.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/new.svg", 36, 36));
        btnNuevoApartado.addActionListener(e -> btnNuevoApartadoActionPerformed());

        JPanel panelFiltros = new JPanel(new MigLayout("insets 0, fillx", "[grow][240!][240!]"));
        panelFiltros.add(txtBuscar, "h 40!, growx");
        panelFiltros.add(cbEstado, "h 40!");
        panelFiltros.add(btnNuevoApartado, "h 40!");
        add(panelFiltros, "growx");

        lblLeyendaVencidos.setFont(new Font("SF Pro Rounded", Font.PLAIN, 12));
        add(lblLeyendaVencidos, "growx");

        tablaApartados.setFont(new Font("SF Compact Rounded", Font.PLAIN, 13));
        tablaApartados.setRowHeight(40);
        tablaApartados.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"Folio", "Cliente", "Total Venta", "Anticipo", " Saldo Pendiente", "Vencimiento", "Acciones"}
        ) {
            final boolean[] canEdit = {false, false, false, false, false, false, true};

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        JScrollPane jScrollPane1 = new JScrollPane(tablaApartados);
        jScrollPane1.putClientProperty("FlatLaf.style", "arc: 20");
        add(jScrollPane1, "grow");

        JPanel panelPie = new JPanel(new MigLayout("insets 0, fillx, wrap 1", "[grow]"));
        lblUsuario.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));
        panelPie.add(lblUsuario, "growx");
        lblFechaHora.setFont(new Font("SF Pro Rounded", Font.PLAIN, 12));
        panelPie.add(lblFechaHora, "growx");
        RelojEnVivo.iniciar(lblFechaHora);
        add(panelPie, "growx");
    }

    private void btnBackActionPerformed() {
        java.awt.Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof MainView main) {
            main.showView(Sesion.getInstancia().isAdmin() ? "admin" : "employee");
        }
    }

    private void btnNuevoApartadoActionPerformed() {
        java.awt.Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof MainView main) {
            main.showView("ventas");
        }
    }

    public JTextField getTxtBuscar() {
        return txtBuscar;
    }

    public JTable getTablaApartados() {
        return tablaApartados;
    }

    public JComboBox<String> getCbEstado() {
        return cbEstado;
    }

    public JButton getBtnBack() {
        return btnBack;
    }

    public JButton getBtnNuevoApartado() {
        return btnNuevoApartado;
    }

    public DefaultTableModel getTableModel() {
        return (DefaultTableModel) tablaApartados.getModel();
    }

    public void setNombreUsuarioActivo(String texto) {
        lblUsuario.setText(texto);
    }
}
