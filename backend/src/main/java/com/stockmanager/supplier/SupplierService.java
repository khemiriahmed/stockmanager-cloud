package com.stockmanager.supplier;

import com.stockmanager.common.dto.PageResponse;
import com.stockmanager.common.exception.BusinessException;
import com.stockmanager.supplier.dto.SupplierDto;
import com.stockmanager.supplier.dto.SupplierRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierDto> list(String search, Boolean active, Pageable pageable) {
        String query = StringUtils.hasText(search) ? search.trim() : "";
        return PageResponse.of(supplierRepository.search(query, active, pageable), SupplierDto::from);
    }

    @Transactional(readOnly = true)
    public List<SupplierDto> listActive() {
        return supplierRepository.findByActiveTrueOrderByNameAsc().stream().map(SupplierDto::from).toList();
    }

    @Transactional(readOnly = true)
    public SupplierDto get(Long id) {
        return SupplierDto.from(find(id));
    }

    public SupplierDto create(SupplierRequest request) {
        Supplier supplier = new Supplier(request.name().trim(), request.company(), request.email(),
                request.phone(), request.address());
        return SupplierDto.from(supplierRepository.save(supplier));
    }

    public SupplierDto update(Long id, SupplierRequest request) {
        Supplier supplier = find(id);
        supplier.setName(request.name().trim());
        supplier.setCompany(request.company());
        supplier.setEmail(request.email());
        supplier.setPhone(request.phone());
        supplier.setAddress(request.address());
        return SupplierDto.from(supplier);
    }

    public SupplierDto setStatus(Long id, boolean active) {
        Supplier supplier = find(id);
        supplier.setActive(active);
        return SupplierDto.from(supplier);
    }

    public void deactivate(Long id) {
        find(id).setActive(false);
    }

    public Supplier find(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Fournisseur introuvable."));
    }
}
