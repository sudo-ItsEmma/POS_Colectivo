package com.tuerca.pos.dao;

import com.tuerca.pos.model.Settlement;
import com.tuerca.pos.pdf.dto.LineaReporteVenta;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de Pago a Emprendedores (FN.9). Una venta puede mezclar productos de
 * varios emprendedores, así que todo aquí opera sobre las líneas
 * ({@code SaleDetail}) que pertenecen al emprendedor seleccionado dentro de
 * cada venta, no sobre la venta completa.
 */
public class SettlementDAO {

    // Fecha del último Settlement de este emprendedor, en el mes calendario actual, que ya
    // incluyó cobro de renta (rentDiscount > 0) — o null si no se le ha cobrado renta este mes.
    // Evita cobrar la renta mensual dos veces si se le paga al emprendedor varias veces en el mes.
    public Date obtenerFechaUltimaRentaCobradaEsteMes(int idEntrepreneur) {
        String sql = "SELECT MAX(settlementDate) AS ultimaFecha FROM Settlement " +
                     "WHERE idEntrepreneur = ? AND rentDiscount > 0 " +
                     "AND YEAR(settlementDate) = YEAR(CURDATE()) AND MONTH(settlementDate) = MONTH(CURDATE())";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idEntrepreneur);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDate("ultimaFecha");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar renta cobrada este mes: " + e.getMessage());
        }
        return null;
    }

    // Una fila por venta (ticket): solo cuenta las líneas de esa venta que son del
    // emprendedor, están pendientes de pago (isSettled=0) y no fueron devueltas.
    public List<Object[]> listarVentasPendientes(int idEntrepreneur, Date fechaInicio, Date fechaFin) {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT s.idSale, s.saleDateTime, SUM(sd.subtotalDetail) AS bruto, SUM(sd.discountApplied) AS descuentos " +
                     "FROM Sale s " +
                     "JOIN SaleDetail sd ON sd.idSale = s.idSale " +
                     "JOIN Product p ON sd.idProduct = p.idProduct " +
                     "LEFT JOIN ProductReturn pr ON pr.idSaleDetail = sd.idSaleDetail " +
                     "WHERE p.idEntrepreneur = ? " +
                     "AND sd.isSettled = 0 " +
                     "AND pr.idReturn IS NULL " +
                     "AND DATE(s.saleDateTime) BETWEEN ? AND ? " +
                     "GROUP BY s.idSale, s.saleDateTime " +
                     "ORDER BY s.saleDateTime";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idEntrepreneur);
            ps.setDate(2, fechaInicio);
            ps.setDate(3, fechaFin);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Object[]{
                        rs.getInt("idSale"),
                        rs.getTimestamp("saleDateTime"),
                        rs.getDouble("bruto"),
                        rs.getDouble("descuentos")
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar ventas pendientes de pago: " + e.getMessage());
        }
        return lista;
    }

    // Registra el pago: recalcula bruto/descuentos de los tickets seleccionados dentro de la
    // misma transacción (no confía en los totales ya mostrados en pantalla, por si algo cambió
    // entre que se calculó y que se confirmó), inserta el Settlement, y marca isSettled=1 +
    // idSettlement en cada línea correspondiente.
    public boolean registrarPago(Settlement settlement, List<Integer> idSalesSeleccionados) {
        if (idSalesSeleccionados == null || idSalesSeleccionados.isEmpty()) {
            return false;
        }

        String inClause = idSalesSeleccionados.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");

        String sqlTotales = "SELECT SUM(sd.subtotalDetail) AS bruto, SUM(sd.discountApplied) AS descuentos " +
                             "FROM SaleDetail sd " +
                             "JOIN Product p ON sd.idProduct = p.idProduct " +
                             "LEFT JOIN ProductReturn pr ON pr.idSaleDetail = sd.idSaleDetail " +
                             "WHERE p.idEntrepreneur = ? AND sd.isSettled = 0 AND pr.idReturn IS NULL " +
                             "AND sd.idSale IN (" + inClause + ")";

        String sqlInsertSettlement = "INSERT INTO Settlement (idEntrepreneur, idUserAccount, settlementDate, " +
                                      "periodStartDate, periodEndDate, grossAmount, totalDiscounts, rentDiscount, otherDiscounts, netAmountPaid) " +
                                      "VALUES (?, ?, CURDATE(), ?, ?, ?, ?, ?, ?, ?)";

        String sqlMarcarLiquidado = "UPDATE SaleDetail sd " +
                                     "JOIN Product p ON sd.idProduct = p.idProduct " +
                                     "LEFT JOIN ProductReturn pr ON pr.idSaleDetail = sd.idSaleDetail " +
                                     "SET sd.isSettled = 1, sd.idSettlement = ? " +
                                     "WHERE p.idEntrepreneur = ? AND sd.isSettled = 0 AND pr.idReturn IS NULL " +
                                     "AND sd.idSale IN (" + inClause + ")";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            double bruto = 0;
            double descuentos = 0;
            try (PreparedStatement ps = con.prepareStatement(sqlTotales)) {
                ps.setInt(1, settlement.getIdEntrepreneur());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        bruto = rs.getDouble("bruto");
                        descuentos = rs.getDouble("descuentos");
                    }
                }
            }

            if (bruto <= 0) {
                // Los tickets seleccionados ya no tienen nada pendiente (se adelantó otro pago, o se devolvieron)
                con.rollback();
                return false;
            }

            double neto = bruto - descuentos - settlement.getRentDiscount() - settlement.getOtherDiscounts();

            int idSettlementGenerado;
            try (PreparedStatement ps = con.prepareStatement(sqlInsertSettlement, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, settlement.getIdEntrepreneur());
                ps.setInt(2, settlement.getIdUserAccount());
                ps.setDate(3, settlement.getPeriodStartDate());
                ps.setDate(4, settlement.getPeriodEndDate());
                ps.setDouble(5, bruto);
                ps.setDouble(6, descuentos);
                ps.setDouble(7, settlement.getRentDiscount());
                ps.setDouble(8, settlement.getOtherDiscounts());
                ps.setDouble(9, neto);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        idSettlementGenerado = rs.getInt(1);
                    } else {
                        throw new SQLException("No se pudo obtener el ID del pago generado.");
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(sqlMarcarLiquidado)) {
                ps.setInt(1, idSettlementGenerado);
                ps.setInt(2, settlement.getIdEntrepreneur());
                ps.executeUpdate();
            }

            settlement.setIdSettlement(idSettlementGenerado);
            settlement.setGrossAmount(bruto);
            settlement.setTotalDiscounts(descuentos);
            settlement.setNetAmountPaid(neto);

            con.commit();
            return true;
        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("Error al registrar pago a emprendedor: " + e.getMessage());
            return false;
        } finally {
            if (con != null) try { con.setAutoCommit(true); con.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // Detalle producto por producto de un pago ya registrado (para el comprobante en PDF).
    // Se consulta por idSettlement en vez de repetir el filtro de tickets/emprendedor: es la
    // fuente de verdad de qué líneas quedaron marcadas por ESTE pago exactamente.
    public List<LineaReporteVenta> obtenerDetallesDelPago(int idSettlement) {
        String sql = "SELECT sd.idSale, s.saleDateTime, p.fullProductCode, p.productDescription, " +
                     "sd.quantitySold, sd.unitPriceAtSale, sd.discountApplied, sd.subtotalDetail, sd.isSettled " +
                     "FROM SaleDetail sd " +
                     "JOIN Sale s ON sd.idSale = s.idSale " +
                     "JOIN Product p ON sd.idProduct = p.idProduct " +
                     "WHERE sd.idSettlement = ? " +
                     "ORDER BY sd.idSale, sd.idSaleDetail";

        return ejecutarConsultaDeLineas(sql, idSettlement);
    }

    // Estado de ventas de un periodo libre (FN.10): TODAS las ventas del emprendedor en el
    // rango, pagadas o no (decisión confirmada con el usuario) — sigue excluyendo las líneas
    // devueltas, igual que el resto de los cálculos de este DAO.
    public List<LineaReporteVenta> obtenerDetalleVentasDelPeriodo(int idEntrepreneur, Date fechaInicio, Date fechaFin) {
        String sql = "SELECT sd.idSale, s.saleDateTime, p.fullProductCode, p.productDescription, " +
                     "sd.quantitySold, sd.unitPriceAtSale, sd.discountApplied, sd.subtotalDetail, sd.isSettled " +
                     "FROM SaleDetail sd " +
                     "JOIN Sale s ON sd.idSale = s.idSale " +
                     "JOIN Product p ON sd.idProduct = p.idProduct " +
                     "LEFT JOIN ProductReturn pr ON pr.idSaleDetail = sd.idSaleDetail " +
                     "WHERE p.idEntrepreneur = ? AND pr.idReturn IS NULL " +
                     "AND DATE(s.saleDateTime) BETWEEN ? AND ? " +
                     "ORDER BY s.saleDateTime, sd.idSaleDetail";

        List<LineaReporteVenta> lista = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idEntrepreneur);
            ps.setDate(2, fechaInicio);
            ps.setDate(3, fechaFin);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearLinea(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener el detalle de ventas del periodo: " + e.getMessage());
        }
        return lista;
    }

    private List<LineaReporteVenta> ejecutarConsultaDeLineas(String sql, int parametro) {
        List<LineaReporteVenta> lista = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearLinea(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener el detalle del pago: " + e.getMessage());
        }
        return lista;
    }

    private LineaReporteVenta mapearLinea(ResultSet rs) throws SQLException {
        return new LineaReporteVenta(
                rs.getInt("idSale"),
                rs.getTimestamp("saleDateTime"),
                rs.getString("fullProductCode"),
                rs.getString("productDescription"),
                rs.getInt("quantitySold"),
                rs.getDouble("unitPriceAtSale"),
                rs.getDouble("discountApplied"),
                rs.getDouble("subtotalDetail"),
                rs.getBoolean("isSettled")
        );
    }
}
