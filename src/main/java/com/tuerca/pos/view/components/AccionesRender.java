/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.view.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author mannycalderon
 */
public class AccionesRender extends JPanel implements TableCellRenderer {
    
    private int currentRow;

    public void setCurrentRow(int row) { this.currentRow = row; }
    public int getCurrentRow() { return currentRow; }

    public JButton getBtnEditar() { return btnEditar; }
    public JButton getBtnEliminar() { return btnEliminar; }

    private final JButton btnEditar = new JButton();
    private final JButton btnEliminar = new JButton();

    private void agregarEfectoHover(JButton btn, Color colorOriginal) {
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                // Al entrar, aclaramos el color un poco
                btn.setBackground(colorOriginal.brighter());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                // Al salir, volvemos al color original
                btn.setBackground(colorOriginal);
            }
        });
    }

    // En tu constructor, después de configurarBoton, llámalo así:
    public AccionesRender() {
        setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        setOpaque(true);

        Color azul = new Color(52, 152, 219);
        Color rojo = new Color(231, 76, 60);

        configurarBoton(btnEditar, new Color(52, 152, 219), "com/tuerca/pos/icons/edit.svg");
    configurarBoton(btnEliminar, new Color(231, 76, 60), "com/tuerca/pos/icons/delete.svg");

        // Activamos el efecto visual
        agregarEfectoHover(btnEditar, azul);
        agregarEfectoHover(btnEliminar, rojo);

        add(btnEditar);
        add(btnEliminar);
    }

    private void configurarBoton(JButton btn, Color fondo, String rutaIcono) {
        btn.setPreferredSize(new Dimension(30, 30));
        btn.setOpaque(true);
        btn.setBackground(fondo);
        btn.setBorderPainted(false);
        btn.setFocusable(false);
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        // Configuración específica de FlatLaf para redondear y cargar SVG
        btn.putClientProperty("JButton.buttonType", "roundRect");
        try {
            btn.setIcon(new com.formdev.flatlaf.extras.FlatSVGIcon(rutaIcono, 20, 20));
        } catch (Exception e) {
            System.err.println("Error cargando SVG: " + rutaIcono);
        }
    }
    
    

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected) {
            setBackground(table.getSelectionBackground());
        } else {
            setBackground(table.getBackground());
        }
        return this;
    }
}
