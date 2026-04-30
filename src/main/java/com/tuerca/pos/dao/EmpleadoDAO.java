/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.dao;

import com.tuerca.pos.model.Empleado;
import java.sql.*;
import javax.swing.JOptionPane;

/**
 *
 * @author mannycalderon
 */
public class EmpleadoDAO {
    
    public boolean registrar(Empleado emp){
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
            psUser.setString(4, emp.getPassword());
            
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
}
