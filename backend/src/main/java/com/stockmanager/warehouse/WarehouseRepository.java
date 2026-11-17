package com.stockmanager.warehouse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    List<Warehouse> findByActiveTrueOrderByNameAsc();

    long countByActiveTrue();

    @Query("""
            select w from Warehouse w
            where (:q = ''
                or lower(w.name) like lower(concat('%', :q, '%'))
                or lower(coalesce(w.manager, '')) like lower(concat('%', :q, '%')))
              and (:active is null or w.active = :active)
            """)
    Page<Warehouse> search(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);
}
