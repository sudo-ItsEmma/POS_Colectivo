/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.dao;

import com.tuerca.pos.model.Emprendedor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mannycalderon
 */
public class EmprendedorDAO {
    
    // registro de nuevo emprendimiento
    public boolean registrar(Emprendedor emp) {
        String sql = "INSERT INTO Entrepreneur (brandName, contactName, contactPhone, " +
                     "emailEntrepreneur, contractSignDate, monthlyRentAmount) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, emp.getMarca());
            ps.setString(2, emp.getNombreContacto());
            ps.setString(3, emp.getTelefono());
            ps.setString(4, emp.getEmail());
            ps.setDate(5, emp.getFechaContrato());
            ps.setDouble(6, emp.getRentaMensual());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al registrar emprendedor: " + e.getMessage());
            return false;
        }
    }
    
    // consultar emprendedores
    public List<Emprendedor> listar() {
        List<Emprendedor> lista = new ArrayList<>();
        String sql = "SELECT * FROM Entrepreneur WHERE isEntityActive = 1";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Emprendedor emp = new Emprendedor();
                emp.setId(rs.getInt("idEntrepreneur"));
                emp.setMarca(rs.getString("brandName"));
                emp.setNombreContacto(rs.getString("contactName"));
                emp.setTelefono(rs.getString("contactPhone"));
                emp.setEmail(rs.getString("emailEntrepreneur"));
                emp.setFechaContrato(rs.getDate("contractSignDate"));
                emp.setRentaMensual(rs.getDouble("monthlyRentAmount"));
                lista.add(emp);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar emprendedores: " + e.getMessage());
        }
        return lista;
    }
}
