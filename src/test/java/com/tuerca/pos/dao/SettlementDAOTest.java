package com.tuerca.pos.dao;

import com.tuerca.pos.model.Settlement;
import com.tuerca.pos.pdf.dto.LineaReporteVenta;
import com.tuerca.pos.support.AbstractDaoIntegrationTest;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Convierte en tests permanentes los escenarios ya verificados a mano con
 * {@code SettlementHarness.java}, {@code RentaHarness.java} y
 * {@code DetallePagoHarness.java} durante los Pasos 9 y 10: exclusión de
 * ventas devueltas, cálculo correcto de bruto/descuentos/neto, marcado de
 * {@code isSettled}/{@code idSettlement}, y detección de renta ya cobrada
 * en el mes calendario.
 */
class SettlementDAOTest extends AbstractDaoIntegrationTest {

    private static final Date DESDE = Date.valueOf("2020-01-01");
    private static final Date HASTA = Date.valueOf("2030-01-01");

    private final SettlementDAO dao = new SettlementDAO();
    private int idEntrepreneur;
    private int idProduct;
    private final List<Integer> idsVentasCreadas = new ArrayList<>();

    @BeforeEach
    void crearEmprendedorYProductoDePrueba() throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Entrepreneur (brandName, contactName, contactPhone, emailEntrepreneur, contractSignDate, monthlyRentAmount) " +
                "VALUES ('JUNIT SETTLEMENT DAO', 'Test', '5555555555', 'test@test.com', CURDATE(), 30.00)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                idEntrepreneur = rs.getInt(1);
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Product (idEntrepreneur, fullProductCode, productDescription, department, currentPrice, currentStock, minStockAlert) " +
                "VALUES (?, 'JT98', 'PRODUCTO DE PRUEBA JUNIT SETTLEMENT', 'TEST', 50.00, 20, 1)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idEntrepreneur);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                idProduct = rs.getInt(1);
            }
        }
    }

    @AfterEach
    void limpiarDatosDePrueba() throws SQLException {
        try (Statement st = DatabaseConnection.getConnection().createStatement()) {
            st.executeUpdate("DELETE FROM ProductReturn WHERE idSaleDetail IN (SELECT idSaleDetail FROM SaleDetail WHERE idProduct = " + idProduct + ")");
            st.executeUpdate("DELETE FROM SaleDetail WHERE idProduct = " + idProduct);
            // Solo se borran exactamente las ventas que este test creó (por id), nunca por
            // heurística — idUserAccount=1 también lo usan datos reales del entorno de desarrollo.
            if (!idsVentasCreadas.isEmpty()) {
                String inClause = idsVentasCreadas.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("0");
                st.executeUpdate("DELETE FROM Sale WHERE idSale IN (" + inClause + ")");
            }
            st.executeUpdate("DELETE FROM Settlement WHERE idEntrepreneur = " + idEntrepreneur);
            st.executeUpdate("DELETE FROM Product WHERE idProduct = " + idProduct);
            st.executeUpdate("DELETE FROM Entrepreneur WHERE idEntrepreneur = " + idEntrepreneur);
        }
    }

    private int crearVenta(double subtotal, double descuento, boolean settled) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        int idSale;
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Sale (idUserAccount, idPaymentMethod, totalSaleAmount, saleStatus) VALUES (1, 1, ?, 'Activa')",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setDouble(1, subtotal);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                idSale = rs.getInt(1);
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO SaleDetail (idSale, idProduct, quantitySold, unitPriceAtSale, discountApplied, subtotalDetail, isSettled) " +
                "VALUES (?, ?, 1, 50.00, ?, ?, ?)")) {
            ps.setInt(1, idSale);
            ps.setInt(2, idProduct);
            ps.setDouble(3, descuento);
            ps.setDouble(4, subtotal);
            ps.setBoolean(5, settled);
            ps.executeUpdate();
        }
        idsVentasCreadas.add(idSale);
        return idSale;
    }

    @Test
    void listarVentasPendientes_excluyeDevueltasYYaLiquidadas() throws SQLException {
        int idSalePendiente = crearVenta(50.00, 0.00, false);
        crearVenta(120.00, 0.00, true); // ya liquidada, no debe aparecer

        int idSaleDevuelto = crearVenta(60.00, 0.00, false);
        int idDetalleDevuelto = idSaleDetailDe(idSaleDevuelto);
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "INSERT INTO ProductReturn (idSaleDetail, idUserAccount, returnReason, refundAmount) VALUES (?, 1, 'test', 60.00)")) {
            ps.setInt(1, idDetalleDevuelto);
            ps.executeUpdate();
        }

        List<Object[]> pendientes = dao.listarVentasPendientes(idEntrepreneur, DESDE, HASTA);

        assertEquals(1, pendientes.size());
        assertEquals(idSalePendiente, pendientes.get(0)[0]);
    }

    @Test
    void registrarPago_calculaTotalesYMarcaLineasComoLiquidadas() throws SQLException {
        int idSaleConDescuento = crearVenta(120.00, 20.00, false);
        int idSaleSinDescuento = crearVenta(50.00, 0.00, false);

        Settlement settlement = new Settlement();
        settlement.setIdEntrepreneur(idEntrepreneur);
        settlement.setIdUserAccount(1);
        settlement.setPeriodStartDate(DESDE);
        settlement.setPeriodEndDate(HASTA);
        settlement.setRentDiscount(30.00);
        settlement.setOtherDiscounts(0);

        List<Integer> seleccionados = new ArrayList<>();
        seleccionados.add(idSaleConDescuento);
        seleccionados.add(idSaleSinDescuento);

        boolean ok = dao.registrarPago(settlement, seleccionados);

        assertTrue(ok);
        assertEquals(170.00, settlement.getGrossAmount(), 0.001);
        assertEquals(20.00, settlement.getTotalDiscounts(), 0.001);
        assertEquals(120.00, settlement.getNetAmountPaid(), 0.001); // 170 - 20 - 30

        assertTrue(dao.listarVentasPendientes(idEntrepreneur, DESDE, HASTA).isEmpty());

        List<LineaReporteVenta> detalles = dao.obtenerDetallesDelPago(settlement.getIdSettlement());
        assertEquals(2, detalles.size());
        assertTrue(detalles.stream().allMatch(LineaReporteVenta::isPagado));
    }

    @Test
    void obtenerDetalleVentasDelPeriodo_incluyeVentasPendientesYYaLiquidadas() throws SQLException {
        crearVenta(60.00, 0.00, false);
        crearVenta(120.00, 20.00, true);
        int idSaleDevuelto = crearVenta(60.00, 0.00, false);
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                "INSERT INTO ProductReturn (idSaleDetail, idUserAccount, returnReason, refundAmount) VALUES (?, 1, 'test', 60.00)")) {
            ps.setInt(1, idSaleDetailDe(idSaleDevuelto));
            ps.executeUpdate();
        }

        List<LineaReporteVenta> lineas = dao.obtenerDetalleVentasDelPeriodo(idEntrepreneur, DESDE, HASTA);

        assertEquals(2, lineas.size(), "debe incluir la pendiente y la ya liquidada, pero no la devuelta");
        long pagadas = lineas.stream().filter(LineaReporteVenta::isPagado).count();
        assertEquals(1, pagadas);
    }

    @Test
    void obtenerFechaUltimaRentaCobradaEsteMes_nullHastaQueSeRegistraUnPagoConRenta() throws SQLException {
        assertNull(dao.obtenerFechaUltimaRentaCobradaEsteMes(idEntrepreneur));

        int idSale = crearVenta(50.00, 0.00, false);
        Settlement settlement = new Settlement();
        settlement.setIdEntrepreneur(idEntrepreneur);
        settlement.setIdUserAccount(1);
        settlement.setPeriodStartDate(DESDE);
        settlement.setPeriodEndDate(HASTA);
        settlement.setRentDiscount(30.00);
        settlement.setOtherDiscounts(0);
        List<Integer> seleccionados = new ArrayList<>();
        seleccionados.add(idSale);
        dao.registrarPago(settlement, seleccionados);

        assertEquals(Date.valueOf(java.time.LocalDate.now()), dao.obtenerFechaUltimaRentaCobradaEsteMes(idEntrepreneur));
    }

    private int idSaleDetailDe(int idSale) throws SQLException {
        try (PreparedStatement ps = DatabaseConnection.getConnection()
                .prepareStatement("SELECT idSaleDetail FROM SaleDetail WHERE idSale = ?")) {
            ps.setInt(1, idSale);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
