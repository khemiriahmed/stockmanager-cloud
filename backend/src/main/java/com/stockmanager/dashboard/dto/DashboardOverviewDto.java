package com.stockmanager.dashboard.dto;

import com.stockmanager.order.dto.OrderSummaryDto;
import com.stockmanager.stock.dto.StockDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardOverviewDto(
        long products,
        long customers,
        long suppliers,
        long warehouses,
        long users,
        long orders,
        BigDecimal stockValue,
        long lowStockProducts,
        long outOfStock,
        long pendingOrders,
        Map<String, Long> ordersByStatus,
        List<SalesPointDto> sales,
        List<StockDto> lowStock,
        List<OrderSummaryDto> recentOrders
) {
}
