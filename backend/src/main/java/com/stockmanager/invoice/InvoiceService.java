package com.stockmanager.invoice;

import com.stockmanager.common.dto.PageResponse;
import com.stockmanager.common.exception.BusinessException;
import com.stockmanager.common.util.NumberGenerator;
import com.stockmanager.invoice.dto.InvoiceDto;
import com.stockmanager.order.CustomerOrder;
import com.stockmanager.storage.FileStorageService;
import com.stockmanager.storage.StoredDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneOffset;

@Service
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final FileStorageService fileStorageService;
    private final InvoicePdfGenerator pdfGenerator;
    private final NumberGenerator numberGenerator;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          FileStorageService fileStorageService,
                          InvoicePdfGenerator pdfGenerator,
                          NumberGenerator numberGenerator) {
        this.invoiceRepository = invoiceRepository;
        this.fileStorageService = fileStorageService;
        this.pdfGenerator = pdfGenerator;
        this.numberGenerator = numberGenerator;
    }

    public InvoiceDto generateForOrder(CustomerOrder order) {
        return invoiceRepository.findByOrderId(order.getId())
                .map(InvoiceDto::from)
                .orElseGet(() -> {
                    String number = numberGenerator.nextInvoiceNumber();
                    byte[] pdf = pdfGenerator.generate(order, number);
                    String key = "invoices/%d/%s.pdf".formatted(
                            order.getDeliveredAt() != null
                                    ? order.getDeliveredAt().atZone(ZoneOffset.UTC).getYear()
                                    : Instant.now().atZone(ZoneOffset.UTC).getYear(),
                            number);
                    fileStorageService.store(key, pdf, "application/pdf");
                    Invoice invoice = new Invoice(number, order, order.getTotalHt(), order.getVatRate(),
                            order.getTotalVat(), order.getTotalTtc(), key);
                    return InvoiceDto.from(invoiceRepository.save(invoice));
                });
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceDto> list(String search, Pageable pageable) {
        String query = StringUtils.hasText(search) ? search.trim() : "";
        return PageResponse.of(invoiceRepository.search(query, pageable), InvoiceDto::from);
    }

    @Transactional(readOnly = true)
    public InvoiceDto get(Long id) {
        return InvoiceDto.from(find(id));
    }

    @Transactional(readOnly = true)
    public StoredDocument download(Long id) {
        Invoice invoice = find(id);
        return fileStorageService.load(invoice.getDocumentKey(), invoice.getNumber() + ".pdf");
    }

    @Transactional(readOnly = true)
    public Invoice find(Long id) {
        return invoiceRepository.findDetailedById(id)
                .orElseThrow(() -> BusinessException.notFound("Facture introuvable."));
    }
}
