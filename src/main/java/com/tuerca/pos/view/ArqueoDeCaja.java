package com.tuerca.pos.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.tuerca.pos.view.components.RelojEnVivo;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import net.miginfocom.swing.MigLayout;

/**
 * Pantalla de Arqueo de Caja (FN.7). Sin `.form`: layout a mano con
 * MigLayout, mismo estilo visual que {@link Ventas}. Toda la lógica vive
 * en {@link com.tuerca.pos.controller.ArqueoCajaController}.
 */
public class ArqueoDeCaja extends JPanel {

    private final JLabel lblTitulo = new JLabel("Arqueo de Caja");
    private final JButton btnBack = new JButton("Volver");
    private final JLabel lblUsuario = new JLabel("Usuario: ");
    private final JLabel lblFechaHora = new JLabel(" ");

    private final JLabel lblTituloVentas = new JLabel("Ventas realizadas");
    private final JTable tablaVentas = new JTable();
    private final JScrollPane scrollTabla = new JScrollPane(tablaVentas);

    private final JLabel lblTituloSaldoTeorico = new JLabel("Saldo en Caja");
    private final JLabel lblSaldoTeorico = new JLabel("$0.00");
    private final JLabel lblTituloTransferencias = new JLabel("Transferencias del día (no incluidas en la caja)");
    private final JLabel lblTransferencias = new JLabel("$0.00");
    private final JLabel lblTituloEfectivoContado = new JLabel("Efectivo Contado");
    private final JLabel lblEfectivoContado = new JLabel("$0.00");
    private final JLabel lblTituloDiferencia = new JLabel("Diferencia");
    private final JLabel lblDiferencia = new JLabel("$0.00");

    private final JButton btnIntroducirCantidad = new JButton("Introducir cantidad");
    private final JButton btnCancelar = new JButton("Cancelar");

    public ArqueoDeCaja() {
        initComponents();
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

        lblTituloVentas.setFont(new Font("SF Pro Rounded", Font.BOLD, 28));
        panel.add(lblTituloVentas, "growx");

        tablaVentas.setFont(new Font("SF Compact Rounded", Font.PLAIN, 13));
        tablaVentas.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"Hora", "Método de Pago", "Total", "Efectivo", "Transferencia"}
        ) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        });

        // La última fila es el resumen "TOTAL" agregado en el controller — se resalta
        // en negrita para que no se confunda con una venta más.
        DefaultTableCellRenderer rendererConTotal = new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                boolean esFilaTotal = row == table.getRowCount() - 1
                        && "TOTAL".equals(table.getValueAt(row, 1));
                c.setFont(c.getFont().deriveFont(esFilaTotal ? Font.BOLD : Font.PLAIN));
                setHorizontalAlignment(column >= 2 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                return c;
            }
        };
        tablaVentas.setDefaultRenderer(Object.class, rendererConTotal);
        tablaVentas.setDefaultRenderer(Number.class, rendererConTotal);

        scrollTabla.putClientProperty("FlatLaf.style", "arc: 20");
        panel.add(scrollTabla, "grow");

        lblUsuario.setFont(new Font("SF Pro Rounded", Font.BOLD, 14));
        panel.add(lblUsuario, "growx");

        lblFechaHora.setFont(new Font("SF Pro Rounded", Font.PLAIN, 12));
        panel.add(lblFechaHora, "growx");
        RelojEnVivo.iniciar(lblFechaHora);

        return panel;
    }

    private JPanel construirPanelDerecho() {
        JPanel panel = new JPanel(new MigLayout("insets 0 0 0 10, fill, wrap 1", "[grow]"));

        panel.add(construirBloqueValor(lblTituloSaldoTeorico, lblSaldoTeorico), "growx, gapbottom 5");
        panel.add(construirBloqueInformativo(lblTituloTransferencias, lblTransferencias), "growx, gapbottom 15");
        panel.add(construirBloqueValor(lblTituloEfectivoContado, lblEfectivoContado), "growx, gapbottom 15");
        panel.add(construirBloqueValor(lblTituloDiferencia, lblDiferencia), "growx, push");

        btnIntroducirCantidad.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 10; focusWidth: 0");
        btnIntroducirCantidad.setBackground(UIManager.getDefaults().getColor("Actions.Green"));
        btnIntroducirCantidad.setForeground(Color.WHITE);
        btnIntroducirCantidad.setFont(new Font("SF Pro Rounded", Font.BOLD, 18));
        btnIntroducirCantidad.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/money.svg", 36, 36));
        panel.add(btnIntroducirCantidad, "growx, h 74!, gaptop 20");

        btnCancelar.putClientProperty("FlatLaf.style", "arc: 20; iconTextGap: 10; focusWidth: 0");
        btnCancelar.setBackground(UIManager.getDefaults().getColor("Actions.Red"));
        btnCancelar.setForeground(Color.WHITE);
        btnCancelar.setFont(new Font("SF Pro Rounded", Font.BOLD, 24));
        btnCancelar.setIcon(new FlatSVGIcon("com/tuerca/pos/icons/cancel.svg", 36, 36));
        panel.add(btnCancelar, "growx, h 74!, gaptop 18");

        return panel;
    }

    private JPanel construirBloqueValor(JLabel titulo, JLabel valor) {
        JPanel bloque = new JPanel(new MigLayout("insets 0, fillx, wrap 1"));
        titulo.setFont(new Font("SF Pro Rounded", Font.BOLD, 28));
        titulo.setHorizontalAlignment(SwingConstants.CENTER);
        valor.setFont(new Font("SF Pro Rounded", Font.BOLD, 48));
        valor.setHorizontalAlignment(SwingConstants.CENTER);
        bloque.add(titulo, "growx");
        bloque.add(valor, "growx");
        return bloque;
    }

    // Bloque más compacto para datos informativos (no forman parte del saldo en caja).
    private JPanel construirBloqueInformativo(JLabel titulo, JLabel valor) {
        JPanel bloque = new JPanel(new MigLayout("insets 0, fillx, wrap 1"));
        titulo.setFont(new Font("SF Pro Rounded", Font.PLAIN, 13));
        titulo.setHorizontalAlignment(SwingConstants.CENTER);
        titulo.setForeground(UIManager.getColor("Label.disabledForeground"));
        valor.setFont(new Font("SF Pro Rounded", Font.BOLD, 22));
        valor.setHorizontalAlignment(SwingConstants.CENTER);
        valor.setForeground(UIManager.getColor("Label.disabledForeground"));
        bloque.add(titulo, "growx");
        bloque.add(valor, "growx");
        return bloque;
    }

    public JButton getBtnBack() {
        return btnBack;
    }

    public JTable getTablaVentas() {
        return tablaVentas;
    }

    public JLabel getLblSaldoTeorico() {
        return lblSaldoTeorico;
    }

    public JLabel getLblTransferencias() {
        return lblTransferencias;
    }

    public JLabel getLblEfectivoContado() {
        return lblEfectivoContado;
    }

    public JLabel getLblDiferencia() {
        return lblDiferencia;
    }

    public JButton getBtnIntroducirCantidad() {
        return btnIntroducirCantidad;
    }

    public JButton getBtnCancelar() {
        return btnCancelar;
    }

    public void setNombreUsuarioActivo(String texto) {
        lblUsuario.setText(texto);
    }
}
