package com.stockmanager.warehouse;

import com.stockmanager.common.dto.PageResponse;
import com.stockmanager.common.exception.BusinessException;
import com.stockmanager.warehouse.dto.WarehouseDto;
import com.stockmanager.warehouse.dto.WarehouseRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<WarehouseDto> list(String search, Boolean active, Pageable pageable) {
        String query = StringUtils.hasText(search) ? search.trim() : "";
        return PageResponse.of(warehouseRepository.search(query, active, pageable), WarehouseDto::from);
    }

    @Transactional(readOnly = true)
    public List<WarehouseDto> listActive() {
        return warehouseRepository.findByActiveTrueOrderByNameAsc().stream().map(WarehouseDto::from).toList();
    }

    @Transactional(readOnly = true)
    public WarehouseDto get(Long id) {
        return WarehouseDto.from(find(id));
    }

    public WarehouseDto create(WarehouseRequest request) {
        Warehouse warehouse = new Warehouse(request.name().trim(), request.address(), request.manager());
        return WarehouseDto.from(warehouseRepository.save(warehouse));
    }

    public WarehouseDto update(Long id, WarehouseRequest request) {
        Warehouse warehouse = find(id);
        warehouse.setName(request.name().trim());
        warehouse.setAddress(request.address());
        warehouse.setManager(request.manager());
        return WarehouseDto.from(warehouse);
    }

    public WarehouseDto setStatus(Long id, boolean active) {
        Warehouse warehouse = find(id);
        warehouse.setActive(active);
        return WarehouseDto.from(warehouse);
    }

    public void deactivate(Long id) {
        find(id).setActive(false);
    }

    public Warehouse find(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Entrepôt introuvable."));
    }
}
