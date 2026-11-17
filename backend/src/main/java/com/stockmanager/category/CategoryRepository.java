package com.stockmanager.category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<Category> findByActiveTrueOrderByNameAsc();

    @Query("""
            select c from Category c
            where (:q = '' or lower(c.name) like lower(concat('%', :q, '%')))
              and (:active is null or c.active = :active)
            """)
    Page<Category> search(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);
}
