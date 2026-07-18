package com.tuerca.pos.view.components;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;

/**
 * Filtros de teclado en tiempo real: bloquean el carácter inválido en cuanto
 * el usuario lo teclea (o lo pega), en vez de avisar con un {@code JOptionPane}
 * después de enviar el formulario. Usados en los campos de teléfono y
 * nombre/apellidos de Empleados, Emprendedores y el diálogo de Apartado.
 */
public class FiltroCampoTexto {

    private FiltroCampoTexto() {
    }

    /** Solo dígitos, hasta {@code maxDigitos} caracteres (ej. teléfono a 10 dígitos). */
    public static void soloNumeros(JTextField campo, int maxDigitos) {
        ((PlainDocument) campo.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String texto, AttributeSet attr) throws BadLocationException {
                reemplazar(fb, offset, 0, texto, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String texto, AttributeSet attr) throws BadLocationException {
                reemplazar(fb, offset, length, texto, attr);
            }

            private void reemplazar(FilterBypass fb, int offset, int length, String texto, AttributeSet attr) throws BadLocationException {
                StringBuilder soloDigitos = new StringBuilder();
                for (int i = 0; i < texto.length(); i++) {
                    char c = texto.charAt(i);
                    if (Character.isDigit(c)) soloDigitos.append(c);
                }

                int longitudFinal = fb.getDocument().getLength() - length;
                int espacioDisponible = maxDigitos - longitudFinal;
                if (espacioDisponible <= 0) return;
                if (soloDigitos.length() > espacioDisponible) {
                    soloDigitos.setLength(espacioDisponible);
                }
                fb.replace(offset, length, soloDigitos.toString(), attr);
            }
        });
    }

    /** Letras (incluye acentos y la letra ñ/Ñ) y espacios; bloquea dígitos y símbolos. */
    public static void soloLetras(JTextField campo) {
        ((PlainDocument) campo.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String texto, AttributeSet attr) throws BadLocationException {
                fb.insertString(offset, filtrar(texto), attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String texto, AttributeSet attr) throws BadLocationException {
                fb.replace(offset, length, filtrar(texto), attr);
            }

            private String filtrar(String texto) {
                StringBuilder resultado = new StringBuilder();
                for (int i = 0; i < texto.length(); i++) {
                    char c = texto.charAt(i);
                    if (Character.isLetter(c) || c == ' ') resultado.append(c);
                }
                return resultado.toString();
            }
        });
    }
}
