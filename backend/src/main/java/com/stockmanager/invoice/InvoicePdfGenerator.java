package com.stockmanager.invoice;

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
import com.stockmanager.order.CustomerOrder;
import com.stockmanager.order.OrderItem;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class InvoicePdfGenerator {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Europe/Paris"));
    private static final DateTimeFormatter DATE_ONLY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("Europe/Paris"));
    private static final Color PRIMARY = new Color(25, 118, 210);
    private static final Color LIGHT = new Color(227, 242, 253);

    public byte[] generate(CustomerOrder order, String invoiceNumber) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 40, 50);
            PdfWriter.getInstance(document, output);
            document.open();

            addHeader(document, invoiceNumber);
            addParties(document, order, invoiceNumber);
            addItems(document, order);
            addTotals(document, order);
            addFooter(document, order);

            document.close();
            return output.toByteArray();
        } catch (DocumentException | java.io.IOException ex) {
            throw new IllegalStateException("Impossible de générer le PDF de la facture " + invoiceNumber, ex);
        }
    }

    private void addHeader(Document document, String invoiceNumber) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, PRIMARY);
        Paragraph title = new Paragraph("StockManager Cloud", titleFont);
        document.add(title);

        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
        document.add(new Paragraph("Gestion commerciale, stocks et commandes", subtitleFont));
        document.add(new Paragraph("contact@stockmanager.local", subtitleFont));

        Paragraph invoiceTitle = new Paragraph("FACTURE " + invoiceNumber,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY));
        invoiceTitle.setSpacingBefore(24f);
        invoiceTitle.setAlignment(Element.ALIGN_RIGHT);
        document.add(invoiceTitle);

        document.add(new Paragraph(" ", subtitleFont));
    }

    private void addParties(Document document, CustomerOrder order, String invoiceNumber) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 1f});

        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

        StringBuilder customer = new StringBuilder(order.getCustomer().getName()).append('\n');
        if (order.getCustomer().getCompany() != null && !order.getCustomer().getCompany().isBlank()) {
            customer.append(order.getCustomer().getCompany()).append('\n');
        }
        if (order.getCustomer().getEmail() != null) {
            customer.append(order.getCustomer().getEmail()).append('\n');
        }
        if (order.getCustomer().getPhone() != null) {
            customer.append(order.getCustomer().getPhone()).append('\n');
        }
        if (order.getCustomer().getAddress() != null) {
            customer.append(order.getCustomer().getAddress());
        }

        PdfPCell customerCell = new PdfPCell();
        customerCell.setBorder(0);
        customerCell.addElement(new Paragraph("FACTURÉ À", labelFont));
        customerCell.addElement(new Paragraph(customer.toString(), valueFont));
        table.addCell(customerCell);

        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(0);
        infoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph info = new Paragraph();
        info.setAlignment(Element.ALIGN_RIGHT);
        info.add(new Phrase("N° facture : " + invoiceNumber + "\n", valueFont));
        info.add(new Phrase("Commande : " + order.getReference() + "\n", valueFont));
        info.add(new Phrase("Date : " + DATE_ONLY.format(order.getDeliveredAt() != null
                ? order.getDeliveredAt() : java.time.Instant.now()) + "\n", valueFont));
        info.add(new Phrase("TVA : " + format(order.getVatRate()) + " %", valueFont));
        infoCell.addElement(info);
        table.addCell(infoCell);

        PdfPCell spacer = new PdfPCell(new Phrase(" "));
        spacer.setBorder(0);
        spacer.setColspan(2);
        table.addCell(spacer);
        document.add(table);
    }

    private void addItems(Document document, CustomerOrder order) throws DocumentException {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3.4f, 1.4f, 2f, 0.8f, 1.3f, 1.4f});
        table.setSpacingBefore(8f);

        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        String[] headers = {"Désignation", "Référence", "Entrepôt", "Qté", "P.U. HT", "Total HT"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headFont));
            cell.setBackgroundColor(PRIMARY);
            cell.setPadding(6f);
            table.addCell(cell);
        }

        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
        boolean alternate = false;
        for (OrderItem item : order.getItems()) {
            Color background = alternate ? LIGHT : Color.WHITE;
            addCell(table, item.getProductName(), cellFont, Element.ALIGN_LEFT, background);
            addCell(table, item.getProductReference(), cellFont, Element.ALIGN_LEFT, background);
            addCell(table, item.getWarehouse().getName(), cellFont, Element.ALIGN_LEFT, background);
            addCell(table, String.valueOf(item.getQuantity()), cellFont, Element.ALIGN_RIGHT, background);
            addCell(table, format(item.getUnitPrice()), cellFont, Element.ALIGN_RIGHT, background);
            addCell(table, format(item.getLineTotal()), cellFont, Element.ALIGN_RIGHT, background);
            alternate = !alternate;
        }
        document.add(table);
    }

    private void addTotals(Document document, CustomerOrder order) throws DocumentException {
        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(45);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.setSpacingBefore(12f);

        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY);
        Font resultFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, PRIMARY);

        addTotalRow(totals, "Total HT", format(order.getTotalHt()), labelFont, Element.ALIGN_RIGHT);
        addTotalRow(totals, "TVA (" + format(order.getVatRate()) + " %)", format(order.getTotalVat()),
                labelFont, Element.ALIGN_RIGHT);
        addTotalRow(totals, "Total TTC", format(order.getTotalTtc()), resultFont, Element.ALIGN_RIGHT);
        document.add(totals);
    }

    private void addFooter(Document document, CustomerOrder order) throws DocumentException {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
        Paragraph footer = new Paragraph(
                "Merci de votre confiance. Facture générée le " + DATE_FORMAT.format(java.time.Instant.now())
                        + " — Commande " + order.getReference() + ".", font);
        footer.setSpacingBefore(30f);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private void addCell(PdfPTable table, String text, Font font, int alignment, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5f);
        cell.setHorizontalAlignment(alignment);
        cell.setBackgroundColor(background);
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font font, int alignment) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(0);
        labelCell.setPadding(4f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value + " €", font));
        valueCell.setBorder(0);
        valueCell.setPadding(4f);
        valueCell.setHorizontalAlignment(alignment);
        table.addCell(valueCell);
    }

    private String format(BigDecimal value) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.FRANCE);
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        return format.format(value);
    }
}
