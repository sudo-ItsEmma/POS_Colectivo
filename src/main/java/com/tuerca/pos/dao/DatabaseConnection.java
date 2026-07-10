package com.tuerca.pos.dao;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author mannycalderon
 */
public class DatabaseConnection {
    // connection data for portable MariaDB
    private static final String CONNECTION_URL = "jdbc:mariadb://localhost:3306/pos_colectivo";
    //private static final String CONNECTION_URL = "jdbc:mariadb://localhost:3306/";
    //private static final String DATABASE_NAME = "pos_colectivo";
    private static final String DATABASE_USER = "root";
    private static final String DATABASE_PASSWORD = "";

    private static Connection connectionInstance;

    public static synchronized Connection getConnection() throws SQLException {
        if (connectionInstance == null || connectionInstance.isClosed()) {
            try {
                Class.forName("org.mariadb.jdbc.Driver");
                // Quitamos "+ DATABASE_NAME" para conectar al motor general primero
                Connection real = DriverManager.getConnection(
                    CONNECTION_URL,
                    DATABASE_USER,
                    DATABASE_PASSWORD
                );
                connectionInstance = envolverSinCierre(real);
                System.out.println("Conexión al motor MariaDB exitosa!");
            } catch (ClassNotFoundException | SQLException e) {
                System.err.println("Connection error: " + e.getMessage());
                throw new SQLException(e);
            }
        }
        return connectionInstance;
    }

    // Los DAOs usan try-with-resources sobre esta conexión (try (Connection con =
    // DatabaseConnection.getConnection()) para cerrar su PreparedStatement), lo que
    // llamaría con.close() y mataría el singleton compartido en cada consulta,
    // forzando reabrir una conexión física nueva la siguiente vez. Este proxy
    // intercepta close() como no-op; isClosed() y todo lo demás sigue yendo a la
    // conexión real, así que la reconexión automática de arriba sigue funcionando
    // si la conexión real se cae por su cuenta.
    private static Connection envolverSinCierre(Connection real) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    try {
                        return method.invoke(real, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }
}
