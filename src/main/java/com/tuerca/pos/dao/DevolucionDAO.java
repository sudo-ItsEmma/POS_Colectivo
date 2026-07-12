package com.tuerca.pos.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO de Devoluciones (FN.5). La devolución opera a nivel de línea de venta
 * ({@code SaleDetail}), no de la venta completa — así lo modela el esquema
 * ({@code ProductReturn.idSaleDetail}).
 */
public class DevolucionDAO {

    // Ventas activas con al menos un producto, para la tabla de Gestión de Devoluciones.
    // Se listan todas las 'Activa' (incluso si ya se devolvió alguna línea suya) porque
    // una venta puede tener varias líneas y solo algunas devueltas.
    public List<Object[]> buscarVentas(String filtro) {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT s.idSale, s.totalSaleAmount, COUNT(sd.idSaleDetail) AS totalProductos, " +
                     "s.saleDateTime, CONCAT(e.firstNameEmployee, ' ', e.lastNameEmployee) AS vendedor " +
                     "FROM Sale s " +
                     "JOIN SaleDetail sd ON sd.idSale = s.idSale " +
                     "JOIN UserAccount u ON s.idUserAccount = u.idUserAccount " +
                     "JOIN Employee e ON u.idEmployee = e.idEmployee " +
                     "WHERE s.saleStatus = 'Activa' " +
                     "AND (CAST(s.idSale AS CHAR) LIKE ? OR CONCAT(e.firstNameEmployee, ' ', e.lastNameEmployee) LIKE ?) " +
                     "GROUP BY s.idSale " +
                     "ORDER BY s.saleDateTime DESC";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String like = "%" + filtro + "%";
            ps.setString(1, like);
            ps.setString(2, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Object[]{
                        rs.getInt("idSale"),
                        rs.getDouble("totalSaleAmount"),
                        rs.getInt("totalProductos"),
                        rs.getTimestamp("saleDateTime"),
                        rs.getString("vendedor")
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar ventas para devolución: " + e.getMessage());
        }
        return lista;
    }

    // Líneas de una venta con su estado de devolución (para el diálogo de detalle).
    public List<Object[]> obtenerDetallesConEstado(int idSale) {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT sd.idSaleDetail, p.fullProductCode, p.productDescription, sd.quantitySold, " +
                     "sd.unitPriceAtSale, sd.subtotalDetail, (pr.idReturn IS NOT NULL) AS yaDevuelto " +
                     "FROM SaleDetail sd " +
                     "JOIN Product p ON sd.idProduct = p.idProduct " +
                     "LEFT JOIN ProductReturn pr ON pr.idSaleDetail = sd.idSaleDetail " +
                     "WHERE sd.idSale = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idSale);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Object[]{
                        rs.getInt("idSaleDetail"),
                        rs.getString("fullProductCode"),
                        rs.getString("productDescription"),
                        rs.getInt("quantitySold"),
                        rs.getDouble("unitPriceAtSale"),
                        rs.getDouble("subtotalDetail"),
                        rs.getBoolean("yaDevuelto")
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener detalles de la venta: " + e.getMessage());
        }
        return lista;
    }

    // Procesa la devolución de una línea de venta: registra el ProductReturn, devuelve el
    // stock, y si con esta ya quedaron devueltas TODAS las líneas de la venta, marca la
    // venta completa como 'Devuelta'. Todo dentro de una sola transacción.
    public boolean procesarDevolucion(int idSaleDetail, int idUserAccountAutoriza, String motivo, double montoReembolso) {
        String sqlInsertReturn = "INSERT INTO ProductReturn (idSaleDetail, idUserAccount, returnReason, refundAmount) VALUES (?, ?, ?, ?)";
        String sqlDatosLinea = "SELECT idSale, idProduct, quantitySold FROM SaleDetail WHERE idSaleDetail = ?";
        String sqlDevolverStock = "UPDATE Product SET currentStock = currentStock + ? WHERE idProduct = ?";
        String sqlLineasSinDevolver = "SELECT COUNT(*) FROM SaleDetail sd " +
                                       "LEFT JOIN ProductReturn pr ON pr.idSaleDetail = sd.idSaleDetail " +
                                       "WHERE sd.idSale = ? AND pr.idReturn IS NULL";
        String sqlMarcarVentaDevuelta = "UPDATE Sale SET saleStatus = 'Devuelta' WHERE idSale = ?";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            int idSale;
            int idProduct;
            int cantidad;
            try (PreparedStatement ps = con.prepareStatement(sqlDatosLinea)) {
                ps.setInt(1, idSaleDetail);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        return false;
                    }
                    idSale = rs.getInt("idSale");
                    idProduct = rs.getInt("idProduct");
                    cantidad = rs.getInt("quantitySold");
                }
            }

            // Registrar la devolución (el UNIQUE en idSaleDetail impide devolver la misma línea dos veces)
            try (PreparedStatement ps = con.prepareStatement(sqlInsertReturn)) {
                ps.setInt(1, idSaleDetail);
                ps.setInt(2, idUserAccountAutoriza);
                ps.setString(3, motivo);
                ps.setDouble(4, montoReembolso);
                ps.executeUpdate();
            }

            // Devolver el stock al inventario
            try (PreparedStatement ps = con.prepareStatement(sqlDevolverStock)) {
                ps.setInt(1, cantidad);
                ps.setInt(2, idProduct);
                ps.executeUpdate();
            }

            // Si ya no queda ninguna línea sin devolver, la venta completa pasa a 'Devuelta'
            try (PreparedStatement ps = con.prepareStatement(sqlLineasSinDevolver)) {
                ps.setInt(1, idSale);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) == 0) {
                        try (PreparedStatement psVenta = con.prepareStatement(sqlMarcarVentaDevuelta)) {
                            psVenta.setInt(1, idSale);
                            psVenta.executeUpdate();
                        }
                    }
                }
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("Error al procesar devolución: " + e.getMessage());
            return false;
        } finally {
            if (con != null) try { con.setAutoCommit(true); con.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}
