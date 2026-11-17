package com.stockmanager.stock;

import com.stockmanager.common.exception.BusinessException;
import com.stockmanager.product.Product;
import com.stockmanager.product.ProductRepository;
import com.stockmanager.stock.dto.StockDto;
import com.stockmanager.stock.dto.StockOperationRequest;
import com.stockmanager.stock.dto.StockTransferRequest;
import com.stockmanager.warehouse.Warehouse;
import com.stockmanager.warehouse.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;
    @Mock
    private StockMovementRepository movementRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private StockService stockService;

    private Product product;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        product = new Product("REF-001", "Produit test", null,
                new BigDecimal("10.00"), new BigDecimal("20.00"), 5, null);
        ReflectionTestUtils.setField(product, "id", 1L);
        warehouse = new Warehouse("Entrepôt test", "Adresse", "Responsable");
        ReflectionTestUtils.setField(warehouse, "id", 10L);
        org.mockito.Mockito.lenient().when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        org.mockito.Mockito.lenient().when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
    }

    @Test
    void entryIncreasesPhysicalQuantityAndRecordsMovement() {
        Stock stock = new Stock(product, warehouse, 10, 2);
        when(stockRepository.findForUpdate(1L, 10L)).thenReturn(Optional.of(stock));

        StockDto result = stockService.entry(new StockOperationRequest(1L, 10L, 5, "Réception"));

        assertThat(result.quantity()).isEqualTo(15);
        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(MovementType.IN);
        assertThat(captor.getValue().getQuantity()).isEqualTo(5);
    }

    @Test
    void exitRejectsWhenAvailableQuantityIsInsufficient() {
        Stock stock = new Stock(product, warehouse, 5, 3);
        when(stockRepository.findForUpdate(1L, 10L)).thenReturn(Optional.of(stock));

        assertThatThrownBy(() -> stockService.exit(new StockOperationRequest(1L, 10L, 3, "Sortie")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Stock insuffisant")
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo("INSUFFICIENT_STOCK");

        assertThat(stock.getQuantity()).isEqualTo(5);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void transferMovesQuantityBetweenBothWarehousesAtomically() {
        Warehouse destination = new Warehouse("Annexe", "Adresse 2", "Responsable 2");
        ReflectionTestUtils.setField(destination, "id", 11L);
        when(warehouseRepository.findById(11L)).thenReturn(Optional.of(destination));

        Stock sourceStock = new Stock(product, warehouse, 10, 2);
        Stock destinationStock = new Stock(product, destination, 3, 0);
        when(stockRepository.findForUpdateByWarehouses(1L, List.of(10L, 11L)))
                .thenReturn(List.of(sourceStock, destinationStock));

        stockService.transfer(new StockTransferRequest(1L, 10L, 11L, 4, "Réapprovisionnement"));

        assertThat(sourceStock.getQuantity()).isEqualTo(6);
        assertThat(destinationStock.getQuantity()).isEqualTo(7);
        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(MovementType.TRANSFER);
        assertThat(captor.getValue().getSourceWarehouse().getId()).isEqualTo(10L);
        assertThat(captor.getValue().getDestinationWarehouse().getId()).isEqualTo(11L);
    }

    @Test
    void transferDoesNotTouchDestinationWhenSourceIsInsufficient() {
        Warehouse destination = new Warehouse("Annexe", "Adresse 2", "Responsable 2");
        ReflectionTestUtils.setField(destination, "id", 11L);
        when(warehouseRepository.findById(11L)).thenReturn(Optional.of(destination));

        Stock sourceStock = new Stock(product, warehouse, 2, 0);
        Stock destinationStock = new Stock(product, destination, 8, 0);
        when(stockRepository.findForUpdateByWarehouses(1L, List.of(10L, 11L)))
                .thenReturn(List.of(sourceStock, destinationStock));

        assertThatThrownBy(() -> stockService.transfer(new StockTransferRequest(1L, 10L, 11L, 5, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Stock insuffisant");

        assertThat(sourceStock.getQuantity()).isEqualTo(2);
        assertThat(destinationStock.getQuantity()).isEqualTo(8);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void reservationThenShippingDeductsPhysicalAndReservedQuantities() {
        Stock stock = new Stock(product, warehouse, 10, 0);
        when(stockRepository.findForUpdate(1L, 10L)).thenReturn(Optional.of(stock));
        List<StockLine> lines = List.of(new StockLine(1L, 10L, 4));

        stockService.reserveForOrder(lines, "ORD-TEST");
        assertThat(stock.getReservedQuantity()).isEqualTo(4);
        assertThat(stock.availableQuantity()).isEqualTo(6);

        stockService.shipForOrder(lines, "ORD-TEST");
        assertThat(stock.getQuantity()).isEqualTo(6);
        assertThat(stock.getReservedQuantity()).isZero();

        stockService.releaseForOrder(lines);
        assertThat(stock.getReservedQuantity()).isZero();
    }
}
