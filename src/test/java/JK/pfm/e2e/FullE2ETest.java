
package JK.pfm.e2e;


import JK.pfm.dto.AccountCreationRequest;
import JK.pfm.dto.BudgetCreationRequest;
import JK.pfm.dto.CashFlowDTO;
import JK.pfm.dto.ExpenseByCategoryDTO;
import JK.pfm.dto.RecurringExpenseCreation;
import JK.pfm.dto.SavingGoalCreation;
import JK.pfm.dto.TransactionCreationRequest;
import JK.pfm.dto.UserLoginRequest;
import JK.pfm.dto.UserRegistrationDto;
import JK.pfm.dto.UserSettingsDto;
import JK.pfm.model.Account;
import JK.pfm.model.Budget;
import JK.pfm.model.Category;
import JK.pfm.model.RecurringExpense;
import JK.pfm.model.SavingsGoal;
import JK.pfm.model.Transaction;
import JK.pfm.repository.AccountRepository;
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
    @Autowired
    private AccountRepository repo;

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
    @Order(5)
    void testAddBudget() {
        Category expenseCategory = categoryRepository.findByName("Some expense").orElseThrow();
        var req = new BudgetCreationRequest(
                new BigDecimal("100"), 
                LocalDate.now().minusDays(5), 
                LocalDate.now().plusDays(5), 
                expenseCategory.getId()
                        );
        
        ResponseEntity<Budget> resp = restTemplate.postForEntity(
        baseUrl + "/api/budgets",
        withAuth(req),
        Budget.class
                );
                
        assertEquals(HttpStatus.CREATED, resp.getStatusCode(),
        "Budget creation should return 201 CREATED");   
        
        assertNotNull(resp.getBody().getId(),
        "Created Budget must have an ID");
    
    }
    
    @Test
    @Order(6)
    void testAddAutoPayment(){
        Category expenseCategory = categoryRepository.findByName("Some expense").orElseThrow();
        var req = new RecurringExpenseCreation("GYM", 
                LocalDate.now(), 
                new BigDecimal("30"), 
                expenseCategory.getId(), 
                "E2E Checking", 
                "MONTHLY"
                        );
        
        ResponseEntity<RecurringExpense> resp = restTemplate.postForEntity(
        baseUrl + "/api/recurring-expenses",
        withAuth(req),
        RecurringExpense.class
                );
                
        assertEquals(HttpStatus.CREATED, resp.getStatusCode(),
        "recurring-expense creation should return 201 CREATED");   
        
        assertNotNull(resp.getBody().getId(),
        "Created recurring-expense must have an ID");
    }
    
    @Test
    @Order(7)
    void testAddSavingsGoal(){
        var req = new SavingGoalCreation(
                new BigDecimal("200"),
                "iPhone",
                "new phone"
                        );
        
        ResponseEntity<SavingsGoal> resp = restTemplate.postForEntity(
        baseUrl + "/api/savings-goals",
        withAuth(req),
        SavingsGoal.class
                );
                
        assertEquals(HttpStatus.CREATED, resp.getStatusCode(),
        "SavingsGoal creation should return 201 CREATED");   
        
        assertNotNull(resp.getBody().getId(),
        "Created SavingsGoal must have an ID");
    }
    
    @Test
    @Order(8)
    void testReportsCategorySpending(){
        
        String url = String.format(
        "%s/api/reports/spending-by-category?start=%s&end=%s",
        baseUrl,
        LocalDate.now().minusDays(3),
        LocalDate.now().plusDays(3)
        );
        
    ResponseEntity<ExpenseByCategoryDTO[]> resp = restTemplate
    .exchange(url, HttpMethod.GET, withAuth(null), ExpenseByCategoryDTO[].class);

    assertEquals(HttpStatus.OK, resp.getStatusCode());
    ExpenseByCategoryDTO[] flows = resp.getBody();
    assertNotNull(flows, "Response body must not be null");
    }
    
    @Test
    @Order(9)
    void testReportsMoneyNetFlow(){
        ResponseEntity<CashFlowDTO[]> resp = restTemplate.exchange(
        baseUrl + "/api/reports/monthly-cashflow",
        HttpMethod.GET,
        withAuth(null),
        CashFlowDTO[].class
        );
        
        assertEquals(HttpStatus.OK, resp.getStatusCode(),
      "GET /api/reports/monthly-cashflow should return 200 OK");
        CashFlowDTO[] flows = resp.getBody();
        assertNotNull(flows, "Response body must not be null");
        
    }

    @Test
    @Order(10)
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

    @Test
    @Order(11)
    void testCreateAccountValidationFailure() {
    // Missing ‘name’ or negative balance
    var invalidReq = new AccountCreationRequest("", new BigDecimal("-10"));
    ResponseEntity<String> resp = restTemplate.postForEntity(
      baseUrl + "/api/accounts", withAuth(invalidReq), String.class);

    assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode(),
      "Invalid account DTO should be rejected with 400 Bad Request");
        }
    
    @Test
    @Order(12)
    void testAnonymousCannotCreateAccount() {
    // No Authorization header
    var req = new AccountCreationRequest("Hacker Account", BigDecimal.ZERO);
    ResponseEntity<String> resp = restTemplate
      .postForEntity(baseUrl + "/api/accounts", req, String.class);

    assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode(),
        "Calling a secured endpoint without JWT should return forbidden");
    }
    
    @Test
    @Order(13)
    void testInvalidJwtIsRejected() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth("Bearer this.is.not.a.valid.token");
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> resp = restTemplate.exchange(
      baseUrl + "/api/accounts",
      HttpMethod.GET,
      entity,
      String.class
    );

    assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode(),
        "Invalid JWT should be rejected with forbidden");
    }

    @Test
    @Order(14)
    void testRegisterDuplicateUsername() {
    // First registration succeeds
    var req1 = new UserRegistrationDto("dupuser", "pass12345");
    assertEquals(HttpStatus.CREATED,
      restTemplate.postForEntity(baseUrl + "/api/users/register", req1, Void.class).getStatusCode());

    // Second registration must fail
    var req2 = new UserRegistrationDto("dupuser", "pass456789");
    ResponseEntity<String> resp = restTemplate.postForEntity(
      baseUrl + "/api/users/register", req2, String.class);

    assertEquals(HttpStatus.CONFLICT, resp.getStatusCode(),
      "Registering the same username twice should return 409 Conflict");
    }

    @Test
    @Order(15)
    void testLoginWithBadCredentials() {
    var badLogin = new UserLoginRequest(USERNAME, "WrongPass!");
    ResponseEntity<String> resp = restTemplate.postForEntity(
      baseUrl + "/api/users/login", badLogin, String.class);

    assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode(),
      "Logging in with incorrect password should return 401 Unauthorized");
    }
    
    @Test
    @Order(16)
    void testUnauthorizedUserCannotDeleteAccount() {
    // ── Arrange: register a totally new user ────────────────────────────────
    String otherUsername = "otherUser";
    String otherPassword = "OtherP@ss1";
    var regReq = new UserRegistrationDto(otherUsername, otherPassword);
    // we expect 201 CREATED
    ResponseEntity<Void> regResp = restTemplate
        .postForEntity(baseUrl + "/api/users/register", regReq, Void.class);
    assertEquals(HttpStatus.CREATED, regResp.getStatusCode(),
      "Second user should register successfully");

    // ── Act: login as that new user ─────────────────────────────────────────
    var loginReq = new UserLoginRequest(otherUsername, otherPassword);
    ResponseEntity<String> loginResp = restTemplate
        .postForEntity(baseUrl + "/api/users/login", loginReq, String.class);
    assertEquals(HttpStatus.OK, loginResp.getStatusCode(),
      "Login for second user should return 200 OK");
    String rawBody = loginResp.getBody();
    assertNotNull(rawBody);
    assertTrue(rawBody.startsWith("Bearer "));
    String otherJwt = rawBody.substring("Bearer ".length());

    // ── Act: attempt to delete the first user’s account ──────────────────────
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(otherJwt);
    HttpEntity<Void> deleteReq = new HttpEntity<>(headers);


    // assumes your DELETE account endpoint is DELETE /api/accounts/{id}
    String deleteUrl = baseUrl + "/api/accounts/" + accountId;
    ResponseEntity<Void> deleteResp = restTemplate.exchange(
        deleteUrl, HttpMethod.DELETE, deleteReq, Void.class);

    // ── Assert: forbidden because this account doesn’t belong to otherUser ────
    assertEquals(HttpStatus.NOT_FOUND, deleteResp.getStatusCode(),
    "Deleting another user’s account should return 404 Not Found");
    }


}