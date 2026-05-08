/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.controller;

import com.tuerca.pos.dao.EmprendedorDAO;
import com.tuerca.pos.dao.ProductoDAO;
import com.tuerca.pos.model.Emprendedor;
import com.tuerca.pos.model.Producto;
import com.tuerca.pos.view.EditarProducto;
import com.tuerca.pos.view.GestionProductos;
import com.tuerca.pos.view.MainView;
import com.tuerca.pos.view.NuevoProducto;
import com.tuerca.pos.view.components.AccionTableEvent;
import com.tuerca.pos.view.components.AccionesEditar;
import com.tuerca.pos.view.components.AccionesRender;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import com.tuerca.pos.view.CargaMasivaProductos;
import java.io.File;
import java.io.FileInputStream;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author mannycalderon
 */
public class ProductoController {
    private int idEdicion = -1; // Variable para saber qué ID estamos editando
    private EditarProducto vistaEdicion;
    private GestionProductos vistaGestion;
    private NuevoProducto vistaRegistro;
    private MainView mainView;
    private ProductoDAO productoDao;
    private CargaMasivaProductos vistaCarga; // Nueva variable
    private File archivoExcel; // Para el manejo de archivos
    
    
    // EL CONSTRUCTOR: Es el corazón de la conexión
    public ProductoController(GestionProductos vistaGestion, NuevoProducto vistaRegistro, EditarProducto vistaEdicion, CargaMasivaProductos vistaCarga, MainView mainView) {
        this.vistaGestion = vistaGestion;
        this.vistaRegistro = vistaRegistro;
        this.vistaEdicion = vistaEdicion;
        this.vistaCarga = vistaCarga;
        this.mainView = mainView;
        this.productoDao = new ProductoDAO();
        
        
        // Listener de visibilidad para carga masiva
        this.vistaCarga.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                limpiarVistaCargaMasiva();
                cargarCombos(); // Actualiza los emprendedores antes de cargar el Excel
            }
        });
        
        this.vistaGestion.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                // Este código se ejecuta SOLITO cada vez que el panel se muestra
                refrescarPantallaCompleta();
            }
        });

        // 1. Cargamos los datos iniciales al arrancar
        cargarCombos();
        vistaGestion.limpiarFiltro();
        configurarTabla();
        cargarTablaProductos();
        
        // 2. Aquí conectaremos los botones y eventos más adelante
        initTablaAcciones();
        initListeners();
    }
    
    // Método auxiliar para agrupar el refresco
    private void refrescarPantallaCompleta() {
        vistaGestion.getRbVerInactivos().setSelected(false);
        cargarCombos();   // Por si se agregaron/eliminaron emprendedores
        filtrarTabla();   // Recarga la tabla desde la DB
    }
    
    private void initTablaAcciones() {
        AccionTableEvent evento = new AccionTableEvent() {
            @Override
            public void onEditar(int row) {
                // Obtenemos el ID de la columna 0
                int id = (int) vistaGestion.getTablaProductos().getValueAt(row, 0);
                prepararEdicion(id);
            }

            @Override
            public void onEliminar(int row) {
                int id = (int) vistaGestion.getTablaProductos().getValueAt(row, 0);
                String codigo = vistaGestion.getTablaProductos().getValueAt(row, 1).toString();

                if (vistaGestion.getRbVerInactivos().isSelected()) {
                    // Si estamos viendo inactivos, la acción es ACTIVAR
                    confirmarActivacion(id, codigo);
                } else {
                    // Si estamos viendo activos, la acción es DESACTIVAR
                    confirmarEliminacion(id, row);
                }
            }
        };

        // La columna de acciones ahora es la 6
        int columnaAcciones = 6; 
        vistaGestion.getTablaProductos().getColumnModel().getColumn(columnaAcciones).setCellRenderer(new AccionesRender());
        vistaGestion.getTablaProductos().getColumnModel().getColumn(columnaAcciones).setCellEditor(new AccionesEditar(evento));

        // OCULTAR LA COLUMNA 0 (ID)
        vistaGestion.getTablaProductos().getColumnModel().getColumn(0).setMinWidth(0);
        vistaGestion.getTablaProductos().getColumnModel().getColumn(0).setMaxWidth(0);
        vistaGestion.getTablaProductos().getColumnModel().getColumn(0).setPreferredWidth(0);

        // Ajustar el cursor (ahora en la col 6)
        vistaGestion.getTablaProductos().addMouseMotionListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int col = vistaGestion.getTablaProductos().columnAtPoint(e.getPoint());
                if (col == 6) {
                    vistaGestion.getTablaProductos().setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                } else {
                    vistaGestion.getTablaProductos().setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }
            }
        });
        // Tip: Aumenta un poco el alto de las filas para que los botones luzcan mejor
        vistaGestion.getTablaProductos().setRowHeight(40);
    }

    private void initListeners() {
        
        // En initListeners
        vistaGestion.getRbVerInactivos().addActionListener(e -> filtrarTabla());
        
        // Cuando el usuario escribe en el buscador
        vistaGestion.getTxtBuscar().addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                filtrarTabla(); // Llama a la lógica unificada
            }
        });

        // Cuando el usuario selecciona un emprendedor en el combo
        vistaGestion.getCbFiltroEmprendedor().addActionListener(e -> filtrarTabla());
        
        // Acción para el botón registrar
        vistaRegistro.getBtnRegistrar().addActionListener(e -> registrarProducto());
        
        // Accion para el boton de actualizar
        vistaEdicion.getBtnActualizar().addActionListener(e -> actualizarProducto());

        // Acción para el botón cancelar o volver 
        vistaRegistro.getBtnBack().addActionListener(e -> {
            vistaGestion.limpiarFiltro();
            vistaRegistro.limpiarFormulario();
            mainView.showView("gestionProductos");
        });
        
        vistaRegistro.getBtnCancelar().addActionListener(e -> {
            vistaGestion.limpiarFiltro();
            vistaRegistro.limpiarFormulario();
            mainView.showView("gestionProductos");
        });
        
        // BOTONES DE CARGA MASIVA
        vistaCarga.getBtnSeleccionarArchivo().addActionListener(e -> seleccionarArchivo());
        vistaCarga.getBtnVisualizar().addActionListener(e -> visualizarExcel());
        vistaCarga.getBtnRegistrar().addActionListener(e -> ejecutarCargaMasiva());
        vistaCarga.getBtnCancelar().addActionListener(e -> mainView.showView("gestionProductos"));
        vistaCarga.getBtnBack().addActionListener(e -> mainView.showView("gestionProductos"));
        
        // AGREGAMOS EL LISTENER DEL MOUSE
        vistaGestion.getTablaProductos().addMouseMotionListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int col = vistaGestion.getTablaProductos().columnAtPoint(e.getPoint());
                if (col == 5) {
                    vistaGestion.getTablaProductos().setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                } else {
                    vistaGestion.getTablaProductos().setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                }
            }
        });
    }
    
    private void configurarTabla() {
        javax.swing.JTable tabla = vistaGestion.getTablaProductos();

        // 0. ID (Sigue oculto)
        tabla.getColumnModel().getColumn(0).setMinWidth(0);
        tabla.getColumnModel().getColumn(0).setMaxWidth(0);

        // 1. Código (Más estrecho, no necesita tanto aire)
        tabla.getColumnModel().getColumn(1).setPreferredWidth(100);
        tabla.getColumnModel().getColumn(1).setMinWidth(80);

        // 2. Descripción (ESTA DEBE SER LA MÁS ANCHA)
        // Al no ponerle MaxWidth, tomará todo el espacio sobrante
        tabla.getColumnModel().getColumn(2).setPreferredWidth(450); 

        // 3. Emprendimiento
        tabla.getColumnModel().getColumn(3).setPreferredWidth(150);

        // 4. Precio
        tabla.getColumnModel().getColumn(4).setPreferredWidth(100);

        // 5. Stock (Pequeño y compacto)
        tabla.getColumnModel().getColumn(5).setPreferredWidth(80);

        // 6. Acciones (Suficiente espacio para que no se corten los botones)
        tabla.getColumnModel().getColumn(6).setPreferredWidth(120);
        tabla.getColumnModel().getColumn(6).setMinWidth(120);
    }
    
    public void cargarTablaProductos() {
        DefaultTableModel modelo = (DefaultTableModel) vistaGestion.getTablaProductos().getModel();
        modelo.setRowCount(0);

        List<Producto> lista = productoDao.listarTodos();

        for (Producto p : lista) {
            modelo.addRow(new Object[]{
                p.getIdProduct(),        // 0. ID
                p.getFullProductCode(),   // 1. Código
                p.getProductDescription(),// 2. Descripción
                p.getBrandName(),         // 3. Emprendimiento
                "$" + String.format("%.2f", p.getCurrentPrice()), // 4. Precio
                p.getCurrentStock(),      // 5. Stock
                ""                        // 6. Acciones
            });
        }
        initTablaAcciones();
    }
    
    public void refrescarCatalogos() {
        // Volvemos a llamar a la función que ya tenías
        cargarCombos();
        cargarTablaProductos();
    }
    
    public void cargarCombos() {
        EmprendedorDAO empDao = new EmprendedorDAO();
        List<Emprendedor> lista = empDao.listarNombresYId();

        // Limpiar y llenar combo de la tabla (filtro)
        vistaGestion.getCbFiltroEmprendedor().removeAllItems();
        vistaGestion.getCbFiltroEmprendedor().addItem("--- Todos ---");
        
        // Limpiar y llenar combo del registro (obligatorio)
        vistaRegistro.getCbEmprendedor().removeAllItems();
        vistaRegistro.getCbEmprendedor().addItem("Selecciona un emprendedor...");
        
        vistaEdicion.getCbEmprendedor().removeAllItems();
        vistaEdicion.getCbEmprendedor().addItem("Selecciona un emprendedor...");
        
        vistaCarga.getCbEmprendedor().removeAllItems();
        vistaCarga.getCbEmprendedor().addItem("Selecciona un emprendedor...");

        for (Emprendedor emp : lista) {
            vistaGestion.getCbFiltroEmprendedor().addItem(emp);
            vistaRegistro.getCbEmprendedor().addItem(emp);
            vistaEdicion.getCbEmprendedor().addItem(emp);
            vistaCarga.getCbEmprendedor().addItem(emp);
        }
    }
    
    public int getSelectedEntrepreneurId() {
        Object seleccion = vistaGestion.getCbFiltroEmprendedor().getSelectedItem();

        if (seleccion instanceof Emprendedor) {
            return ((Emprendedor) seleccion).getId();
        }
        return 0; // Si seleccionó "--- Todos ---"
    }
    
    private void registrarProducto() {
        // 1. Validar selección del Emprendedor
        Object seleccion = vistaRegistro.getCbEmprendedor().getSelectedItem();
        if (!(seleccion instanceof Emprendedor)) {
            JOptionPane.showMessageDialog(vistaRegistro, "Por favor, selecciona un emprendedor válido.");
            return;
        }
        Emprendedor emp = (Emprendedor) seleccion;

        // 2. Capturar datos de texto
        String codigo = vistaRegistro.getCodigoField().toUpperCase();
        String desc = vistaRegistro.getDescripcionField().toUpperCase();
        String depto = vistaRegistro.getDepartamentoField().toUpperCase();
        String precioStr = vistaRegistro.getPrecioField();
        String stockStr = vistaRegistro.getStockField();

        // 3. Validaciones de campos vacíos
        if (codigo.isEmpty() || desc.isEmpty() || precioStr.isEmpty() || stockStr.isEmpty()) {
            JOptionPane.showMessageDialog(vistaRegistro, "Completa todos los campos obligatorios.");
            return;
        }

        try {
            // 4. Empaquetar datos y conversión numérica
            Producto p = new Producto();
            p.setIdEntrepreneur(emp.getId());
            p.setFullProductCode(codigo);
            p.setProductDescription(desc);
            p.setDepartment(depto);
            p.setCurrentPrice(Double.parseDouble(precioStr));
            p.setCurrentStock(Integer.parseInt(stockStr));
            p.setMinStockAlert(1);

            // 5. Ejecutar registro
            if (productoDao.registrar(p)) {
                JOptionPane.showMessageDialog(vistaRegistro, "¡Producto " + codigo + " registrado!");
                // El controlador le da la orden a la vista
                vistaGestion.limpiarFiltro();
                vistaRegistro.limpiarFormulario();
                cargarTablaProductos(); // Refrescamos la tabla de gestión
                mainView.showView("products"); // Regresamos a la tabla
            } else {
                JOptionPane.showMessageDialog(vistaRegistro, "Error al guardar. Verifica que el código no esté duplicado.");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(vistaRegistro, "El precio y el stock deben ser valores numéricos.");
        }
    }
    
    
    private void confirmarEliminacion(int id, int row) {
    // Extraemos el código de la columna 1 para una mejor UX
        String codigo = vistaGestion.getTablaProductos().getValueAt(row, 1).toString();

        int confirm = JOptionPane.showConfirmDialog(
            mainView, 
            "¿Estás seguro de que deseas desactivar el producto: " + codigo + "?\n" +
            "Ya no aparecerá en el inventario activo.",
            "Confirmar Baja de Producto", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (productoDao.eliminarLogico(id)) {
                JOptionPane.showMessageDialog(mainView, "El producto " + codigo + " ha sido desactivado.");
                cargarTablaProductos();
            } else {
                JOptionPane.showMessageDialog(mainView, "No se pudo desactivar el producto.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    // función del controlador para editar un empleado
    private void prepararEdicion(int id) {
        Producto pro = productoDao.buscarPorId(id);
        cargarCombos();

        if (pro != null) {
            this.idEdicion = id;

            // Seteamos los textos (Estandarizados en mayúsculas por seguridad)
            vistaEdicion.getCodigoField().setText(pro.getFullProductCode());
            vistaEdicion.getDescripcionField().setText(pro.getProductDescription());
            vistaEdicion.getDepartamentoField().setText(pro.getDepartment());
            vistaEdicion.getPrecioField().setText(String.valueOf(pro.getCurrentPrice()));
            vistaEdicion.getStockField().setText(String.valueOf(pro.getCurrentStock()));

            // Seleccionar el emprendedor correcto en el Combo
            for (int i = 0; i < vistaEdicion.getCbEmprendedor().getItemCount(); i++) {
                Object item = vistaEdicion.getCbEmprendedor().getItemAt(i);
                if (item instanceof Emprendedor emp) {
                    if (emp.getId() == pro.getIdEntrepreneur()) {
                        vistaEdicion.getCbEmprendedor().setSelectedIndex(i);
                        break;
                    }
                }
            }

            mainView.showView("editarProducto"); 
        }
    }
    
    private void actualizarProducto() {
        // 1. Validar combo
        Object seleccion = vistaEdicion.getCbEmprendedor().getSelectedItem();
        if (!(seleccion instanceof Emprendedor emp)) {
            JOptionPane.showMessageDialog(vistaEdicion, "Selecciona un emprendedor.");
            return;
        }

        try {
            // 2. Empaquetar y Estandarizar
            Producto p = new Producto();
            p.setIdProduct(this.idEdicion);
            p.setIdEntrepreneur(emp.getId());
            p.setFullProductCode(vistaEdicion.getCodigoField().getText().toUpperCase());
            p.setProductDescription(vistaEdicion.getDescripcionField().getText().toUpperCase());
            p.setDepartment(vistaEdicion.getDepartamentoField().getText().toUpperCase());
            p.setCurrentPrice(Double.parseDouble(vistaEdicion.getPrecioField().getText()));
            p.setCurrentStock(Integer.parseInt(vistaEdicion.getStockField().getText()));

            // 3. Guardar en DB
            if (productoDao.actualizar(p)) {
                JOptionPane.showMessageDialog(vistaEdicion, "Producto actualizado correctamente.");
                cargarTablaProductos();
                mainView.showView("products");
            } else {
                JOptionPane.showMessageDialog(vistaEdicion, "Error al actualizar. Posible código duplicado.");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(vistaEdicion, "Verifica que el precio y stock sean números válidos.");
        }
    }
    
    public void filtrarTabla() {
        // Capturamos el estado de ambos filtros
        String texto = vistaGestion.getTxtBuscar().getText().trim().toUpperCase();

        int idEmp = 0;
        Object seleccionado = vistaGestion.getCbFiltroEmprendedor().getSelectedItem();
        if (seleccionado instanceof Emprendedor emp) {
            idEmp = emp.getId();
        }
        
        // Leemos si el Radio Button está seleccionado
        boolean mostrarInactivos = vistaGestion.getRbVerInactivos().isSelected();

        // Pedimos al DAO que nos de la lista filtrada por ambos criterios
        List<Producto> lista = productoDao.buscarAvanzado(texto, idEmp, mostrarInactivos);

        // Actualizamos la tabla
        DefaultTableModel modelo = (DefaultTableModel) vistaGestion.getTablaProductos().getModel();
        modelo.setRowCount(0);

        for (Producto p : lista) {
            modelo.addRow(new Object[]{
                p.getIdProduct(),        // Índice 0 (Oculto)
                p.getFullProductCode(),   // Índice 1 (Código)
                p.getProductDescription(),// Índice 2 (Descripción)
                p.getBrandName(),         // Índice 3 (Emprendimiento)
                "$" + String.format("%.2f", p.getCurrentPrice()), // Índice 4 (Precio)
                p.getCurrentStock(),      // Índice 5 (Stock)
                ""                        // Índice 6 (Acciones)
            });
        }
    }
    
    private void confirmarActivacion(int id, String codigo) {
        int confirm = JOptionPane.showConfirmDialog(mainView, 
            "¿Deseas reactivar el producto " + codigo + "?", 
            "Reactivar Producto", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            int resultado = productoDao.activarProductoConValidacion(id);

            if (resultado == 1) {
                JOptionPane.showMessageDialog(mainView, "Producto reactivado con éxito.");
                filtrarTabla();
            } else if (resultado == -1) {
                JOptionPane.showMessageDialog(mainView, 
                    "No se puede activar el producto porque el Emprendedor ligado está DESACTIVADO.\n" +
                    "Primero debes activar al emprendedor en el módulo de Emprendedores.", 
                    "Acción Denegada", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(mainView, "Error al intentar activar el producto.");
            }
        }
    }
    
    private void seleccionarArchivo() {
        JFileChooser buscador = new JFileChooser();
        buscador.setFileFilter(new FileNameExtensionFilter("Excel (.xlsx, .xls)", "xlsx", "xls"));

        if (buscador.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            archivoExcel = buscador.getSelectedFile();
            vistaCarga.getLblNombreArchivo().setText("Archivo: " + archivoExcel.getName());
        }
    }
    
    private void visualizarExcel() {
        if (archivoExcel == null) {
            JOptionPane.showMessageDialog(vistaCarga, "Selecciona un archivo primero.");
            return;
        }

        DefaultTableModel modelo = (DefaultTableModel) vistaCarga.getVistaTablaProductos().getModel();
        modelo.setRowCount(0);

        try (FileInputStream fis = new FileInputStream(archivoExcel);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet hoja = workbook.getSheetAt(0);

            // Empezamos en i = 2 para saltar encabezados (Ajusta si es necesario)
            // Pero lo más seguro es validar el contenido:
            for (int i = 1; i <= hoja.getLastRowNum(); i++) {
                Row fila = hoja.getRow(i);
                if (fila == null) continue;

                // 1. LEER LOS DATOS
                String codigo = getCellValue(fila.getCell(1)).trim();
                String desc = getCellValue(fila.getCell(2)).trim();

                // 2. FILTRO DE SEGURIDAD: 
                // Si el código es igual a "CODIGO" (título) o está vacío, saltamos la fila
                if (codigo.isEmpty() || codigo.equalsIgnoreCase("CODIGO")) {
                    continue; 
                }

                double precio = getNumericValue(fila.getCell(3));
                int stock = (int) getNumericValue(fila.getCell(4));
                String depto = getCellValue(fila.getCell(5)).trim();

                // Solo agregamos si realmente hay información útil
                modelo.addRow(new Object[]{codigo, desc, precio, stock, depto});
            }

            if (modelo.getRowCount() == 0) {
                JOptionPane.showMessageDialog(vistaCarga, "No se encontraron productos válidos en el archivo.");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(vistaCarga, "Error al leer Excel: " + e.getMessage());
        }
    }

    private void ejecutarCargaMasiva() {
        Object seleccion = vistaCarga.getCbEmprendedor().getSelectedItem();
        if (!(seleccion instanceof Emprendedor emp)) {
            JOptionPane.showMessageDialog(vistaCarga, "Selecciona un emprendedor.");
            return;
        }

        DefaultTableModel modelo = (DefaultTableModel) vistaCarga.getVistaTablaProductos().getModel();
        int exitos = 0, errores = 0;

        for (int i = 0; i < modelo.getRowCount(); i++) {
            try {
                // Validar que la celda de código no sea null o vacía en la tabla
                Object valCodigo = modelo.getValueAt(i, 0);
                if (valCodigo == null || valCodigo.toString().trim().isEmpty()) {
                    continue; // Si el usuario dejó la fila vacía en la tabla, la ignoramos
                }

                Producto p = new Producto();
                p.setIdEntrepreneur(emp.getId());
                p.setFullProductCode(valCodigo.toString().toUpperCase().trim());
                p.setProductDescription(modelo.getValueAt(i, 1).toString().toUpperCase().trim());
                p.setCurrentPrice(Double.parseDouble(modelo.getValueAt(i, 2).toString()));
                p.setCurrentStock(Integer.parseInt(modelo.getValueAt(i, 3).toString()));
                p.setDepartment(modelo.getValueAt(i, 4).toString().toUpperCase().trim());
                p.setMinStockAlert(1);

                if (productoDao.registrar(p)) {
                    exitos++;
                } else {
                    errores++;
                }
            } catch (Exception e) {
                errores++;
                System.err.println("Error en fila " + i + ": " + e.getMessage());
            }
        }

        JOptionPane.showMessageDialog(vistaCarga, "Carga terminada.\nÉxitos: " + exitos + "\nErrores: " + errores);

        if (exitos > 0) {
            limpiarVistaCargaMasiva(); // <--- Limpiar después de cargar con éxito
            filtrarTabla();
            mainView.showView("products");
        }
    }

    // Métodos de apoyo para POI
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            default -> "";
        };
    }

    private double getNumericValue(Cell cell) {
        if (cell == null) return 0;
        try { return cell.getNumericCellValue(); } catch (Exception e) { return 0; }
    }
    
    private void limpiarVistaCargaMasiva() {
        archivoExcel = null;
        vistaCarga.getLblNombreArchivo().setText("Ningún archivo seleccionado");
        DefaultTableModel modelo = (DefaultTableModel) vistaCarga.getVistaTablaProductos().getModel();
        modelo.setRowCount(0);
        if (vistaCarga.getCbEmprendedor().getItemCount() > 0) {
            vistaCarga.getCbEmprendedor().setSelectedIndex(0);
        }
    }
}
