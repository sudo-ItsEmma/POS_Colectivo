/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.dao;

import com.tuerca.pos.model.Apartado;
import com.tuerca.pos.model.ApartadoDetail;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;

/**
 *
 * @author mannycalderon
 */
public class ApartadoDAO {
    
    public boolean registrarApartadoCompleto(Apartado apt, List<ApartadoDetail> detalles) throws SQLException {
        String sqlBooking = "INSERT INTO Booking (idUserAccount, customerName, customerPhone, " +
                            "expirationDate, totalAmount, advanceAmount, pendingBalance) " +
                            "VALUES (?, ?, ?, DATE_ADD(CURDATE(), INTERVAL 14 DAY), ?, ?, ?)";

        String sqlDetail = "INSERT INTO BookingDetail (idBooking, idProduct, quantity, unitPrice, subtotalDetail) " +
                           "VALUES (?, ?, ?, ?, ?)";

        String sqlPayment = "INSERT INTO BookingPayment (idBooking, paymentAmount) VALUES (?, ?)";

        // Mismo patrón atómico que ya usa liquidarApartadoCompleto(): si no alcanza el stock,
        // 0 filas afectadas y se lanza la excepción para que se revierta toda la transacción.
        String sqlUpdateStock = "UPDATE Product SET currentStock = currentStock - ? WHERE idProduct = ? AND currentStock >= ?";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            // 1. Insertar Cabecera
            try (PreparedStatement psB = con.prepareStatement(sqlBooking, Statement.RETURN_GENERATED_KEYS)) {
                psB.setInt(1, apt.getIdUserAccount());
                psB.setString(2, apt.getCustomerName());
                psB.setString(3, apt.getCustomerPhone());
                psB.setBigDecimal(4, apt.getTotalAmount());
                psB.setBigDecimal(5, apt.getAdvanceAmount());
                psB.setBigDecimal(6, apt.getPendingBalance());
                psB.executeUpdate();

                try (ResultSet rs = psB.getGeneratedKeys()) {
                    if (rs.next()) {
                        int idGenerado = rs.getInt(1);

                        // 2. Insertar Detalles y reservar stock (para que el producto ya no
                        // aparezca disponible mientras el apartado esté Activo)
                        try (PreparedStatement psD = con.prepareStatement(sqlDetail);
                             PreparedStatement psStock = con.prepareStatement(sqlUpdateStock)) {
                            for (ApartadoDetail det : detalles) {
                                psD.setInt(1, idGenerado);
                                psD.setInt(2, det.getIdProduct());
                                psD.setInt(3, det.getQuantity());
                                psD.setBigDecimal(4, det.getUnitPrice());
                                psD.setBigDecimal(5, det.getSubtotalDetail());
                                psD.executeUpdate();

                                psStock.setInt(1, det.getQuantity());
                                psStock.setInt(2, det.getIdProduct());
                                psStock.setInt(3, det.getQuantity());
                                if (psStock.executeUpdate() == 0) {
                                    throw new SQLException("Stock insuficiente para el producto con ID: " + det.getIdProduct());
                                }
                            }
                        }

                        // 3. Registrar Abono Inicial
                        try (PreparedStatement psP = con.prepareStatement(sqlPayment)) {
                            psP.setInt(1, idGenerado);
                            psP.setBigDecimal(2, apt.getAdvanceAmount());
                            psP.executeUpdate();
                        }
                    }
                }
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            throw e;
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
                    a.setTotalAmount(rs.getBigDecimal("totalAmount"));
                    a.setAdvanceAmount(rs.getBigDecimal("advanceAmount"));
                    a.setPendingBalance(rs.getBigDecimal("pendingBalance"));
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
                    apt.setTotalAmount(rs.getBigDecimal("totalAmount"));
                    apt.setAdvanceAmount(rs.getBigDecimal("advanceAmount"));
                    apt.setPendingBalance(rs.getBigDecimal("pendingBalance"));
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
                    fila[3] = rs.getBigDecimal("unitPrice");
                    fila[4] = rs.getBigDecimal("subtotalDetail");
                    lista.add(fila);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener detalles del apartado: " + e.getMessage());
        }
        return lista;
    }
    
    // Busca el idPaymentMethod real a partir del nombre elegido en pantalla (Efectivo/
    // Transferencia). Si por algún motivo no se encuentra, cae en Efectivo (id 1) como
    // valor seguro, en vez de fallar la transacción completa por esto.
    private int obtenerIdMetodoPago(Connection con, String metodoPago) throws SQLException {
        String sql = "SELECT idPaymentMethod FROM PaymentMethod WHERE methodName = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, metodoPago);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("idPaymentMethod");
            }
        }
        return 1;
    }

    public boolean registrarNuevoAbono(int idBooking, BigDecimal montoAbono, String metodoPago) {
        String sqlInsertPago = "INSERT INTO BookingPayment (idBooking, idPaymentMethod, paymentAmount) VALUES (?, ?, ?)";
        String sqlUpdateBooking = "UPDATE Booking SET advanceAmount = advanceAmount + ?, " +
                                   "pendingBalance = pendingBalance - ? WHERE idBooking = ?";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false); // Iniciamos transacción

            int idMetodoPago = obtenerIdMetodoPago(con, metodoPago);

            // 1. Registrar el pago en el historial
            try (PreparedStatement psPago = con.prepareStatement(sqlInsertPago)) {
                psPago.setInt(1, idBooking);
                psPago.setInt(2, idMetodoPago);
                psPago.setBigDecimal(3, montoAbono);
                psPago.executeUpdate();
            }

            // 2. Actualizar los saldos en la cabecera
            try (PreparedStatement psUpdate = con.prepareStatement(sqlUpdateBooking)) {
                psUpdate.setBigDecimal(1, montoAbono);
                psUpdate.setBigDecimal(2, montoAbono);
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
        String sqlInsertVenta = "INSERT INTO Sale (idUserAccount, idPaymentMethod, totalSaleAmount, saleStatus, idBooking) VALUES (?, ?, ?, 'Activa', ?)";
        // isSettled queda en su DEFAULT 0 (pendiente de pago al emprendedor), igual que una
        // venta normal (VentaDAO.registrarVenta()) — antes se hardcodeaba a 1 aquí, lo que
        // hacía que el emprendedor nunca cobrara por productos vendidos vía apartado.
        String sqlInsertVentaDetalle = "INSERT INTO SaleDetail (idSale, idProduct, quantitySold, unitPriceAtSale, discountApplied, subtotalDetail) VALUES (?, ?, ?, ?, 0.00, ?)";
        String sqlUpdateBooking = "UPDATE Booking SET advanceAmount = totalAmount, pendingBalance = 0.00, bookingStatus = 'Liquidado' WHERE idBooking = ?";
        // El pago final (lo que realmente entra en efectivo/transferencia al liquidar, no el
        // total del apartado) se registra como un abono más, igual que el anticipo inicial y
        // los abonos posteriores — así BookingPayment queda con el historial completo.
        String sqlInsertPagoFinal = "INSERT INTO BookingPayment (idBooking, idPaymentMethod, paymentAmount) VALUES (?, ?, ?)";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false); // Iniciamos Transacción

            // 1. Obtener ID del método de pago
            int idMetodoPago = obtenerIdMetodoPago(con, metodoPago);

            // 2. Obtener el monto total del apartado y el saldo pendiente (lo que se cobra ahora)
            BigDecimal totalVenta = BigDecimal.ZERO;
            BigDecimal saldoPendiente = BigDecimal.ZERO;
            String sqlTotalApt = "SELECT totalAmount, pendingBalance FROM Booking WHERE idBooking = ?";
            try (PreparedStatement psTot = con.prepareStatement(sqlTotalApt)) {
                psTot.setInt(1, idBooking);
                try (ResultSet rs = psTot.executeQuery()) {
                    if (rs.next()) {
                        totalVenta = rs.getBigDecimal("totalAmount");
                        saldoPendiente = rs.getBigDecimal("pendingBalance");
                    }
                }
            }

            // 3. Insertar la Cabecera de la Venta (con idBooking para que Arqueo/Corte de Caja
            // puedan excluirla del cálculo de efectivo/transferencia — su dinero ya se cuenta
            // a través de BookingPayment, sumarla también aquí duplicaría el anticipo)
            int idVentaGenerada = 0;
            try (PreparedStatement psVenta = con.prepareStatement(sqlInsertVenta, Statement.RETURN_GENERATED_KEYS)) {
                psVenta.setInt(1, idUsuario);
                psVenta.setInt(2, idMetodoPago);
                psVenta.setBigDecimal(3, totalVenta);
                psVenta.setInt(4, idBooking);
                psVenta.executeUpdate();

                try (ResultSet generatedKeys = psVenta.getGeneratedKeys()) {
                    if (generatedKeys.next()) idVentaGenerada = generatedKeys.getInt(1);
                    else throw new SQLException("No se pudo obtener el ID de la venta generada.");
                }
            }

            // 3.1 Registrar el pago final como abono, solo si en efecto quedaba saldo por cobrar
            if (saldoPendiente.compareTo(BigDecimal.ZERO) > 0) {
                try (PreparedStatement psPagoFinal = con.prepareStatement(sqlInsertPagoFinal)) {
                    psPagoFinal.setInt(1, idBooking);
                    psPagoFinal.setInt(2, idMetodoPago);
                    psPagoFinal.setBigDecimal(3, saldoPendiente);
                    psPagoFinal.executeUpdate();
                }
            }

            // 4. Procesar cada producto: Insertar detalle en SaleDetail. El stock NO se toca
            // aquí — ya se reservó/descontó al crear el apartado (registrarApartadoCompleto()),
            // descontarlo otra vez al liquidar sería un doble descuento.
            try (PreparedStatement psVentaDet = con.prepareStatement(sqlInsertVentaDetalle)) {

                for (Object[] prod : detallesProductos) {
                    int cantidad = (int) prod[0];
                    String codigo = (String) prod[1];
                    BigDecimal precio = (BigDecimal) prod[3];
                    BigDecimal subtotal = (BigDecimal) prod[4];

                    // Consultar ID del producto por su código
                    int idProduct = 0;
                    String sqlIdProd = "SELECT idProduct FROM Product WHERE fullProductCode = ?";
                    try (PreparedStatement psIdP = con.prepareStatement(sqlIdProd)) {
                        psIdP.setString(1, codigo);
                        try (ResultSet rs = psIdP.executeQuery()) {
                            if (rs.next()) idProduct = rs.getInt("idProduct");
                        }
                    }

                    // Insertar en SaleDetail
                    psVentaDet.setInt(1, idVentaGenerada);
                    psVentaDet.setInt(2, idProduct);
                    psVentaDet.setInt(3, cantidad);
                    psVentaDet.setBigDecimal(4, precio);
                    psVentaDet.setBigDecimal(5, subtotal);
                    psVentaDet.addBatch();
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

    // Cancela un apartado Activo y devuelve al inventario el stock que se había reservado
    // al crearlo (registrarApartadoCompleto()). Solo aplica sobre apartados 'Activo' — un
    // apartado ya Liquidado o Cancelado no puede cancelarse otra vez.
    public boolean cancelarApartado(int idBooking) {
        String sqlDetalles = "SELECT idProduct, quantity FROM BookingDetail WHERE idBooking = ?";
        String sqlDevolverStock = "UPDATE Product SET currentStock = currentStock + ? WHERE idProduct = ?";
        String sqlCancelarBooking = "UPDATE Booking SET bookingStatus = 'Cancelado' WHERE idBooking = ? AND bookingStatus = 'Activo'";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            try (PreparedStatement psCancel = con.prepareStatement(sqlCancelarBooking)) {
                psCancel.setInt(1, idBooking);
                if (psCancel.executeUpdate() == 0) {
                    // No había ningún Booking 'Activo' con ese id (ya liquidado/cancelado, o no existe)
                    con.rollback();
                    return false;
                }
            }

            try (PreparedStatement psDet = con.prepareStatement(sqlDetalles);
                 PreparedStatement psStock = con.prepareStatement(sqlDevolverStock)) {
                psDet.setInt(1, idBooking);
                try (ResultSet rs = psDet.executeQuery()) {
                    while (rs.next()) {
                        psStock.setInt(1, rs.getInt("quantity"));
                        psStock.setInt(2, rs.getInt("idProduct"));
                        psStock.addBatch();
                    }
                }
                psStock.executeBatch();
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("Error al cancelar apartado: " + e.getMessage());
            return false;
        } finally {
            if (con != null) try { con.setAutoCommit(true); con.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}
