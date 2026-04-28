package com.tuerca.pos;

import com.tuerca.pos.controller.SplashController;
import com.tuerca.pos.view.SplashView;
import javax.swing.UIManager;

public class POS_Colectivo {

    public static void main(String[] args) {
        // configurar FlatLaf
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());

            // --- PERSONALIZACIÓN DE BOTONES ---
            // '10' es un valor ideal para un redondeado sutil y moderno
            UIManager.put("Button.arc", 10); 

            // Esto asegura que si usas botones cuadrados (como los de tu grid), 
            // también respeten el redondeado sutil
            UIManager.put("Component.arc", 10);

            // Opcional: Hace que el foco del botón (la línea que sale al tabular) 
            // sea más discreto
            UIManager.put("Button.focusWidth", 1);

        } catch (Exception e) {
            System.err.println("Theme failed");
        }

        // lanzar el flujo MVC del Splash
        SplashView splash = new SplashView(); 
        SplashController controller = new SplashController(splash);
        controller.start();
    }
}