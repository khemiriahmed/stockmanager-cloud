package com.stockmanager.bootstrap;

import com.stockmanager.category.Category;
import com.stockmanager.category.CategoryRepository;
import com.stockmanager.config.AdminProperties;
import com.stockmanager.config.SeedProperties;
import com.stockmanager.customer.Customer;
import com.stockmanager.customer.CustomerRepository;
import com.stockmanager.order.OrderService;
import com.stockmanager.order.dto.CreateOrderItemRequest;
import com.stockmanager.order.dto.CreateOrderRequest;
import com.stockmanager.product.Product;
import com.stockmanager.product.ProductRepository;
import com.stockmanager.role.Role;
import com.stockmanager.role.RoleName;
import com.stockmanager.role.RoleRepository;
import com.stockmanager.stock.StockService;
import com.stockmanager.stock.dto.StockOperationRequest;
import com.stockmanager.supplier.Supplier;
import com.stockmanager.supplier.SupplierRepository;
import com.stockmanager.user.User;
import com.stockmanager.user.UserRepository;
import com.stockmanager.warehouse.Warehouse;
import com.stockmanager.warehouse.WarehouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;
    private final SeedProperties seedProperties;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final CustomerRepository customerRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;
    private final OrderService orderService;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AdminProperties adminProperties,
                           SeedProperties seedProperties,
                           CategoryRepository categoryRepository,
                           SupplierRepository supplierRepository,
                           CustomerRepository customerRepository,
                           WarehouseRepository warehouseRepository,
                           ProductRepository productRepository,
                           StockService stockService,
                           OrderService orderService) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
        this.seedProperties = seedProperties;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.customerRepository = customerRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.stockService = stockService;
        this.orderService = orderService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createRoles();
        createAdmin();
        if (seedProperties.demoEnabled()) {
            seedDemoData();
        }
    }

    private void createRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(new Role(roleName));
                log.info("Rôle créé: {}", roleName);
            }
        }
    }

    private void createAdmin() {
        if (userRepository.existsByEmailIgnoreCase(adminProperties.email())) {
            return;
        }
        Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        User admin = new User(
                adminProperties.firstName(),
                adminProperties.lastName(),
                adminProperties.email(),
                passwordEncoder.encode(adminProperties.password()),
                Set.of(adminRole));
        userRepository.save(admin);
        log.info("Compte administrateur initial créé: {}", adminProperties.email());
    }

    private void seedDemoData() {
        if (productRepository.count() > 0) {
            return;
        }
        log.info("Initialisation des données de démonstration...");

        Role managerRole = roleRepository.findByName(RoleName.MANAGER).orElseThrow();
        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE).orElseThrow();
        createDemoUser("Karim", "Manager", "manager@stockmanager.local", "Manager@12345", Set.of(managerRole));
        createDemoUser("Lea", "Employée", "employee@stockmanager.local", "Employee@12345", Set.of(employeeRole));

        Category it = categoryRepository.save(new Category("Informatique", "Matériel et accessoires informatiques"));
        Category furniture = categoryRepository.save(new Category("Mobilier", "Mobilier de bureau"));
        Category supplies = categoryRepository.save(new Category("Consommables", "Fournitures et consommables"));

        Supplier tech = supplierRepository.save(new Supplier("TechDistrib", "TechDistrib SAS",
                "contact@techdistrib.fr", "+33 1 23 45 67 89", "12 rue de la Paix, 75002 Paris"));
        Supplier office = supplierRepository.save(new Supplier("BureauPlus", "BureauPlus SARL",
                "contact@bureauplus.fr", "+33 4 78 90 12 34", "8 avenue des Champs, 69002 Lyon"));

        Customer alpha = customerRepository.save(new Customer("Alpha SARL", "Alpha SARL",
                "achats@alpha.fr", "+33 6 11 22 33 44", "1 place Bellecour, 69002 Lyon"));
        Customer beta = customerRepository.save(new Customer("Beta Industries", "Beta Industries",
                "commandes@beta.fr", "+33 6 55 66 77 88", "22 rue de la République, 13001 Marseille"));
        Customer gamma = customerRepository.save(new Customer("Gamma Conseil", null,
                "contact@gamma-conseil.fr", "+33 6 99 00 11 22", "5 boulevard Haussmann, 75009 Paris"));

        Warehouse main = warehouseRepository.save(new Warehouse("Entrepôt principal",
                "45 rue de l'Industrie, 69003 Lyon", "Karim Manager"));
        Warehouse secondary = warehouseRepository.save(new Warehouse("Entrepôt annexe",
                "3 rue du Dépôt, 75011 Paris", "Lea Employée"));

        Product laptop = productRepository.save(new Product("INF-LAP-001", "Ordinateur portable Pro 14",
                "Ultrabook professionnel 14 pouces, 16 Go RAM, 512 Go SSD",
                new BigDecimal("720.00"), new BigDecimal("1099.00"), 5, null));
        laptop.setCategory(it);
        laptop.setSupplier(tech);

        Product screen = productRepository.save(new Product("INF-ECR-002", "Écran 27 pouces QHD",
                "Écran IPS 27 pouces, résolution 2560x1440",
                new BigDecimal("160.00"), new BigDecimal("259.00"), 8, null));
        screen.setCategory(it);
        screen.setSupplier(tech);

        Product keyboard = productRepository.save(new Product("INF-CLA-003", "Clavier mécanique",
                "Clavier mécanique filaire, switches brown",
                new BigDecimal("42.00"), new BigDecimal("79.00"), 10, null));
        keyboard.setCategory(it);
        keyboard.setSupplier(tech);

        Product chair = productRepository.save(new Product("MOB-CHA-001", "Chaise ergonomique",
                "Chaise de bureau ergonomique réglable avec appui lombaire",
                new BigDecimal("190.00"), new BigDecimal("329.00"), 4, null));
        chair.setCategory(furniture);
        chair.setSupplier(office);

        Product desk = productRepository.save(new Product("MOB-BUR-002", "Bureau assis-debout",
                "Bureau électrique assis-debout 140x70 cm",
                new BigDecimal("310.00"), new BigDecimal("549.00"), 3, null));
        desk.setCategory(furniture);
        desk.setSupplier(office);

        Product paper = productRepository.save(new Product("CON-PAP-001", "Ramette papier A4",
                "Ramette 500 feuilles papier A4 80g",
                new BigDecimal("3.20"), new BigDecimal("5.90"), 25, null));
        paper.setCategory(supplies);
        paper.setSupplier(office);

        productRepository.saveAll(List.of(laptop, screen, keyboard, chair, desk, paper));

        stockService.entry(new StockOperationRequest(laptop.getId(), main.getId(), 12, "Stock initial"));
        stockService.entry(new StockOperationRequest(screen.getId(), main.getId(), 20, "Stock initial"));
        stockService.entry(new StockOperationRequest(keyboard.getId(), main.getId(), 35, "Stock initial"));
        stockService.entry(new StockOperationRequest(chair.getId(), main.getId(), 6, "Stock initial"));
        stockService.entry(new StockOperationRequest(desk.getId(), main.getId(), 2, "Stock initial"));
        stockService.entry(new StockOperationRequest(paper.getId(), main.getId(), 120, "Stock initial"));
        stockService.entry(new StockOperationRequest(laptop.getId(), secondary.getId(), 3, "Stock initial"));
        stockService.entry(new StockOperationRequest(screen.getId(), secondary.getId(), 4, "Stock initial"));

        orderService.create(new CreateOrderRequest(alpha.getId(), new BigDecimal("20.00"),
                "Commande de démonstration", List.of(
                new CreateOrderItemRequest(laptop.getId(), main.getId(), 2),
                new CreateOrderItemRequest(screen.getId(), main.getId(), 2))));
        var confirmed = orderService.create(new CreateOrderRequest(beta.getId(), new BigDecimal("20.00"),
                "Commande confirmée de démonstration", List.of(
                new CreateOrderItemRequest(chair.getId(), main.getId(), 1),
                new CreateOrderItemRequest(paper.getId(), main.getId(), 10))));
        orderService.confirm(confirmed.id());
        orderService.create(new CreateOrderRequest(gamma.getId(), new BigDecimal("20.00"),
                null, List.of(new CreateOrderItemRequest(keyboard.getId(), main.getId(), 3))));

        log.info("Données de démonstration créées.");
    }

    private void createDemoUser(String firstName, String lastName, String email, String password, Set<Role> roles) {
        if (!userRepository.existsByEmailIgnoreCase(email)) {
            userRepository.save(new User(firstName, lastName, email, passwordEncoder.encode(password), roles));
            log.info("Compte de démonstration créé: {}", email);
        }
    }
}
