package com.tuerca.pos.dao;

import com.tuerca.pos.model.Emprendedor;
import com.tuerca.pos.support.AbstractDaoIntegrationTest;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de {@link EmprendedorDAO}. Cada test crea su propio Entrepreneur
 * (marca única) y lo borra por ID exacto en {@code @AfterEach}.
 */
class EmprendedorDAOTest extends AbstractDaoIntegrationTest {

    private final EmprendedorDAO dao = new EmprendedorDAO();
    private Integer idCreado;

    @AfterEach
    void limpiarFixtures() throws SQLException {
        if (idCreado == null) return;
        try (Statement st = DatabaseConnection.getConnection().createStatement()) {
            st.executeUpdate("DELETE FROM Product WHERE idEntrepreneur = " + idCreado);
            st.executeUpdate("DELETE FROM Entrepreneur WHERE idEntrepreneur = " + idCreado);
        }
    }

    private Emprendedor nuevoEmprendedor(String marca) {
        Emprendedor emp = new Emprendedor();
        emp.setMarca(marca);
        emp.setNombreContacto("Contacto Test");
        emp.setTelefono("5555555555");
        emp.setEmail("test@test.com");
        emp.setFechaContrato(Date.valueOf("2026-01-01"));
        emp.setRentaMensual(500.0);
        return emp;
    }

    private int registrarYObtenerId(String marca) throws SQLException {
        dao.registrar(nuevoEmprendedor(marca));
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "SELECT idEntrepreneur FROM Entrepreneur WHERE brandName = ?")) {
            ps.setString(1, marca);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                idCreado = rs.getInt(1);
                return idCreado;
            }
        }
    }

    @Test
    void registrar_creaElEmprendedorConLosDatosDados() throws SQLException {
        int id = registrarYObtenerId("JUNIT EMPRENDEDOR UNO");

        Emprendedor guardado = dao.buscarPorId(id);
        assertNotNull(guardado);
        assertEquals("Contacto Test", guardado.getNombreContacto());
        assertEquals(500.0, guardado.getRentaMensual());
    }

    @Test
    void listar_incluyeElEmprendedorActivoRecienCreado() throws SQLException {
        int id = registrarYObtenerId("JUNIT EMPRENDEDOR DOS");

        assertTrue(dao.listar().stream().anyMatch(e -> e.getId() == id));
    }

    @Test
    void buscarPorId_devuelveLosDatosCorrectos() throws SQLException {
        int id = registrarYObtenerId("JUNIT EMPRENDEDOR TRES");

        Emprendedor emp = dao.buscarPorId(id);

        assertNotNull(emp);
        assertEquals("JUNIT EMPRENDEDOR TRES", emp.getMarca());
    }

    @Test
    void actualizar_modificaLosDatos() throws SQLException {
        int id = registrarYObtenerId("JUNIT EMPRENDEDOR CUATRO");

        Emprendedor cambios = dao.buscarPorId(id);
        cambios.setNombreContacto("Nuevo Contacto");
        cambios.setRentaMensual(750.0);

        boolean ok = dao.actualizar(cambios);
        assertTrue(ok);

        Emprendedor actualizado = dao.buscarPorId(id);
        assertEquals("Nuevo Contacto", actualizado.getNombreContacto());
        assertEquals(750.0, actualizado.getRentaMensual());
    }

    @Test
    void eliminarLogico_desactivaElEmprendedorYSusProductosEnCascada() throws SQLException {
        int id = registrarYObtenerId("JUNIT EMPRENDEDOR CINCO");
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "INSERT INTO Product (idEntrepreneur, fullProductCode, productDescription, department, currentPrice, currentStock, minStockAlert) " +
                "VALUES (?, 'JB01', 'PRODUCTO JUNIT CASCADA', 'TEST', 10.00, 5, 1)")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }

        boolean ok = dao.eliminarLogico(id);
        assertTrue(ok);

        assertFalse(dao.listar().stream().anyMatch(e -> e.getId() == id));
        assertEquals(0, productoActivo("JB01"), "el producto del emprendedor desactivado también debe quedar inactivo");
    }

    private int productoActivo(String codigo) throws SQLException {
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "SELECT isProductActive FROM Product WHERE fullProductCode = ?")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Test
    void buscarAvanzado_filtraPorTextoYEstado() throws SQLException {
        int id = registrarYObtenerId("JUNIT EMPRENDEDOR SEIS");

        assertTrue(dao.buscarAvanzado("JUNIT EMPRENDEDOR SEIS", false).stream().anyMatch(e -> e.getId() == id));

        dao.eliminarLogico(id);

        assertFalse(dao.buscarAvanzado("JUNIT EMPRENDEDOR SEIS", false).stream().anyMatch(e -> e.getId() == id));
        assertTrue(dao.buscarAvanzado("JUNIT EMPRENDEDOR SEIS", true).stream().anyMatch(e -> e.getId() == id));
    }

    @Test
    void activar_reactivaElEmprendedor() throws SQLException {
        int id = registrarYObtenerId("JUNIT EMPRENDEDOR SIETE");
        dao.eliminarLogico(id);

        boolean ok = dao.activar(id);
        assertTrue(ok);

        assertTrue(dao.listar().stream().anyMatch(e -> e.getId() == id));
    }

    @Test
    void listarNombresYId_incluyeSoloActivos() throws SQLException {
        int id = registrarYObtenerId("JUNIT EMPRENDEDOR OCHO");

        List<Emprendedor> lista = dao.listarNombresYId();
        assertTrue(lista.stream().anyMatch(e -> e.getId() == id && "JUNIT EMPRENDEDOR OCHO".equals(e.getMarca())));

        dao.eliminarLogico(id);
        assertFalse(dao.listarNombresYId().stream().anyMatch(e -> e.getId() == id));
    }
}
