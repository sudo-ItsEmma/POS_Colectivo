package com.tuerca.pos.view;

import com.tuerca.pos.controller.AdminDashboardController;
import com.tuerca.pos.controller.ApartadoController;
import com.tuerca.pos.controller.AperturaCajaController;
import com.tuerca.pos.controller.ArqueoCajaController;
import com.tuerca.pos.controller.CorteCajaController;
import com.tuerca.pos.controller.DevolucionController;
import com.tuerca.pos.controller.EmpleadoController;
import com.tuerca.pos.controller.EmployeeDashboardController;
import com.tuerca.pos.controller.EmprendedorController;
import com.tuerca.pos.controller.LoginController;
import com.tuerca.pos.controller.PagoEmprendedoresController;
import com.tuerca.pos.controller.ProductoController;
import com.tuerca.pos.controller.VentaController;
import com.tuerca.pos.model.Sesion;

import java.awt.CardLayout;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 * Ventana principal de la aplicación. Contiene todas las pantallas del
 * sistema como cartas de un {@link CardLayout} y expone {@link #showView}
 * para navegar entre ellas.
 */
public class MainView extends JFrame {

    private final LoginPanel loginPanel1 = new LoginPanel();
    private final AperturaCajaPanel aperturaCajaPanel1 = new AperturaCajaPanel();
    private final EmployeePanel employeePanel2 = new EmployeePanel();
    private final AdminPanel adminPanel2 = new AdminPanel();
    private final GestionEmprendedores gestionEmprendedores1 = new GestionEmprendedores();
    private final GestionProductos gestionProductos1 = new GestionProductos();
    private final Ventas ventas1 = new Ventas();
    private final GestionDevoluciones gestionDevoluciones1 = new GestionDevoluciones();
    private final GestionApartados gestionApartados1 = new GestionApartados();
    private final ArqueoDeCaja arqueoDeCaja1 = new ArqueoDeCaja();
    private final CorteDeCaja corteDeCaja1 = new CorteDeCaja();
    private final PagoEmprendedores pagoEmprendedores1 = new PagoEmprendedores();
    private final GenerarReportes generarReportes1 = new GenerarReportes();
    private final GestionEmpleados gestionEmpleados1 = new GestionEmpleados();
    private final NuevoEmpleado nuevoEmpleado1 = new NuevoEmpleado();
    private final EditarEmpleado editarEmpleado1 = new EditarEmpleado();
    private final NuevoEmprendedor nuevoEmprendedor1 = new NuevoEmprendedor();
    private final EditarEmprendimiento editarEmprendimiento1 = new EditarEmprendimiento();
    private final NuevoProducto nuevoProducto1 = new NuevoProducto();
    private final EditarProducto editarProducto1 = new EditarProducto();
    private final CargaMasivaProductos cargaMasivaProductos1 = new CargaMasivaProductos();

    private LoginController loginController;
    private AperturaCajaController aperturaCajaController;
    private EmployeeDashboardController employeeDashboardController;
    private AdminDashboardController adminDashboardController;
    private ArqueoCajaController arqueoCajaController;
    private CorteCajaController corteCajaController;
    private EmpleadoController empController;
    private EmprendedorController empreController;
    private ProductoController prodController;
    private VentaController ventaController;
    private ApartadoController apartadoController;
    private DevolucionController devolucionController;
    private PagoEmprendedoresController pagoEmprendedoresController;

    public MainView() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new CardLayout());

        getContentPane().add(loginPanel1, "login");
        getContentPane().add(aperturaCajaPanel1, "aperturaCaja");
        getContentPane().add(employeePanel2, "employee");
        getContentPane().add(adminPanel2, "admin");
        getContentPane().add(gestionEmprendedores1, "entrepreneur");
        getContentPane().add(gestionProductos1, "products");
        getContentPane().add(ventas1, "ventas");
        getContentPane().add(gestionDevoluciones1, "devoluciones");
        getContentPane().add(gestionApartados1, "apartados");
        getContentPane().add(arqueoDeCaja1, "arqueo");
        getContentPane().add(corteDeCaja1, "corte");
        getContentPane().add(pagoEmprendedores1, "pagoEmprendedores");
        getContentPane().add(generarReportes1, "reportes");
        getContentPane().add(gestionEmpleados1, "empleados");
        getContentPane().add(nuevoEmpleado1, "nuevoEmpleado");
        getContentPane().add(editarEmpleado1, "editarEmpleado");
        getContentPane().add(nuevoEmprendedor1, "nuevoEmprendedor");
        getContentPane().add(editarEmprendimiento1, "editarEmprendimiento");
        getContentPane().add(nuevoProducto1, "nuevoProducto");
        getContentPane().add(editarProducto1, "editarProducto");
        getContentPane().add(cargaMasivaProductos1, "cargaMasiva");

        pack();
        this.setSize(1280, 720);
        this.setLocationRelativeTo(null);

        loginController = new LoginController(loginPanel1, aperturaCajaPanel1, this);
        aperturaCajaController = new AperturaCajaController(aperturaCajaPanel1, this);
        employeeDashboardController = new EmployeeDashboardController(employeePanel2, this);
        adminDashboardController = new AdminDashboardController(adminPanel2, this);
        arqueoCajaController = new ArqueoCajaController(arqueoDeCaja1, this);
        corteCajaController = new CorteCajaController(corteDeCaja1, this);

        empController = new EmpleadoController(
                nuevoEmpleado1,
                editarEmpleado1,
                gestionEmpleados1,
                this
        );
        empController.cargarTabla();

        empreController = new EmprendedorController(
                nuevoEmprendedor1,
                editarEmprendimiento1,
                gestionEmprendedores1,
                this
        );
        empreController.cargarTabla();

        prodController = new ProductoController(
                gestionProductos1,
                nuevoProducto1,
                editarProducto1,
                cargaMasivaProductos1,
                this
        );

        ventaController = new VentaController(
                ventas1,
                this
        );

        apartadoController = new ApartadoController(
                ventas1,
                gestionApartados1,
                this
        );

        devolucionController = new DevolucionController(
                gestionDevoluciones1,
                this
        );

        pagoEmprendedoresController = new PagoEmprendedoresController(
                pagoEmprendedores1,
                this
        );

        showView("login");
    }

    public ProductoController getProdController() {
        return prodController;
    }

    public void showView(String viewName) {
        if ("employee".equals(viewName)) {
            employeePanel2.setNombreUsuarioActivo(textoUsuarioActivo());
        } else if ("admin".equals(viewName)) {
            adminPanel2.setNombreUsuarioActivo(textoUsuarioActivo());
        } else if ("entrepreneur".equals(viewName)) {
            gestionEmprendedores1.setNombreUsuarioActivo("Usuario: " + Sesion.getInstancia().getNombreCompleto());
        } else if ("products".equals(viewName)) {
            gestionProductos1.setNombreUsuarioActivo("Usuario: " + Sesion.getInstancia().getNombreCompleto());
        } else if ("ventas".equals(viewName)) {
            ventas1.setNombreUsuarioActivo("Usuario: " + Sesion.getInstancia().getNombreCompleto());
        } else if ("apartados".equals(viewName)) {
            gestionApartados1.setNombreUsuarioActivo("Usuario: " + Sesion.getInstancia().getNombreCompleto());
        } else if ("devoluciones".equals(viewName)) {
            gestionDevoluciones1.setNombreUsuarioActivo("Usuario: " + Sesion.getInstancia().getNombreCompleto());
        } else if ("pagoEmprendedores".equals(viewName)) {
            pagoEmprendedores1.setNombreUsuarioActivo("Usuario: " + Sesion.getInstancia().getNombreCompleto());
        } else if ("arqueo".equals(viewName)) {
            arqueoDeCaja1.setNombreUsuarioActivo("Usuario: " + Sesion.getInstancia().getNombreCompleto());
            arqueoCajaController.actualizarDatos();
        } else if ("corte".equals(viewName)) {
            corteDeCaja1.setNombreUsuarioActivo("Usuario: " + Sesion.getInstancia().getNombreCompleto());
            corteCajaController.actualizarDatos();
        }

        CardLayout cl = (CardLayout) getContentPane().getLayout();
        cl.show(getContentPane(), viewName);
    }

    private String textoUsuarioActivo() {
        Sesion sesion = Sesion.getInstancia();
        return "Usuario activo: " + sesion.getNombreCompleto() + " (" + sesion.getRoleName() + ")";
    }

    /** Cierra la sesión activa y regresa al login. La caja (CashSession) sigue abierta. */
    public void cerrarSesion() {
        Sesion.getInstancia().cerrarSesion();
        loginPanel1.limpiarFormulario();
        showView("login");
    }
}
