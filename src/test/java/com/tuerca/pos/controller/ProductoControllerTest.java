package com.tuerca.pos.controller;

import com.tuerca.pos.dao.EmprendedorDAO;
import com.tuerca.pos.dao.ProductoDAO;
import com.tuerca.pos.model.Emprendedor;
import com.tuerca.pos.model.Producto;
import com.tuerca.pos.view.CargaMasivaProductos;
import com.tuerca.pos.view.EditarProducto;
import com.tuerca.pos.view.GestionProductos;
import com.tuerca.pos.view.MainView;
import com.tuerca.pos.view.NuevoProducto;

import java.io.File;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductoControllerTest {

    @Mock
    private GestionProductos vistaGestion;
    @Mock
    private NuevoProducto vistaRegistro;
    @Mock
    private EditarProducto vistaEdicion;
    @Mock
    private CargaMasivaProductos vistaCarga;
    @Mock
    private MainView mainView;

    private final JTable tablaProductos = new JTable(new DefaultTableModel(
            new Object[][]{}, new String[]{"Id", "Codigo", "Descripcion", "Emprendimiento", "Precio", "Stock", "Acciones"}));
    private final JComboBox<Object> cbFiltroEmprendedor = new JComboBox<>();
    private final JTextField txtBuscar = new JTextField();
    private final JRadioButton rbVerInactivos = new JRadioButton();

    private final JComboBox<Object> cbEmprendedorRegistro = new JComboBox<>();
    private final JButton btnRegistrar = new JButton();
    private final JButton btnCancelarRegistro = new JButton();
    private final JButton btnBackRegistro = new JButton();

    private final JComboBox<Object> cbEmprendedorEdicion = new JComboBox<>();
    private final JButton btnActualizar = new JButton();

    private final JComboBox<Object> cbEmprendedorCarga = new JComboBox<>();
    private final JButton btnSeleccionarArchivo = new JButton();
    private final JButton btnVisualizar = new JButton();
    private final JButton btnRegistrarCarga = new JButton();
    private final JLabel lblNombreArchivo = new JLabel();
    private final JTable tablaCarga = new JTable(new DefaultTableModel(
            new Object[][]{}, new String[]{"Codigo", "Descripcion", "Precio", "Stock", "Departamento"}));

    private MockedConstruction<ProductoDAO> construccionProductoDao;
    private MockedConstruction<EmprendedorDAO> construccionEmprendedorDao;
    private ProductoDAO dao;
    private ProductoController controller;

    @BeforeEach
    void construirController() {
        when(vistaGestion.getTablaProductos()).thenReturn(tablaProductos);
        when(vistaGestion.getCbFiltroEmprendedor()).thenReturn(cbFiltroEmprendedor);
        when(vistaGestion.getTxtBuscar()).thenReturn(txtBuscar);
        when(vistaGestion.getRbVerInactivos()).thenReturn(rbVerInactivos);

        when(vistaRegistro.getCbEmprendedor()).thenReturn(cbEmprendedorRegistro);
        when(vistaRegistro.getBtnRegistrar()).thenReturn(btnRegistrar);
        when(vistaRegistro.getBtnCancelar()).thenReturn(btnCancelarRegistro);
        when(vistaRegistro.getBtnBack()).thenReturn(btnBackRegistro);
        when(vistaRegistro.getDepartamentoField()).thenReturn("");

        when(vistaEdicion.getCbEmprendedor()).thenReturn(cbEmprendedorEdicion);
        when(vistaEdicion.getBtnActualizar()).thenReturn(btnActualizar);
        when(vistaEdicion.getDescripcionField()).thenReturn("");
        when(vistaEdicion.getDepartamentoField()).thenReturn("");

        when(vistaCarga.getCbEmprendedor()).thenReturn(cbEmprendedorCarga);
        when(vistaCarga.getBtnSeleccionarArchivo()).thenReturn(btnSeleccionarArchivo);
        when(vistaCarga.getBtnVisualizar()).thenReturn(btnVisualizar);
        when(vistaCarga.getBtnRegistrar()).thenReturn(btnRegistrarCarga);
        when(vistaCarga.getLblNombreArchivo()).thenReturn(lblNombreArchivo);
        when(vistaCarga.getVistaTablaProductos()).thenReturn(tablaCarga);

        construccionProductoDao = mockConstruction(ProductoDAO.class,
                (m, ctx) -> when(m.listarTodos()).thenReturn(List.of()));
        construccionEmprendedorDao = mockConstruction(EmprendedorDAO.class,
                (m, ctx) -> when(m.listarNombresYId()).thenReturn(List.of()));

        controller = new ProductoController(vistaGestion, vistaRegistro, vistaEdicion, vistaCarga, mainView);
        dao = construccionProductoDao.constructed().get(0);
    }

    @AfterEach
    void cerrarMocks() {
        construccionProductoDao.close();
        construccionEmprendedorDao.close();
    }

    private Emprendedor emprendedorDePrueba(int id) {
        Emprendedor emp = new Emprendedor();
        emp.setId(id);
        emp.setMarca("JUNIT MARCA");
        return emp;
    }

    private Producto productoDePrueba(int id) {
        Producto p = new Producto();
        p.setIdProduct(id);
        p.setIdEntrepreneur(1);
        p.setFullProductCode("JT01");
        p.setProductDescription("PRODUCTO JUNIT");
        p.setDepartment("GENERAL");
        p.setCurrentPrice(new BigDecimal("50.00"));
        p.setCurrentStock(10);
        return p;
    }

    private void seleccionarEmprendedor(JComboBox<Object> combo, Emprendedor emp) {
        combo.addItem(emp);
        combo.setSelectedItem(emp);
    }

    private Object invocarPrivado(String nombre, Class<?>[] tipos, Object... args) throws Exception {
        Method m = ProductoController.class.getDeclaredMethod(nombre, tipos);
        m.setAccessible(true);
        return m.invoke(controller, args);
    }

    // --- registrarProducto() ---

    @Test
    void registrarProducto_sinEmprendedorSeleccionado_muestraAviso() {
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnRegistrar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaRegistro), eq("Por favor, selecciona un emprendedor válido.")));
        }
        verify(dao, never()).registrar(any());
    }

    @Test
    void registrarProducto_camposVacios_muestraAviso() {
        seleccionarEmprendedor(cbEmprendedorRegistro, emprendedorDePrueba(1));
        when(vistaRegistro.getCodigoField()).thenReturn("");
        when(vistaRegistro.getDescripcionField()).thenReturn("");
        when(vistaRegistro.getPrecioField()).thenReturn("");
        when(vistaRegistro.getStockField()).thenReturn("");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnRegistrar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaRegistro), eq("Completa todos los campos obligatorios.")));
        }
        verify(dao, never()).registrar(any());
    }

    @Test
    void registrarProducto_codigoFormatoInvalido_muestraError() {
        seleccionarEmprendedor(cbEmprendedorRegistro, emprendedorDePrueba(1));
        when(vistaRegistro.getCodigoField()).thenReturn("ABC");
        when(vistaRegistro.getDescripcionField()).thenReturn("Desc");
        when(vistaRegistro.getPrecioField()).thenReturn("10.00");
        when(vistaRegistro.getStockField()).thenReturn("5");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnRegistrar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaRegistro), eq("El código debe tener el formato AA00 (2 letras seguidas de 2 números).")));
        }
        verify(dao, never()).registrar(any());
    }

    @Test
    void registrarProducto_precioNoNumerico_muestraError() {
        seleccionarEmprendedor(cbEmprendedorRegistro, emprendedorDePrueba(1));
        when(vistaRegistro.getCodigoField()).thenReturn("AA00");
        when(vistaRegistro.getDescripcionField()).thenReturn("Desc");
        when(vistaRegistro.getPrecioField()).thenReturn("no-numero");
        when(vistaRegistro.getStockField()).thenReturn("5");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnRegistrar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaRegistro), eq("El precio y el stock deben ser valores numéricos.")));
        }
        verify(dao, never()).registrar(any());
    }

    @Test
    void registrarProducto_exitoso_registraYNavega() {
        seleccionarEmprendedor(cbEmprendedorRegistro, emprendedorDePrueba(1));
        when(vistaRegistro.getCodigoField()).thenReturn("AA00");
        when(vistaRegistro.getDescripcionField()).thenReturn("Desc");
        when(vistaRegistro.getDepartamentoField()).thenReturn("General");
        when(vistaRegistro.getPrecioField()).thenReturn("10.00");
        when(vistaRegistro.getStockField()).thenReturn("5");
        when(dao.registrar(any())).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnRegistrar.doClick();
        }

        verify(vistaRegistro).limpiarFormulario();
        verify(mainView).showView("products");
    }

    @Test
    void registrarProducto_elDaoFalla_muestraError() {
        seleccionarEmprendedor(cbEmprendedorRegistro, emprendedorDePrueba(1));
        when(vistaRegistro.getCodigoField()).thenReturn("AA00");
        when(vistaRegistro.getDescripcionField()).thenReturn("Desc");
        when(vistaRegistro.getPrecioField()).thenReturn("10.00");
        when(vistaRegistro.getStockField()).thenReturn("5");
        when(dao.registrar(any())).thenReturn(false);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnRegistrar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaRegistro), eq("Error al guardar. Verifica que el código no esté duplicado.")));
        }
        verify(mainView, never()).showView("products");
    }

    // --- cargarTablaProductos() / filtrarTabla() ---

    @Test
    void cargarTablaProductos_llenaLaTablaConLosProductosListados() {
        when(dao.listarTodos()).thenReturn(List.of(productoDePrueba(1)));

        controller.cargarTablaProductos();

        assertEquals(1, tablaProductos.getModel().getRowCount());
    }

    @Test
    void filtrarTabla_llenaLaTablaConLosResultadosDeBusqueda() {
        when(dao.buscarAvanzado(anyString(), anyInt(), anyBoolean())).thenReturn(List.of(productoDePrueba(1)));
        txtBuscar.setText("junit");

        controller.filtrarTabla();

        assertEquals(1, tablaProductos.getModel().getRowCount());
    }

    // --- getSelectedEntrepreneurId() ---

    @Test
    void getSelectedEntrepreneurId_conEmprendedorSeleccionado_devuelveSuId() {
        seleccionarEmprendedor(cbFiltroEmprendedor, emprendedorDePrueba(7));

        assertEquals(7, controller.getSelectedEntrepreneurId());
    }

    @Test
    void getSelectedEntrepreneurId_sinSeleccionEspecifica_devuelveCero() {
        cbFiltroEmprendedor.addItem("--- Todos ---");
        cbFiltroEmprendedor.setSelectedItem("--- Todos ---");

        assertEquals(0, controller.getSelectedEntrepreneurId());
    }

    // --- prepararEdicion() / actualizarProducto() ---

    @Test
    void prepararEdicion_cargaLosDatosEnLaVistaDeEdicionYNavega() throws Exception {
        when(dao.buscarPorId(1)).thenReturn(productoDePrueba(1));

        invocarPrivado("prepararEdicion", new Class<?>[]{int.class}, 1);

        verify(vistaEdicion).setCodigoField("JT01");
        verify(mainView).showView("editarProducto");
    }

    @Test
    void prepararEdicion_productoNoExiste_noNavega() throws Exception {
        when(dao.buscarPorId(99)).thenReturn(null);

        invocarPrivado("prepararEdicion", new Class<?>[]{int.class}, 99);

        verify(mainView, never()).showView("editarProducto");
    }

    @Test
    void actualizarProducto_sinEmprendedorSeleccionado_muestraAviso() {
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnActualizar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaEdicion), eq("Selecciona un emprendedor.")));
        }
        verify(dao, never()).actualizar(any());
    }

    @Test
    void actualizarProducto_codigoFormatoInvalido_muestraError() {
        seleccionarEmprendedor(cbEmprendedorEdicion, emprendedorDePrueba(1));
        when(vistaEdicion.getCodigoField()).thenReturn("MAL");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnActualizar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaEdicion), eq("El código debe tener el formato AA00 (2 letras seguidas de 2 números).")));
        }
        verify(dao, never()).actualizar(any());
    }

    @Test
    void actualizarProducto_precioNoNumerico_muestraError() {
        seleccionarEmprendedor(cbEmprendedorEdicion, emprendedorDePrueba(1));
        when(vistaEdicion.getCodigoField()).thenReturn("AA00");
        when(vistaEdicion.getPrecioField()).thenReturn("no-numero");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnActualizar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaEdicion), eq("Verifica que el precio y stock sean números válidos.")));
        }
        verify(dao, never()).actualizar(any());
    }

    @Test
    void actualizarProducto_exitoso_actualizaYNavega() {
        seleccionarEmprendedor(cbEmprendedorEdicion, emprendedorDePrueba(1));
        when(vistaEdicion.getCodigoField()).thenReturn("AA00");
        when(vistaEdicion.getPrecioField()).thenReturn("10.00");
        when(vistaEdicion.getStockField()).thenReturn("5");
        when(dao.actualizar(any())).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnActualizar.doClick();
        }

        verify(mainView).showView("products");
    }

    @Test
    void actualizarProducto_elDaoFalla_muestraError() {
        seleccionarEmprendedor(cbEmprendedorEdicion, emprendedorDePrueba(1));
        when(vistaEdicion.getCodigoField()).thenReturn("AA00");
        when(vistaEdicion.getPrecioField()).thenReturn("10.00");
        when(vistaEdicion.getStockField()).thenReturn("5");
        when(dao.actualizar(any())).thenReturn(false);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnActualizar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaEdicion), eq("Error al actualizar. Posible código duplicado.")));
        }
        verify(mainView, never()).showView("products");
    }

    // --- confirmarEliminacion(int, int) ---

    @Test
    void confirmarEliminacion_usuarioConfirma_desactivaYRecarga() throws Exception {
        DefaultTableModel modelo = (DefaultTableModel) tablaProductos.getModel();
        modelo.addRow(new Object[]{1, "AA00", "Desc", "Marca", "$10.00", 5, ""});
        when(dao.eliminarLogico(1)).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), eq("Confirmar Baja de Producto"), eq(JOptionPane.YES_NO_OPTION), eq(JOptionPane.WARNING_MESSAGE)))
                    .thenReturn(JOptionPane.YES_OPTION);

            invocarPrivado("confirmarEliminacion", new Class<?>[]{int.class, int.class}, 1, 0);

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(mainView), eq("El producto AA00 ha sido desactivado.")));
        }
        verify(dao).eliminarLogico(1);
    }

    @Test
    void confirmarEliminacion_usuarioCancela_noHaceNada() throws Exception {
        DefaultTableModel modelo = (DefaultTableModel) tablaProductos.getModel();
        modelo.addRow(new Object[]{1, "AA00", "Desc", "Marca", "$10.00", 5, ""});

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(JOptionPane.NO_OPTION);

            invocarPrivado("confirmarEliminacion", new Class<?>[]{int.class, int.class}, 1, 0);
        }
        verify(dao, never()).eliminarLogico(anyInt());
    }

    @Test
    void confirmarEliminacion_elDaoFalla_muestraError() throws Exception {
        DefaultTableModel modelo = (DefaultTableModel) tablaProductos.getModel();
        modelo.addRow(new Object[]{1, "AA00", "Desc", "Marca", "$10.00", 5, ""});
        when(dao.eliminarLogico(1)).thenReturn(false);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(JOptionPane.YES_OPTION);

            invocarPrivado("confirmarEliminacion", new Class<?>[]{int.class, int.class}, 1, 0);

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(mainView), eq("No se pudo desactivar el producto."), eq("Error"), eq(JOptionPane.ERROR_MESSAGE)));
        }
    }

    // --- confirmarActivacion(int, String) ---

    @Test
    void confirmarActivacion_usuarioCancela_noHaceNada() throws Exception {
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt()))
                    .thenReturn(JOptionPane.NO_OPTION);

            invocarPrivado("confirmarActivacion", new Class<?>[]{int.class, String.class}, 1, "AA00");
        }
        verify(dao, never()).activarProductoConValidacion(anyInt());
    }

    @Test
    void confirmarActivacion_exitoso_reactiva() throws Exception {
        when(dao.activarProductoConValidacion(1)).thenReturn(1);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), eq("Reactivar Producto"), eq(JOptionPane.YES_NO_OPTION)))
                    .thenReturn(JOptionPane.YES_OPTION);

            invocarPrivado("confirmarActivacion", new Class<?>[]{int.class, String.class}, 1, "AA00");

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(mainView), eq("Producto reactivado con éxito.")));
        }
    }

    @Test
    void confirmarActivacion_emprendedorDesactivado_muestraErrorEspecifico() throws Exception {
        when(dao.activarProductoConValidacion(1)).thenReturn(-1);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt()))
                    .thenReturn(JOptionPane.YES_OPTION);

            invocarPrivado("confirmarActivacion", new Class<?>[]{int.class, String.class}, 1, "AA00");

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(
                    eq(mainView), anyString(), eq("Acción Denegada"), eq(JOptionPane.ERROR_MESSAGE)));
        }
    }

    @Test
    void confirmarActivacion_errorGenerico_muestraError() throws Exception {
        when(dao.activarProductoConValidacion(1)).thenReturn(0);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt()))
                    .thenReturn(JOptionPane.YES_OPTION);

            invocarPrivado("confirmarActivacion", new Class<?>[]{int.class, String.class}, 1, "AA00");

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(mainView), eq("Error al intentar activar el producto.")));
        }
    }

    // --- Carga masiva ---

    @Test
    void seleccionarArchivo_usuarioCancela_noAsignaArchivo() {
        try (MockedConstruction<JFileChooser> construccionChooser = mockConstruction(JFileChooser.class,
                (mock, ctx) -> when(mock.showOpenDialog(any())).thenReturn(JFileChooser.CANCEL_OPTION))) {
            btnSeleccionarArchivo.doClick();
        }
        verify(vistaCarga, never()).getLblNombreArchivo();
    }

    @Test
    void seleccionarArchivo_usuarioSeleccionaArchivo_actualizaLaEtiqueta() {
        File archivo = new File("productos_prueba.xlsx");
        try (MockedConstruction<JFileChooser> construccionChooser = mockConstruction(JFileChooser.class,
                (mock, ctx) -> {
                    when(mock.showOpenDialog(any())).thenReturn(JFileChooser.APPROVE_OPTION);
                    when(mock.getSelectedFile()).thenReturn(archivo);
                })) {
            btnSeleccionarArchivo.doClick();
        }
        assertEquals("Archivo: productos_prueba.xlsx", lblNombreArchivo.getText());
    }

    @Test
    void visualizarExcel_sinArchivoSeleccionado_muestraAviso() throws Exception {
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            invocarPrivado("visualizarExcel", new Class<?>[]{});
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaCarga), eq("Selecciona un archivo primero.")));
        }
    }

    @Test
    void ejecutarCargaMasiva_sinEmprendedorSeleccionado_muestraAviso() {
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnRegistrarCarga.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaCarga), eq("Selecciona un emprendedor.")));
        }
        verify(dao, never()).registrarOSumarStock(any());
    }

    @Test
    void ejecutarCargaMasiva_filaVacia_seIgnoraYNoLlamaAlDao() {
        seleccionarEmprendedor(cbEmprendedorCarga, emprendedorDePrueba(1));
        DefaultTableModel modelo = (DefaultTableModel) tablaCarga.getModel();
        modelo.addRow(new Object[]{"", "", "", "", ""});

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnRegistrarCarga.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaCarga), anyString()));
        }
        verify(dao, never()).registrarOSumarStock(any());
    }

    @Test
    void ejecutarCargaMasiva_codigoConFormatoInvalido_cuentaError() {
        seleccionarEmprendedor(cbEmprendedorCarga, emprendedorDePrueba(1));
        DefaultTableModel modelo = (DefaultTableModel) tablaCarga.getModel();
        modelo.addRow(new Object[]{"MAL", "Desc", "10.00", "5", "General"});

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnRegistrarCarga.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaCarga), eq(
                    "Carga terminada.\nNuevos: 0\nStock sumado (ya existían): 0\nErrores: 1")));
        }
        verify(dao, never()).registrarOSumarStock(any());
    }

    @Test
    void ejecutarCargaMasiva_productoNuevoYExistente_muestraResumenYNavega() {
        seleccionarEmprendedor(cbEmprendedorCarga, emprendedorDePrueba(1));
        DefaultTableModel modelo = (DefaultTableModel) tablaCarga.getModel();
        modelo.addRow(new Object[]{"AA00", "Desc Nuevo", "10.00", "5", "General"});
        modelo.addRow(new Object[]{"BB11", "Desc Existente", "20.00", "3", "General"});
        when(dao.registrarOSumarStock(any())).thenReturn(1, 2);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnRegistrarCarga.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaCarga), eq(
                    "Carga terminada.\nNuevos: 1\nStock sumado (ya existían): 1\nErrores: 0")));
        }
        verify(mainView).showView("products");
    }
}
