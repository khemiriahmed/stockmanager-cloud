package com.stockmanager.stock;

import com.stockmanager.common.dto.PageResponse;
import com.stockmanager.stock.dto.StockAdjustmentRequest;
import com.stockmanager.stock.dto.StockDto;
import com.stockmanager.stock.dto.StockMovementDto;
import com.stockmanager.stock.dto.StockOperationRequest;
import com.stockmanager.stock.dto.StockTransferRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stocks")
@Tag(name = "Stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    @Operation(summary = "Consulter les stocks par produit et entrepôt")
    public PageResponse<StockDto> list(@RequestParam(required = false) String search,
                                       @RequestParam(required = false) Long productId,
                                       @RequestParam(required = false) Long warehouseId,
                                       @PageableDefault(size = 20, sort = "product.name") Pageable pageable) {
        return stockService.list(search, productId, warehouseId, pageable);
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Produits en rupture ou sous le seuil d'alerte")
    public PageResponse<StockDto> lowStock(@PageableDefault(size = 20) Pageable pageable) {
        return stockService.lowStock(pageable);
    }

    @GetMapping("/movements")
    @Operation(summary = "Historique des mouvements de stock")
    public PageResponse<StockMovementDto> movements(@RequestParam(required = false) Long productId,
                                                    @RequestParam(required = false) Long warehouseId,
                                                    @RequestParam(required = false) MovementType type,
                                                    @PageableDefault(size = 20) Pageable pageable) {
        return stockService.movements(productId, warehouseId, type, pageable);
    }

    @PostMapping("/entry")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    @Operation(summary = "Entrée de stock")
    public StockDto entry(@Valid @RequestBody StockOperationRequest request) {
        return stockService.entry(request);
    }

    @PostMapping("/exit")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    @Operation(summary = "Sortie de stock")
    public StockDto exit(@Valid @RequestBody StockOperationRequest request) {
        return stockService.exit(request);
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Transfert atomique entre deux entrepôts")
    public StockDto transfer(@Valid @RequestBody StockTransferRequest request) {
        return stockService.transfer(request);
    }

    @PostMapping("/adjustment")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Ajustement de stock justifié")
    public StockDto adjustment(@Valid @RequestBody StockAdjustmentRequest request) {
        return stockService.adjustment(request);
    }
}
