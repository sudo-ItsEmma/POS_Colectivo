/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.controller;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author mannycalderon
 */
public class DatabaseManager {
    private Process dbProcess;

    
    public void startDatabase() {
        try {
            // Rutas absolutas para portabilidad en macOS
            String dbPath = new File("db_engine/bin/mariadbd").getAbsolutePath();
            String dataPath = new File("db_engine/data").getAbsolutePath();
            String baseDir = new File("db_engine").getAbsolutePath();
            File dataDir = new File(dataPath);

            ProcessBuilder pb = new ProcessBuilder();
            pb.redirectErrorStream(true);

            // 1. Inicialización (Solo si la carpeta data está vacía)
            if (dataDir.list() == null || dataDir.list().length <= 1) { 
                System.out.println("Primera ejecución: Inicializando diccionarios de sistema...");
                pb.command(dbPath, 
                    "--basedir=" + baseDir, 
                    "--datadir=" + dataPath, 
                    "--initialize-insecure", 
                    "--lower-case-table-names=2");
                Process initProcess = pb.start();
                initProcess.waitFor(); // Espera obligatoria para crear tablas 'mysql'
            }

            // 2. Arranque del servidor (Persistente en puerto 3306)
            System.out.println("Arrancando motor MariaDB portable...");
            pb.command(dbPath, 
                "--basedir=" + baseDir, 
                "--datadir=" + dataPath, 
                "--port=3306", 
                "--lower-case-table-names=2", 
                "--skip-grant-tables"); // Evita bloqueos de privilegios iniciales
            
            dbProcess = pb.start();

        } catch (Exception e) {
            System.err.println("Error crítico en el motor: " + e.getMessage());
        }
    }
    
    
    public void stopDatabase() {
        if (dbProcess != null) {
            dbProcess.destroy();
            System.out.println("Database engine stopped.");
        }
    }
}
