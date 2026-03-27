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
            // Path to the bin folder inside your project
            String dbPath = new File("db_engine/bin/mariadbd").getAbsolutePath();
            String dataPath = new File("db_engine/data").getAbsolutePath();

            System.out.println("Starting database engine from: " + dbPath);

            // Command to start MariaDB pointing to our local data folder
            ProcessBuilder processBuilder = new ProcessBuilder(
                dbPath, 
                "--datadir=" + dataPath, 
                "--port=3306"
            );

            dbProcess = processBuilder.start();
            System.out.println("Database engine is warming up...");
            
        } catch (IOException e) {
            System.err.println("Failed to start database: " + e.getMessage());
        }
    }

    public void stopDatabase() {
        if (dbProcess != null) {
            dbProcess.destroy();
            System.out.println("Database engine stopped.");
        }
    }
}
