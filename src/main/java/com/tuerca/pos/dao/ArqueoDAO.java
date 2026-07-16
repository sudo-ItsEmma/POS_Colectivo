package com.tuerca.pos.dao;

import com.tuerca.pos.model.CashCount;
import com.tuerca.pos.model.CashSession;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Arqueo de caja (FN.7): calcula el saldo teórico de la sesión abierta y
 * registra la comparación contra el efectivo contado. A diferencia del
 * corte de caja, esto se puede hacer varias veces al día sin bloquear
 * ventas — por eso cada arqueo queda como una fila en {@code CashCount},
 * no como parte de {@code CashSession}.
 */
public class ArqueoDAO {

    // función que calcula el saldo teórico: fondo de apertura + ventas en efectivo
    // (incluida la parte efectivo de las ventas Mixto) + abonos de apartados en
    // efectivo, todo desde que se abrió la caja actual.
    //
    // Las ventas con idBooking (las que genera liquidarApartadoCompleto()) se
    // excluyen a propósito: su dinero ya se cuenta a través de BookingPayment
    // (anticipo + abonos + pago final), así que sumarlas también aquí duplicaría
    // el anticipo que el cliente ya había dado antes de liquidar.
    //
    // NOTA: desde el Paso 13, los abonos parciales de apartados también preguntan el
    // método de pago real (ApartadoController.procesarPago() / ApartadoDAO.registrarNuevoAbono()),
    // así que este filtro por 'Efectivo' ya excluye correctamente los abonos por
    // transferencia — antes todos caían siempre en Efectivo por default, así que el
    // filtro nunca se ponía a prueba.
    public BigDecimal calcularSaldoTeorico(CashSession sesion) {
        BigDecimal saldo = sesion.getInitialCashAmount();
        saldo = saldo.add(calcularVentasEfectivo(sesion.getOpeningDateTime()));
        saldo = saldo.add(calcularAbonosEfectivo(sesion.getOpeningDateTime()));
        return saldo;
    }

    public BigDecimal calcularVentasEfectivo(LocalDateTime desde) {
        BigDecimal total = BigDecimal.ZERO;

        String sql = "SELECT pm.methodName, s.totalSaleAmount, s.paymentDetails "
                + "FROM Sale s "
                + "JOIN PaymentMethod pm ON s.idPaymentMethod = pm.idPaymentMethod "
                + "WHERE s.saleDateTime >= ? AND pm.methodName IN ('Efectivo', 'Mixto') AND s.idBooking IS NULL";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(desde));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String metodo = rs.getString("methodName");

                    if (metodo.equalsIgnoreCase("Efectivo")) {
                        total = total.add(rs.getBigDecimal("totalSaleAmount"));
                    } else {
                        // Mixto: solo cuenta la parte que sí entró en físico ("E:100.00|T:150.00")
                        total = total.add(extraerMontoDeMixto(rs.getString("paymentDetails"), "E:"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al calcular ventas en efectivo: " + e.getMessage());
        }
        return total;
    }

    // función informativa: cuánto de lo vendido fue por transferencia, para que el
    // arqueo pueda mostrarlo por separado (ese dinero no cuenta para el saldo en
    // caja, pero el cajero necesita verlo para poder cuadrar sus números).
    public BigDecimal calcularVentasTransferencia(LocalDateTime desde) {
        BigDecimal total = BigDecimal.ZERO;

        String sql = "SELECT pm.methodName, s.totalSaleAmount, s.paymentDetails "
                + "FROM Sale s "
                + "JOIN PaymentMethod pm ON s.idPaymentMethod = pm.idPaymentMethod "
                + "WHERE s.saleDateTime >= ? AND pm.methodName IN ('Transferencia', 'Mixto') AND s.idBooking IS NULL";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(desde));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String metodo = rs.getString("methodName");

                    if (metodo.equalsIgnoreCase("Transferencia")) {
                        total = total.add(rs.getBigDecimal("totalSaleAmount"));
                    } else {
                        // Mixto: solo cuenta la parte que se transfirió ("E:100.00|T:150.00")
                        total = total.add(extraerMontoDeMixto(rs.getString("paymentDetails"), "T:"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al calcular ventas por transferencia: " + e.getMessage());
        }
        return total;
    }

    // función informativa para el Corte de Caja: cuántas ventas del día incluyeron
    // una parte de transferencia (puras o Mixto), para el resumen de transferencias.
    public int contarVentasConTransferencia(LocalDateTime desde) {
        int cantidad = 0;

        String sql = "SELECT pm.methodName, s.paymentDetails "
                + "FROM Sale s "
                + "JOIN PaymentMethod pm ON s.idPaymentMethod = pm.idPaymentMethod "
                + "WHERE s.saleDateTime >= ? AND pm.methodName IN ('Transferencia', 'Mixto') AND s.idBooking IS NULL";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(desde));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String metodo = rs.getString("methodName");
                    if (metodo.equalsIgnoreCase("Transferencia")
                            || extraerMontoDeMixto(rs.getString("paymentDetails"), "T:").compareTo(BigDecimal.ZERO) > 0) {
                        cantidad++;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al contar ventas con transferencia: " + e.getMessage());
        }
        return cantidad;
    }

    private BigDecimal extraerMontoDeMixto(String paymentDetails, String prefijo) {
        if (paymentDetails == null) {
            return BigDecimal.ZERO;
        }
        for (String parte : paymentDetails.split("\\|")) {
            if (parte.startsWith(prefijo)) {
                return new BigDecimal(parte.substring(prefijo.length()));
            }
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal calcularAbonosEfectivo(LocalDateTime desde) {
        BigDecimal total = BigDecimal.ZERO;

        String sql = "SELECT bp.paymentAmount "
                + "FROM BookingPayment bp "
                + "JOIN PaymentMethod pm ON bp.idPaymentMethod = pm.idPaymentMethod "
                + "WHERE bp.paymentDate >= ? AND pm.methodName = 'Efectivo'";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(desde));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    total = total.add(rs.getBigDecimal("paymentAmount"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al calcular abonos en efectivo: " + e.getMessage());
        }
        return total;
    }

    // función para llenar la tabla "Ventas realizadas": una fila por venta (no por
    // producto), con el desglose Efectivo/Transferencia de cada una. Antes solo
    // mostraba el subtotal de cada producto, y en una venta Mixta eso hacía parecer
    // que todo el monto era efectivo (ej. una venta de $80 con $50 en transferencia
    // se veía igual que una venta de $80 en efectivo) — así el cajero puede sumar
    // la columna Efectivo a mano y verificar el saldo teórico él mismo.
    public List<Object[]> obtenerVentasDesde(LocalDateTime desde) {
        List<Object[]> lista = new ArrayList<>();

        String sql = "SELECT s.saleDateTime, pm.methodName, s.totalSaleAmount, s.paymentDetails "
                + "FROM Sale s "
                + "JOIN PaymentMethod pm ON s.idPaymentMethod = pm.idPaymentMethod "
                + "WHERE s.saleDateTime >= ? AND s.idBooking IS NULL "
                + "ORDER BY s.saleDateTime";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(desde));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String metodo = rs.getString("methodName");
                    BigDecimal total = rs.getBigDecimal("totalSaleAmount");
                    String detalles = rs.getString("paymentDetails");

                    BigDecimal montoEfectivo;
                    BigDecimal montoTransferencia;

                    if (metodo.equalsIgnoreCase("Efectivo")) {
                        montoEfectivo = total;
                        montoTransferencia = BigDecimal.ZERO;
                    } else if (metodo.equalsIgnoreCase("Transferencia")) {
                        montoEfectivo = BigDecimal.ZERO;
                        montoTransferencia = total;
                    } else {
                        montoEfectivo = extraerMontoDeMixto(detalles, "E:");
                        montoTransferencia = extraerMontoDeMixto(detalles, "T:");
                    }

                    String hora = rs.getTimestamp("saleDateTime").toLocalDateTime()
                            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

                    lista.add(new Object[]{hora, metodo, total, montoEfectivo, montoTransferencia});
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener las ventas del día: " + e.getMessage());
        }
        return lista;
    }

    // función para dejar registro auditable del arqueo (puede haber varios por sesión)
    public boolean registrarArqueo(CashCount registro) {
        String sql = "INSERT INTO CashCount (idCashSession, idUserAccount, theoricalAmount, "
                + "countedAmount, cashDifference, justificationComment) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, registro.getIdCashSession());
            ps.setInt(2, registro.getIdUserAccount());
            ps.setBigDecimal(3, registro.getTheoricalAmount());
            ps.setBigDecimal(4, registro.getCountedAmount());
            ps.setBigDecimal(5, registro.getCashDifference());
            ps.setString(6, registro.getJustificationComment());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al registrar el arqueo: " + e.getMessage());
            return false;
        }
    }
}
