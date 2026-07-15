package com.tuerca.pos.pdf.dto;

import java.util.Date;

/**
 * Una línea de producto vendido dentro de un reporte/comprobante en PDF
 * (FN.9/FN.10). Separa los datos ya leídos de la BD del formato final del
 * documento — el mismo DTO alimenta tanto el comprobante de un pago
 * puntual como el "Estado de Ventas" de un periodo libre.
 */
public class LineaReporteVenta {

    private final int idSale;
    private final Date fecha;
    private final String codigo;
    private final String descripcion;
    private final int cantidad;
    private final double precioUnitario;
    private final double descuento;
    private final double subtotal;
    private final boolean pagado;

    public LineaReporteVenta(int idSale, Date fecha, String codigo, String descripcion,
                              int cantidad, double precioUnitario, double descuento, double subtotal, boolean pagado) {
        this.idSale = idSale;
        this.fecha = fecha;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.descuento = descuento;
        this.subtotal = subtotal;
        this.pagado = pagado;
    }

    public int getIdSale() {
        return idSale;
    }

    public Date getFecha() {
        return fecha;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getCantidad() {
        return cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public double getDescuento() {
        return descuento;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public boolean isPagado() {
        return pagado;
    }
}
