package com.tuerca.pos.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.tuerca.pos.pdf.dto.LineaReporteVenta;
import com.tuerca.pos.pdf.dto.ReporteEstadoVentas;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Genera documentos PDF de ventas de un emprendedor: el comprobante de un
 * pago puntual (FN.9, título "Comprobante de Pago a Emprendedor") y el
 * estado de cuenta de un periodo libre (FN.10, título "Estado de Ventas del
 * Emprendedor") comparten exactamente el mismo formato — solo cambia el
 * título del documento y de dónde salieron los datos del {@link ReporteEstadoVentas}.
 */
public class ReporteVentasPDF {

    private static final String NOMBRE_COLECTIVO = "Aura Tienda Colectiva";
    private static final SimpleDateFormat FMT_FECHA = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat FMT_FECHA_HORA = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private ReporteVentasPDF() {
    }

    public static void generar(File destino, String tituloDocumento, ReporteEstadoVentas datos)
            throws DocumentException, IOException {

        Document documento = new Document(PageSize.LETTER, 40, 40, 50, 50);
        PdfWriter.getInstance(documento, new FileOutputStream(destino));
        documento.open();

        try {
            Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font fuenteSubtitulo = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font fuenteNormal = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font fuenteNegrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font fuentePie = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8);

            Paragraph titulo = new Paragraph(NOMBRE_COLECTIVO, fuenteTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            documento.add(titulo);

            Paragraph subtitulo = new Paragraph(tituloDocumento, fuenteSubtitulo);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(20);
            documento.add(subtitulo);

            documento.add(new Paragraph("Emprendedor: " + datos.getMarcaEmprendedor(), fuenteNegrita));
            documento.add(new Paragraph("Periodo: " + FMT_FECHA.format(datos.getPeriodoInicio())
                    + " al " + FMT_FECHA.format(datos.getPeriodoFin()), FontFactory.getFont(FontFactory.HELVETICA, 10)));
            documento.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(9);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{0.7f, 1.0f, 0.8f, 2.1f, 0.6f, 0.8f, 0.8f, 0.8f, 0.9f});
            for (String encabezado : new String[]{"Ticket", "Fecha", "Código", "Descripción", "Cant.", "Precio U.", "Descuento", "Subtotal", "Estado"}) {
                agregarCeldaEncabezado(tabla, encabezado);
            }

            double montoPagado = 0;
            double montoPendiente = 0;
            for (LineaReporteVenta l : datos.getLineas()) {
                agregarCeldaCentrada(tabla, String.valueOf(l.getIdSale()), fuenteNormal);
                agregarCeldaCentrada(tabla, FMT_FECHA.format(l.getFecha()), fuenteNormal);
                agregarCeldaCentrada(tabla, l.getCodigo(), fuenteNormal);
                tabla.addCell(new Phrase(l.getDescripcion(), fuenteNormal));
                agregarCeldaCentrada(tabla, String.valueOf(l.getCantidad()), fuenteNormal);
                agregarCeldaCentrada(tabla, "$" + String.format("%.2f", l.getPrecioUnitario()), fuenteNormal);
                agregarCeldaCentrada(tabla, l.getDescuento() > 0 ? "$" + String.format("%.2f", l.getDescuento()) : "—", fuenteNormal);
                agregarCeldaCentrada(tabla, "$" + String.format("%.2f", l.getSubtotal()), fuenteNormal);
                agregarCeldaCentrada(tabla, l.isPagado() ? "Pagado" : "Pendiente", fuenteNormal);

                if (l.isPagado()) {
                    montoPagado += l.getSubtotal();
                } else {
                    montoPendiente += l.getSubtotal();
                }
            }
            documento.add(tabla);
            documento.add(new Paragraph(" "));

            documento.add(new Paragraph("Ya pagado: $" + String.format("%.2f", montoPagado)
                    + "   |   Pendiente de pagar: $" + String.format("%.2f", montoPendiente), fuenteNormal));
            documento.add(new Paragraph(" "));

            documento.add(new Paragraph("Ventas Brutas: $" + String.format("%.2f", datos.getVentasBrutas()), fuenteNormal));
            documento.add(new Paragraph("Descuentos: $" + String.format("%.2f", datos.getDescuentos()), fuenteNormal));

            String estadoRenta = datos.isRentaPagadaEsteMes()
                    ? "✓ Ya cobrada este mes el " + FMT_FECHA.format(datos.getFechaUltimoPagoRenta())
                    : "⚠ Pendiente de cobrar este mes";
            documento.add(new Paragraph("Renta mensual: $" + String.format("%.2f", datos.getRentaFija())
                    + "  (" + estadoRenta + ")", fuenteNormal));
            documento.add(new Paragraph(" "));
            documento.add(new Paragraph("Total Neto: $" + String.format("%.2f", datos.getTotalNeto()), fuenteNegrita));
            documento.add(new Paragraph(" "));

            documento.add(new Paragraph(
                    "Generado el " + FMT_FECHA_HORA.format(new Date()) + " por " + datos.getUsuarioEmisor(), fuentePie));
        } finally {
            documento.close();
        }
    }

    // Nombre sugerido tipo "Liquidacion_MarcaX_08_04_2026.pdf" (mismo criterio que CONTEXTO_PROYECTO.md).
    public static String nombreSugerido(String prefijo, String marcaEmprendedor) {
        String marcaLimpia = marcaEmprendedor.replaceAll("[^A-Za-z0-9]", "");
        return prefijo + "_" + marcaLimpia + "_" + new SimpleDateFormat("dd_MM_yyyy").format(new Date()) + ".pdf";
    }

    // Ticket/Código/Cantidad/Precio/Descuento/Subtotal/Estado se ven mejor centrados que
    // pegados a la izquierda (a diferencia de Descripción, que se deja alineada a la izquierda
    // por ser texto largo) — pedido explícito del usuario tras revisar el primer PDF.
    private static void agregarCeldaCentrada(PdfPTable tabla, String texto, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabla.addCell(celda);
    }

    private static void agregarCeldaEncabezado(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        celda.setBackgroundColor(new Color(230, 230, 230));
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabla.addCell(celda);
    }
}
