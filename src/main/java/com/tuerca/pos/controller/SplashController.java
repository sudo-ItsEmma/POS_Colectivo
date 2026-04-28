/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.controller;

import com.tuerca.pos.dao.DatabaseConnection;
import com.tuerca.pos.view.SplashView;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

/**
 *
 * @author mannycalderon
 */
public class SplashController {
    private final SplashView view;

    public SplashController(SplashView view) {
        this.view = view;
    }

    public void start() {
        view.setVisible(true);
        new Thread(() -> {
            try {
                // 1. Despertar el ejecutable MariaDB (Lo que ya tenías)
                view.progressBar.setString("Iniciando motor de base de datos...");
                DatabaseManager dbManager = new DatabaseManager();
                dbManager.startDatabase();
                actualizarProgreso(20);
                Thread.sleep(5000); // Espera necesaria para que el .exe abra

                // 2. Conexión y Creación de BD (Lógica de decisión)
                view.progressBar.setString("Verificando integridad...");
                Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();

                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS pos_colectivo");
                stmt.execute("USE pos_colectivo");
                actualizarProgreso(50);

                // 3. Verificación de Tablas / Script
                ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'UserAccount'");
                if (!rs.next()) {
                    view.progressBar.setString("Configurando tablas por primera vez...");
                    ejecutarScriptSQL(conn, "src/main/resources/db_setup.sql");
                }
                actualizarProgreso(100);

                // 4. Salto al Login
                Thread.sleep(500);
                view.dispose();
                // Abrimos el MainView (que ya contiene al LoginPanel por defecto)
                java.awt.EventQueue.invokeLater(() -> {
                    com.tuerca.pos.view.MainView main = new com.tuerca.pos.view.MainView();
                    main.setVisible(true);
                    main.setLocationRelativeTo(null); // Centra la ventana en tu pantalla
                });

            } catch (Exception e) {
                JOptionPane.showMessageDialog(view, "Error crítico: " + e.getMessage());
                System.exit(0);
            }
        }).start();
    }

    private void actualizarProgreso(int v) {
        view.progressBar.setValue(v);
    }
    
    private void ejecutarScriptSQL(Connection conn, String rutaScript) {
        try (BufferedReader br = new BufferedReader(new FileReader(rutaScript))) {
            StringBuilder sql = new StringBuilder();
            String line;
            Statement stmt = conn.createStatement();

            while ((line = br.readLine()) != null) {
                // Ignorar comentarios y líneas vacías
                if (line.trim().isEmpty() || line.trim().startsWith("--") || line.trim().startsWith("/*")) {
                    continue;
                }
                sql.append(line);
                // Si la línea termina en ';', ejecutamos esa instrucción
                if (line.trim().endsWith(";")) {
                    stmt.execute(sql.toString());
                    sql.setLength(0); // Limpiar para la siguiente instrucción
                }
            }
            System.out.println("Script ejecutado correctamente.");
        } catch (Exception e) {
            System.err.println("Error al ejecutar script: " + e.getMessage());
        }
    }
}
