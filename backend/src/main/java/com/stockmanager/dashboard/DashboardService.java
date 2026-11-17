package com.stockmanager.dashboard;

import com.stockmanager.customer.CustomerRepository;
import com.stockmanager.dashboard.dto.DashboardOverviewDto;
import com.stockmanager.dashboard.dto.SalesPointDto;
import com.stockmanager.invoice.Invoice;
import com.stockmanager.invoice.InvoiceRepository;
import com.stockmanager.order.CustomerOrderRepository;
import com.stockmanager.order.OrderStatus;
import com.stockmanager.order.dto.OrderSummaryDto;
import com.stockmanager.product.ProductRepository;
import com.stockmanager.stock.StockRepository;
import com.stockmanager.stock.dto.StockDto;
import com.stockmanager.supplier.SupplierRepository;
import com.stockmanager.user.UserRepository;
import com.stockmanager.warehouse.WarehouseRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final CustomerOrderRepository orderRepository;
    private final StockRepository stockRepository;
    private final InvoiceRepository invoiceRepository;

    public DashboardService(ProductRepository productRepository,
                            CustomerRepository customerRepository,
                            SupplierRepository supplierRepository,
                            WarehouseRepository warehouseRepository,
                            UserRepository userRepository,
                            CustomerOrderRepository orderRepository,
                            StockRepository stockRepository,
                            InvoiceRepository invoiceRepository) {
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.warehouseRepository = warehouseRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.stockRepository = stockRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public DashboardOverviewDto overview() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            byStatus.put(status.name(), orderRepository.countByStatus(status));
        }

        YearMonth current = YearMonth.now(ZoneOffset.UTC);
        Instant from = current.minusMonths(11).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Map<String, BigDecimal> salesByPeriod = new LinkedHashMap<>();
        for (Invoice invoice : invoiceRepository.findForPeriod(from)) {
            String period = YearMonth.from(invoice.getIssuedAt().atZone(ZoneOffset.UTC)).toString();
            salesByPeriod.merge(period, invoice.getTotalTtc(), BigDecimal::add);
        }
        Map<String, Long> ordersByPeriod = new LinkedHashMap<>();
        for (Object[] row : orderRepository.countByMonthSince(from)) {
            ordersByPeriod.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }

        List<SalesPointDto> sales = new java.util.ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            String period = current.minusMonths(i).toString();
            sales.add(new SalesPointDto(
                    period,
                    salesByPeriod.getOrDefault(period, BigDecimal.ZERO),
                    ordersByPeriod.getOrDefault(period, 0L)));
        }

        List<StockDto> lowStock = stockRepository
                .findLowStock(PageRequest.of(0, 5))
                .getContent().stream()
                .map(StockDto::from)
                .toList();

        List<OrderSummaryDto> recentOrders = orderRepository
                .findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent().stream()
                .map(OrderSummaryDto::from)
                .toList();

        return new DashboardOverviewDto(
                productRepository.countByActiveTrue(),
                customerRepository.countByActiveTrue(),
                supplierRepository.countByActiveTrue(),
                warehouseRepository.countByActiveTrue(),
                userRepository.count(),
                orderRepository.count(),
                stockRepository.totalStockValue(),
                stockRepository.countLowStockProducts(),
                stockRepository.countOutOfStock(),
                orderRepository.countByStatusIn(List.of(OrderStatus.CONFIRMED, OrderStatus.PROCESSING)),
                byStatus,
                sales,
                lowStock,
                recentOrders
        );
    }
}
