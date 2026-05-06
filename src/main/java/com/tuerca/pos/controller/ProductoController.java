/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.controller;

import com.tuerca.pos.dao.EmprendedorDAO;
import com.tuerca.pos.dao.ProductoDAO;
import com.tuerca.pos.model.Emprendedor;
import com.tuerca.pos.view.GestionProductos;
import com.tuerca.pos.view.MainView;
import java.util.List;

/**
 *
 * @author mannycalderon
 */
public class ProductoController {
    private GestionProductos vistaGestion;
    private MainView mainView;
    private ProductoDAO productoDao;
    
    // EL CONSTRUCTOR: Es el corazón de la conexión
    public ProductoController(GestionProductos vistaGestion, MainView mainView) {
        this.vistaGestion = vistaGestion;
        this.mainView = mainView;
        this.productoDao = new ProductoDAO();

        // 1. Cargamos los datos iniciales al arrancar
        cargarComboEmprendedores();
        
        // 2. Aquí conectaremos los botones y eventos más adelante
        initListeners();
    }

    private void initListeners() {
        // Por ejemplo, cuando cambien la selección del combo:
        // vistaGestion.getCbFiltroEmprendedor().addActionListener(e -> filtrarTabla());
    }
    
    
    public void cargarComboEmprendedores() {
        // 1. Limpiamos el combo
        vistaGestion.getCbFiltroEmprendedor().removeAllItems();

        // 2. Agregamos la opción por defecto
        // Usamos un String o un objeto "dummy" para representar el ID 0
        vistaGestion.getCbFiltroEmprendedor().addItem("--- Todos ---");

        // 3. Traemos la lista del DAO
        EmprendedorDAO empDao = new EmprendedorDAO();
        List<Emprendedor> lista = empDao.listarNombresYId();

        // 4. Llenamos el combo con los OBJETOS completos
        for (Emprendedor emp : lista) {
            vistaGestion.getCbFiltroEmprendedor().addItem(emp);
        }
    }
    
    public int getSelectedEntrepreneurId() {
        Object seleccion = vistaGestion.getCbFiltroEmprendedor().getSelectedItem();

        if (seleccion instanceof Emprendedor) {
            return ((Emprendedor) seleccion).getId();
        }
        return 0; // Si seleccionó "--- Todos ---"
    }
}
