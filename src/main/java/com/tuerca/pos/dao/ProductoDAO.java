/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.dao;

import com.tuerca.pos.model.Producto;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mannycalderon
 */
public class ProductoDAO {
    // 1. LISTAR TODOS (Con JOIN para traer el nombre de la marca)
    public List<Producto> listarTodos() {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT p.*, e.brandName FROM Product p " +
                     "JOIN Entrepreneur e ON p.idEntrepreneur = e.idEntrepreneur " +
                     "WHERE p.isProductActive = 1 ORDER BY p.idProduct ASC";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearProducto(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar productos: " + e.getMessage());
        }
        return lista;
    }

    // 2. BUSCAR CON FILTRO (Marca + Texto de búsqueda)
    public List<Producto> buscar(String texto, int idEmprendedor) {
        List<Producto> lista = new ArrayList<>();
        // Construimos la base de la consulta
        StringBuilder sql = new StringBuilder(
            "SELECT p.*, e.brandName FROM Product p " +
            "JOIN Entrepreneur e ON p.idEntrepreneur = e.idEntrepreneur " +
            "WHERE p.isProductActive = 1 "
        );

        // Si hay un emprendedor seleccionado en el ComboBox (id > 0)
        if (idEmprendedor > 0) {
            sql.append("AND p.idEntrepreneur = ? ");
        }

        // Si hay texto en la barra de búsqueda
        if (!texto.isEmpty()) {
            sql.append("AND (p.fullProductCode LIKE ? OR p.productDescription LIKE ?) ");
        }

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            int index = 1;
            if (idEmprendedor > 0) ps.setInt(index++, idEmprendedor);
            if (!texto.isEmpty()) {
                String likeText = "%" + texto + "%";
                ps.setString(index++, likeText);
                ps.setString(index++, likeText);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearProducto(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en búsqueda dinámica: " + e.getMessage());
        }
        return lista;
    }

    // Método auxiliar para no repetir código de mapeo
    private Producto mapearProducto(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setIdProduct(rs.getInt("idProduct"));
        p.setIdEntrepreneur(rs.getInt("idEntrepreneur"));
        p.setBrandName(rs.getString("brandName")); // Viene del JOIN
        p.setFullProductCode(rs.getString("fullProductCode"));
        p.setProductDescription(rs.getString("productDescription"));
        p.setDepartment(rs.getString("department"));
        p.setCurrentPrice(rs.getDouble("currentPrice"));
        p.setCurrentStock(rs.getInt("currentStock"));
        p.setMinStockAlert(rs.getInt("minStockAlert"));
        return p;
    }
    
    // registrar producto
    public boolean registrar(Producto p) {
        String sql = "INSERT INTO Product (idEntrepreneur, fullProductCode, productDescription, " +
                     "department, currentPrice, currentStock, minStockAlert) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, p.getIdEntrepreneur());
            ps.setString(2, p.getFullProductCode());
            ps.setString(3, p.getProductDescription());
            ps.setString(4, p.getDepartment());
            ps.setDouble(5, p.getCurrentPrice());
            ps.setInt(6, p.getCurrentStock());
            ps.setInt(7, p.getMinStockAlert());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al registrar producto: " + e.getMessage());
            return false;
        }
    }
}
