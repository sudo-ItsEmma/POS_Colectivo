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
    
    // función para eliminar a los empleados de manera lógica
        public boolean eliminarLogico(int id) {
        String sqlEmp = "UPDATE Entrepreneur SET isEntityActive = 0 WHERE idEntrepreneur = ?";
        String sqlProd = "UPDATE Product SET isProductActive = 0 WHERE idEntrepreneur = ?";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false); // Iniciamos transacción

            // 1. Desactivar todos sus productos ligados primero
            try (PreparedStatement psProd = con.prepareStatement(sqlProd)) {
                psProd.setInt(1, id);
                psProd.executeUpdate();
            }

            // 2. Desactivar Emprendedor después
            try (PreparedStatement psEmp = con.prepareStatement(sqlEmp)) {
                psEmp.setInt(1, id);
                psEmp.executeUpdate();
            }

            con.commit();
            return true;
        }catch (SQLException e) {
            // ESTO TE DIRÁ EL ERROR REAL (Ej: "Foreign key constraint fails")
            System.err.println("Error detallado: " + e.getMessage()); 
            if (con != null) try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        }
        finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    
    
    // buscar al emprendimiento por id
    public Emprendedor buscarPorId(int id) {
        String sql = "SELECT * FROM Entrepreneur WHERE idEntrepreneur = ?";
        
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Emprendedor emp = new Emprendedor();
                emp.setId(rs.getInt("idEntrepreneur"));
                emp.setMarca(rs.getString("brandName"));
                emp.setNombreContacto(rs.getString("contactName"));
                emp.setTelefono(rs.getString("contactPhone"));
                emp.setEmail(rs.getString("emailEntrepreneur"));
                emp.setRentaMensual(rs.getDouble("monthlyRentAmount"));
                emp.setFechaContrato(rs.getDate("contractSignDate"));
                return emp;
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar empleado: " + e.getMessage());
        }
        return null;
    }
    
    // función para actualizar al emprendimiento
    public boolean actualizar(Emprendedor emp) {
        String sql = "UPDATE Entrepreneur SET brandName = ?, contactName = ?, contactPhone = ?, "
               + "emailEntrepreneur = ?, contractSignDate = ?, monthlyRentAmount = ? "
               + "WHERE idEntrepreneur = ?";
    
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, emp.getMarca());
            ps.setString(2, emp.getNombreContacto());
            ps.setString(3, emp.getTelefono());
            ps.setString(4, emp.getEmail());
            ps.setDate(5, emp.getFechaContrato());
            ps.setDouble(6, emp.getRentaMensual());
            ps.setInt(7, emp.getId()); // El ID que recuperamos en prepararEdicion

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al actualizar emprendedor: " + e.getMessage());
            return false;
        }
    }
    
    public List<Emprendedor> buscarAvanzado(String texto, boolean verInactivos) {
        List<Emprendedor> lista = new ArrayList<>();
        int estado = verInactivos ? 0 : 1;

        // Usamos StringBuilder para manejar el texto opcional
        StringBuilder sql = new StringBuilder("SELECT * FROM Entrepreneur WHERE isEntityActive = " + estado);

        if (!texto.isEmpty()) {
            sql.append(" AND (brandName LIKE ? OR contactName LIKE ?)");
        }

        sql.append(" ORDER BY brandName ASC");

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            if (!texto.isEmpty()) {
                String busqueda = "%" + texto + "%";
                ps.setString(1, busqueda);
                ps.setString(2, busqueda);
            }

            try (ResultSet rs = ps.executeQuery()) {
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
            }
        } catch (SQLException e) {
            System.err.println("Error en búsqueda avanzada de emprendedores: " + e.getMessage());
        }
        return lista;
    }

    public boolean activar(int id) {
        String sql = "UPDATE Entrepreneur SET isEntityActive = 1 WHERE idEntrepreneur = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al activar emprendedor: " + e.getMessage());
            return false;
        }
    }
    
    // Método para llenar el ComboBox de filtros en la Gestión de Productos
    public List<Emprendedor> listarNombresYId() {
        List<Emprendedor> lista = new ArrayList<>();
        // Solo necesitamos el ID y el Nombre para el Combo
        String sql = "SELECT idEntrepreneur, brandName FROM Entrepreneur WHERE isEntityActive = 1 ORDER BY brandName ASC";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Emprendedor emp = new Emprendedor();
                emp.setId(rs.getInt("idEntrepreneur"));
                emp.setMarca(rs.getString("brandName"));
                lista.add(emp);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar nombres de emprendedores: " + e.getMessage());
        }
        return lista;
    }
}
