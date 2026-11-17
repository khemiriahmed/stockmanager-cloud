package com.stockmanager.supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByActiveTrueOrderByNameAsc();

    long countByActiveTrue();

    @Query("""
            select s from Supplier s
            where (:q = ''
                or lower(s.name) like lower(concat('%', :q, '%'))
                or lower(coalesce(s.company, '')) like lower(concat('%', :q, '%'))
                or lower(coalesce(s.email, '')) like lower(concat('%', :q, '%')))
              and (:active is null or s.active = :active)
            """)
    Page<Supplier> search(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);
}
