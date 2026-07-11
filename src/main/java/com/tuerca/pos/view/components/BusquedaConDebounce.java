package com.tuerca.pos.view.components;

import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Aplica un retraso ("debounce") a un campo de búsqueda: la acción solo se
 * ejecuta cuando el usuario deja de escribir por {@code delayMs} milisegundos,
 * en vez de en cada tecla. Reduce las consultas a la base de datos en los
 * buscadores "en vivo" (Emprendedores, Productos, Ventas).
 */
public class BusquedaConDebounce {

    private BusquedaConDebounce() {
    }

    public static void aplicar(JTextField campo, int delayMs, Runnable accion) {
        Timer timer = new Timer(delayMs, e -> accion.run());
        timer.setRepeats(false);

        campo.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { timer.restart(); }
            @Override public void removeUpdate(DocumentEvent e) { timer.restart(); }
            @Override public void changedUpdate(DocumentEvent e) { timer.restart(); }
        });
    }
}
