
package JK.pfm.e2e;


import JK.pfm.dto.AccountCreationRequest;
import JK.pfm.dto.TransactionCreationRequest;
import JK.pfm.dto.UserLoginRequest;
import JK.pfm.dto.UserRegistrationDto;
import JK.pfm.dto.UserSettingsDto;
import JK.pfm.model.Account;
import JK.pfm.model.Category;
import JK.pfm.model.Transaction;
import JK.pfm.repository.CategoryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FullE2ETest {
    
    private static final String USERNAME = "e2euser";
    private static final String PASSWORD = "P@ssword123";
    
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private CategoryRepository categoryRepository;

    private String baseUrl;
    private String jwtToken;
    private Long accountId;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }
    
     private <T> HttpEntity<T> withAuth(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
     
     @BeforeAll
     void seedCategories() {
         categoryRepository.deleteAll();
      var cat1 = new Category("Opening Balance");
      var cat2 = new Category("Fund Transfer");
      var cat3 = new Category("Salary");
      var cat4 = new Category("Some expense");
      cat1.setIsDefault(false);
      cat2.setIsDefault(false);
      cat3.setIsDefault(true);
      cat4.setIsDefault(true);
      categoryRepository.saveAll(List.of(cat1, cat2, cat3, cat4));
    }

    @Test
    @Order(1)
    void testRegisterUser() {
        // 1⃣ Arrange
        var req = new UserRegistrationDto(USERNAME, PASSWORD);

        // 2⃣ Act
        ResponseEntity<Void> resp = restTemplate
        .postForEntity(baseUrl + "/api/users/register", req, Void.class);

        // 3⃣ Assert
        assertEquals(HttpStatus.CREATED, resp.getStatusCode(),
        "Registration endpoint should return 201 CREATED");
    }
    
    @Test
    @Order(2)
    void testLoginAndExtractToken() {
    // Arrange
    var req = new UserLoginRequest(USERNAME, PASSWORD);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<UserLoginRequest> request = new HttpEntity<>(req, headers);

    // Act
    ResponseEntity<String> resp = restTemplate
      .postForEntity(baseUrl + "/api/users/login", request, String.class);

    // Assert status
    assertEquals(HttpStatus.OK, resp.getStatusCode());

    // Extract token (assuming your login returns {"token":"…"})
    String body = resp.getBody();
    assertNotNull(body, "JWT token must be present in login response");
    
    assertTrue(body.startsWith("Bearer "), 
      "Login response should start with 'Bearer '");
    String token = body.substring("Bearer ".length());

    this.jwtToken = token;  // store for later
    }
    
    @Test
    @Order(3)
    void testCreateAccount() {
    // Arrange
    var req = new AccountCreationRequest("E2E Checking", new BigDecimal("500.00"));

    // Act
    ResponseEntity<Account> resp = restTemplate
      .postForEntity(baseUrl + "/api/accounts", withAuth(req), Account.class);

    // Assert
    assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    assertNotNull(resp.getBody().getId(), "Created account must have an ID");
    this.accountId = resp.getBody().getId(); 
    }
    
    @Test
    @Order(4)
    void testAddTransactions() {
        Category depositCategory = categoryRepository.findByName("Salary").orElseThrow();
        Category expenseCategory = categoryRepository.findByName("Some expense").orElseThrow();
    var txn1 = new TransactionCreationRequest(
      LocalDate.now(), new BigDecimal("100"), depositCategory.getId(), "E2E Checking", "Deposit", "t1");
    var txn2 = new TransactionCreationRequest(
      LocalDate.now(), new BigDecimal("25"), expenseCategory.getId(), "E2E Checking", "Expense", "t2");

    var r1 = restTemplate.postForEntity(
        baseUrl + "/api/transactions", withAuth(txn1), Transaction.class);
    var r2 = restTemplate.postForEntity(
        baseUrl + "/api/transactions", withAuth(txn2), Transaction.class);

    assertAll(
      () -> assertEquals(HttpStatus.CREATED, r1.getStatusCode()),
      () -> assertEquals(HttpStatus.CREATED, r2.getStatusCode())
    );
    }

    @Test
    @Order(9)
    void testChangeCurrency() {
    Map<String,String> body = Map.of("currency", "USD");
    
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(jwtToken);
    headers.setContentType(MediaType.APPLICATION_JSON);
    
    HttpEntity<Map<String,String>> patchRequest = new HttpEntity<>(body, headers);
    
    
    String patchUrl = baseUrl + "/api/users/settings/currency";
    ResponseEntity<Void> patchResp = restTemplate.exchange(
        patchUrl,
        HttpMethod.PATCH,
        patchRequest,
        Void.class
    );
    
    assertEquals(HttpStatus.NO_CONTENT, patchResp.getStatusCode(),
        "PATCH /currency should return 204 No Content");
    
    HttpEntity<Void> getRequest = new HttpEntity<>(headers);
    String getUrl = baseUrl + "/api/users/settings";
    ResponseEntity<UserSettingsDto> getResp = restTemplate.exchange(
        getUrl,
        HttpMethod.GET,
        getRequest,
        UserSettingsDto.class
    );
    
    assertEquals(HttpStatus.OK, getResp.getStatusCode(),
        "GET /settings should return 200 OK");
    assertEquals("USD", getResp.getBody().getCurrency(),
        "After PATCH, currency must be EUR");
    
    }






}