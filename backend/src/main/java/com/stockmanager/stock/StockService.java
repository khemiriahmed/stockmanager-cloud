package com.stockmanager.stock;

import com.stockmanager.common.dto.PageResponse;
import com.stockmanager.common.exception.BusinessException;
import com.stockmanager.common.security.SecurityUtils;
import com.stockmanager.product.Product;
import com.stockmanager.product.ProductRepository;
import com.stockmanager.stock.dto.StockAdjustmentRequest;
import com.stockmanager.stock.dto.StockDto;
import com.stockmanager.stock.dto.StockMovementDto;
import com.stockmanager.stock.dto.StockOperationRequest;
import com.stockmanager.stock.dto.StockTransferRequest;
import com.stockmanager.warehouse.Warehouse;
import com.stockmanager.warehouse.WarehouseRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class StockService {

    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    public StockService(StockRepository stockRepository,
                        StockMovementRepository movementRepository,
                        ProductRepository productRepository,
                        WarehouseRepository warehouseRepository) {
        this.stockRepository = stockRepository;
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<StockDto> list(String search, Long productId, Long warehouseId, Pageable pageable) {
        String query = StringUtils.hasText(search) ? search.trim() : "";
        return PageResponse.of(stockRepository.search(query, productId, warehouseId, pageable), StockDto::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockDto> lowStock(Pageable pageable) {
        return PageResponse.of(stockRepository.findLowStock(pageable), StockDto::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementDto> movements(Long productId, Long warehouseId,
                                                    MovementType type, Pageable pageable) {
        return PageResponse.of(movementRepository.search(productId, warehouseId, type, pageable),
                StockMovementDto::from);
    }

    public StockDto entry(StockOperationRequest request) {
        Product product = findProduct(request.productId());
        Warehouse warehouse = findWarehouse(request.warehouseId());
        Stock stock = lockStock(product.getId(), warehouse.getId());
        stock.setQuantity(stock.getQuantity() + request.quantity());
        recordMovement(MovementType.IN, product, null, warehouse, request.quantity(),
                StringUtils.hasText(request.reason()) ? request.reason() : "Entrée de stock");
        return StockDto.from(stock);
    }

    public StockDto exit(StockOperationRequest request) {
        Product product = findProduct(request.productId());
        Warehouse warehouse = findWarehouse(request.warehouseId());
        Stock stock = lockStock(product.getId(), warehouse.getId());
        if (stock.availableQuantity() < request.quantity()) {
            throw insufficient(product, warehouse, stock.availableQuantity(), request.quantity());
        }
        stock.setQuantity(stock.getQuantity() - request.quantity());
        recordMovement(MovementType.OUT, product, warehouse, null, request.quantity(),
                StringUtils.hasText(request.reason()) ? request.reason() : "Sortie de stock");
        return StockDto.from(stock);
    }

    public StockDto adjustment(StockAdjustmentRequest request) {
        Product product = findProduct(request.productId());
        Warehouse warehouse = findWarehouse(request.warehouseId());
        Stock stock = lockStock(product.getId(), warehouse.getId());
        if (request.newQuantity() < stock.getReservedQuantity()) {
            throw new BusinessException(
                    "La nouvelle quantité ne peut pas être inférieure à la quantité réservée ("
                            + stock.getReservedQuantity() + ").", "RESERVED_STOCK_CONFLICT", HttpStatus.CONFLICT);
        }
        int delta = request.newQuantity() - stock.getQuantity();
        stock.setQuantity(request.newQuantity());
        if (delta != 0) {
            recordMovement(MovementType.ADJUSTMENT, product, warehouse, warehouse, Math.abs(delta),
                    "Ajustement (" + (delta > 0 ? "+" : "") + delta + ") : " + request.reason().trim());
        }
        return StockDto.from(stock);
    }

    public StockDto transfer(StockTransferRequest request) {
        if (request.sourceWarehouseId().equals(request.destinationWarehouseId())) {
            throw new BusinessException("Les entrepôts source et destination doivent être différents.",
                    "INVALID_TRANSFER");
        }
        Product product = findProduct(request.productId());
        Warehouse source = findWarehouse(request.sourceWarehouseId());
        Warehouse destination = findWarehouse(request.destinationWarehouseId());

        stockRepository.ensureStockRow(product.getId(), source.getId());
        stockRepository.ensureStockRow(product.getId(), destination.getId());
        List<Stock> locked = stockRepository.findForUpdateByWarehouses(
                product.getId(), List.of(source.getId(), destination.getId()));
        Stock sourceStock = locked.stream()
                .filter(stock -> stock.getWarehouse().getId().equals(source.getId()))
                .findFirst()
                .orElseThrow(() -> BusinessException.notFound("Stock source introuvable."));
        Stock destinationStock = locked.stream()
                .filter(stock -> stock.getWarehouse().getId().equals(destination.getId()))
                .findFirst()
                .orElseThrow(() -> BusinessException.notFound("Stock destination introuvable."));

        if (sourceStock.availableQuantity() < request.quantity()) {
            throw insufficient(product, source, sourceStock.availableQuantity(), request.quantity());
        }
        sourceStock.setQuantity(sourceStock.getQuantity() - request.quantity());
        destinationStock.setQuantity(destinationStock.getQuantity() + request.quantity());
        recordMovement(MovementType.TRANSFER, product, source, destination, request.quantity(),
                StringUtils.hasText(request.reason()) ? request.reason() : "Transfert entre entrepôts");
        return StockDto.from(sourceStock);
    }

    // ------------------------------------------------------------------
    // Opérations internes utilisées par le module commandes
    // ------------------------------------------------------------------

    public void reserveForOrder(List<StockLine> lines, String orderReference) {
        for (StockLine line : sorted(lines)) {
            Stock stock = lockStock(line.productId(), line.warehouseId());
            if (stock.availableQuantity() < line.quantity()) {
                throw insufficient(stock.getProduct(), stock.getWarehouse(),
                        stock.availableQuantity(), line.quantity());
            }
            stock.setReservedQuantity(stock.getReservedQuantity() + line.quantity());
        }
    }

    public void releaseForOrder(List<StockLine> lines) {
        for (StockLine line : sorted(lines)) {
            Stock stock = lockStock(line.productId(), line.warehouseId());
            stock.setReservedQuantity(Math.max(0, stock.getReservedQuantity() - line.quantity()));
        }
    }

    public void shipForOrder(List<StockLine> lines, String orderReference) {
        for (StockLine line : sorted(lines)) {
            Stock stock = lockStock(line.productId(), line.warehouseId());
            if (stock.getQuantity() < line.quantity() || stock.getReservedQuantity() < line.quantity()) {
                throw new BusinessException(
                        "Le stock réservé est insuffisant pour expédier la commande " + orderReference + ".",
                        "INSUFFICIENT_RESERVED_STOCK", HttpStatus.CONFLICT);
            }
            stock.setQuantity(stock.getQuantity() - line.quantity());
            stock.setReservedQuantity(stock.getReservedQuantity() - line.quantity());
            recordMovement(MovementType.OUT, stock.getProduct(), stock.getWarehouse(), null, line.quantity(),
                    "Expédition commande " + orderReference);
        }
    }

    public StockDto getForProductAndWarehouse(Long productId, Long warehouseId) {
        return stockRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .map(StockDto::from)
                .orElseThrow(() -> BusinessException.notFound("Aucun stock pour ce produit dans cet entrepôt."));
    }

    private List<StockLine> sorted(List<StockLine> lines) {
        return lines.stream()
                .sorted(Comparator.comparing(StockLine::productId).thenComparing(StockLine::warehouseId))
                .toList();
    }

    private Stock lockStock(Long productId, Long warehouseId) {
        stockRepository.ensureStockRow(productId, warehouseId);
        return stockRepository.findForUpdate(productId, warehouseId)
                .orElseThrow(() -> BusinessException.notFound("Stock introuvable."));
    }

    private void recordMovement(MovementType type, Product product, Warehouse source,
                                Warehouse destination, int quantity, String reason) {
        movementRepository.save(new StockMovement(type, product, source, destination, quantity,
                reason, SecurityUtils.currentUserEmail()));
    }

    private BusinessException insufficient(Product product, Warehouse warehouse, int available, int requested) {
        return new BusinessException(
                "Stock insuffisant pour le produit " + product.getReference() + " dans l'entrepôt "
                        + warehouse.getName() + " : disponible=" + available + ", demandé=" + requested + ".",
                "INSUFFICIENT_STOCK", HttpStatus.CONFLICT);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Produit introuvable."));
    }

    private Warehouse findWarehouse(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Entrepôt introuvable."));
    }
}
