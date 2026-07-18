package com.tuerca.pos.view.components;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.JLabel;
import javax.swing.Timer;

/**
 * Reloj en vivo (fecha, hora, minuto y segundo) para mostrar debajo del label
 * de "Usuario activo" en las pantallas que lo tienen. Un solo
 * {@code javax.swing.Timer} por label, arrancado una vez al construir la
 * vista — mismo patrón que {@link BusquedaConDebounce#aplicar}.
 */
public class RelojEnVivo {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private RelojEnVivo() {
    }

    public static void iniciar(JLabel label) {
        actualizar(label);
        Timer timer = new Timer(1000, e -> actualizar(label));
        timer.start();
    }

    private static void actualizar(JLabel label) {
        label.setText(LocalDateTime.now().format(FORMATO));
    }
}
