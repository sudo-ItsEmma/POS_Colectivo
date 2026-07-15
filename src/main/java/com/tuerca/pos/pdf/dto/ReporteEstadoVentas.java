package com.tuerca.pos.pdf.dto;

import java.util.Date;
import java.util.List;

/**
 * Contenido completo de un reporte/comprobante de ventas de un emprendedor
 * (FN.9/FN.10): encabezado, líneas de producto, y el pie con los totales.
 * {@link com.tuerca.pos.pdf.ReporteVentasPDF} solo se encarga de darle
 * formato — no conoce la base de datos.
 */
public class ReporteEstadoVentas {

    private final String marcaEmprendedor;
    private final Date periodoInicio;
    private final Date periodoFin;
    private final List<LineaReporteVenta> lineas;
    private final double ventasBrutas;
    private final double descuentos;
    private final double rentaFija;
    private final double totalNeto;
    private final String usuarioEmisor;
    // Estado de la renta mensual respecto a HOY (no al periodo del reporte, que puede ser
    // pasado): si ya se cobró este mes calendario y, si sí, cuándo. Responde directamente a
    // "¿ya le pagamos la renta de este mes o sigue pendiente?".
    private final boolean rentaPagadaEsteMes;
    private final Date fechaUltimoPagoRenta;

    public ReporteEstadoVentas(String marcaEmprendedor, Date periodoInicio, Date periodoFin,
                                List<LineaReporteVenta> lineas, double ventasBrutas, double descuentos,
                                double rentaFija, double totalNeto, String usuarioEmisor,
                                boolean rentaPagadaEsteMes, Date fechaUltimoPagoRenta) {
        this.marcaEmprendedor = marcaEmprendedor;
        this.periodoInicio = periodoInicio;
        this.periodoFin = periodoFin;
        this.lineas = lineas;
        this.ventasBrutas = ventasBrutas;
        this.descuentos = descuentos;
        this.rentaFija = rentaFija;
        this.totalNeto = totalNeto;
        this.usuarioEmisor = usuarioEmisor;
        this.rentaPagadaEsteMes = rentaPagadaEsteMes;
        this.fechaUltimoPagoRenta = fechaUltimoPagoRenta;
    }

    public String getMarcaEmprendedor() {
        return marcaEmprendedor;
    }

    public Date getPeriodoInicio() {
        return periodoInicio;
    }

    public Date getPeriodoFin() {
        return periodoFin;
    }

    public List<LineaReporteVenta> getLineas() {
        return lineas;
    }

    public double getVentasBrutas() {
        return ventasBrutas;
    }

    public double getDescuentos() {
        return descuentos;
    }

    public double getRentaFija() {
        return rentaFija;
    }

    public double getTotalNeto() {
        return totalNeto;
    }

    public String getUsuarioEmisor() {
        return usuarioEmisor;
    }

    public boolean isRentaPagadaEsteMes() {
        return rentaPagadaEsteMes;
    }

    public Date getFechaUltimoPagoRenta() {
        return fechaUltimoPagoRenta;
    }
}
