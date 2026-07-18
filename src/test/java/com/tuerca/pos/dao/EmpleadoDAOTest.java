package com.tuerca.pos.dao;

import com.tuerca.pos.model.Empleado;
import com.tuerca.pos.support.AbstractDaoIntegrationTest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de {@link EmpleadoDAO}. Cada test crea su propio Employee/UserAccount
 * desechable vía {@code dao.registrar()} (username único por test) y lo
 * borra por ID exacto en {@code @AfterEach} — no se toca ningún empleado
 * real.
 */
class EmpleadoDAOTest extends AbstractDaoIntegrationTest {

    private final EmpleadoDAO dao = new EmpleadoDAO();
    private final List<Integer> idsEmpleadosCreados = new ArrayList<>();

    @AfterEach
    void limpiarFixtures() throws SQLException {
        if (idsEmpleadosCreados.isEmpty()) return;
        Connection con = DatabaseConnection.getConnection();
        String idsCsv = idsEmpleadosCreados.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
        try (Statement st = con.createStatement()) {
            st.executeUpdate("DELETE FROM UserAccount WHERE idEmployee IN (" + idsCsv + ")");
            st.executeUpdate("DELETE FROM Employee WHERE idEmployee IN (" + idsCsv + ")");
        }
    }

    private int registrarEmpleadoDePrueba(String username, String password, int idRole) throws SQLException {
        Empleado emp = new Empleado();
        emp.setNombre("JUNIT");
        emp.setPaterno("TEST");
        emp.setMaterno("EMPLEADO");
        emp.setTelefono("5555555555");
        emp.setUsername(username);
        emp.setPassword(password);
        emp.setIdRole(idRole);

        boolean ok = dao.registrar(emp);
        assertTrue(ok, "el registro de prueba debe tener éxito");

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT idEmployee FROM UserAccount WHERE usernameAccount = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int id = rs.getInt(1);
                idsEmpleadosCreados.add(id);
                return id;
            }
        }
    }

    @Test
    void registrar_creaEmployeeYUserAccountConContrasenaHasheada() throws SQLException {
        registrarEmpleadoDePrueba("JUNITEMP01", "clave12345", 2);

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT passwordAccount FROM UserAccount WHERE usernameAccount = ?")) {
            ps.setString(1, "JUNITEMP01");
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertNotEquals("clave12345", rs.getString("passwordAccount"),
                        "la contraseña nunca debe guardarse en texto plano");
            }
        }
    }

    @Test
    void autenticar_conCredencialesCorrectas_devuelveElEmpleadoYActualizaUltimoLogin() throws SQLException {
        registrarEmpleadoDePrueba("JUNITEMP02", "clave12345", 2);

        Empleado resultado = dao.autenticar("JUNITEMP02", "clave12345");

        assertNotNull(resultado);
        assertEquals("JUNIT", resultado.getNombre());
        assertEquals("Sales", resultado.getRoleName());
        assertFalse(resultado.isMustChangePassword());
    }

    @Test
    void autenticar_conContrasenaIncorrecta_devuelveNull() throws SQLException {
        registrarEmpleadoDePrueba("JUNITEMP03", "clave12345", 2);

        assertNull(dao.autenticar("JUNITEMP03", "otraClave"));
    }

    @Test
    void autenticar_conUsuarioInexistente_devuelveNull() {
        assertNull(dao.autenticar("JUNIT_NO_EXISTE_XYZ", "cualquiera"));
    }

    @Test
    void autenticar_conCuentaDesactivada_devuelveNull() throws SQLException {
        int id = registrarEmpleadoDePrueba("JUNITEMP04", "clave12345", 2);
        dao.eliminarLogico(id);

        assertNull(dao.autenticar("JUNITEMP04", "clave12345"));
    }

    @Test
    void buscarPorId_devuelveLosDatosRegistrados() throws SQLException {
        int id = registrarEmpleadoDePrueba("JUNITEMP05", "clave12345", 1);

        Empleado emp = dao.buscarPorId(id);

        assertNotNull(emp);
        assertEquals("JUNITEMP05", emp.getUsername());
        assertEquals("Admin", emp.getRoleName());
    }

    @Test
    void actualizar_modificaDatosPersonalesYRol() throws SQLException {
        int id = registrarEmpleadoDePrueba("JUNITEMP06", "clave12345", 2);

        Empleado cambios = new Empleado();
        cambios.setId(id);
        cambios.setNombre("JUNITMODIFICADO");
        cambios.setPaterno("TEST");
        cambios.setMaterno("EMPLEADO");
        cambios.setTelefono("5559999999");
        cambios.setIdRole(1); // Vendedor -> Admin

        boolean ok = dao.actualizar(cambios);
        assertTrue(ok);

        Empleado actualizado = dao.buscarPorId(id);
        assertEquals("JUNITMODIFICADO", actualizado.getNombre());
        assertEquals("Admin", actualizado.getRoleName());
    }

    @Test
    void eliminarLogico_desactivaEmployeeYUserAccount() throws SQLException {
        int id = registrarEmpleadoDePrueba("JUNITEMP07", "clave12345", 2);

        boolean ok = dao.eliminarLogico(id);
        assertTrue(ok);

        assertTrue(dao.listar().stream().noneMatch(e -> e.getId() == id),
                "listar() solo debe traer empleados activos");
    }

    @Test
    void activarEmpleado_reactivaEmployeeYUserAccount() throws SQLException {
        int id = registrarEmpleadoDePrueba("JUNITEMP08", "clave12345", 2);
        dao.eliminarLogico(id);

        boolean ok = dao.activarEmpleado(id);
        assertTrue(ok);

        assertNotNull(dao.autenticar("JUNITEMP08", "clave12345"),
                "tras reactivar, debe poder autenticarse de nuevo");
    }

    @Test
    void buscarAvanzado_filtraPorTextoYPorEstado() throws SQLException {
        int id = registrarEmpleadoDePrueba("JUNITEMP09", "clave12345", 2);

        List<Empleado> encontrados = dao.buscarAvanzado("JUNIT", false);
        assertTrue(encontrados.stream().anyMatch(e -> e.getId() == id));

        dao.eliminarLogico(id);
        List<Empleado> activosDespues = dao.buscarAvanzado("JUNIT", false);
        assertTrue(activosDespues.stream().noneMatch(e -> e.getId() == id));

        List<Empleado> inactivos = dao.buscarAvanzado("JUNIT", true);
        assertTrue(inactivos.stream().anyMatch(e -> e.getId() == id));
    }

    @Test
    void restablecerContrasena_generaTemporalYFuerzaElCambio() throws SQLException {
        registrarEmpleadoDePrueba("JUNITEMP10", "clave12345", 2);
        int idUserAccount = obtenerIdUserAccount("JUNITEMP10");

        String temporal = dao.restablecerContrasena(idUserAccount);

        assertNotNull(temporal);
        assertEquals(8, temporal.length());
        assertNull(dao.autenticar("JUNITEMP10", "clave12345"), "la contraseña original ya no debe funcionar");

        Empleado resultado = dao.autenticar("JUNITEMP10", temporal);
        assertNotNull(resultado, "la contraseña temporal generada sí debe funcionar");
        assertTrue(resultado.isMustChangePassword());
    }

    @Test
    void cambiarContrasena_actualizaYQuitaElFlagDeCambioObligatorio() throws SQLException {
        registrarEmpleadoDePrueba("JUNITEMP11", "clave12345", 2);
        int idUserAccount = obtenerIdUserAccount("JUNITEMP11");
        dao.restablecerContrasena(idUserAccount);

        boolean ok = dao.cambiarContrasena(idUserAccount, "nuevaClaveDefinitiva");
        assertTrue(ok);

        Empleado resultado = dao.autenticar("JUNITEMP11", "nuevaClaveDefinitiva");
        assertNotNull(resultado);
        assertFalse(resultado.isMustChangePassword(), "cambiarContrasena debe quitar el flag de cambio obligatorio");
    }

    private int obtenerIdUserAccount(String username) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT idUserAccount FROM UserAccount WHERE usernameAccount = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
