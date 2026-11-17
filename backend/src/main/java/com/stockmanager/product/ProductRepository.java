package com.stockmanager.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByReferenceIgnoreCase(String reference);

    long countByActiveTrue();

    @EntityGraph(attributePaths = {"category", "supplier"})
    Optional<Product> findWithRelationsById(Long id);

    @EntityGraph(attributePaths = {"category", "supplier"})
    List<Product> findByActiveTrueOrderByNameAsc();

    @EntityGraph(attributePaths = {"category", "supplier"})
    @Query("""
            select p from Product p
            where (:q = ''
                or lower(p.name) like lower(concat('%', :q, '%'))
                or lower(p.reference) like lower(concat('%', :q, '%')))
              and (:categoryId is null or p.category.id = :categoryId)
              and (:supplierId is null or p.supplier.id = :supplierId)
              and (:active is null or p.active = :active)
            """)
    Page<Product> search(@Param("q") String q,
                         @Param("categoryId") Long categoryId,
                         @Param("supplierId") Long supplierId,
                         @Param("active") Boolean active,
                         Pageable pageable);
}
