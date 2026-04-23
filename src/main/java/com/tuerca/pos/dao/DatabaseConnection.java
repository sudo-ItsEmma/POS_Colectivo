package com.tuerca.pos.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author mannycalderon
 */
public class DatabaseConnection {
    // connection data for portable MariaDB
    private static final String CONNECTION_URL = "jdbc:mariadb://localhost:3306/";
    private static final String DATABASE_NAME = "pos_colectivo";
    private static final String DATABASE_USER = "root";
    private static final String DATABASE_PASSWORD = "";
    
    private static Connection connectionInstance;
    
    public static Connection getConnection() throws SQLException {
        if (connectionInstance == null || connectionInstance.isClosed()) {
            try {
                Class.forName("org.mariadb.jdbc.Driver");
                // Quitamos "+ DATABASE_NAME" para conectar al motor general primero
                connectionInstance = DriverManager.getConnection(
                    CONNECTION_URL, 
                    DATABASE_USER, 
                    DATABASE_PASSWORD
                );
                System.out.println("Conexión al motor MariaDB exitosa!");
            } catch (ClassNotFoundException | SQLException e) {
                System.err.println("Connection error: " + e.getMessage());
                throw new SQLException(e);
            }
        }
        return connectionInstance;
    }
}
