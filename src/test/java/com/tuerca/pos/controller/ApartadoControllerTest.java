package com.tuerca.pos.controller;

import com.tuerca.pos.dao.ApartadoDAO;
import com.tuerca.pos.dao.ProductoDAO;
import com.tuerca.pos.model.Apartado;
import com.tuerca.pos.model.Empleado;
import com.tuerca.pos.model.Sesion;
import com.tuerca.pos.view.GestionApartados;
import com.tuerca.pos.view.MainView;
import com.tuerca.pos.view.Ventas;
import com.tuerca.pos.view.components.DatosApartadoDialog;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code abrirOpcionesApartado()} solo es alcanzable desde el editor de celda
 * de la columna de Acciones (un {@link javax.swing.table.TableCellEditor}
 * anidado, sin getter en la vista) — igual que en {@code DevolucionController},
 * se invoca por reflexión junto con los métodos privados que dependen de ella
 * ({@code procesarCancelacion}, {@code procesarPago}, {@code ejecutarLiquidacion}).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApartadoControllerTest {

    @Mock
    private Ventas vista;
    @Mock
    private GestionApartados vistaGestion;
    @Mock
    private MainView mainView;

    private final JButton btnApartarProductos = new JButton();
    private final JTable tablaVenta = new JTable(new DefaultTableModel(
            new Object[][]{}, new String[]{"Cant", "Codigo", "Descripcion", "Precio", "?", "Subtotal"}));
    private final JLabel lblTotal = new JLabel();
    private final JTextField txtBusqueda = new JTextField();

    private final JTextField txtBuscar = new JTextField();
    private final JComboBox<String> cbEstado = new JComboBox<>(new String[]{"Activos", "Liquidados", "Cancelados", "Vencidos"});
    private final JTable tablaApartados = new JTable(new DefaultTableModel(
            new Object[][]{}, new String[]{"Folio", "Cliente", "Total", "Abono", "Saldo", "Vence", "Acciones"}));

    private MockedConstruction<ApartadoDAO> construccionApartadoDao;
    private MockedConstruction<ProductoDAO> construccionProductoDao;
    private ApartadoDAO dao;
    private ApartadoController controller;

    @BeforeEach
    void construirController() {
        when(vista.getBtnApartarProductos()).thenReturn(btnApartarProductos);
        when(vista.getTablaVenta()).thenReturn(tablaVenta);
        when(vista.getLblTotal()).thenReturn(lblTotal);
        when(vista.getTxtBusqueda()).thenReturn(txtBusqueda);

        when(vistaGestion.getTxtBuscar()).thenReturn(txtBuscar);
        when(vistaGestion.getCbEstado()).thenReturn(cbEstado);
        when(vistaGestion.getTablaApartados()).thenReturn(tablaApartados);

        construccionApartadoDao = mockConstruction(ApartadoDAO.class,
                (m, ctx) -> when(m.listarApartados(any(), any())).thenReturn(List.of()));
        construccionProductoDao = mockConstruction(ProductoDAO.class);

        controller = new ApartadoController(vista, vistaGestion, mainView);
        dao = construccionApartadoDao.constructed().get(0);

        Empleado empleado = new Empleado();
        empleado.setIdUserAccount(15);
        empleado.setId(1);
        empleado.setNombre("Test");
        empleado.setPaterno("User");
        empleado.setUsername("testuser");
        empleado.setIdRole(2);
        empleado.setRoleName("Sales");
        Sesion.getInstancia().iniciarSesion(empleado);
    }

    @AfterEach
    void cerrarMocksYSesion() {
        construccionApartadoDao.close();
        construccionProductoDao.close();
        Sesion.getInstancia().cerrarSesion();
    }

    private void agregarFilaAlCarrito(String codigo, int cantidad, double precio, double subtotal) {
        DefaultTableModel modelo = (DefaultTableModel) tablaVenta.getModel();
        modelo.addRow(new Object[]{cantidad, codigo, "Producto de prueba", precio, "", subtotal});
    }

    private Apartado apartadoDePrueba(int folio) {
        Apartado a = new Apartado();
        a.setIdBooking(folio);
        a.setCustomerName("CLIENTE JUNIT");
        a.setCustomerPhone("5555555555");
        a.setTotalAmount(new BigDecimal("100.00"));
        a.setAdvanceAmount(new BigDecimal("20.00"));
        a.setPendingBalance(new BigDecimal("80.00"));
        a.setBookingStatus("Activo");
        return a;
    }

    private Object invocarPrivado(String nombre, Class<?>[] tipos, Object... args) throws Exception {
        Method m = ApartadoController.class.getDeclaredMethod(nombre, tipos);
        m.setAccessible(true);
        return m.invoke(controller, args);
    }

    // El constructor de Resultado es de paquete (package-private) y este test vive en
    // otro paquete — se instancia por reflexión en vez de exponerlo públicamente solo
    // para pruebas.
    private DatosApartadoDialog.Resultado crearResultado(String nombre, String telefono, BigDecimal monto, String metodoPago) throws Exception {
        var constructor = DatosApartadoDialog.Resultado.class.getDeclaredConstructor(
                String.class, String.class, BigDecimal.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(nombre, telefono, monto, metodoPago);
    }

    // --- procesarApartado() (venta -> apartado) ---

    @Test
    void procesarApartado_carritoVacio_muestraAviso() throws SQLException {
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnApartarProductos.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vista), eq("No hay productos para apartar.")));
        }
        verify(dao, never()).registrarApartadoCompleto(any(), any(), any());
    }

    @Test
    void procesarApartado_usuarioCancelaElDialogo_noRegistraNada() throws Exception {
        agregarFilaAlCarrito("JT01", 2, 10.00, 20.00);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class);
             MockedStatic<DatosApartadoDialog> dialogo = mockStatic(DatosApartadoDialog.class)) {
            dialogo.when(() -> DatosApartadoDialog.solicitar(any(), any(), any())).thenReturn(null);

            btnApartarProductos.doClick();
        }
        verify(dao, never()).registrarApartadoCompleto(any(), any(), any());
    }

    @Test
    void procesarApartado_exitoso_registraLimpiaCarritoYRefresca() throws Exception {
        agregarFilaAlCarrito("JT01", 2, 10.00, 20.00);
        when(dao.registrarApartadoCompleto(any(), any(), any())).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class);
             MockedStatic<DatosApartadoDialog> dialogo = mockStatic(DatosApartadoDialog.class)) {
            DatosApartadoDialog.Resultado resultado = crearResultado(
                    "cliente junit", "5555555555", new BigDecimal("2.00"), "Efectivo");
            dialogo.when(() -> DatosApartadoDialog.solicitar(any(), any(), any())).thenReturn(resultado);

            btnApartarProductos.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vista), anyString()));
        }
        assertEquals(0, tablaVenta.getModel().getRowCount());
        assertEquals("$0.00", lblTotal.getText());
    }

    @Test
    void procesarApartado_elDaoRetornaFalse_muestraError() throws Exception {
        agregarFilaAlCarrito("JT01", 2, 10.00, 20.00);
        when(dao.registrarApartadoCompleto(any(), any(), any())).thenReturn(false);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class);
             MockedStatic<DatosApartadoDialog> dialogo = mockStatic(DatosApartadoDialog.class)) {
            DatosApartadoDialog.Resultado resultado = crearResultado(
                    "cliente", "5555555555", new BigDecimal("2.00"), "Efectivo");
            dialogo.when(() -> DatosApartadoDialog.solicitar(any(), any(), any())).thenReturn(resultado);

            btnApartarProductos.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vista), eq("Error al registrar el apartado en la base de datos.")));
        }
    }

    @Test
    void procesarApartado_elDaoLanzaSQLException_muestraErrorDeInventario() throws Exception {
        agregarFilaAlCarrito("JT01", 2, 10.00, 20.00);
        when(dao.registrarApartadoCompleto(any(), any(), any())).thenThrow(new SQLException("Sin stock suficiente"));

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class);
             MockedStatic<DatosApartadoDialog> dialogo = mockStatic(DatosApartadoDialog.class)) {
            DatosApartadoDialog.Resultado resultado = crearResultado(
                    "cliente", "5555555555", new BigDecimal("2.00"), "Efectivo");
            dialogo.when(() -> DatosApartadoDialog.solicitar(any(), any(), any())).thenReturn(resultado);

            btnApartarProductos.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vista), anyString(), eq("Error de Inventario / Sistema"), eq(JOptionPane.WARNING_MESSAGE)));
        }
    }

    // --- Gestión de apartados ---

    @Test
    void llenarTablaGestion_llenaLaTablaConLosApartadosListados() {
        when(dao.listarApartados("", "Activo")).thenReturn(List.of(apartadoDePrueba(1)));

        controller.llenarTablaGestion("", "Activo");

        assertEquals(1, tablaApartados.getModel().getRowCount());
    }

    @Test
    void abrirOpcionesApartado_apartadoNoExiste_noHaceNada() throws Exception {
        when(dao.obtenerApartadoPorId(99)).thenReturn(null);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            invocarPrivado("abrirOpcionesApartado", new Class<?>[]{int.class}, 99);

            jOptionPane.verify(() -> JOptionPane.showOptionDialog(any(), any(), any(), anyInt(), anyInt(), any(), any(), any()), never());
        }
    }

    @Test
    void abrirOpcionesApartado_usuarioElijeCerrar_noHaceNada() throws Exception {
        when(dao.obtenerApartadoPorId(1)).thenReturn(apartadoDePrueba(1));
        when(dao.obtenerResumenDetallesPorFolio(1)).thenReturn(List.of());

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showOptionDialog(any(), any(), any(), anyInt(), anyInt(), any(), any(), any()))
                    .thenReturn(2); // "Cerrar"

            invocarPrivado("abrirOpcionesApartado", new Class<?>[]{int.class}, 1);
        }
        verify(dao, never()).cancelarApartado(anyInt());
    }

    @Test
    void abrirOpcionesApartado_usuarioElijeCancelarApartado_pideConfirmacion() throws Exception {
        when(dao.obtenerApartadoPorId(1)).thenReturn(apartadoDePrueba(1));
        when(dao.obtenerResumenDetallesPorFolio(1)).thenReturn(List.of());

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showOptionDialog(any(), any(), any(), anyInt(), anyInt(), any(), any(), any()))
                    .thenReturn(1); // "Cancelar Apartado"
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(JOptionPane.NO_OPTION);

            invocarPrivado("abrirOpcionesApartado", new Class<?>[]{int.class}, 1);
        }
        verify(dao, never()).cancelarApartado(anyInt());
    }

    // --- procesarCancelacion(Apartado) ---

    @Test
    void procesarCancelacion_usuarioCancelaLaConfirmacion_noHaceNada() throws Exception {
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(JOptionPane.NO_OPTION);

            invocarPrivado("procesarCancelacion", new Class<?>[]{Apartado.class}, apartadoDePrueba(1));
        }
        verify(dao, never()).cancelarApartado(anyInt());
    }

    @Test
    void procesarCancelacion_exitoso_muestraMensajeYRefresca() throws Exception {
        when(dao.cancelarApartado(1)).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(JOptionPane.YES_OPTION);

            invocarPrivado("procesarCancelacion", new Class<?>[]{Apartado.class}, apartadoDePrueba(1));

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaGestion), eq("Apartado cancelado. El stock fue devuelto al inventario.")));
        }
    }

    @Test
    void procesarCancelacion_elDaoFalla_muestraError() throws Exception {
        when(dao.cancelarApartado(1)).thenReturn(false);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(JOptionPane.YES_OPTION);

            invocarPrivado("procesarCancelacion", new Class<?>[]{Apartado.class}, apartadoDePrueba(1));

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vistaGestion), eq("No se pudo cancelar el apartado (puede que ya no esté Activo)."), eq("Error"), eq(JOptionPane.ERROR_MESSAGE)));
        }
    }

    // --- procesarPago(Apartado, List<Object[]>) ---

    @Test
    void procesarPago_usuarioCancelaElInputDeMonto_noHaceNada() throws Exception {
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(any(), anyString(), anyString(), anyInt()))
                    .thenReturn(null);

            invocarPrivado("procesarPago", new Class<?>[]{Apartado.class, List.class}, apartadoDePrueba(1), List.of());
        }
        verify(dao, never()).registrarNuevoAbono(anyInt(), any(), any());
    }

    @Test
    void procesarPago_montoNoNumerico_muestraError() throws Exception {
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(any(), anyString(), anyString(), anyInt()))
                    .thenReturn("no-es-numero");

            invocarPrivado("procesarPago", new Class<?>[]{Apartado.class, List.class}, apartadoDePrueba(1), List.of());

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaGestion), eq("Por favor, ingrese un monto numérico válido.")));
        }
    }

    @Test
    void procesarPago_montoCeroONegativo_muestraError() throws Exception {
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(any(), anyString(), anyString(), anyInt()))
                    .thenReturn("0");

            invocarPrivado("procesarPago", new Class<?>[]{Apartado.class, List.class}, apartadoDePrueba(1), List.of());

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaGestion), eq("El monto debe ser mayor a cero.")));
        }
    }

    @Test
    void procesarPago_abonoParcial_usuarioCancelaMetodoDePago_noRegistraNada() throws Exception {
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(any(), anyString(), anyString(), anyInt()))
                    .thenReturn("20.00"); // menor al saldo (80.00) -> abono parcial
            jOptionPane.when(() -> JOptionPane.showInputDialog(any(), anyString(), eq("Método de Pago"), anyInt(), any(), any(), any()))
                    .thenReturn(null);

            invocarPrivado("procesarPago", new Class<?>[]{Apartado.class, List.class}, apartadoDePrueba(1), List.of());
        }
        verify(dao, never()).registrarNuevoAbono(anyInt(), any(), any());
    }

    @Test
    void procesarPago_abonoParcialExitoso_muestraMensajeYRefresca() throws Exception {
        when(dao.registrarNuevoAbono(1, new BigDecimal("20.00"), "Efectivo")).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(any(), anyString(), anyString(), anyInt()))
                    .thenReturn("20.00");
            jOptionPane.when(() -> JOptionPane.showInputDialog(any(), anyString(), eq("Método de Pago"), anyInt(), any(), any(), any()))
                    .thenReturn("Efectivo");

            invocarPrivado("procesarPago", new Class<?>[]{Apartado.class, List.class}, apartadoDePrueba(1), List.of());

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaGestion), eq("¡Abono registrado con éxito!")));
        }
    }

    @Test
    void procesarPago_abonoParcialElDaoFalla_muestraError() throws Exception {
        when(dao.registrarNuevoAbono(anyInt(), any(), any())).thenReturn(false);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(any(), anyString(), anyString(), anyInt()))
                    .thenReturn("20.00");
            jOptionPane.when(() -> JOptionPane.showInputDialog(any(), anyString(), eq("Método de Pago"), anyInt(), any(), any(), any()))
                    .thenReturn("Efectivo");

            invocarPrivado("procesarPago", new Class<?>[]{Apartado.class, List.class}, apartadoDePrueba(1), List.of());

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaGestion), eq("Error al procesar el pago en la base de datos.")));
        }
    }

    @Test
    void procesarPago_liquidacionTotal_usuarioCancelaLaConfirmacion_noLiquida() throws Exception {
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(any(), anyString(), anyString(), anyInt()))
                    .thenReturn("80.00"); // igual al saldo pendiente -> liquidación
            jOptionPane.when(() -> JOptionPane.showInputDialog(any(), anyString(), eq("Método de Pago"), anyInt(), any(), any(), any()))
                    .thenReturn("Efectivo");
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(JOptionPane.NO_OPTION);

            invocarPrivado("procesarPago", new Class<?>[]{Apartado.class, List.class}, apartadoDePrueba(1), List.of());
        }
        verify(dao, never()).liquidarApartadoCompleto(anyInt(), anyInt(), any(), any());
    }

    @Test
    void procesarPago_liquidacionTotal_usuarioConfirma_ejecutaLiquidacion() throws Exception {
        when(dao.liquidarApartadoCompleto(eq(1), anyInt(), eq("Efectivo"), any())).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showInputDialog(any(), anyString(), anyString(), anyInt()))
                    .thenReturn("80.00");
            jOptionPane.when(() -> JOptionPane.showInputDialog(any(), anyString(), eq("Método de Pago"), anyInt(), any(), any(), any()))
                    .thenReturn("Efectivo");
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(JOptionPane.YES_OPTION);

            invocarPrivado("procesarPago", new Class<?>[]{Apartado.class, List.class}, apartadoDePrueba(1), List.of());

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaGestion), anyString()));
        }
        verify(dao).liquidarApartadoCompleto(eq(1), anyInt(), eq("Efectivo"), any());
    }

    // --- ejecutarLiquidacion(Apartado, List<Object[]>, String) ---

    @Test
    void ejecutarLiquidacion_exitoso_muestraMensajeYRefresca() throws Exception {
        when(dao.liquidarApartadoCompleto(eq(1), anyInt(), eq("Efectivo"), any())).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            invocarPrivado("ejecutarLiquidacion", new Class<?>[]{Apartado.class, List.class, String.class},
                    apartadoDePrueba(1), List.of(), "Efectivo");

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaGestion), anyString()));
        }
    }

    @Test
    void ejecutarLiquidacion_elDaoFalla_muestraError() throws Exception {
        when(dao.liquidarApartadoCompleto(eq(1), anyInt(), eq("Efectivo"), any())).thenReturn(false);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            invocarPrivado("ejecutarLiquidacion", new Class<?>[]{Apartado.class, List.class, String.class},
                    apartadoDePrueba(1), List.of(), "Efectivo");

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vistaGestion), eq("No se pudo completar la transacción por un error interno."), eq("Error"), eq(JOptionPane.ERROR_MESSAGE)));
        }
    }

    @Test
    void ejecutarLiquidacion_sqlException_muestraErrorDeInventario() throws Exception {
        when(dao.liquidarApartadoCompleto(eq(1), anyInt(), eq("Efectivo"), any()))
                .thenThrow(new SQLException("Sin stock suficiente"));

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            invocarPrivado("ejecutarLiquidacion", new Class<?>[]{Apartado.class, List.class, String.class},
                    apartadoDePrueba(1), List.of(), "Efectivo");

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(vistaGestion), anyString(), eq("Error de Inventario / Sistema"), eq(JOptionPane.WARNING_MESSAGE)));
        }
    }
}
