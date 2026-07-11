package com.tuerca.pos.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Corte de caja (FN.8): resumen de apartados del día y cierre formal de la
 * {@code CashSession}. A diferencia del arqueo (que se puede repetir
 * varias veces sin bloquear ventas), esto es el evento único que consolida
 * el día y deja la caja en estado "Cerrada".
 */
public class CorteDAO {

    // función que suma los anticipos de los apartados creados desde que se abrió
    // la caja (Booking.bookingDate es solo DATE, así que se compara por día).
    //
    // OJO: no se puede leer Booking.advanceAmount directo — esa columna es mutable
    // (procesarNuevoAbono() y liquidarApartadoCompleto() la van actualizando con el
    // acumulado a la fecha, no el anticipo original). Por eso se busca el PRIMER
    // BookingPayment de cada folio (el de menor idBookingPayment), que es el único
    // que sí representa el anticipo real del momento en que se creó el apartado.
    public BigDecimal calcularApartadosNuevos(LocalDateTime desde) {
        BigDecimal total = BigDecimal.ZERO;

        String sql = "SELECT bp.paymentAmount "
                + "FROM BookingPayment bp "
                + "JOIN Booking b ON bp.idBooking = b.idBooking "
                + "WHERE b.bookingDate >= ? "
                + "AND bp.idBookingPayment = (SELECT MIN(bp2.idBookingPayment) FROM BookingPayment bp2 WHERE bp2.idBooking = bp.idBooking)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(desde.toLocalDate()));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    total = total.add(rs.getBigDecimal("paymentAmount"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al calcular apartados nuevos: " + e.getMessage());
        }
        return total;
    }

    // función que suma los abonos del día que NO son el anticipo inicial de un
    // apartado nuevo (ese ya se cuenta en calcularApartadosNuevos). Como
    // registrarApartadoCompleto() inserta el anticipo inicial como el primer
    // BookingPayment del folio, restar los "nuevos" del total de BookingPayment
    // del día deja exactamente los abonos posteriores (de folios nuevos o viejos).
    public BigDecimal calcularAbonosApartados(LocalDateTime desde, BigDecimal apartadosNuevos) {
        BigDecimal totalPagos = BigDecimal.ZERO;

        String sql = "SELECT paymentAmount FROM BookingPayment WHERE paymentDate >= ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(desde));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    totalPagos = totalPagos.add(rs.getBigDecimal("paymentAmount"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al calcular abonos de apartados: " + e.getMessage());
        }
        return totalPagos.subtract(apartadosNuevos);
    }

    // función para cerrar formalmente la caja: registra el conteo final y cambia
    // el estado a 'Cerrada'. En cuanto se cierra, CashSessionDAO.obtenerSesionAbierta()
    // deja de encontrarla, y eso ya bloquea ventas/arqueos hasta que se abra una
    // caja nueva — no hace falta ningún bloqueo adicional por fecha.
    public boolean cerrarCaja(int idCashSession, BigDecimal efectivoContado, BigDecimal saldoTeorico, BigDecimal diferencia) {
        String sql = "UPDATE CashSession SET closingDateTime = NOW(), finalCashAmount = ?, "
                + "theoricalAmount = ?, cashDifference = ?, sessionStatus = 'Cerrada' "
                + "WHERE idCashSession = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBigDecimal(1, efectivoContado);
            ps.setBigDecimal(2, saldoTeorico);
            ps.setBigDecimal(3, diferencia);
            ps.setInt(4, idCashSession);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al cerrar la caja: " + e.getMessage());
            return false;
        }
    }
}
