package com.stockmanager.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    @EntityGraph(attributePaths = {"customer", "items", "items.product", "items.warehouse"})
    Optional<CustomerOrder> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"customer"})
    @Query("""
            select o from CustomerOrder o
            where (:q = ''
                or lower(o.reference) like lower(concat('%', :q, '%'))
                or lower(o.customer.name) like lower(concat('%', :q, '%'))
                or lower(coalesce(o.customer.company, '')) like lower(concat('%', :q, '%')))
              and (:status is null or o.status = :status)
              and (:customerId is null or o.customer.id = :customerId)
            """)
    Page<CustomerOrder> search(@Param("q") String q,
                               @Param("status") OrderStatus status,
                               @Param("customerId") Long customerId,
                               Pageable pageable);

    long countByStatus(OrderStatus status);

    long countByStatusIn(java.util.Collection<OrderStatus> statuses);

    @Query(value = """
            select to_char(created_at, 'YYYY-MM') as period, count(*) as total
            from customer_orders
            where created_at >= :from
            group by to_char(created_at, 'YYYY-MM')
            order by period
            """, nativeQuery = true)
    java.util.List<Object[]> countByMonthSince(@Param("from") java.time.Instant from);
}
