package com.stockmanager.invoice;

import com.stockmanager.common.dto.PageResponse;
import com.stockmanager.invoice.dto.InvoiceDto;
import com.stockmanager.storage.StoredDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/invoices")
@Tag(name = "Factures")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    @Operation(summary = "Lister les factures")
    public PageResponse<InvoiceDto> list(@RequestParam(required = false) String search,
                                         @PageableDefault(size = 20) Pageable pageable) {
        return invoiceService.list(search, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une facture")
    public InvoiceDto get(@PathVariable Long id) {
        return invoiceService.get(id);
    }

    @GetMapping("/{id}/document")
    @Operation(summary = "Télécharger le PDF d'une facture")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        StoredDocument document = invoiceService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(document.filename(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(document.content());
    }
}
