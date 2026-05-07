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
    
     // función para eliminar a los empleados de manera lógica
    public boolean eliminarLogico(int id) {
        String sql = "UPDATE Product SET isProductActive = 0 WHERE idProduct = ?";
        Connection con = null;

        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false); // Iniciamos transacción

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                int filasAfectadas = ps.executeUpdate();

                if (filasAfectadas > 0) {
                    con.commit();
                    return true;
                } else {
                    con.rollback();
                    return false;
                }
            }
        } catch (SQLException e) {
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            System.err.println("Error en eliminación lógica de producto: " + e.getMessage());
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
    
    // buscar el producto por id
    public Producto buscarPorId(int id) {
        String sql = "SELECT * FROM Product WHERE idProduct = ?";
        
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Producto pro = new Producto();
                pro.setIdProduct(rs.getInt("idProduct"));
                pro.setIdEntrepreneur(rs.getInt("idEntrepreneur"));
                pro.setFullProductCode(rs.getString("fullProductCode"));
                pro.setProductDescription(rs.getString("productDescription"));
                pro.setDepartment(rs.getString("department"));
                pro.setCurrentPrice(rs.getDouble("currentPrice"));
                pro.setCurrentStock(rs.getInt("currentStock"));
                return pro;
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar empleado: " + e.getMessage());
        }
        return null;
    }
    
    public boolean actualizar(Producto p) {
        String sql = "UPDATE Product SET idEntrepreneur = ?, fullProductCode = ?, productDescription = ?, " +
                     "department = ?, currentPrice = ?, currentStock = ? WHERE idProduct = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, p.getIdEntrepreneur());
            ps.setString(2, p.getFullProductCode());
            ps.setString(3, p.getProductDescription());
            ps.setString(4, p.getDepartment());
            ps.setDouble(5, p.getCurrentPrice());
            ps.setInt(6, p.getCurrentStock());
            ps.setInt(7, p.getIdProduct());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar producto: " + e.getMessage());
            return false;
        }
    }
    
    public List<Producto> buscar(String criterio) {
        List<Producto> lista = new ArrayList<>();
        // Buscamos en código O en descripción, siempre que estén activos
        String sql = "SELECT p.*, e.brandName FROM Product p " +
                     "JOIN Entrepreneur e ON p.idEntrepreneur = e.idEntrepreneur " +
                     "WHERE (p.fullProductCode LIKE ? OR p.productDescription LIKE ?) " +
                     "AND p.isProductActive = 1";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String query = "%" + criterio + "%";
            ps.setString(1, query);
            ps.setString(2, query);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Producto pro = new Producto();
                pro.setIdProduct(rs.getInt("idProduct"));
                pro.setFullProductCode(rs.getString("fullProductCode"));
                pro.setProductDescription(rs.getString("productDescription"));
                pro.setBrandName(rs.getString("brandName")); // Para mostrar en la tabla
                pro.setCurrentPrice(rs.getDouble("currentPrice"));
                pro.setCurrentStock(rs.getInt("currentStock"));
                lista.add(pro);
            }
        } catch (SQLException e) {
            System.err.println("Error en búsqueda dinámica: " + e.getMessage());
        }
        return lista;
    }
    
    public List<Producto> buscarAvanzado(String texto, int idEmp) {
        List<Producto> lista = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT p.*, e.brandName FROM Product p " +
            "JOIN Entrepreneur e ON p.idEntrepreneur = e.idEntrepreneur " +
            "WHERE p.isProductActive = 1 "
        );

        if (!texto.isEmpty()) {
            // SQL detectará coincidencias sin importar si el usuario escribe minúsculas
            sql.append("AND (p.fullProductCode LIKE ? OR p.productDescription LIKE ?) ");
        }

        // Si seleccionó un emprendedor específico (ID > 0), filtramos por él
        if (idEmp > 0) {
            sql.append("AND p.idEntrepreneur = ? ");
        }

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (!texto.isEmpty()) {
                String query = "%" + texto.toUpperCase() + "%";
                ps.setString(paramIndex++, query);
                ps.setString(paramIndex++, query);
            }
            if (idEmp > 0) {
                ps.setInt(paramIndex, idEmp);
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Producto pro = new Producto();
                pro.setIdProduct(rs.getInt("idProduct"));
                pro.setFullProductCode(rs.getString("fullProductCode"));
                pro.setProductDescription(rs.getString("productDescription"));
                pro.setBrandName(rs.getString("brandName")); // <-- Verifica que este nombre coincida con tu JOIN
                pro.setCurrentPrice(rs.getDouble("currentPrice"));
                pro.setCurrentStock(rs.getInt("currentStock"));
                lista.add(pro);
            }
        } catch (SQLException e) {
            System.err.println("Error en búsqueda avanzada: " + e.getMessage());
        }
        return lista;
    }
}
