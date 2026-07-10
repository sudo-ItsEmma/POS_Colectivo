package com.tuerca.pos.dao;

import com.tuerca.pos.model.CashSession;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * Acceso a datos de {@code CashSession}: apertura de caja con fondo fijo
 * y consulta de la sesión abierta actual. Solo puede haber una sesión con
 * sessionStatus = "Abierta" a la vez.
 */
public class CashSessionDAO {

    // función que revisa si ya hay una caja abierta (a lo mucho debería haber una)
    public CashSession obtenerSesionAbierta() {
        String sql = "SELECT cs.*, e.firstNameEmployee, e.lastNameEmployee "
                + "FROM CashSession cs "
                + "JOIN UserAccount u ON cs.idUserAccount = u.idUserAccount "
                + "JOIN Employee e ON u.idEmployee = e.idEmployee "
                + "WHERE cs.sessionStatus = 'Abierta' "
                + "ORDER BY cs.openingDateTime DESC LIMIT 1";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return mapear(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener sesión de caja abierta: " + e.getMessage());
        }
        return null;
    }

    // función para abrir la caja con el fondo fijo del día; falla si ya hay una abierta
    public CashSession abrirCaja(int idUserAccount, BigDecimal initialCashAmount) {
        if (obtenerSesionAbierta() != null) {
            System.err.println("Ya existe una sesión de caja abierta.");
            return null;
        }

        String sql = "INSERT INTO CashSession (idUserAccount, initialCashAmount) VALUES (?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, idUserAccount);
            ps.setBigDecimal(2, initialCashAmount);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("No se pudo obtener el ID de la sesión de caja generada");
                }
                return obtenerPorId(rs.getInt(1));
            }
        } catch (SQLException e) {
            System.err.println("Error al abrir caja: " + e.getMessage());
            return null;
        }
    }

    private CashSession obtenerPorId(int idCashSession) throws SQLException {
        String sql = "SELECT cs.*, e.firstNameEmployee, e.lastNameEmployee "
                + "FROM CashSession cs "
                + "JOIN UserAccount u ON cs.idUserAccount = u.idUserAccount "
                + "JOIN Employee e ON u.idEmployee = e.idEmployee "
                + "WHERE cs.idCashSession = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCashSession);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    private CashSession mapear(ResultSet rs) throws SQLException {
        CashSession cs = new CashSession();
        cs.setIdCashSession(rs.getInt("idCashSession"));
        cs.setIdUserAccount(rs.getInt("idUserAccount"));

        Timestamp apertura = rs.getTimestamp("openingDateTime");
        if (apertura != null) {
            cs.setOpeningDateTime(apertura.toLocalDateTime());
        }

        Timestamp cierre = rs.getTimestamp("closingDateTime");
        if (cierre != null) {
            cs.setClosingDateTime(cierre.toLocalDateTime());
        }

        cs.setInitialCashAmount(rs.getBigDecimal("initialCashAmount"));
        cs.setFinalCashAmount(rs.getBigDecimal("finalCashAmount"));
        cs.setTheoricalAmount(rs.getBigDecimal("theoricalAmount"));
        cs.setCashDifference(rs.getBigDecimal("cashDifference"));
        cs.setSessionStatus(rs.getString("sessionStatus"));
        cs.setNombreUsuarioApertura(
                (rs.getString("firstNameEmployee") + " " + rs.getString("lastNameEmployee")).trim());

        return cs;
    }
}
