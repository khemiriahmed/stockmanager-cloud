package com.stockmanager.order;

import com.stockmanager.common.dto.PageResponse;
import com.stockmanager.common.exception.BusinessException;
import com.stockmanager.common.security.SecurityUtils;
import com.stockmanager.common.util.NumberGenerator;
import com.stockmanager.customer.Customer;
import com.stockmanager.customer.CustomerRepository;
import com.stockmanager.invoice.InvoiceService;
import com.stockmanager.order.dto.CreateOrderItemRequest;
import com.stockmanager.order.dto.CreateOrderRequest;
import com.stockmanager.order.dto.OrderDto;
import com.stockmanager.order.dto.OrderSummaryDto;
import com.stockmanager.product.Product;
import com.stockmanager.product.ProductRepository;
import com.stockmanager.stock.StockLine;
import com.stockmanager.stock.StockService;
import com.stockmanager.warehouse.Warehouse;
import com.stockmanager.warehouse.WarehouseRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockService stockService;
    private final InvoiceService invoiceService;
    private final NumberGenerator numberGenerator;

    public OrderService(CustomerOrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository,
                        WarehouseRepository warehouseRepository,
                        StockService stockService,
                        InvoiceService invoiceService,
                        NumberGenerator numberGenerator) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockService = stockService;
        this.invoiceService = invoiceService;
        this.numberGenerator = numberGenerator;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryDto> list(String search, OrderStatus status, Long customerId,
                                              Pageable pageable) {
        String query = StringUtils.hasText(search) ? search.trim() : "";
        return PageResponse.of(orderRepository.search(query, status, customerId, pageable), OrderSummaryDto::from);
    }

    @Transactional(readOnly = true)
    public OrderDto get(Long id) {
        return OrderDto.from(findDetailed(id));
    }

    public OrderDto create(CreateOrderRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> BusinessException.notFound("Client introuvable."));
        if (!customer.isActive()) {
            throw new BusinessException("Le client sélectionné est désactivé.", "CUSTOMER_INACTIVE");
        }

        BigDecimal vatRate = request.vatRate() != null
                ? request.vatRate().setScale(2, java.math.RoundingMode.HALF_UP)
                : new BigDecimal("20.00");
        CustomerOrder order = new CustomerOrder(numberGenerator.nextOrderReference(), customer, vatRate,
                request.notes(), SecurityUtils.currentUserEmail());

        for (CreateOrderItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> BusinessException.notFound("Produit introuvable: " + itemRequest.productId()));
            if (!product.isActive()) {
                throw new BusinessException("Le produit " + product.getReference() + " est désactivé.",
                        "PRODUCT_INACTIVE");
            }
            Warehouse warehouse = warehouseRepository.findById(itemRequest.warehouseId())
                    .orElseThrow(() -> BusinessException.notFound(
                            "Entrepôt introuvable: " + itemRequest.warehouseId()));
            if (!warehouse.isActive()) {
                throw new BusinessException("L'entrepôt " + warehouse.getName() + " est désactivé.",
                        "WAREHOUSE_INACTIVE");
            }
            order.addItem(new OrderItem(product, warehouse, product.getSellingPrice(), itemRequest.quantity()));
        }
        order.recalculateTotals();
        return OrderDto.from(orderRepository.save(order));
    }

    public OrderDto confirm(Long id) {
        CustomerOrder order = findDetailed(id);
        requireStatus(order, OrderStatus.DRAFT, "confirmer");
        stockService.reserveForOrder(lines(order), order.getReference());
        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(Instant.now());
        return OrderDto.from(order);
    }

    public OrderDto process(Long id) {
        CustomerOrder order = findDetailed(id);
        requireStatus(order, OrderStatus.CONFIRMED, "mettre en préparation");
        order.setStatus(OrderStatus.PROCESSING);
        order.setProcessingAt(Instant.now());
        return OrderDto.from(order);
    }

    public OrderDto ship(Long id) {
        CustomerOrder order = findDetailed(id);
        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.PROCESSING) {
            throw invalidTransition(order, "expédier");
        }
        stockService.shipForOrder(lines(order), order.getReference());
        order.setStatus(OrderStatus.SHIPPED);
        order.setShippedAt(Instant.now());
        return OrderDto.from(order);
    }

    public OrderDto deliver(Long id) {
        CustomerOrder order = findDetailed(id);
        requireStatus(order, OrderStatus.SHIPPED, "livrer");
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(Instant.now());
        invoiceService.generateForOrder(order);
        return OrderDto.from(order);
    }

    public OrderDto cancel(Long id) {
        CustomerOrder order = findDetailed(id);
        if (order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw invalidTransition(order, "annuler");
        }
        if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PROCESSING) {
            stockService.releaseForOrder(lines(order));
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        return OrderDto.from(order);
    }

    private List<StockLine> lines(CustomerOrder order) {
        return order.getItems().stream()
                .map(item -> new StockLine(item.getProduct().getId(), item.getWarehouse().getId(),
                        item.getQuantity()))
                .toList();
    }

    private CustomerOrder findDetailed(Long id) {
        return orderRepository.findDetailedById(id)
                .orElseThrow(() -> BusinessException.notFound("Commande introuvable."));
    }

    private void requireStatus(CustomerOrder order, OrderStatus expected, String action) {
        if (order.getStatus() != expected) {
            throw invalidTransition(order, action);
        }
    }

    private BusinessException invalidTransition(CustomerOrder order, String action) {
        return new BusinessException(
                "Impossible de " + action + " la commande " + order.getReference()
                        + " depuis le statut " + order.getStatus() + ".",
                "INVALID_ORDER_TRANSITION", org.springframework.http.HttpStatus.CONFLICT);
    }
}
