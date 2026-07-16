package com.tuerca.pos.support;

import com.tuerca.pos.controller.DatabaseManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.junit.jupiter.api.BeforeAll;

/**
 * Clase base para tests de integración de DAOs contra el motor MariaDB
 * portable real del proyecto (no mocks, no H2) — mismo criterio que ya se
 * usó con los harnesses desechables de las Fases 0-1: varias consultas del
 * proyecto usan sintaxis específica de MariaDB (UPDATE con JOIN, ON
 * DUPLICATE KEY UPDATE) que una BD en memoria no reproduciría fielmente.
 *
 * Si el motor ya está corriendo (caso normal durante desarrollo, o si el
 * propio usuario tiene la app abierta), no se toca — nunca se detiene el
 * proceso al terminar, es el mismo motor compartido de desarrollo.
 */
public abstract class AbstractDaoIntegrationTest {

    @BeforeAll
    static void asegurarMotorDisponible() throws InterruptedException {
        if (motorDisponible()) {
            return;
        }
        new DatabaseManager().startDatabase();
        esperarDisponibilidad();
    }

    private static boolean motorDisponible() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 3306), 500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void esperarDisponibilidad() throws InterruptedException {
        for (int intento = 0; intento < 30; intento++) {
            if (motorDisponible()) {
                return;
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("El motor MariaDB portable no respondió tras 15s de espera.");
    }
}
