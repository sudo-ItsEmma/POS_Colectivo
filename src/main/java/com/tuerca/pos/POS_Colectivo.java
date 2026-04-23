package com.tuerca.pos;

import com.tuerca.pos.controller.SplashController;
import com.tuerca.pos.view.SplashView;
import javax.swing.UIManager;

public class POS_Colectivo {

    public static void main(String[] args) {
        // configurar FlatLaf
        try{
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
        } catch (Exception e) {
            System.err.println("Theme failed");
        }
        
        // lanzar el flujo MVC del Splash
        SplashView splash = new SplashView(); 
        SplashController controller = new SplashController(splash);
        controller.start();
    }
}