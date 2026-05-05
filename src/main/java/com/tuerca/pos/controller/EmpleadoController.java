/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.controller;

import com.tuerca.pos.dao.EmpleadoDAO;
import com.tuerca.pos.model.Empleado;
import com.tuerca.pos.view.EditarEmpleado;
import com.tuerca.pos.view.NuevoEmpleado;
import com.tuerca.pos.view.GestionEmpleados;
import com.tuerca.pos.view.MainView;
import com.tuerca.pos.view.components.AccionTableEvent;
import com.tuerca.pos.view.components.AccionesRender;
import com.tuerca.pos.view.components.AccionesEditar;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author mannycalderon
 */
public class EmpleadoController {
    private int idEdicion = -1; // Variable para saber qué ID estamos editando
    private EditarEmpleado vistaEdicion;
    private NuevoEmpleado vista;
    private EmpleadoDAO dao;
    private MainView mainView;
    private GestionEmpleados vistaGestion;;
    
    // El constructor recibe la instancia del panel
    public EmpleadoController(NuevoEmpleado vReg, EditarEmpleado vEdit, GestionEmpleados vGest, MainView main){
        this.vista = vReg;
        this.vistaEdicion = vEdit;
        this.vistaGestion = vGest;
        this.mainView = main;
        this.dao = new EmpleadoDAO();
        
        // inicializamos los listeners
        initTablaAcciones();
        initListeners();
    }
    
    private void initTablaAcciones() {
        // Definimos qué pasa cuando se pulsan los botones
        AccionTableEvent evento = new AccionTableEvent() {
            @Override
            public void onEditar(int row) {
                // Obtenemos el ID del empleado de la fila seleccionada (columna 0)
                int id = (int) vistaGestion.getTablaEmpleados().getValueAt(row, 0);
                prepararEdicion(id);
            }

            @Override
            public void onEliminar(int row) {
                int id = (int) vistaGestion.getTablaEmpleados().getValueAt(row, 0);
                confirmarEliminacion(id, row);
            }
        };

        // Aplicamos el Render y el Editor a la columna 6 (Acciones)
        vistaGestion.getTablaEmpleados().getColumnModel().getColumn(6).setCellRenderer(new AccionesRender());
        vistaGestion.getTablaEmpleados().getColumnModel().getColumn(6).setCellEditor(new AccionesEditar(evento));

        // Tip: Aumenta un poco el alto de las filas para que los botones luzcan mejor
        vistaGestion.getTablaEmpleados().setRowHeight(40);
    }
    
    private void initListeners(){
        this.vista.getBtnRegistrar().addActionListener(e -> registrarEmpleado());
    
        this.vista.getBtnCancelar().addActionListener(e -> vista.limpiarFormulario());
        
        this.vistaEdicion.getBtnActualizar().addActionListener(e -> actualizarEmpleado());

        this.vista.getBtnBack().addActionListener(e -> {
            vista.limpiarFormulario();
            mainView.showView("empleados"); // Asegúrate de que este sea el nombre de la vista
        });

        // AGREGAMOS EL LISTENER DEL MOUSE
        vistaGestion.getTablaEmpleados().addMouseMotionListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int col = vistaGestion.getTablaEmpleados().columnAtPoint(e.getPoint());
                if (col == 6) {
                    vistaGestion.getTablaEmpleados().setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                } else {
                    vistaGestion.getTablaEmpleados().setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }
            }
        });
        
        // listener para buscar por la barra de busqueda
        vistaGestion.getTxtBuscar().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrarTabla(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrarTabla(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrarTabla(); }
        });
    }
    
    private void registrarEmpleado(){
        // capturar datos
        String nombre = vista.getNombre();
        String paterno = vista.getPaterno();
        String materno = vista.getMaterno();
        String numero = vista.getTelefono();
        String contra = vista.getContra();
        String confirma = vista.getConfirmarContra();
        
        // validación de datos
        // nombre, apellido paterno y contraseña no esten vacios
        if(nombre.isEmpty() || paterno.isEmpty() || contra.isEmpty()){
            JOptionPane.showMessageDialog(vista, "Por favor, rellena los campos obligatorios.");
            return;
        }
        
        // si el apellido materno esta vacio agregamos una X
        if(materno.trim().isEmpty()){
            materno = "X";
        }
        
        // contraseña con almenos 8 caracteres
        if(contra.length() < 8){
            JOptionPane.showMessageDialog(vista, "La contraseña debe tener al menos 8 caracteres.");
            return;
        }
        
        // contraseñas iguales
        if(!contra.equals(confirma)){
            JOptionPane.showMessageDialog(vista, "Las contraseñas no coinciden.");
            return;
        }
        
        // procesamos los datos
        // Generamos nombre de usuario
        String rolSeleccionado = vista.getRol(); //Administrador o Vendedor (Admin o Sales)
        String userName = generarUsername(nombre, paterno, materno, rolSeleccionado);
        
        // Empaquetamos para llamar al DAO
        Empleado emp = new Empleado();
        emp.setNombre(nombre);
        emp.setPaterno(paterno);
        emp.setMaterno(materno);
        emp.setTelefono(numero);
        emp.setUsername(userName);
        emp.setPassword(contra); // La contraseña ya validada
        emp.setIdRole(rolSeleccionado.equals("Administrador") ? 1 : 2);
        if (dao.registrar(emp)) {
            JOptionPane.showMessageDialog(vista, "¡Empleado registrados con éxito!");
            vista.limpiarFormulario();
            cargarTabla();
            mainView.showView("empleados");
        }
    }
    
    private String generarUsername(String nom, String pat, String mat, String rol){
        String prefijo = rol.equals("Administrador") ? "AD" : "SA";
        String p1 = pat.substring(0,2).toUpperCase();
        String p2 = mat.substring(0,1).toUpperCase();
        String p3 = nom.substring(0,1).toUpperCase();
        int random = (int)(Math.random()*90+10);
        
        return prefijo + p1 + p2 + p3 + random;
    }
    
    public void cargarTabla() {
        DefaultTableModel modelo = vistaGestion.getTableModel(); // vista de gestión
        modelo.setRowCount(0); // Limpiar datos previos

        List<Empleado> lista = dao.listar();
        Object[] objeto = new Object[7]; // 7 columnas según tu diseño

        for (int i = 0; i < lista.size(); i++) {
            objeto[0] = lista.get(i).getId();
            objeto[1] = lista.get(i).getNombre();
            objeto[2] = lista.get(i).getPaterno();
            objeto[3] = lista.get(i).getMaterno();
            objeto[4] = lista.get(i).getTelefono();
            objeto[5] = lista.get(i).getRoleName();
            objeto[6] = "EDITAR / ELIMINAR";

            modelo.addRow(objeto);
        }
        initTablaAcciones();
    }
    
    private void confirmarEliminacion(int id, int row) {
        // 1. Notificación de confirmación
        int confirm = JOptionPane.showConfirmDialog(
            mainView, 
            "¿Estás seguro de que deseas eliminar al empleado con ID: " + id + "?\n" +
            "Esta acción lo eliminará de la lista de gestión.",
            "Confirmar Eliminación Lógica", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            // 2. Realizar la petición
            if (dao.eliminarLogico(id)) {
                // 3. Notificación de éxito
                JOptionPane.showMessageDialog(mainView, "Empleado desactivado con éxito.");

                // 4. Refrescar estado (la tabla volverá a consultar solo los activos)
                cargarTabla(); 
            } else {
                JOptionPane.showMessageDialog(mainView, "Error al intentar desactivar al empleado.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    // función del controlador para editar un empleado
    private void prepararEdicion(int id) {
        Empleado emp = dao.buscarPorId(id);

        if (emp != null) {
            // Ahora sí usamos getNombreField() que devuelve el JTextField
            vistaEdicion.getNombreField().setText(emp.getNombre());
            vistaEdicion.getPaternoField().setText(emp.getPaterno());
            vistaEdicion.getMaternoField().setText(emp.getMaterno());
            vistaEdicion.getNumeroField().setText(emp.getTelefono());

            // Para el ComboBox igual
            vistaEdicion.getRolComboBox().setSelectedItem(emp.getRoleName().equals("Admin") ? "Administrador" : "Vendedor");

            this.idEdicion = id; 
            mainView.showView("editarEmpleado"); 
        }
    }
    
    private void actualizarEmpleado() {
        // 1. Capturamos los datos de la vista de edición usando tus getters
        // Extraemos el texto directamente de los componentes Field
        String nombre = vistaEdicion.getNombreField().getText().trim();
        String paterno = vistaEdicion.getPaternoField().getText().trim();
        String materno = vistaEdicion.getMaternoField().getText().trim();
        String numero = vistaEdicion.getNumeroField().getText().trim();

        // Obtenemos el String seleccionado del ComboBox
        String rol = vistaEdicion.getRolComboBox().getSelectedItem().toString();

        // 2. Validaciones básicas (opcional pero recomendado)
        if(nombre.isEmpty() || paterno.isEmpty()) {
            JOptionPane.showMessageDialog(vistaEdicion, "Nombre y Apellido Paterno son obligatorios.");
            return;
        }

        // 3. Empaquetamos en el objeto Empleado usando el idEdicion que guardamos antes
        Empleado emp = new Empleado();
        emp.setId(this.idEdicion); 
        emp.setNombre(nombre);
        emp.setPaterno(paterno);
        emp.setMaterno(materno.isEmpty() ? "X" : materno);
        emp.setTelefono(numero);
        emp.setIdRole(rol.equals("Administrador") ? 1 : 2);

        // 4. Intentamos actualizar en la base de datos
        if (dao.actualizar(emp)) {
            JOptionPane.showMessageDialog(vistaEdicion, "¡Datos actualizados con éxito!");
            cargarTabla(); // Refrescamos la tabla para ver los cambios
            mainView.showView("empleados"); // Regresamos a la gestión
        } else {
            JOptionPane.showMessageDialog(vistaEdicion, "Error al actualizar la información.");
        }
    }
    
    private void filtrarTabla() {
        String texto = vistaGestion.getTxtBuscar().getText().trim();
        DefaultTableModel modelo = vistaGestion.getTableModel();
        modelo.setRowCount(0);

        // Si el campo está vacío, cargamos todos. Si no, buscamos.
        List<Empleado> lista = texto.isEmpty() ? dao.listar() : dao.buscar(texto);

        for (Empleado emp : lista) {
            modelo.addRow(new Object[]{
                emp.getId(),
                emp.getNombre(),
                emp.getPaterno(),
                emp.getMaterno(),
                emp.getTelefono(),
                emp.getRoleName(),
                "" // Columna de acciones
            });
        }
        // Re-aplicamos el renderizador para que no se pierdan los botones
        initTablaAcciones();
    }
}
