package com.tuerca.pos.dao;

import com.tuerca.pos.model.CashSession;
import com.tuerca.pos.support.AbstractDaoIntegrationTest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests de {@link CashSessionDAO}. El entorno de desarrollo siempre tiene
 * una {@code CashSession} real y abierta (la caja real del usuario) — por
 * el {@code UNIQUE} sobre {@code openSessionGuard} (ver db_setup.sql), no
 * se puede crear una segunda fila 'Abierta' sin arriesgar esa sesión real,
 * así que aquí solo se cubre lo que es seguro probar sin tocarla: la
 * lectura y el rechazo de {@code abrirCaja()} cuando ya hay una abierta
 * (que es, de hecho, el estado real y permanente de este entorno).
 */
class CashSessionDAOTest extends AbstractDaoIntegrationTest {

    private final CashSessionDAO dao = new CashSessionDAO();

    @Test
    void obtenerSesionAbierta_devuelveUnaSesionConDatosValidos() {
        // En este entorno de desarrollo siempre hay una CashSession real abierta.
        CashSession sesion = dao.obtenerSesionAbierta();

        assertNotNull(sesion, "debe existir una sesión de caja abierta en el entorno de desarrollo");
        assertEquals("Abierta", sesion.getSessionStatus());
        assertNotNull(sesion.getOpeningDateTime());
        assertNotNull(sesion.getInitialCashAmount());
        assertNotNull(sesion.getNombreUsuarioApertura());
        assertNull(sesion.getClosingDateTime(), "una sesión abierta no debe tener fecha de cierre");
    }

    @Test
    void abrirCaja_siYaHayUnaAbierta_devuelveNullYNoInsertaOtra() throws SQLException {
        int filasAbiertasAntes = contarSesionesAbiertas();
        assertEquals(1, filasAbiertasAntes,
                "precondición del entorno: debe haber exactamente una sesión abierta");

        int idUserAccount = obtenerCualquierIdUserAccount();
        CashSession resultado = dao.abrirCaja(idUserAccount, new java.math.BigDecimal("999.00"));

        assertNull(resultado, "no debe poder abrirse una segunda caja mientras haya una abierta");
        assertEquals(filasAbiertasAntes, contarSesionesAbiertas(),
                "el intento rechazado no debe haber insertado ninguna fila nueva");
    }

    private int contarSesionesAbiertas() throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT COUNT(*) FROM CashSession WHERE sessionStatus = 'Abierta'");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int obtenerCualquierIdUserAccount() throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT idUserAccount FROM UserAccount LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
