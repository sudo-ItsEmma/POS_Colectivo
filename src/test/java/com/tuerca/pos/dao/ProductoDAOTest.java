package com.tuerca.pos.dao;

import com.tuerca.pos.model.Producto;
import com.tuerca.pos.support.AbstractDaoIntegrationTest;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de {@link ProductoDAO}. Cada test usa un {@code Entrepreneur} propio
 * (marca "JUNIT PRODUCTO DAO") y productos con códigos únicos por test, para
 * poder borrar todo por ID exacto en {@code @AfterEach}.
 */
class ProductoDAOTest extends AbstractDaoIntegrationTest {

    private final ProductoDAO dao = new ProductoDAO();
    private int idEntrepreneur;

    @BeforeEach
    void crearEmprendedorDePrueba() throws SQLException {
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "INSERT INTO Entrepreneur (brandName, contactName, contactPhone, emailEntrepreneur, contractSignDate, monthlyRentAmount) " +
                "VALUES ('JUNIT PRODUCTO DAO', 'Test', '5555555555', 'test@test.com', CURDATE(), 0.00)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                idEntrepreneur = rs.getInt(1);
            }
        }
    }

    @AfterEach
    void limpiarFixtures() throws SQLException {
        try (Statement st = DatabaseConnection.getConnection().createStatement()) {
            st.executeUpdate("DELETE FROM Product WHERE idEntrepreneur = " + idEntrepreneur);
            st.executeUpdate("DELETE FROM Entrepreneur WHERE idEntrepreneur = " + idEntrepreneur);
        }
    }

    private Producto nuevoProducto(String codigo, String descripcion, BigDecimal precio, int stock) {
        Producto p = new Producto();
        p.setIdEntrepreneur(idEntrepreneur);
        p.setFullProductCode(codigo);
        p.setProductDescription(descripcion);
        p.setDepartment("TEST");
        p.setCurrentPrice(precio);
        p.setCurrentStock(stock);
        p.setMinStockAlert(1);
        return p;
    }

    @Test
    void registrar_creaProductoActivoConLosDatosDados() {
        boolean ok = dao.registrar(nuevoProducto("JA01", "PRODUCTO JUNIT UNO", new BigDecimal("15.50"), 5));

        assertTrue(ok);
        Producto guardado = dao.buscarPorCriterio("JA01").stream().findFirst().orElse(null);
        assertNotNull(guardado);
        assertEquals(0, new BigDecimal("15.50").compareTo(guardado.getCurrentPrice()));
        assertEquals(5, guardado.getCurrentStock());
    }

    @Test
    void registrarOSumarStock_codigoNuevo_insertaConUnaFilaAfectada() {
        int filas = dao.registrarOSumarStock(nuevoProducto("JA02", "PRODUCTO JUNIT DOS", new BigDecimal("10.00"), 3));

        assertEquals(1, filas, "un INSERT nuevo debe afectar 1 fila");
        assertEquals(3, dao.obtenerStockReal("JA02"));
    }

    @Test
    void registrarOSumarStock_codigoExistente_sumaElStockEnVezDeFallar() {
        dao.registrarOSumarStock(nuevoProducto("JA03", "PRODUCTO JUNIT TRES", new BigDecimal("10.00"), 3));

        int filas = dao.registrarOSumarStock(nuevoProducto("JA03", "PRODUCTO JUNIT TRES", new BigDecimal("10.00"), 4));

        assertEquals(2, filas, "MariaDB reporta 2 filas afectadas cuando ON DUPLICATE KEY actualiza");
        assertEquals(7, dao.obtenerStockReal("JA03"), "el stock del Excel se debe sumar al existente (3+4)");
    }

    @Test
    void buscarPorId_devuelveLosDatosRegistrados() {
        dao.registrar(nuevoProducto("JA04", "PRODUCTO JUNIT CUATRO", new BigDecimal("22.00"), 8));
        int id = dao.obtenerIdPorCodigo("JA04");

        Producto p = dao.buscarPorId(id);

        assertNotNull(p);
        assertEquals("JA04", p.getFullProductCode());
        assertEquals(8, p.getCurrentStock());
    }

    @Test
    void actualizar_modificaPrecioYStock() {
        dao.registrar(nuevoProducto("JA05", "PRODUCTO JUNIT CINCO", new BigDecimal("22.00"), 8));
        int id = dao.obtenerIdPorCodigo("JA05");

        Producto cambios = dao.buscarPorId(id);
        cambios.setCurrentPrice(new BigDecimal("30.00"));
        cambios.setCurrentStock(20);

        boolean ok = dao.actualizar(cambios);
        assertTrue(ok);

        Producto actualizado = dao.buscarPorId(id);
        assertEquals(0, new BigDecimal("30.00").compareTo(actualizado.getCurrentPrice()));
        assertEquals(20, actualizado.getCurrentStock());
    }

    @Test
    void eliminarLogico_desactivaYLoExcluyeDeBusquedaPorCriterio() {
        dao.registrar(nuevoProducto("JA06", "PRODUCTO JUNIT SEIS", new BigDecimal("12.00"), 4));
        int id = dao.obtenerIdPorCodigo("JA06");

        boolean ok = dao.eliminarLogico(id);
        assertTrue(ok);

        assertTrue(dao.buscarPorCriterio("JA06").isEmpty(), "un producto desactivado no debe aparecer en la búsqueda normal");
    }

    @Test
    void activarProductoConValidacion_conEmprendedorActivo_reactivaElProducto() {
        dao.registrar(nuevoProducto("JA07", "PRODUCTO JUNIT SIETE", new BigDecimal("12.00"), 4));
        int id = dao.obtenerIdPorCodigo("JA07");
        dao.eliminarLogico(id);

        int resultado = dao.activarProductoConValidacion(id);

        assertEquals(1, resultado, "1 = reactivado con éxito");
        assertFalse(dao.buscarPorCriterio("JA07").isEmpty());
    }

    @Test
    void activarProductoConValidacion_conEmprendedorInactivo_bloqueaLaReactivacion() throws SQLException {
        dao.registrar(nuevoProducto("JA08", "PRODUCTO JUNIT OCHO", new BigDecimal("12.00"), 4));
        int id = dao.obtenerIdPorCodigo("JA08");
        dao.eliminarLogico(id);

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "UPDATE Entrepreneur SET isEntityActive = 0 WHERE idEntrepreneur = ?")) {
            ps.setInt(1, idEntrepreneur);
            ps.executeUpdate();
        }

        int resultado = dao.activarProductoConValidacion(id);

        assertEquals(-1, resultado, "-1 = bloqueado porque el emprendedor sigue inactivo");
    }

    @Test
    void buscarAvanzado_filtraPorEstadoTextoYEmprendedor() {
        dao.registrar(nuevoProducto("JA09", "PRODUCTO JUNIT NUEVE", new BigDecimal("12.00"), 4));

        List<Producto> activos = dao.buscarAvanzado("JUNIT NUEVE", idEntrepreneur, false);
        assertEquals(1, activos.size());

        List<Producto> inactivos = dao.buscarAvanzado("JUNIT NUEVE", idEntrepreneur, true);
        assertTrue(inactivos.isEmpty());
    }

    @Test
    void buscarPorCriterio_encuentraPorMarcaYExcluyeSinStock() {
        dao.registrar(nuevoProducto("JA10", "PRODUCTO JUNIT DIEZ", new BigDecimal("12.00"), 4));
        dao.registrar(nuevoProducto("JA11", "PRODUCTO JUNIT ONCE SIN STOCK", new BigDecimal("12.00"), 0));

        List<Producto> porMarca = dao.buscarPorCriterio("JUNIT PRODUCTO DAO");
        assertEquals(1, porMarca.size(), "debe encontrar por brandName y excluir el que no tiene stock");
        assertEquals("JA10", porMarca.get(0).getFullProductCode());
    }

    @Test
    void obtenerIdPorCodigo_conCodigoInexistente_devuelveMenosUno() {
        assertEquals(-1, dao.obtenerIdPorCodigo("NOEXISTE99"));
    }
}
