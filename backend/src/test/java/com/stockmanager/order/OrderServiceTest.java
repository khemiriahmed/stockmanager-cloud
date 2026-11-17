package com.stockmanager.order;

import com.stockmanager.common.exception.BusinessException;
import com.stockmanager.common.util.NumberGenerator;
import com.stockmanager.customer.Customer;
import com.stockmanager.customer.CustomerRepository;
import com.stockmanager.invoice.InvoiceService;
import com.stockmanager.product.Product;
import com.stockmanager.product.ProductRepository;
import com.stockmanager.stock.StockService;
import com.stockmanager.warehouse.Warehouse;
import com.stockmanager.warehouse.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CustomerOrderRepository orderRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private StockService stockService;
    @Mock
    private InvoiceService invoiceService;
    @Mock
    private NumberGenerator numberGenerator;

    @InjectMocks
    private OrderService orderService;

    private CustomerOrder order;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer("Client Test", "Société", "client@test.fr", "0102030405", "Adresse");
        ReflectionTestUtils.setField(customer, "id", 100L);
        Product product = new Product("REF-01", "Produit", null, BigDecimal.TEN, new BigDecimal("25.00"), 2, null);
        ReflectionTestUtils.setField(product, "id", 1L);
        Warehouse warehouse = new Warehouse("Entrepôt", "Adresse", "Manager");
        ReflectionTestUtils.setField(warehouse, "id", 10L);

        order = new CustomerOrder("ORD-2026-000001", customer, new BigDecimal("20.00"), null, "system");
        ReflectionTestUtils.setField(order, "id", 1L);
        order.addItem(new OrderItem(product, warehouse, new BigDecimal("25.00"), 2));
        order.recalculateTotals();
        org.mockito.Mockito.lenient().when(orderRepository.findDetailedById(1L)).thenReturn(Optional.of(order));
    }

    @Test
    void confirmReservesStockAndSetsConfirmedStatus() {
        orderService.confirm(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getConfirmedAt()).isNotNull();
        verify(stockService).reserveForOrder(anyList(), eq("ORD-2026-000001"));
    }

    @Test
    void doubleConfirmationIsRejectedWithoutReservingAgain() {
        order.setStatus(OrderStatus.CONFIRMED);

        assertThatThrownBy(() -> orderService.confirm(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo("INVALID_ORDER_TRANSITION");

        verify(stockService, never()).reserveForOrder(anyList(), eq("ORD-2026-000001"));
    }

    @Test
    void cancellingAConfirmedOrderReleasesReservations() {
        order.setStatus(OrderStatus.CONFIRMED);

        orderService.cancel(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isNotNull();
        verify(stockService).releaseForOrder(anyList());
    }

    @Test
    void cancellingADraftOrderDoesNotTouchReservations() {
        orderService.cancel(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(stockService, never()).releaseForOrder(anyList());
    }

    @Test
    void shippingADraftOrderIsRejected() {
        assertThatThrownBy(() -> orderService.ship(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Impossible de expédier");
        verify(stockService, never()).shipForOrder(anyList(), eq("ORD-2026-000001"));
    }

    @Test
    void cancellingAShippedOrderIsRejected() {
        order.setStatus(OrderStatus.SHIPPED);

        assertThatThrownBy(() -> orderService.cancel(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Impossible de annuler");
    }

    @Test
    void deliveringAShippedOrderGeneratesInvoice() {
        order.setStatus(OrderStatus.SHIPPED);

        orderService.deliver(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getDeliveredAt()).isNotNull();
        verify(invoiceService).generateForOrder(order);
    }

    @Test
    void totalsIncludeVat() {
        assertThat(order.getTotalHt()).isEqualByComparingTo("50.00");
        assertThat(order.getTotalVat()).isEqualByComparingTo("10.00");
        assertThat(order.getTotalTtc()).isEqualByComparingTo("60.00");
    }
}
