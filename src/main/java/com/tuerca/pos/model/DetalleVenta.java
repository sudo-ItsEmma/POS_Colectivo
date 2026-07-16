/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tuerca.pos.model;

import java.math.BigDecimal;

/**
 *
 * @author mannycalderon
 */
public class DetalleVenta {
    private int id;
    private int idVenta;          // Relación con la cabecera
    private int idProducto;       // Qué producto es
    private int cantidad;
    private BigDecimal precioUnitario; // Precio al momento de la venta
    private BigDecimal descuento;      // Monto en dinero descontado (ya calculado a partir del % capturado en pantalla)
    private BigDecimal subtotal;       // (Cantidad * Precio) - Descuento

    public DetalleVenta() {}

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdVenta() { return idVenta; }
    public void setIdVenta(int idVenta) { this.idVenta = idVenta; }

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
