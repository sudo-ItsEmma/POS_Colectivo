/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.dao;

import com.tuerca.pos.model.Apartado;
import com.tuerca.pos.model.ApartadoDetail;
import java.sql.*;
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
}
