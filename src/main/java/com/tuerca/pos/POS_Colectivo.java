package com.tuerca.pos;

import com.tuerca.pos.controller.DatabaseManager;
import com.tuerca.pos.dao.DatabaseConnection;
import java.sql.Connection; //
import java.sql.Statement;  // También añade esta para evitar el siguiente error

import com.formdev.flatlaf.FlatLightLaf; // Importamos el tema claro
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

public class POS_Colectivo {

    public static void main(String[] args) {
        // 1. Setup Look and Feel
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
        } catch (Exception e) {
            System.err.println("Theme failed");
        }

        // 2. Start Database Engine
        DatabaseManager dbManager = new DatabaseManager();
        dbManager.startDatabase();

        // 3. Test Connection (Wait a couple of seconds for the engine to boot)
        try {
            Thread.sleep(4000); // Damos 4 segundos para que el motor despierte
            Connection conn = com.tuerca.pos.dao.DatabaseConnection.getConnection();

            // Crear la base de datos si no existe
            var statement = conn.createStatement();
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS pos_colectivo");
            System.out.println("Database 'pos_colectivo' is ready!");
        } catch (Exception e) {
            System.err.println("Initial handshake failed: " + e.getMessage());
        }

        // 4. Show UI
        JFrame mainFrame = new JFrame("POS Colectivo - Ver. 0.1");
        mainFrame.setSize(500, 400);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }
}