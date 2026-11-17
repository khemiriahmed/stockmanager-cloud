package com.stockmanager.order;

import com.stockmanager.common.dto.PageResponse;
import com.stockmanager.order.dto.CreateOrderRequest;
import com.stockmanager.order.dto.OrderDto;
import com.stockmanager.order.dto.OrderSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Commandes")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "Lister les commandes")
    public PageResponse<OrderSummaryDto> list(@RequestParam(required = false) String search,
                                              @RequestParam(required = false) OrderStatus status,
                                              @RequestParam(required = false) Long customerId,
                                              @PageableDefault(size = 20) Pageable pageable) {
        return orderService.list(search, status, customerId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une commande avec ses lignes")
    public OrderDto get(@PathVariable Long id) {
        return orderService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    @Operation(summary = "Créer une commande au statut DRAFT")
    public ResponseEntity<OrderDto> create(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    @Operation(summary = "Confirmer la commande et réserver le stock")
    public OrderDto confirm(@PathVariable Long id) {
        return orderService.confirm(id);
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Passer la commande en préparation")
    public OrderDto process(@PathVariable Long id) {
        return orderService.process(id);
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Expédier la commande et déduire le stock physique")
    public OrderDto ship(@PathVariable Long id) {
        return orderService.ship(id);
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Livrer la commande et générer la facture")
    public OrderDto deliver(@PathVariable Long id) {
        return orderService.deliver(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    @Operation(summary = "Annuler la commande et libérer les réservations")
    public OrderDto cancel(@PathVariable Long id) {
        return orderService.cancel(id);
    }
}
