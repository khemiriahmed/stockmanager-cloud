package com.stockmanager.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByActiveTrueOrderByNameAsc();

    long countByActiveTrue();

    @Query("""
            select c from Customer c
            where (:q = ''
                or lower(c.name) like lower(concat('%', :q, '%'))
                or lower(coalesce(c.company, '')) like lower(concat('%', :q, '%'))
                or lower(coalesce(c.email, '')) like lower(concat('%', :q, '%')))
              and (:active is null or c.active = :active)
            """)
    Page<Customer> search(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);
}
