package com.tuerca.pos.controller;

import com.tuerca.pos.dao.EmpleadoDAO;
import com.tuerca.pos.model.Empleado;
import com.tuerca.pos.view.EditarEmpleado;
import com.tuerca.pos.view.GestionEmpleados;
import com.tuerca.pos.view.MainView;
import com.tuerca.pos.view.NuevoEmpleado;

import java.lang.reflect.Method;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
class EmpleadoControllerTest {

    @Mock
    private NuevoEmpleado vistaRegistro;
    @Mock
    private EditarEmpleado vistaEdicion;
    @Mock
    private GestionEmpleados vistaGestion;
    @Mock
    private MainView mainView;

    private final JButton btnRegistrar = new JButton();
    private final JButton btnCancelar = new JButton();
    private final JButton btnBack = new JButton();
    private final JButton btnActualizar = new JButton();
    private final JButton btnRestablecerContrasena = new JButton();
    private final JRadioButton rbVerInactivos = new JRadioButton();
    private final JTextField txtBuscar = new JTextField();
    private final JComboBox<String> rolComboBox = new JComboBox<>(new String[]{"Administrador", "Vendedor"});
    private final JTable tablaEmpleados = new JTable(new DefaultTableModel(
            new Object[][]{}, new String[]{"Id", "Nombre", "Paterno", "Materno", "Telefono", "Usuario", "Rol", "Acciones"}));

    private MockedConstruction<EmpleadoDAO> construccionDao;
    private EmpleadoDAO dao;
    private EmpleadoController controller;

    @BeforeEach
    void construirController() {
        when(vistaRegistro.getBtnRegistrar()).thenReturn(btnRegistrar);
        when(vistaRegistro.getBtnCancelar()).thenReturn(btnCancelar);
        when(vistaRegistro.getBtnBack()).thenReturn(btnBack);
        when(vistaEdicion.getBtnActualizar()).thenReturn(btnActualizar);
        when(vistaEdicion.getBtnRestablecerContrasena()).thenReturn(btnRestablecerContrasena);
        when(vistaEdicion.getRolComboBox()).thenReturn(rolComboBox);
        when(vistaGestion.getRbVerInactivos()).thenReturn(rbVerInactivos);
        when(vistaGestion.getTxtBuscar()).thenReturn(txtBuscar);
        when(vistaGestion.getTablaEmpleados()).thenReturn(tablaEmpleados);
        when(vistaGestion.getTableModel()).thenReturn((DefaultTableModel) tablaEmpleados.getModel());
        when(vistaRegistro.getMaterno()).thenReturn("");
        when(vistaEdicion.getMaternoField()).thenReturn("");

        construccionDao = mockConstruction(EmpleadoDAO.class,
                (m, ctx) -> when(m.listar()).thenReturn(List.of()));

        controller = new EmpleadoController(vistaRegistro, vistaEdicion, vistaGestion, mainView);
        dao = construccionDao.constructed().get(0);
    }

    @AfterEach
    void cerrarMocks() {
        construccionDao.close();
    }

    private Empleado empleadoDePrueba(int id) {
        Empleado emp = new Empleado();
        emp.setId(id);
        emp.setIdUserAccount(id + 100);
        emp.setNombre("JUNIT");
        emp.setPaterno("PRUEBA");
        emp.setMaterno("X");
        emp.setTelefono("5555555555");
        emp.setUsername("SAPXJ99");
        emp.setIdRole(2);
        emp.setRoleName("Sales");
        return emp;
    }

    private void invocarPrivado(String nombre, Class<?>[] tipos, Object... args) throws Exception {
        Method m = EmpleadoController.class.getDeclaredMethod(nombre, tipos);
        m.setAccessible(true);
        m.invoke(controller, args);
    }

    @Test
    void registrarEmpleado_camposVacios_muestraAviso() {
        when(vistaRegistro.getNombre()).thenReturn("");
        when(vistaRegistro.getPaterno()).thenReturn("");
        when(vistaRegistro.getContra()).thenReturn("");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnRegistrar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaRegistro), eq("Por favor, rellena los campos obligatorios.")));
        }
        verify(dao, never()).registrar(any());
    }

    @Test
    void registrarEmpleado_contraCorta_muestraError() {
        when(vistaRegistro.getNombre()).thenReturn("Nombre");
        when(vistaRegistro.getPaterno()).thenReturn("Paterno");
        when(vistaRegistro.getContra()).thenReturn("1234567");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnRegistrar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaRegistro), eq("La contraseña debe tener al menos 8 caracteres.")));
        }
        verify(dao, never()).registrar(any());
    }

    @Test
    void registrarEmpleado_contrasenasNoCoinciden_muestraError() {
        when(vistaRegistro.getNombre()).thenReturn("Nombre");
        when(vistaRegistro.getPaterno()).thenReturn("Paterno");
        when(vistaRegistro.getContra()).thenReturn("12345678");
        when(vistaRegistro.getConfirmarContra()).thenReturn("distinta1");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnRegistrar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaRegistro), eq("Las contraseñas no coinciden.")));
        }
        verify(dao, never()).registrar(any());
    }

    @Test
    void registrarEmpleado_exitoso_muestraUsuarioYNavega() {
        when(vistaRegistro.getNombre()).thenReturn("Nombre");
        when(vistaRegistro.getPaterno()).thenReturn("Paterno");
        when(vistaRegistro.getMaterno()).thenReturn("");
        when(vistaRegistro.getTelefono()).thenReturn("5555555555");
        when(vistaRegistro.getContra()).thenReturn("12345678");
        when(vistaRegistro.getConfirmarContra()).thenReturn("12345678");
        when(vistaRegistro.getRol()).thenReturn("Vendedor");
        when(dao.registrar(any())).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnRegistrar.doClick();
        }

        verify(vistaRegistro).limpiarFormulario();
        verify(mainView).showView("empleados");
    }

    @Test
    void prepararEdicion_cargaLosDatosEnLaVistaDeEdicionYNavega() throws Exception {
        when(dao.buscarPorId(1)).thenReturn(empleadoDePrueba(1));

        invocarPrivado("prepararEdicion", new Class<?>[]{int.class}, 1);

        verify(vistaEdicion).setNombreField("JUNIT");
        verify(mainView).showView("editarEmpleado");
    }

    @Test
    void actualizarEmpleado_camposVacios_muestraAviso() throws Exception {
        prepararEdicionDePrueba();
        when(vistaEdicion.getNombreField()).thenReturn("");
        when(vistaEdicion.getPaternoField()).thenReturn("");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnActualizar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaEdicion), eq("Nombre y Apellido Paterno son obligatorios.")));
        }
        verify(dao, never()).actualizar(any());
    }

    @Test
    void actualizarEmpleado_exitoso_actualizaYRegresaALaGestion() throws Exception {
        prepararEdicionDePrueba();
        when(vistaEdicion.getNombreField()).thenReturn("Editado");
        when(vistaEdicion.getPaternoField()).thenReturn("Paterno");
        when(vistaEdicion.getMaternoField()).thenReturn("Materno");
        when(dao.actualizar(any())).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnActualizar.doClick();
        }

        verify(mainView).showView("empleados");
    }

    @Test
    void actualizarEmpleado_elDaoFalla_muestraError() throws Exception {
        prepararEdicionDePrueba();
        when(vistaEdicion.getNombreField()).thenReturn("Editado");
        when(vistaEdicion.getPaternoField()).thenReturn("Paterno");
        when(dao.actualizar(any())).thenReturn(false);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            btnActualizar.doClick();
            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaEdicion), eq("Error al actualizar la información.")));
        }
        verify(mainView, never()).showView("empleados");
    }

    private void prepararEdicionDePrueba() throws Exception {
        when(dao.buscarPorId(1)).thenReturn(empleadoDePrueba(1));
        invocarPrivado("prepararEdicion", new Class<?>[]{int.class}, 1);
    }

    @Test
    void restablecerContrasena_usuarioCancelaLaConfirmacion_noLlamaAlDao() {
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(
                    eq(vistaEdicion), any(), eq("Confirmar restablecimiento"), eq(JOptionPane.YES_NO_OPTION), eq(JOptionPane.WARNING_MESSAGE)))
                    .thenReturn(JOptionPane.NO_OPTION);

            btnRestablecerContrasena.doClick();
        }
        verify(dao, never()).restablecerContrasena(anyInt());
    }

    @Test
    void restablecerContrasena_elDaoFalla_muestraError() throws Exception {
        prepararEdicionDePrueba();
        when(dao.restablecerContrasena(anyInt())).thenReturn(null);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(JOptionPane.YES_OPTION);

            btnRestablecerContrasena.doClick();

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(vistaEdicion), eq("No se pudo restablecer la contraseña."), eq("Error"), eq(JOptionPane.ERROR_MESSAGE)));
        }
    }

    @Test
    void restablecerContrasena_exitoso_muestraTemporal() throws Exception {
        prepararEdicionDePrueba();
        when(dao.restablecerContrasena(anyInt())).thenReturn("TempPass123");

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(JOptionPane.YES_OPTION);

            btnRestablecerContrasena.doClick();
        }
        verify(dao).restablecerContrasena(anyInt());
    }

    @Test
    void confirmarEliminacion_usuarioConfirma_desactivaYRecarga() throws Exception {
        when(dao.eliminarLogico(1)).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), eq("Confirmar Eliminación Lógica"), eq(JOptionPane.YES_NO_OPTION), eq(JOptionPane.WARNING_MESSAGE)))
                    .thenReturn(JOptionPane.YES_OPTION);

            invocarPrivado("confirmarEliminacion", new Class<?>[]{int.class, String.class}, 1, "JUNIT");

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(mainView), eq("Empleado desactivado con éxito.")));
        }
        verify(dao).eliminarLogico(1);
    }

    @Test
    void confirmarEliminacion_usuarioCancela_noHaceNada() throws Exception {
        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(JOptionPane.NO_OPTION);

            invocarPrivado("confirmarEliminacion", new Class<?>[]{int.class, String.class}, 1, "JUNIT");
        }
        verify(dao, never()).eliminarLogico(anyInt());
    }

    @Test
    void confirmarEliminacion_elDaoFalla_muestraError() throws Exception {
        when(dao.eliminarLogico(1)).thenReturn(false);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(JOptionPane.YES_OPTION);

            invocarPrivado("confirmarEliminacion", new Class<?>[]{int.class, String.class}, 1, "JUNIT");

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(mainView), eq("Error al intentar desactivar al empleado."), eq("Error"), eq(JOptionPane.ERROR_MESSAGE)));
        }
    }

    @Test
    void confirmarActivacion_usuarioConfirma_reactiva() throws Exception {
        when(dao.activarEmpleado(1)).thenReturn(true);

        try (MockedStatic<JOptionPane> jOptionPane = mockStatic(JOptionPane.class)) {
            jOptionPane.when(() -> JOptionPane.showConfirmDialog(eq(mainView), any(), eq("Reactivar Empleado"), eq(JOptionPane.YES_NO_OPTION)))
                    .thenReturn(JOptionPane.YES_OPTION);

            invocarPrivado("confirmarActivacion", new Class<?>[]{int.class, String.class}, 1, "JUNIT");

            jOptionPane.verify(() -> JOptionPane.showMessageDialog(eq(mainView), eq("Empleado reactivado con éxito.")));
        }
    }

    @Test
    void filtrarTabla_llenaLaTablaConLosResultadosDeBusqueda() {
        when(dao.buscarAvanzado("JUNIT", false)).thenReturn(List.of(empleadoDePrueba(1)));
        txtBuscar.setText("junit");

        controller.filtrarTabla();

        assertEquals(1, tablaEmpleados.getModel().getRowCount());
    }

    @Test
    void cargarTabla_llenaLaTablaConLosEmpleadosListados() {
        when(dao.listar()).thenReturn(List.of(empleadoDePrueba(1)));

        controller.cargarTabla();

        assertEquals(1, tablaEmpleados.getModel().getRowCount());
    }
}
