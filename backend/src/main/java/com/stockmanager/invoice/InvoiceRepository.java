package com.stockmanager.invoice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByOrderId(Long orderId);

    Optional<Invoice> findByOrderId(Long orderId);

    @EntityGraph(attributePaths = {"customer", "order"})
    Optional<Invoice> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"customer", "order"})
    @Query("""
            select i from Invoice i
            where (:q = ''
                or lower(i.number) like lower(concat('%', :q, '%'))
                or lower(i.customer.name) like lower(concat('%', :q, '%'))
                or lower(i.order.reference) like lower(concat('%', :q, '%')))
            """)
    Page<Invoice> search(@Param("q") String q, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "order"})
    java.util.List<Invoice> findAllByOrderByIssuedAtDesc();

    @Query("""
            select i from Invoice i
            where i.issuedAt >= :from
            order by i.issuedAt asc
            """)
    java.util.List<Invoice> findForPeriod(@Param("from") java.time.Instant from);
}
