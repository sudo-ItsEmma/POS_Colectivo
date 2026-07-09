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
    
    // función para autenticar un login: valida usuario+contraseña contra UserAccount
    // y no revela si falló el usuario o la contraseña (mismo resultado: null)
    public Empleado autenticar(String username, String password) {
        String sql = "SELECT e.idEmployee, e.firstNameEmployee, e.lastNameEmployee, e.secondLastNameEmployee, "
                + "u.idUserAccount, u.passwordAccount, u.idRole, u.isAccountActive, r.roleName "
                + "FROM UserAccount u "
                + "JOIN Employee e ON u.idEmployee = e.idEmployee "
                + "JOIN Role r ON u.idRole = r.idRole "
                + "WHERE u.usernameAccount = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                if (rs.getInt("isAccountActive") == 0) {
                    return null;
                }

                if (!BCrypt.checkpw(password, rs.getString("passwordAccount"))) {
                    return null;
                }

                Empleado emp = new Empleado();
                emp.setId(rs.getInt("idEmployee"));
                emp.setIdUserAccount(rs.getInt("idUserAccount"));
                emp.setNombre(rs.getString("firstNameEmployee"));
                emp.setPaterno(rs.getString("lastNameEmployee"));
                emp.setMaterno(rs.getString("secondLastNameEmployee"));
                emp.setUsername(username);
                emp.setIdRole(rs.getInt("idRole"));
                emp.setRoleName(rs.getString("roleName"));

                actualizarUltimoLogin(rs.getInt("idUserAccount"));

                return emp;
            }
        } catch (SQLException e) {
            System.err.println("Error en autenticación: " + e.getMessage());
            return null;
        }
    }

    private void actualizarUltimoLogin(int idUserAccount) {
        String sql = "UPDATE UserAccount SET lastLoginDate = NOW() WHERE idUserAccount = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUserAccount);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al actualizar último login: " + e.getMessage());
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
        String sqlEmpleado = "UPDATE Employee SET isEmployeeActive = 0 WHERE idEmployee = ?";
        String sqlUsuario = "UPDATE UserAccount SET isAccountActive = 0 WHERE idEmployee = ?";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false); // Iniciamos la transacción

            // 1. Desactivar al Empleado
            try (PreparedStatement psEmp = con.prepareStatement(sqlEmpleado)) {
                psEmp.setInt(1, id);
                psEmp.executeUpdate();
            }

            // 2. Desactivar la Cuenta de Usuario
            try (PreparedStatement psUser = con.prepareStatement(sqlUsuario)) {
                psUser.setInt(1, id);
                psUser.executeUpdate();
            }

            con.commit();
            return true;

        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback(); // Si falla uno, deshacemos ambos
                } catch (SQLException rollbackEx) {
                    System.err.println("Error en rollback: " + rollbackEx.getMessage());
                }
            }
            System.err.println("Error en eliminación lógica (Transaction): " + e.getMessage());
            return false;
        } finally {
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
    
    public List<Empleado> buscarAvanzado(String texto, boolean verInactivos) {
        List<Empleado> lista = new ArrayList<>();
        int estado = verInactivos ? 0 : 1;

        StringBuilder sql = new StringBuilder(
            "SELECT e.*, r.roleName " +
            "FROM Employee e " +
            "JOIN UserAccount ua ON e.idEmployee = ua.idEmployee " + // Primer salto
            "JOIN Role r ON ua.idRole = r.idRole " +                // Segundo salto
            "WHERE e.isEmployeeActive = " + estado + " "
        );

        if (!texto.isEmpty()) {
            sql.append("AND (e.firstNameEmployee LIKE ? OR e.lastNameEmployee LIKE ?) ");
        }

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            if (!texto.isEmpty()) {
                String query = "%" + texto.toUpperCase() + "%";
                ps.setString(1, query);
                ps.setString(2, query);
                ps.setString(3, query);
            }

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
            System.err.println("Error en búsqueda de empleados: " + e.getMessage());
        }
        return lista;
    }
    
    
    
    public boolean activarEmpleado(int id) {
        String sqlEmpleado = "UPDATE Employee SET isEmployeeActive = 1 WHERE idEmployee = ?";
        String sqlUsuario = "UPDATE UserAccount SET isAccountActive = 1 WHERE idEmployee = ?";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false); // Iniciamos transacción

            // 1. Reactivar al Empleado
            try (PreparedStatement psEmp = con.prepareStatement(sqlEmpleado)) {
                psEmp.setInt(1, id);
                psEmp.executeUpdate();
            }

            // 2. Reactivar la Cuenta de Usuario vinculada
            try (PreparedStatement psUser = con.prepareStatement(sqlUsuario)) {
                psUser.setInt(1, id);
                psUser.executeUpdate();
            }

            con.commit(); // Si ambos tienen éxito, guardamos cambios
            return true;

        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback(); // Si falla uno, no activamos nada
                } catch (SQLException rollbackEx) {
                    System.err.println("Error en rollback: " + rollbackEx.getMessage());
                }
            }
            System.err.println("Error en reactivación (Transaction): " + e.getMessage());
            return false;
        } finally {
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
}
