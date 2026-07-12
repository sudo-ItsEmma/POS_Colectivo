package com.tuerca.pos.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.tuerca.pos.model.Sesion;
import java.awt.Font;
import javax.swing.JButton;
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
 * Pantalla de Gestión de Devoluciones (FN.5). Sin `.form`: layout a mano con
 * MigLayout, mismo estilo visual que {@link GestionApartados}. Toda la
 * lógica vive en {@link com.tuerca.pos.controller.DevolucionController}.
 */
public class GestionDevoluciones extends JPanel {

    private final JLabel lblTitulo = new JLabel("Gestión de Devoluciones");
    private final JButton btnBack = new JButton("Volver");
    private final JTextField txtBuscar = new JTextField();
    private final JTable tablaVentas = new JTable();
    private final JLabel lblUsuario = new JLabel("Usuario: ");

    public GestionDevoluciones() {
        initComponents();
        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar por folio o vendedor...");
        txtBuscar.putClientProperty("JTextField.showClearButton", true);
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
        add(txtBuscar, "growx, h 40!");

        tablaVentas.setFont(new Font("SF Compact Rounded", Font.PLAIN, 13));
        tablaVentas.setRowHeight(40);
        tablaVentas.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"Folio", "Total Venta", "Total de Productos", "Fecha de Venta", "Vendedor", "Acciones"}
        ) {
            final boolean[] canEdit = {false, false, false, false, false, true};

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        JScrollPane jScrollPane1 = new JScrollPane(tablaVentas);
        jScrollPane1.putClientProperty("FlatLaf.style", "arc: 20");
        add(jScrollPane1, "grow");

        JPanel panelPie = new JPanel(new MigLayout("insets 0, fillx", "[grow]"));
        lblUsuario.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));
        panelPie.add(lblUsuario, "growx");
        add(panelPie, "growx");
    }

    private void btnBackActionPerformed() {
        java.awt.Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof MainView main) {
            main.showView(Sesion.getInstancia().isAdmin() ? "admin" : "employee");
        }
    }

    public JTextField getTxtBuscar() {
        return txtBuscar;
    }

    public JTable getTablaVentas() {
        return tablaVentas;
    }

    public JButton getBtnBack() {
        return btnBack;
    }

    public void setNombreUsuarioActivo(String texto) {
        lblUsuario.setText(texto);
    }
}
