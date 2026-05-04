/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.dao;

import com.tuerca.pos.model.Empleado;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import org.mindrot.jbcrypt.BCrypt;

/**
 *
 * @author mannycalderon
 */
public class EmpleadoDAO {
    
    // función para registrar un empleado y su usuario en la base de datos
    // mejora: hacerlo atomico y en cadena, si se registra el empleado correctamente, llamar a otra función
    // que haga el registro del usuario 
    public boolean registrar(Empleado emp){
        
        // gensalt() genera el salt y hashpw hace el trabajo pesado
        String passwordEncriptada = BCrypt.hashpw(emp.getPassword(), BCrypt.gensalt());
        
        String sqlEmployee = "INSERT INTO Employee (firstNameEmployee, lastNameEmployee, secondLastNameEmployee, phoneEmployee) VALUES (?, ?, ?, ?)";
        String sqlUser = "INSERT INTO UserAccount (idEmployee, idRole, usernameAccount, passwordAccount) VALUES (?, ?, ?, ?)";
        
        Connection con = null;
        PreparedStatement psEmp = null;
        PreparedStatement psUser = null;
        
        try{
            // obtenemos la conexión de la clase DatabaseConnection
            con = DatabaseConnection.getConnection();
            
            // desactivamos el autocommit para manejar la transacción manualmente
            con.setAutoCommit(false);
            
            // insertamos en la tabla Employee
            psEmp = con.prepareStatement(sqlEmployee, Statement.RETURN_GENERATED_KEYS);
            psEmp.setString(1, emp.getNombre());
            psEmp.setString(2, emp.getPaterno());
            psEmp.setString(3, emp.getMaterno());
            psEmp.setString(4, emp.getTelefono());
            
            int affectedRows = psEmp.executeUpdate();
            
            if(affectedRows == 0){
                throw new SQLException("No se pudo crear el registro del empleado");
            }
            
            // obtenemos el id generado (idEmployee)
            int idGenerado;
            try(ResultSet rs = psEmp.getGeneratedKeys()){
                if(rs.next()){
                    idGenerado = rs.getInt(1);
                } else{
                    throw new SQLException("Error al obtener el ID del empleado");
                }
            }
            
            // insertar en la tabla UserAccount usando el ID obtenido
            psUser = con.prepareStatement(sqlUser);
            psUser.setInt(1, idGenerado);
            psUser.setInt(2, emp.getIdRole());
            psUser.setString(3, emp.getUsername());
            psUser.setString(4, passwordEncriptada);
            
            psUser.executeUpdate();
            
            // si todo salio bion, confirmamos el commit en la BD
            con.commit();
            //System.out.println("¡COMMIT ejecutado con éxito en la base de datos!");
            return true;
        
        } catch(SQLException e){
            // si algo falla, deshacemos cualquier cambio para evitar datos incompletos
            if(con != null){
                try{
                    con.rollback();
                    System.out.println("Transacción revertida debido a un error.");
                } catch (SQLException ex){
                    System.out.println("Error en rollback: " + ex.getMessage());
                }
            }
            
            JOptionPane.showMessageDialog(null, "Error en la base de datos: "+ e.getMessage());
            return false;
        } finally {
            // reseteamos el autocommit, no cerramos la conexion pero si los preparedstatement para liberar recursos
            try {
                if(psEmp != null) psEmp.close();
                if(psUser != null) psUser.close();
                if(con != null) con.setAutoCommit(true);
            } catch(SQLException e){
                System.out.println("Error al cerrar recursos: " + e.getMessage());
            }
        }
        
    }
    
    // función que muestra los empleados registrados en el sistema
    public List<Empleado> listar() {
        List<Empleado> lista = new ArrayList<>();
        // JOIN para traer el nombre del rol basado en el idRole de la cuenta
        String sql = "SELECT e.*, r.roleName FROM Employee e " +
                     "JOIN UserAccount u ON e.idEmployee = u.idEmployee " +
                     "JOIN Role r ON u.idRole = r.idRole " +
                     "WHERE e.isEmployeeActive = 1";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Empleado emp = new Empleado();
                emp.setId(rs.getInt("idEmployee"));
                emp.setNombre(rs.getString("firstNameEmployee"));
                emp.setPaterno(rs.getString("lastNameEmployee"));
                emp.setMaterno(rs.getString("secondLastNameEmployee"));
                emp.setTelefono(rs.getString("phoneEmployee"));
                // Usamos un campo temporal o el objeto para el nombre del rol
                emp.setRoleName(rs.getString("roleName")); 
                lista.add(emp);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar: " + e.getMessage());
        }
        return lista;
    }
    
    
    // función para eliminar a los empleados de manera lógica
    public boolean eliminarLogico(int id) {
        String sql = "UPDATE Employee SET isEmployeeActive = 0 WHERE idEmployee = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error en eliminación lógica: " + e.getMessage());
            return false;
        }
    }
    
    // función para obtener el empleado que vamos a editar
    public Empleado buscarPorId(int id) {
        String sql = "SELECT e.*, u.idRole, r.roleName FROM Employee e " +
                     "JOIN UserAccount u ON e.idEmployee = u.idEmployee " +
                     "JOIN Role r ON u.idRole = r.idRole " +
                     "WHERE e.idEmployee = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Empleado emp = new Empleado();
                emp.setId(rs.getInt("idEmployee"));
                emp.setNombre(rs.getString("firstNameEmployee"));
                emp.setPaterno(rs.getString("lastNameEmployee"));
                emp.setMaterno(rs.getString("secondLastNameEmployee"));
                emp.setTelefono(rs.getString("phoneEmployee"));
                emp.setRoleName(rs.getString("roleName"));
                return emp;
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar empleado: " + e.getMessage());
        }
        return null;
    }
    
    // función para actualizar al empleado
    public boolean actualizar(Empleado emp) {
        String sqlEmpleado = "UPDATE Employee SET firstNameEmployee=?, lastNameEmployee=?, "
                + "secondLastNameEmployee=?, phoneEmployee=? WHERE idEmployee=?";

        String sqlUsuario = "UPDATE UserAccount SET idRole=? WHERE idEmployee=?";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            // Desactivamos el auto-commit para manejar la transacción manualmente
            con.setAutoCommit(false);

            // 1. Actualizar datos personales en Employee
            try (PreparedStatement psEmp = con.prepareStatement(sqlEmpleado)) {
                psEmp.setString(1, emp.getNombre());
                psEmp.setString(2, emp.getPaterno());
                psEmp.setString(3, emp.getMaterno());
                psEmp.setString(4, emp.getTelefono());
                psEmp.setInt(5, emp.getId());
                psEmp.executeUpdate();
            }

            // 2. Actualizar el Rol en UserAccount
            try (PreparedStatement psUser = con.prepareStatement(sqlUsuario)) {
                psUser.setInt(1, emp.getIdRole()); // El ID del rol (1 o 2)
                psUser.setInt(2, emp.getId());
                psUser.executeUpdate();
            }

            // Si todo salió bien, confirmamos los cambios
            con.commit();
            return true;

        } catch (SQLException e) {
            // Si algo falla, deshacemos lo que se haya alcanzado a hacer
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Error en rollback: " + rollbackEx.getMessage());
                }
            }
            System.err.println("Error al actualizar (Transaction): " + e.getMessage());
            return false;
        } finally {
            // Siempre restauramos el estado de la conexión
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
    
    public List<Empleado> buscar(String texto) {
        List<Empleado> lista = new java.util.ArrayList<>();
        // Buscamos por nombre, paterno o materno que contengan el texto
        String sql = "SELECT e.*, r.roleName FROM Employee e " +
                     "JOIN UserAccount u ON e.idEmployee = u.idEmployee " +
                     "JOIN Role r ON u.idRole = r.idRole " +
                     "WHERE e.isEmployeeActive = 1 AND (" +
                     "e.firstNameEmployee LIKE ? OR " +
                     "e.lastNameEmployee LIKE ? OR " +
                     "e.secondLastNameEmployee LIKE ?) " +
                     "ORDER BY e.idEmployee ASC";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String query = "%" + texto + "%";
            ps.setString(1, query);
            ps.setString(2, query);
            ps.setString(3, query);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Empleado emp = new Empleado();
                emp.setId(rs.getInt("idEmployee"));
                emp.setNombre(rs.getString("firstNameEmployee"));
                emp.setPaterno(rs.getString("lastNameEmployee"));
                emp.setMaterno(rs.getString("secondLastNameEmployee"));
                emp.setTelefono(rs.getString("phoneEmployee"));
                emp.setRoleName(rs.getString("roleName"));
                lista.add(emp);
            }
        } catch (SQLException e) {
            System.err.println("Error en búsqueda: " + e.getMessage());
        }
        return lista;
    }
}
