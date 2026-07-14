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

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Genera el comprobante en PDF de un pago a emprendedor (FN.9), justo
 * después de registrarlo — no es el reporte "Estado de Ventas" completo de
 * FN.10, solo el recibo de este pago puntual.
 */
public class ReciboPagoPDF {

    private static final String NOMBRE_COLECTIVO = "Aura Tienda Colectiva";
    private static final SimpleDateFormat FMT_FECHA = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat FMT_FECHA_HORA = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private ReciboPagoPDF() {
    }

    // detallesProductos: filas {idSale, fecha (Date/Timestamp), código, descripción,
    // cantidad (Integer), precioUnitario (Double), descuento (Double), subtotal (Double)}
    // — una fila por producto vendido, igual que un ticket, agrupado por Ticket.
    public static void generar(File destino, String marcaEmprendedor, Date periodoInicio, Date periodoFin,
                                List<Object[]> detallesProductos, double bruto, double descuentos,
                                double renta, double neto, String usuarioEmisor) throws DocumentException, IOException {

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

            Paragraph subtitulo = new Paragraph("Comprobante de Pago a Emprendedor", fuenteSubtitulo);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(20);
            documento.add(subtitulo);

            documento.add(new Paragraph("Emprendedor: " + marcaEmprendedor, fuenteNegrita));
            documento.add(new Paragraph("Periodo liquidado: " + FMT_FECHA.format(periodoInicio) + " al " + FMT_FECHA.format(periodoFin), FontFactory.getFont(FontFactory.HELVETICA, 10)));
            documento.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(8);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{0.8f, 1.1f, 0.9f, 2.3f, 0.7f, 0.9f, 0.9f, 0.9f});
            for (String encabezado : new String[]{"Ticket", "Fecha", "Código", "Descripción", "Cant.", "Precio U.", "Descuento", "Subtotal"}) {
                agregarCeldaEncabezado(tabla, encabezado);
            }

            for (Object[] d : detallesProductos) {
                tabla.addCell(new Phrase(String.valueOf(d[0]), fuenteNormal));
                tabla.addCell(new Phrase(FMT_FECHA.format((Date) d[1]), fuenteNormal));
                tabla.addCell(new Phrase(String.valueOf(d[2]), fuenteNormal));
                tabla.addCell(new Phrase(String.valueOf(d[3]), fuenteNormal));
                tabla.addCell(new Phrase(String.valueOf(d[4]), fuenteNormal));
                tabla.addCell(new Phrase("$" + String.format("%.2f", (double) d[5]), fuenteNormal));
                double descuentoLinea = (double) d[6];
                tabla.addCell(new Phrase(descuentoLinea > 0 ? "$" + String.format("%.2f", descuentoLinea) : "—", fuenteNormal));
                tabla.addCell(new Phrase("$" + String.format("%.2f", (double) d[7]), fuenteNormal));
            }
            documento.add(tabla);
            documento.add(new Paragraph(" "));

            documento.add(new Paragraph("Ventas Brutas: $" + String.format("%.2f", bruto), fuenteNormal));
            documento.add(new Paragraph("Descuentos: $" + String.format("%.2f", descuentos), fuenteNormal));
            documento.add(new Paragraph("Renta mensual: $" + String.format("%.2f", renta), fuenteNormal));
            documento.add(new Paragraph(" "));
            documento.add(new Paragraph("Total Neto Pagado: $" + String.format("%.2f", neto), fuenteNegrita));
            documento.add(new Paragraph(" "));

            documento.add(new Paragraph(
                    "Generado el " + FMT_FECHA_HORA.format(new Date()) + " por " + usuarioEmisor, fuentePie));
        } finally {
            documento.close();
        }
    }

    // Nombre sugerido tipo "Liquidacion_MarcaX_08_04_2026.pdf" (mismo criterio que CONTEXTO_PROYECTO.md).
    public static String nombreSugerido(String marcaEmprendedor) {
        String marcaLimpia = marcaEmprendedor.replaceAll("[^A-Za-z0-9]", "");
        return "Liquidacion_" + marcaLimpia + "_" + new SimpleDateFormat("dd_MM_yyyy").format(new Date()) + ".pdf";
    }

    private static void agregarCeldaEncabezado(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        celda.setBackgroundColor(new Color(230, 230, 230));
        tabla.addCell(celda);
    }
}
