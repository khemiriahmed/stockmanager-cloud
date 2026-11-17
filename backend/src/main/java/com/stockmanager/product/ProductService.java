package com.stockmanager.product;

import com.stockmanager.category.Category;
import com.stockmanager.category.CategoryRepository;
import com.stockmanager.common.dto.PageResponse;
import com.stockmanager.common.exception.BusinessException;
import com.stockmanager.product.dto.ProductDto;
import com.stockmanager.product.dto.ProductRequest;
import com.stockmanager.supplier.Supplier;
import com.stockmanager.supplier.SupplierRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          SupplierRepository supplierRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> list(String search, Long categoryId, Long supplierId,
                                         Boolean active, Pageable pageable) {
        String query = StringUtils.hasText(search) ? search.trim() : "";
        return PageResponse.of(productRepository.search(query, categoryId, supplierId, active, pageable), ProductDto::from);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> listActive() {
        return productRepository.findByActiveTrueOrderByNameAsc().stream().map(ProductDto::from).toList();
    }

    @Transactional(readOnly = true)
    public ProductDto get(Long id) {
        return ProductDto.from(find(id));
    }

    public ProductDto create(ProductRequest request) {
        String reference = request.reference().trim();
        if (productRepository.existsByReferenceIgnoreCase(reference)) {
            throw BusinessException.conflict("Une référence produit identique existe déjà.");
        }
        Product product = new Product(reference, request.name().trim(), request.description(),
                request.purchasePrice(), request.sellingPrice(), request.alertThreshold(), request.imageUrl());
        applyRelations(product, request);
        return ProductDto.from(productRepository.save(product));
    }

    public ProductDto update(Long id, ProductRequest request) {
        Product product = find(id);
        String reference = request.reference().trim();
        if (!product.getReference().equalsIgnoreCase(reference)
                && productRepository.existsByReferenceIgnoreCase(reference)) {
            throw BusinessException.conflict("Une référence produit identique existe déjà.");
        }
        product.setReference(reference);
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPurchasePrice(request.purchasePrice());
        product.setSellingPrice(request.sellingPrice());
        product.setAlertThreshold(request.alertThreshold());
        product.setImageUrl(request.imageUrl());
        applyRelations(product, request);
        return ProductDto.from(product);
    }

    public ProductDto setStatus(Long id, boolean active) {
        Product product = find(id);
        product.setActive(active);
        return ProductDto.from(product);
    }

    public void deactivate(Long id) {
        find(id).setActive(false);
    }

    public Product find(Long id) {
        return productRepository.findWithRelationsById(id)
                .orElseThrow(() -> BusinessException.notFound("Produit introuvable."));
    }

    private void applyRelations(Product product, ProductRequest request) {
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> BusinessException.notFound("Catégorie introuvable."));
        }
        Supplier supplier = null;
        if (request.supplierId() != null) {
            supplier = supplierRepository.findById(request.supplierId())
                    .orElseThrow(() -> BusinessException.notFound("Fournisseur introuvable."));
        }
        product.setCategory(category);
        product.setSupplier(supplier);
    }
}
