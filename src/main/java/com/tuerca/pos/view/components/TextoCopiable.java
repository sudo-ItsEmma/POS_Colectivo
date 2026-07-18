package com.tuerca.pos.view.components;

import java.awt.Component;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import net.miginfocom.swing.MigLayout;

/**
 * Diálogo informativo con uno o más valores mostrados en {@link JTextField}
 * de solo lectura (en vez de texto plano de {@code JOptionPane}), para que se
 * puedan seleccionar y copiar — usernames y contraseñas temporales generados
 * por el sistema se necesitan copiar para compartirlos (ej. por WhatsApp).
 */
public class TextoCopiable {

    private TextoCopiable() {
    }

    /** Un solo valor copiable, con su propia etiqueta. */
    public static void mostrar(Component parent, String titulo, String mensaje,
            String etiquetaValor, String valor) {
        mostrar(parent, titulo, mensaje, new String[]{etiquetaValor}, new String[]{valor});
    }

    /** Varios valores copiables (ej. usuario + contraseña temporal en el mismo diálogo). */
    public static void mostrar(Component parent, String titulo, String mensaje,
            String[] etiquetas, String[] valores) {
        JPanel panel = new JPanel(new MigLayout("insets 0, wrap 1", "[grow]"));
        panel.add(new JLabel("<html><div style='width: 320px;'>" + mensaje + "</div></html>"), "growx");

        JTextField primerCampo = null;
        for (int i = 0; i < valores.length; i++) {
            JLabel lblEtiqueta = new JLabel(etiquetas[i]);
            lblEtiqueta.setFont(lblEtiqueta.getFont().deriveFont(Font.BOLD));
            panel.add(lblEtiqueta, "growx, gaptop 12");

            JTextField campo = new JTextField(valores[i]);
            campo.setEditable(false);
            campo.setFont(new Font("SF Compact Rounded", Font.BOLD, 16));
            campo.putClientProperty("FlatLaf.style", "arc: 10");
            panel.add(campo, "growx, h 32!, gaptop 4");

            if (primerCampo == null) primerCampo = campo;
        }

        // selectAll()/requestFocus() no surten efecto hasta que el diálogo ya esté visible
        // (showMessageDialog bloquea el hilo) — se agendan para correr en cuanto el diálogo
        // termine de mostrarse, así el usuario puede copiar con un solo Cmd/Ctrl+C.
        JTextField campoAEnfocar = primerCampo;
        if (campoAEnfocar != null) {
            SwingUtilities.invokeLater(() -> {
                campoAEnfocar.requestFocusInWindow();
                campoAEnfocar.selectAll();
            });
        }

        JOptionPane.showMessageDialog(parent, panel, titulo, JOptionPane.INFORMATION_MESSAGE);
    }
}
