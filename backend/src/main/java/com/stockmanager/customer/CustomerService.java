package com.stockmanager.customer;

import com.stockmanager.common.dto.PageResponse;
import com.stockmanager.common.exception.BusinessException;
import com.stockmanager.customer.dto.CustomerDto;
import com.stockmanager.customer.dto.CustomerRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerDto> list(String search, Boolean active, Pageable pageable) {
        String query = StringUtils.hasText(search) ? search.trim() : "";
        return PageResponse.of(customerRepository.search(query, active, pageable), CustomerDto::from);
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> listActive() {
        return customerRepository.findByActiveTrueOrderByNameAsc().stream().map(CustomerDto::from).toList();
    }

    @Transactional(readOnly = true)
    public CustomerDto get(Long id) {
        return CustomerDto.from(find(id));
    }

    public CustomerDto create(CustomerRequest request) {
        Customer customer = new Customer(request.name().trim(), request.company(), request.email(),
                request.phone(), request.address());
        return CustomerDto.from(customerRepository.save(customer));
    }

    public CustomerDto update(Long id, CustomerRequest request) {
        Customer customer = find(id);
        customer.setName(request.name().trim());
        customer.setCompany(request.company());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setAddress(request.address());
        return CustomerDto.from(customer);
    }

    public CustomerDto setStatus(Long id, boolean active) {
        Customer customer = find(id);
        customer.setActive(active);
        return CustomerDto.from(customer);
    }

    public void deactivate(Long id) {
        find(id).setActive(false);
    }

    public Customer find(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Client introuvable."));
    }
}
