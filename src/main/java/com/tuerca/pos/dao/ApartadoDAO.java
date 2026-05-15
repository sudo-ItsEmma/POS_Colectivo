/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.dao;

import com.tuerca.pos.model.Apartado;
import com.tuerca.pos.model.ApartadoDetail;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;

/**
 *
 * @author mannycalderon
 */
public class ApartadoDAO {
    
    public boolean registrarApartadoCompleto(Apartado apt, List<ApartadoDetail> detalles) {
        String sqlBooking = "INSERT INTO Booking (idUserAccount, customerName, customerPhone, " +
                            "expirationDate, totalAmount, advanceAmount, pendingBalance) " +
                            "VALUES (?, ?, ?, DATE_ADD(CURDATE(), INTERVAL 14 DAY), ?, ?, ?)";

        String sqlDetail = "INSERT INTO BookingDetail (idBooking, idProduct, quantity, unitPrice, subtotalDetail) " +
                           "VALUES (?, ?, ?, ?, ?)";

        String sqlPayment = "INSERT INTO BookingPayment (idBooking, paymentAmount) VALUES (?, ?)";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            // 1. Insertar Cabecera
            try (PreparedStatement psB = con.prepareStatement(sqlBooking, Statement.RETURN_GENERATED_KEYS)) {
                psB.setInt(1, apt.getIdUserAccount());
                psB.setString(2, apt.getCustomerName());
                psB.setString(3, apt.getCustomerPhone());
                psB.setDouble(4, apt.getTotalAmount());
                psB.setDouble(5, apt.getAdvanceAmount());
                psB.setDouble(6, apt.getPendingBalance());
                psB.executeUpdate();

                try (ResultSet rs = psB.getGeneratedKeys()) {
                    if (rs.next()) {
                        int idGenerado = rs.getInt(1);

                        // 2. Insertar Detalles
                        try (PreparedStatement psD = con.prepareStatement(sqlDetail)) {
                            for (ApartadoDetail det : detalles) {
                                psD.setInt(1, idGenerado);
                                psD.setInt(2, det.getIdProduct());
                                psD.setInt(3, det.getQuantity());
                                psD.setDouble(4, det.getUnitPrice());
                                psD.setDouble(5, det.getSubtotalDetail());
                                psD.executeUpdate();
                            }
                        }

                        // 3. Registrar Abono Inicial
                        try (PreparedStatement psP = con.prepareStatement(sqlPayment)) {
                            psP.setInt(1, idGenerado);
                            psP.setDouble(2, apt.getAdvanceAmount());
                            psP.executeUpdate();
                        }
                    }
                }
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("Error en transacción de apartado: " + e.getMessage());
            return false;
        } finally {
            if (con != null) try { con.setAutoCommit(true); con.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
    
    public List<Apartado> listarApartados(String filtroNombre, String estado) {
        List<Apartado> lista = new ArrayList<>();
        // SQL dinámico: filtra por nombre y por el estado del ComboBox
        String sql = "SELECT * FROM Booking WHERE customerName LIKE ? AND bookingStatus = ? ORDER BY bookingDate DESC";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + filtroNombre + "%");
            ps.setString(2, estado);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Apartado a = new Apartado();
                    a.setIdBooking(rs.getInt("idBooking"));
                    a.setCustomerName(rs.getString("customerName"));
                    a.setTotalAmount(rs.getDouble("totalAmount"));
                    a.setAdvanceAmount(rs.getDouble("advanceAmount"));
                    a.setPendingBalance(rs.getDouble("pendingBalance"));
                    a.setExpirationDate(rs.getDate("expirationDate"));
                    a.setBookingStatus(rs.getString("bookingStatus"));
                    lista.add(a);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
    
    public Apartado obtenerApartadoPorId(int idFolio) {
        String sql = "SELECT idBooking, idUserAccount, customerName, customerPhone, " +
                     "totalAmount, advanceAmount, pendingBalance, bookingStatus, expirationDate " +
                     "FROM Booking WHERE idBooking = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idFolio);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Apartado apt = new Apartado();
                    apt.setIdBooking(rs.getInt("idBooking"));
                    apt.setIdUserAccount(rs.getInt("idUserAccount"));
                    apt.setCustomerName(rs.getString("customerName"));
                    apt.setCustomerPhone(rs.getString("customerPhone"));
                    apt.setTotalAmount(rs.getDouble("totalAmount"));
                    apt.setAdvanceAmount(rs.getDouble("advanceAmount"));
                    apt.setPendingBalance(rs.getDouble("pendingBalance"));
                    apt.setBookingStatus(rs.getString("bookingStatus"));
                    apt.setExpirationDate(rs.getDate("expirationDate"));
                    return apt;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener la cabecera del apartado por ID: " + e.getMessage());
        }
        return null; // Retorna null si no encuentra el folio o si ocurre un error
    }
    
    public List<Object[]> obtenerResumenDetallesPorFolio(int idFolio) {
        // Usamos Object[] para devolver una estructura mixta de datos sin modificar los modelos
        List<Object[]> lista = new ArrayList<>();

        // Consulta con JOIN para traer los datos de ambas tablas
        String sql = "SELECT d.quantity, p.fullProductCode, p.productDescription, d.unitPrice, d.subtotalDetail " +
                     "FROM BookingDetail d " +
                     "JOIN Product p ON d.idProduct = p.idProduct " +
                     "WHERE d.idBooking = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idFolio);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] fila = new Object[5];
                    fila[0] = rs.getInt("quantity");
                    fila[1] = rs.getString("fullProductCode");
                    fila[2] = rs.getString("productDescription");
                    fila[3] = rs.getDouble("unitPrice");
                    fila[4] = rs.getDouble("subtotalDetail");
                    lista.add(fila);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener detalles del apartado: " + e.getMessage());
        }
        return lista;
    }
    
    public boolean registrarNuevoAbono(int idBooking, double montoAbono) {
        String sqlInsertPago = "INSERT INTO BookingPayment (idBooking, paymentAmount) VALUES (?, ?)";
        String sqlUpdateBooking = "UPDATE Booking SET advanceAmount = advanceAmount + ?, " +
                                   "pendingBalance = pendingBalance - ? WHERE idBooking = ?";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false); // Iniciamos transacción

            // 1. Registrar el pago en el historial
            try (PreparedStatement psPago = con.prepareStatement(sqlInsertPago)) {
                psPago.setInt(1, idBooking);
                psPago.setDouble(2, montoAbono);
                psPago.executeUpdate();
            }

            // 2. Actualizar los saldos en la cabecera
            try (PreparedStatement psUpdate = con.prepareStatement(sqlUpdateBooking)) {
                psUpdate.setDouble(1, montoAbono);
                psUpdate.setDouble(2, montoAbono);
                psUpdate.setInt(3, idBooking);
                psUpdate.executeUpdate();
            }

            con.commit(); // Todo bien, guardamos cambios
            return true;
        } catch (SQLException e) {
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            System.err.println("Error al registrar abono: " + e.getMessage());
            return false;
        } finally {
            if (con != null) {
                try { con.setAutoCommit(true); con.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
    
    public boolean liquidarApartadoCompleto(int idBooking, int idUsuario, String metodoPago, List<Object[]> detallesProductos) throws SQLException {
        String sqlInsertVenta = "INSERT INTO Sale (idUserAccount, idPaymentMethod, totalSaleAmount, saleStatus) VALUES (?, ?, ?, 'Activa')";
        String sqlInsertVentaDetalle = "INSERT INTO SaleDetail (idSale, idProduct, quantitySold, unitPriceAtSale, discountApplied, subtotalDetail, isSettled) VALUES (?, ?, ?, ?, 0.00, ?, 1)";
        String sqlUpdateStock = "UPDATE Product SET currentStock = currentStock - ? WHERE idProduct = ? AND currentStock >= ?";
        String sqlUpdateBooking = "UPDATE Booking SET advanceAmount = totalAmount, pendingBalance = 0.00, bookingStatus = 'Liquidado' WHERE idBooking = ?";
        String sqlIdPago = "SELECT idPaymentMethod FROM PaymentMethod WHERE methodName = ?";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false); // Iniciamos Transacción

            // 1. Obtener ID del método de pago
            int idMetodoPago = 1; 
            try (PreparedStatement psPago = con.prepareStatement(sqlIdPago)) {
                psPago.setString(1, metodoPago);
                try (ResultSet rs = psPago.executeQuery()) {
                    if (rs.next()) idMetodoPago = rs.getInt("idPaymentMethod");
                }
            }

            // 2. Obtener el monto total del apartado
            double totalVenta = 0;
            String sqlTotalApt = "SELECT totalAmount FROM Booking WHERE idBooking = ?";
            try (PreparedStatement psTot = con.prepareStatement(sqlTotalApt)) {
                psTot.setInt(1, idBooking);
                try (ResultSet rs = psTot.executeQuery()) {
                    if (rs.next()) totalVenta = rs.getDouble("totalAmount");
                }
            }

            // 3. Insertar la Cabecera de la Venta
            int idVentaGenerada = 0;
            try (PreparedStatement psVenta = con.prepareStatement(sqlInsertVenta, Statement.RETURN_GENERATED_KEYS)) {
                psVenta.setInt(1, idUsuario);
                psVenta.setInt(2, idMetodoPago);
                psVenta.setDouble(3, totalVenta);
                psVenta.executeUpdate();

                try (ResultSet generatedKeys = psVenta.getGeneratedKeys()) {
                    if (generatedKeys.next()) idVentaGenerada = generatedKeys.getInt(1);
                    else throw new SQLException("No se pudo obtener el ID de la venta generada.");
                }
            }

            // 4. Procesar cada producto: Insertar detalle y Descontar Stock
            try (PreparedStatement psVentaDet = con.prepareStatement(sqlInsertVentaDetalle);
                 PreparedStatement psStock = con.prepareStatement(sqlUpdateStock)) {

                for (Object[] prod : detallesProductos) {
                    int cantidad = (int) prod[0];
                    String codigo = (String) prod[1];
                    double precio = (double) prod[3];
                    double subtotal = (double) prod[4];

                    // Consultar ID del producto por su código
                    int idProduct = 0;
                    String sqlIdProd = "SELECT idProduct FROM Product WHERE fullProductCode = ?";
                    try (PreparedStatement psIdP = con.prepareStatement(sqlIdProd)) {
                        psIdP.setString(1, codigo);
                        try (ResultSet rs = psIdP.executeQuery()) {
                            if (rs.next()) idProduct = rs.getInt("idProduct");
                        }
                    }

                    // A) Insertar en SaleDetail
                    psVentaDet.setInt(1, idVentaGenerada);
                    psVentaDet.setInt(2, idProduct);
                    psVentaDet.setInt(3, cantidad);
                    psVentaDet.setDouble(4, precio);
                    psVentaDet.setDouble(5, subtotal);
                    psVentaDet.addBatch();

                    // B) Actualizar Inventario con validación de stock
                    psStock.setInt(1, cantidad);
                    psStock.setInt(2, idProduct);
                    psStock.setInt(3, cantidad); 
                    int filasAfectadas = psStock.executeUpdate();

                    if (filasAfectadas == 0) {
                        // Lanzamos la excepción para cortar el flujo y que el controlador la cachee
                        throw new SQLException("Stock insuficiente para el producto con código: " + codigo);
                    }
                }
                psVentaDet.executeBatch(); 
            }

            // 5. Actualizar la cabecera del Apartado a 'Liquidado'
            try (PreparedStatement psBook = con.prepareStatement(sqlUpdateBooking)) {
                psBook.setInt(1, idBooking);
                psBook.executeUpdate();
            }

            con.commit(); // Todo correcto, guardamos la transacción
            return true;

        } catch (SQLException e) {
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            // Volvemos a lanzar la excepción para que el controlador lea el mensaje
            throw e; 
        } finally {
            if (con != null) {
                try { con.setAutoCommit(true); con.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
}
