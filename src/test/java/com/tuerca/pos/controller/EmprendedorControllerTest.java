package com.tuerca.pos.controller;

import com.tuerca.pos.dao.EmprendedorDAO;
import com.tuerca.pos.model.Emprendedor;
import com.tuerca.pos.view.EditarEmprendimiento;
import com.tuerca.pos.view.GestionEmprendedores;
import com.tuerca.pos.view.MainView;
import com.tuerca.pos.view.NuevoEmprendedor;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.List;
import javax.swing.JButton;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmprendedorControllerTest {

    @Mock
    private NuevoEmprendedor vistaRegistro;
    @Mock
    private EditarEmprendimiento vistaEdicion;
    @Mock
    private GestionEmprendedores vistaGestion;
    @Mock
    private MainView mainView;
    @Mock
    private ProductoController prodController;

    private final JButton btnRegistrar = new JButton();
    private final JButton btnActualizar = new JButton();
    private final JRadioButton rbVerInactivos = new JRadioButton();
    private final JTextField txtBuscar = new JTextField();
    private final JTable tablaEmprendedores = new JTable(new DefaultTableModel(
            new Object[][]{}, new String[]{"Id", "Marca", "Contacto", "Tel", "Email", "Fecha", "Renta", "Acciones"}));

    private MockedConstruction<EmprendedorDAO> construccionDao;
    private EmprendedorDAO dao;
    private EmprendedorController controller;

    @BeforeEach
    void construirController() {
        when(vistaRegistro.getBtnRegistrar()).thenReturn(btnRegistrar);
        when(vistaEdicion.getBtnActualizar()).thenReturn(btnActualizar);
        when(vistaGestion.getRbVerInactivos()).thenReturn(rbVerInactivos);
        when(vistaGestion.getTxtBuscar()).thenReturn(txtBuscar);
        when(vistaGestion.getTablaEmprendedores()).thenReturn(tablaEmprendedores);
        when(mainView.getProdController()).thenReturn(prodController);

        construccionDao = mockConstruction(EmprendedorDAO.class,
                (m, ctx) -> when(m.listar()).thenReturn(List.of()));

        controller = new EmprendedorController(vistaRegistro, vistaEdicion, vistaGestion, mainView);
        dao = construccionDao.constructed().get(0);
    }

    @AfterEach
    void cerrarMocks() {
        construccionDao.close();
    }

    private Emprendedor emprendedorDePrueba(int id) {
        Emprendedor emp = new Emprendedor();
        emp.setId(id);
        emp.setMarca("JUNIT MARCA");
        emp.setNombreContacto("Contacto");
        emp.setTelefono("5555555555");
        emp.setEmail("test@test.com");
        emp.setRentaMensual(500.0);
        emp.setFechaContrato(new java.sql.Date(fecha(2026, 1, 1).getTime()));
        return emp;
    }

    private java.util.Date fecha(int anio, int mes, int dia) {
        Calendar cal = Calendar.getInstance();
        cal.set(anio, mes - 1, dia, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private void invocarPrivado(String nombre, Class<?>[] tipos, Object... args) throws Exception {
        Method m = EmprendedorController.class.getDeclaredMethod(nombre, tipos);
        m.setAccessible(true);
        m.invoke(controller, args);
    }

    @Test
    void registrarEmprendedor_camposVacios_muestraAviso() {
        when(vistaRegistro.getBrandName()).thenReturn("");
        when(vistaRegistro.getContactName()).thenReturn("");
        when(vistaRegistro.getRent()).thenReturn("");
        when(vistaRegistro.getFechaSeleccionada()).thenReturn(null);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnRegistrar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaRegistro), eq("Por favor, completa los campos obligatorios.")));
        }
        verify(dao, never()).registrar(any());
    }

    @Test
    void registrarEmprendedor_rentaNoNumerica_muestraError() {
        when(vistaRegistro.getBrandName()).thenReturn("Marca");
        when(vistaRegistro.getContactName()).thenReturn("Contacto");
        when(vistaRegistro.getRent()).thenReturn("no-es-numero");
        when(vistaRegistro.getFechaSeleccionada()).thenReturn(fecha(2026, 1, 1));

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnRegistrar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaRegistro), eq("La renta debe ser un número válido.")));
        }
        verify(dao, never()).registrar(any());
    }

    @Test
    void registrarEmprendedor_exitoso_refrescaCatalogosYNavega() {
        when(vistaRegistro.getBrandName()).thenReturn("Marca");
        when(vistaRegistro.getContactName()).thenReturn("Contacto");
        when(vistaRegistro.getContactPhone()).thenReturn("5555555555");
        when(vistaRegistro.getEmail()).thenReturn("test@test.com");
        when(vistaRegistro.getRent()).thenReturn("500.00");
        when(vistaRegistro.getFechaSeleccionada()).thenReturn(fecha(2026, 1, 1));
        when(dao.registrar(any())).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnRegistrar.doClick();
        }

        verify(prodController).refrescarCatalogos();
        verify(vistaRegistro).limpiarFormulario();
        verify(mainView).showView("entrepreneur");
    }

    @Test
    void registrarEmprendedor_elDaoFalla_muestraError() {
        when(vistaRegistro.getBrandName()).thenReturn("Marca");
        when(vistaRegistro.getContactName()).thenReturn("Contacto");
        when(vistaRegistro.getRent()).thenReturn("500.00");
        when(vistaRegistro.getFechaSeleccionada()).thenReturn(fecha(2026, 1, 1));
        when(dao.registrar(any())).thenReturn(false);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnRegistrar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaRegistro), eq("Error al guardar en la base de datos.")));
        }
        verify(mainView, never()).showView(any());
    }

    @Test
    void cargarTabla_llenaLaTablaConLosEmprendedoresListados() {
        when(dao.listar()).thenReturn(List.of(emprendedorDePrueba(1)));

        controller.cargarTabla();

        assertEquals(1, tablaEmprendedores.getModel().getRowCount());
    }

    @Test
    void prepararEdicion_cargaLosDatosEnLaVistaDeEdicionYNavega() throws Exception {
        when(dao.buscarPorId(1)).thenReturn(emprendedorDePrueba(1));

        invocarPrivado("prepararEdicion", new Class<?>[]{int.class}, 1);

        verify(vistaEdicion).setBrandName("JUNIT MARCA");
        verify(mainView).showView("editarEmprendimiento");
    }

    @Test
    void actualizarEmpleado_sinFecha_muestraAviso() throws Exception {
        prepararEdicionDePrueba();
        when(vistaEdicion.getFechaSeleccionada()).thenReturn(null);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnActualizar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaEdicion), eq("Por favor, selecciona una fecha válida.")));
        }
        verify(dao, never()).actualizar(any());
    }

    @Test
    void actualizarEmpleado_exitoso_actualizaYRegresaALaGestion() throws Exception {
        prepararEdicionDePrueba();
        when(vistaEdicion.getBrandName()).thenReturn("Marca Editada");
        when(vistaEdicion.getContactName()).thenReturn("Contacto");
        when(vistaEdicion.getRent()).thenReturn("600.00");
        when(vistaEdicion.getFechaSeleccionada()).thenReturn(fecha(2026, 2, 1));
        when(dao.actualizar(any())).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnActualizar.doClick();
        }

        verify(mainView).showView("entrepreneur");
    }

    private void prepararEdicionDePrueba() throws Exception {
        when(dao.buscarPorId(1)).thenReturn(emprendedorDePrueba(1));
        invocarPrivado("prepararEdicion", new Class<?>[]{int.class}, 1);
    }

    @Test
    void confirmarEliminacion_usuarioConfirma_desactivaYRecarga() throws Exception {
        when(dao.eliminarLogico(1)).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), eq("Confirmar Eliminación Lógica"), eq(JOptionPane.YES_NO_OPTION), eq(JOptionPane.WARNING_MESSAGE)))
                    .thenReturn(JOptionPane.YES_OPTION);

            invocarPrivado("confirmarEliminacion", new Class<?>[]{int.class, int.class}, 1, 0);

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(mainView), eq("Emprendimiento desactivado con éxito.")));
        }
        verify(dao).eliminarLogico(1);
    }

    @Test
    void confirmarEliminacion_usuarioCancela_noHaceNada() throws Exception {
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(JOptionPane.NO_OPTION);

            invocarPrivado("confirmarEliminacion", new Class<?>[]{int.class, int.class}, 1, 0);
        }
        verify(dao, never()).eliminarLogico(anyInt());
    }

    @Test
    void confirmarActivacion_usuarioConfirma_reactiva() throws Exception {
        when(dao.activar(1)).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), eq("Reactivar Emprendedor"), eq(JOptionPane.YES_NO_OPTION)))
                    .thenReturn(JOptionPane.YES_OPTION);

            invocarPrivado("confirmarActivacion", new Class<?>[]{int.class, String.class}, 1, "JUNIT MARCA");

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(mainView), eq("Emprendimiento reactivado con éxito.")));
        }
    }
}
