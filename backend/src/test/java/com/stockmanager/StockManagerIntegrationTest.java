package com.stockmanager;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StockManagerIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@test.local";
    private static final String ADMIN_PASSWORD = "Admin@Test123";
    private static final String STORAGE_PATH = Path.of(System.getProperty("java.io.tmpdir"),
            "stockmanager-test-" + UUID.randomUUID()).toString();

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("stockmanager_test")
            .withUsername("stockmanager")
            .withPassword("stockmanager");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "integration-test-secret-key-with-more-than-32-chars");
        registry.add("app.admin.email", () -> ADMIN_EMAIL);
        registry.add("app.admin.password", () -> ADMIN_PASSWORD);
        registry.add("app.admin.first-name", () -> "Test");
        registry.add("app.admin.last-name", () -> "Admin");
        registry.add("app.seed.demo-enabled", () -> "false");
        registry.add("app.storage.type", () -> "local");
        registry.add("app.storage.local-path", () -> STORAGE_PATH);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void fullCommercialFlowFromLoginToInvoicePdf() {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        ResponseEntity<JsonNode> unauthorized = restTemplate.getForEntity("/api/v1/products", JsonNode.class);
        assertThat(unauthorized.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        long categoryId = idOf(request(HttpMethod.POST, "/api/v1/categories",
                Map.of("name", "Catégorie test", "description", "Test"), adminToken));
        long supplierId = idOf(request(HttpMethod.POST, "/api/v1/suppliers",
                Map.of("name", "Fournisseur test", "email", "supplier@test.fr"), adminToken));
        long warehouseId = idOf(request(HttpMethod.POST, "/api/v1/warehouses",
                Map.of("name", "Entrepôt test", "address", "Adresse test", "manager", "Testeur"), adminToken));
        long customerId = idOf(request(HttpMethod.POST, "/api/v1/customers",
                Map.of("name", "Client test", "email", "client@test.fr"), adminToken));

        JsonNode product = request(HttpMethod.POST, "/api/v1/products", Map.of(
                "reference", "TEST-REF-001",
                "name", "Produit test",
                "purchasePrice", 10.00,
                "sellingPrice", 25.00,
                "alertThreshold", 2,
                "categoryId", categoryId,
                "supplierId", supplierId), adminToken);
        long productId = product.get("id").asLong();

        ResponseEntity<JsonNode> duplicate = exchange(HttpMethod.POST, "/api/v1/products", Map.of(
                "reference", "TEST-REF-001",
                "name", "Doublon",
                "purchasePrice", 1.00,
                "sellingPrice", 2.00,
                "alertThreshold", 0), adminToken);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        request(HttpMethod.POST, "/api/v1/stocks/entry", Map.of(
                "productId", productId, "warehouseId", warehouseId, "quantity", 10, "reason", "Réception"), adminToken);

        JsonNode order = request(HttpMethod.POST, "/api/v1/orders", Map.of(
                "customerId", customerId,
                "vatRate", 20,
                "items", List.of(Map.of("productId", productId, "warehouseId", warehouseId, "quantity", 4))),
                adminToken);
        assertThat(order.get("status").asText()).isEqualTo("DRAFT");
        assertThat(order.get("totalHt").asDouble()).isEqualTo(100.0);
        assertThat(order.get("totalTtc").asDouble()).isEqualTo(120.0);

        JsonNode stockBefore = stockOf(productId, warehouseId, adminToken);
        assertThat(stockBefore.get("reservedQuantity").asInt()).isZero();

        long orderId = order.get("id").asLong();
        JsonNode confirmed = request(HttpMethod.POST, "/api/v1/orders/" + orderId + "/confirm", null, adminToken);
        assertThat(confirmed.get("status").asText()).isEqualTo("CONFIRMED");

        JsonNode stockReserved = stockOf(productId, warehouseId, adminToken);
        assertThat(stockReserved.get("reservedQuantity").asInt()).isEqualTo(4);
        assertThat(stockReserved.get("availableQuantity").asInt()).isEqualTo(6);

        ResponseEntity<JsonNode> doubleConfirm = exchange(HttpMethod.POST,
                "/api/v1/orders/" + orderId + "/confirm", null, adminToken);
        assertThat(doubleConfirm.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(doubleConfirm.getBody().get("code").asText()).isEqualTo("INVALID_ORDER_TRANSITION");

        JsonNode shipped = request(HttpMethod.POST, "/api/v1/orders/" + orderId + "/ship", null, adminToken);
        assertThat(shipped.get("status").asText()).isEqualTo("SHIPPED");
        JsonNode stockShipped = stockOf(productId, warehouseId, adminToken);
        assertThat(stockShipped.get("quantity").asInt()).isEqualTo(6);
        assertThat(stockShipped.get("reservedQuantity").asInt()).isZero();

        JsonNode delivered = request(HttpMethod.POST, "/api/v1/orders/" + orderId + "/deliver", null, adminToken);
        assertThat(delivered.get("status").asText()).isEqualTo("DELIVERED");

        JsonNode invoices = request(HttpMethod.GET, "/api/v1/invoices", null, adminToken);
        assertThat(invoices.get("totalElements").asInt()).isEqualTo(1);
        JsonNode invoice = invoices.get("content").get(0);
        assertThat(invoice.get("orderReference").asText()).isEqualTo(order.get("reference").asText());

        ResponseEntity<byte[]> pdf = restTemplate.exchange(
                invoice.get("downloadPath").asText(), HttpMethod.GET,
                new HttpEntity<>(headers(adminToken)), byte[].class);
        assertThat(pdf.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pdf.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(new String(pdf.getBody(), 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");

        JsonNode oversizedOrder = request(HttpMethod.POST, "/api/v1/orders", Map.of(
                "customerId", customerId,
                "items", List.of(Map.of("productId", productId, "warehouseId", warehouseId, "quantity", 500))),
                adminToken);
        ResponseEntity<JsonNode> refused = exchange(HttpMethod.POST,
                "/api/v1/orders/" + oversizedOrder.get("id").asLong() + "/confirm", null, adminToken);
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(refused.getBody().get("code").asText()).isEqualTo("INSUFFICIENT_STOCK");
        JsonNode stockAfterRefusal = stockOf(productId, warehouseId, adminToken);
        assertThat(stockAfterRefusal.get("reservedQuantity").asInt()).isZero();
    }

    @Test
    void employeeCannotModifyCatalog() {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String email = "employee-" + UUID.randomUUID() + "@test.local";
        request(HttpMethod.POST, "/api/v1/users", Map.of(
                "firstName", "Employé",
                "lastName", "Test",
                "email", email,
                "password", "Employee@Test123",
                "roles", List.of("EMPLOYEE")), adminToken);

        String employeeToken = login(email, "Employee@Test123");
        ResponseEntity<JsonNode> list = exchange(HttpMethod.GET, "/api/v1/products", null, employeeToken);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<JsonNode> forbidden = exchange(HttpMethod.POST, "/api/v1/products", Map.of(
                "reference", "EMP-FORBIDDEN",
                "name", "Interdit",
                "purchasePrice", 1.00,
                "sellingPrice", 2.00,
                "alertThreshold", 0), employeeToken);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private String login(String email, String password) {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity("/api/v1/auth/login",
                Map.of("email", email, "password", password), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = response.getBody().get("accessToken").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private JsonNode stockOf(long productId, long warehouseId, String token) {
        JsonNode page = request(HttpMethod.GET,
                "/api/v1/stocks?productId=" + productId + "&warehouseId=" + warehouseId, null, token);
        assertThat(page.get("content")).isNotEmpty();
        return page.get("content").get(0);
    }

    private JsonNode request(HttpMethod method, String path, Object body, String token) {
        ResponseEntity<JsonNode> response = exchange(method, path, body, token);
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Réponse attendue 2xx pour %s %s: %s", method, path, response.getBody())
                .isTrue();
        return response.getBody();
    }

    private ResponseEntity<JsonNode> exchange(HttpMethod method, String path, Object body, String token) {
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers(token)), JsonNode.class);
    }

    private HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    private long idOf(JsonNode node) {
        return node.get("id").asLong();
    }
}
