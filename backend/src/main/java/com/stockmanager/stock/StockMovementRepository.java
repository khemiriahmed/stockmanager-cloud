package com.stockmanager.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @EntityGraph(attributePaths = {"product", "sourceWarehouse", "destinationWarehouse"})
    @Query("""
            select m from StockMovement m
            where (:productId is null or m.product.id = :productId)
              and (:warehouseId is null
                  or m.sourceWarehouse.id = :warehouseId
                  or m.destinationWarehouse.id = :warehouseId)
              and (:type is null or m.type = :type)
            order by m.createdAt desc
            """)
    Page<StockMovement> search(@Param("productId") Long productId,
                               @Param("warehouseId") Long warehouseId,
                               @Param("type") MovementType type,
                               Pageable pageable);
}
