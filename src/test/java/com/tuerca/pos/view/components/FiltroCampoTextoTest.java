package com.tuerca.pos.view.components;

import javax.swing.JTextField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FiltroCampoTextoTest {

    @Test
    void soloNumeros_bloqueaLetrasYSimbolos() {
        JTextField campo = new JTextField();
        FiltroCampoTexto.soloNumeros(campo, 10);

        campo.setText("abc123-456");

        assertEquals("123456", campo.getText());
    }

    @Test
    void soloNumeros_bloqueaMasDelMaximoDeDigitos() {
        JTextField campo = new JTextField();
        FiltroCampoTexto.soloNumeros(campo, 10);

        campo.setText("12345678901234");

        assertEquals("1234567890", campo.getText());
    }

    @Test
    void soloNumeros_permiteExactamenteElMaximo() {
        JTextField campo = new JTextField();
        FiltroCampoTexto.soloNumeros(campo, 10);

        campo.setText("7771234567");

        assertEquals("7771234567", campo.getText());
    }

    @Test
    void soloLetras_bloqueaDigitosYSimbolos() {
        JTextField campo = new JTextField();
        FiltroCampoTexto.soloLetras(campo);

        campo.setText("Juan123 P3rez!$%");

        assertEquals("Juan Prez", campo.getText());
    }

    @Test
    void soloLetras_permiteAcentosYEnie() {
        JTextField campo = new JTextField();
        FiltroCampoTexto.soloLetras(campo);

        campo.setText("Íñigo Muñóz Peña");

        assertEquals("Íñigo Muñóz Peña", campo.getText());
    }

    @Test
    void soloLetras_permiteEspacios() {
        JTextField campo = new JTextField();
        FiltroCampoTexto.soloLetras(campo);

        campo.setText("Maria Jose");

        assertEquals("Maria Jose", campo.getText());
    }
}
