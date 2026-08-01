/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.controller;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 *
 * @author mannycalderon
 */
public class DatabaseManager {
    private Process dbProcess;


    public void startDatabase() {
        try {
            // Rutas resueltas contra la carpeta de instalación real (no el directorio de
            // trabajo del proceso) — necesario para que funcione igual corriendo desde
            // el código fuente (mvn exec:java) que empaquetado con jpackage, donde el
            // cwd al hacer doble clic en el .exe no es predecible. Windows usa
            // "mariadbd.exe"; macOS/Linux, "mariadbd" sin extensión.
            File carpetaMotor = new File(resolverCarpetaInstalacion(), "db_engine");
            String dbPath = new File(carpetaMotor, "bin/" + nombreEjecutableMariadbd()).getAbsolutePath();
            String dataPath = new File(carpetaMotor, "data").getAbsolutePath();
            String baseDir = carpetaMotor.getAbsolutePath();
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

    private String nombreEjecutableMariadbd() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win") ? "mariadbd.exe" : "mariadbd";
    }

    // Empaquetado con jpackage: el .jar corre desde "<instalación>/app/POS_Colectivo.jar",
    // así que la carpeta de instalación real es dos niveles arriba del .jar. Corriendo
    // desde el código fuente (mvn exec:java, IDE), el "código" vive como directorio de
    // clases sueltas (no un .jar) — en ese caso se usa el directorio de trabajo actual,
    // igual que se hacía antes de este cambio.
    private File resolverCarpetaInstalacion() {
        try {
            File origen = new File(DatabaseManager.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (origen.isFile()) {
                return origen.getParentFile().getParentFile();
            }
        } catch (URISyntaxException | NullPointerException e) {
            // Sigue al valor por defecto de abajo.
        }
        return new File(System.getProperty("user.dir"));
    }
}
