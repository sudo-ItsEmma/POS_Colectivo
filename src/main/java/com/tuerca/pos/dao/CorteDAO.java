package com.tuerca.pos.dao;

import com.tuerca.pos.model.CashSession;
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
    // la caja actual.
    //
    // OJO: no se puede leer Booking.advanceAmount directo — esa columna es mutable
    // (procesarNuevoAbono() y liquidarApartadoCompleto() la van actualizando con el
    // acumulado a la fecha, no el anticipo original). Tampoco se puede filtrar por
    // Booking.bookingDate (es solo DATE, sin hora) — eso mezclaría apartados de una
    // sesión anterior del mismo día calendario con los de la sesión actual. En vez
    // de eso, se busca el PRIMER BookingPayment de cada folio (el de menor
    // idBookingPayment, que es el anticipo real) y se filtra por su propio
    // paymentDate (con precisión de hora), igual que calcularAbonosApartados() —
    // así ambos cálculos usan el mismo corte de tiempo y la resta entre ellos
    // siempre cuadra, sin importar cuántas sesiones haya habido en el mismo día.
    public BigDecimal calcularApartadosNuevos(LocalDateTime desde) {
        BigDecimal total = BigDecimal.ZERO;

        String sql = "SELECT bp.paymentAmount "
                + "FROM BookingPayment bp "
                + "WHERE bp.paymentDate >= ? "
                + "AND bp.idBookingPayment = (SELECT MIN(bp2.idBookingPayment) FROM BookingPayment bp2 WHERE bp2.idBooking = bp.idBooking)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(desde));

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

    // Igual que calcularApartadosNuevos() pero filtrado por método de pago
    // (Efectivo/Transferencia) — permite mostrar y persistir el desglose de los
    // anticipos nuevos, para poder cuadrar cuentas y reportes sin tener que releer
    // BookingPayment fila por fila.
    public BigDecimal calcularApartadosNuevosPorMetodo(LocalDateTime desde, String metodoPago) {
        BigDecimal total = BigDecimal.ZERO;

        String sql = "SELECT bp.paymentAmount FROM BookingPayment bp "
                + "JOIN PaymentMethod pm ON bp.idPaymentMethod = pm.idPaymentMethod "
                + "WHERE bp.paymentDate >= ? AND pm.methodName = ? "
                + "AND bp.idBookingPayment = (SELECT MIN(bp2.idBookingPayment) FROM BookingPayment bp2 WHERE bp2.idBooking = bp.idBooking)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(desde));
            ps.setString(2, metodoPago);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    total = total.add(rs.getBigDecimal("paymentAmount"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al calcular apartados nuevos por método: " + e.getMessage());
        }
        return total;
    }

    // Igual que calcularAbonosApartados() pero filtrado por método de pago: suma
    // todos los BookingPayment de ese método y le resta los anticipos nuevos de ese
    // mismo método, dejando solo los abonos posteriores por Efectivo/Transferencia.
    public BigDecimal calcularAbonosApartadosPorMetodo(LocalDateTime desde, String metodoPago, BigDecimal apartadosNuevosDelMetodo) {
        BigDecimal totalPagos = BigDecimal.ZERO;

        String sql = "SELECT bp.paymentAmount FROM BookingPayment bp "
                + "JOIN PaymentMethod pm ON bp.idPaymentMethod = pm.idPaymentMethod "
                + "WHERE bp.paymentDate >= ? AND pm.methodName = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(desde));
            ps.setString(2, metodoPago);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    totalPagos = totalPagos.add(rs.getBigDecimal("paymentAmount"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al calcular abonos de apartados por método: " + e.getMessage());
        }
        return totalPagos.subtract(apartadosNuevosDelMetodo);
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

    // función para cerrar formalmente la caja: registra el conteo final, el desglose
    // completo del día (para reportes/auditorías futuras sin tener que recalcular
    // desde Sale/BookingPayment) y cambia el estado a 'Cerrada'. En cuanto se cierra,
    // CashSessionDAO.obtenerSesionAbierta() deja de encontrarla, y eso ya bloquea
    // ventas/arqueos hasta que se abra una caja nueva — no hace falta ningún bloqueo
    // adicional por fecha.
    public boolean cerrarCaja(CashSession cierre) {
        String sql = "UPDATE CashSession SET closingDateTime = NOW(), finalCashAmount = ?, "
                + "theoricalAmount = ?, cashDifference = ?, cashSalesAmount = ?, "
                + "cashBookingPaymentsAmount = ?, transferSalesAmount = ?, transferSalesCount = ?, "
                + "bookingsNewAmount = ?, bookingsPaymentsAmount = ?, bookingsNewAmountTransfer = ?, "
                + "bookingsPaymentsAmountTransfer = ?, sessionStatus = 'Cerrada' "
                + "WHERE idCashSession = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBigDecimal(1, cierre.getFinalCashAmount());
            ps.setBigDecimal(2, cierre.getTheoricalAmount());
            ps.setBigDecimal(3, cierre.getCashDifference());
            ps.setBigDecimal(4, cierre.getCashSalesAmount());
            ps.setBigDecimal(5, cierre.getCashBookingPaymentsAmount());
            ps.setBigDecimal(6, cierre.getTransferSalesAmount());
            ps.setInt(7, cierre.getTransferSalesCount());
            ps.setBigDecimal(8, cierre.getBookingsNewAmount());
            ps.setBigDecimal(9, cierre.getBookingsPaymentsAmount());
            ps.setBigDecimal(10, cierre.getBookingsNewAmountTransfer());
            ps.setBigDecimal(11, cierre.getBookingsPaymentsAmountTransfer());
            ps.setInt(12, cierre.getIdCashSession());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al cerrar la caja: " + e.getMessage());
            return false;
        }
    }
}
