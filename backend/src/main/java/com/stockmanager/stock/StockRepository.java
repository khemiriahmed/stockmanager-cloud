package com.stockmanager.stock;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.product.id = :productId and s.warehouse.id = :warehouseId")
    Optional<Stock> findForUpdate(@Param("productId") Long productId, @Param("warehouseId") Long warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.product.id = :productId and s.warehouse.id in :warehouseIds order by s.id")
    List<Stock> findForUpdateByWarehouses(@Param("productId") Long productId,
                                          @Param("warehouseIds") List<Long> warehouseIds);

    @Modifying
    @Query(value = """
            insert into stocks (product_id, warehouse_id, quantity, reserved_quantity, version, created_at, updated_at)
            values (:productId, :warehouseId, 0, 0, 0, now(), now())
            on conflict (product_id, warehouse_id) do nothing
            """, nativeQuery = true)
    void ensureStockRow(@Param("productId") Long productId, @Param("warehouseId") Long warehouseId);

    @EntityGraph(attributePaths = {"product", "warehouse"})
    @Query("""
            select s from Stock s
            where (:productId is null or s.product.id = :productId)
              and (:warehouseId is null or s.warehouse.id = :warehouseId)
              and (:q = ''
                  or lower(s.product.name) like lower(concat('%', :q, '%'))
                  or lower(s.product.reference) like lower(concat('%', :q, '%')))
            """)
    Page<Stock> search(@Param("q") String q,
                       @Param("productId") Long productId,
                       @Param("warehouseId") Long warehouseId,
                       Pageable pageable);

    @EntityGraph(attributePaths = {"product", "warehouse"})
    @Query("""
            select s from Stock s
            where s.product.active = true
              and (s.quantity - s.reservedQuantity) <= s.product.alertThreshold
            order by (s.quantity - s.reservedQuantity) asc
            """)
    Page<Stock> findLowStock(Pageable pageable);

    @Query("select coalesce(sum(s.quantity * p.purchasePrice), 0) from Stock s join s.product p")
    BigDecimal totalStockValue();

    @Query("""
            select count(distinct s.product.id) from Stock s
            where s.product.active = true
              and (s.quantity - s.reservedQuantity) <= s.product.alertThreshold
            """)
    long countLowStockProducts();

    @Query("""
            select count(s) from Stock s
            where s.product.active = true and s.quantity = 0
            """)
    long countOutOfStock();
}
