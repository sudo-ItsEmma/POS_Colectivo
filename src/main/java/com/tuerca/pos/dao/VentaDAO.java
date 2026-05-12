/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.dao;

import com.tuerca.pos.model.DetalleVenta;
import com.tuerca.pos.model.Venta;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 *
 * @author mannycalderon
 */
public class VentaDAO {
    
    public boolean registrarVenta(Venta venta, List<DetalleVenta> detalles) {
        String sqlVenta = "INSERT INTO Sale (idUserAccount, idPaymentMethod, totalSaleAmount, paymentDetails) VALUES (?, ?, ?, ?)";
        String sqlDetalle = "INSERT INTO SaleDetail (idSale, idProduct, quantitySold, unitPriceAtSale, discountApplied, subtotalDetail) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlStock = "UPDATE Product SET currentStock = currentStock - ? WHERE idProduct = ?";

        Connection con = null;

        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false); 

            // 1. Mapeo de IDs de Pago (Asegúrate de que 'Mixto' sea ID 3 en tu DB)
            int idMetodo;
            if (venta.getMetodoPago().equalsIgnoreCase("Efectivo")) {
                idMetodo = 1;
            } else if (venta.getMetodoPago().equalsIgnoreCase("Transferencia")) {
                idMetodo = 2;
            } else {
                idMetodo = 3; // ID para el nuevo método 'Mixto'
            }

            // 2. Insertar Cabecera de la Venta (Se hace una sola vez)
            try (PreparedStatement psVenta = con.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                psVenta.setInt(1, venta.getIdUsuario());
                psVenta.setInt(2, idMetodo);
                psVenta.setDouble(3, venta.getTotal());
                // Guardamos el detalle (E:100|T:150) o null si es pago simple
                psVenta.setString(4, venta.getPaymentDetails()); 
                psVenta.executeUpdate();

                try (ResultSet rs = psVenta.getGeneratedKeys()) {
                    if (rs.next()) {
                        int idGenerado = rs.getInt(1);

                        // 3. Insertar Detalles y Actualizar Stock (Bucle)
                        try (PreparedStatement psDetalle = con.prepareStatement(sqlDetalle);
                             PreparedStatement psStock = con.prepareStatement(sqlStock)) {

                            for (DetalleVenta item : detalles) {
                                // SaleDetail
                                psDetalle.setInt(1, idGenerado);
                                psDetalle.setInt(2, item.getIdProducto());
                                psDetalle.setInt(3, item.getCantidad());
                                psDetalle.setDouble(4, item.getPrecioUnitario());
                                psDetalle.setDouble(5, item.getDescuento());
                                psDetalle.setDouble(6, item.getSubtotal());
                                psDetalle.executeUpdate();

                                // Update Stock
                                psStock.setInt(1, item.getCantidad());
                                psStock.setInt(2, item.getIdProducto());
                                psStock.executeUpdate();
                            }
                        }
                    }
                }
            }

            con.commit(); 
            return true;

        } catch (SQLException e) {
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            System.err.println("Error en transacción de venta: " + e.getMessage());
            return false;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
}
