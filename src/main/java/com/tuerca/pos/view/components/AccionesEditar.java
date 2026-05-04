/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.view.components;

import java.awt.Component;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;

/**
 *
 * @author mannycalderon
 */
public class AccionesEditar extends DefaultCellEditor {

    private AccionTableEvent event;
    private AccionesRender panel; // Usamos el panel que ya tiene los botones

    public AccionesEditar(AccionTableEvent event) {
        super(new JCheckBox());
        this.event = event;
        this.setClickCountToStart(1);
        this.panel = new AccionesRender(); // Instanciamos el panel visual

        // Agregamos la lógica a los botones del panel del EDITOR
        panel.getBtnEditar().addActionListener(e -> {
            // Detenemos la edición de la celda para que no se quede "trabada"
            stopCellEditing(); 
            // Ejecutamos el evento que definimos en el controlador
            int row = panel.getCurrentRow(); // Necesitaremos guardar la fila actual
            event.onEditar(row);
        });

        panel.getBtnEliminar().addActionListener(e -> {
            stopCellEditing();
            int row = panel.getCurrentRow();
            event.onEliminar(row);
        });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        // Le pasamos al panel en qué fila estamos para que sepa a quién editar/eliminar
        panel.setCurrentRow(row);
        // Pintamos el fondo igual que la selección para que sea fluido
        panel.setBackground(table.getSelectionBackground());
        return panel;
    }

    @Override
    public Object getCellEditorValue() {
        return "";
    }
}