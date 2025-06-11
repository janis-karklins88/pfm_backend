
package JK.pfm.service;

import JK.pfm.dto.BudgetCreationRequest;
import JK.pfm.dto.UpdateBudgetAmountDto;
import JK.pfm.model.Budget;
import JK.pfm.model.Category;
import JK.pfm.model.User;
import JK.pfm.repository.BudgetRepository;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.service.BudgetService;
import JK.pfm.util.AccountUtil;
import JK.pfm.util.SecurityUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BudgetServiceTest {
    @Mock UserRepository       userRepository;
    @Mock BudgetRepository budgetRepository;
    @Mock AccountUtil accountUtil;
    @Mock CategoryRepository categoryRepository;
    
    @InjectMocks
    BudgetService budgetService;
    
    private MockedStatic<SecurityUtil> securityUtilMock;
    private User    stubUser;
    private Budget budget;
    private BudgetCreationRequest req;
    private Category category;
    private List<Long> stubAccountIds;
    
    
    @BeforeEach
    void setUp(){
        //user mock
        securityUtilMock = mockStatic(SecurityUtil.class);
        stubUser = new User();
        stubUser.setId(1L);
        securityUtilMock.when(() -> SecurityUtil.getUser(userRepository)).thenReturn(stubUser);
        securityUtilMock.when(SecurityUtil::getUserId).thenReturn(1L);
        
        //default budget missing
        when(budgetRepository.findById(anyLong()))
        .thenReturn(Optional.empty());
        
        //category
        category = new Category();
        category.setName("Food");
        category.setId(7L);
        
        //budget
        budget = new Budget();
        budget.setId(5L);
        budget.setAmount(new BigDecimal("100"));
        budget.setCategory(category);
        budget.setUser(SecurityUtil.getUser(userRepository));
        budget.setStartDate(LocalDate.of(2025,1,1));
        budget.setEndDate(LocalDate.of(2025,1,31));
        
    }
    
    @AfterEach
    void tearDown(){
        securityUtilMock.close();
    }
    
    @Test
    void getTotalSpentOnBudget_notFound_throws404() {
    // findById returns Optional.empty()
    assertThatThrownBy(() -> budgetService.getTotalSpentOnBudget(5L))
      .isInstanceOf(ResponseStatusException.class)
      .satisfies(ex -> {
        ResponseStatusException rse = (ResponseStatusException)ex;
        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rse.getReason()).isEqualTo("Budget not found");
      });
  }
    
    @Test
    void getTotalSpentOnBudget_happyPath_delegatesAndReturns() {
    // override the findById stub
    when(budgetRepository.findById(5L))
      .thenReturn(Optional.of(budget));
    
    // stub account IDs
    stubAccountIds = List.of(10L,11L);
    when(accountUtil.getUserAccountIds())
      .thenReturn(stubAccountIds);

    // prepare the repository’s sum
    BigDecimal expected = new BigDecimal("1234.56");
    when(budgetRepository.getTotalSpentOnBudget(
           eq(budget.getCategory().getId()),
           eq(budget.getStartDate()),
           eq(budget.getEndDate()),
           eq(stubAccountIds)))
      .thenReturn(expected);

    // call
    BigDecimal actual = budgetService.getTotalSpentOnBudget(5L);

    // verify delegation
    verify(accountUtil).getUserAccountIds();
    verify(budgetRepository).getTotalSpentOnBudget(
      budget.getCategory().getId(),
      budget.getStartDate(),
      budget.getEndDate(),
      stubAccountIds
    );

    // assert return value
    assertThat(actual).isSameAs(expected);
  }
  
    @Test
    void saveBudget_categoryNotFound_throws404() {
    // given
    BudgetCreationRequest request = new BudgetCreationRequest(
        new BigDecimal("100.00"),
        LocalDate.of(2025, 6, 1),
        LocalDate.of(2025, 6, 30),
        123L);
    when(categoryRepository.findById(123L))
        .thenReturn(Optional.empty());

    // expect
    assertThatThrownBy(() -> budgetService.saveBudget(request))
      .isInstanceOf(ResponseStatusException.class)
      .satisfies(ex -> {
        ResponseStatusException rse = (ResponseStatusException) ex;
        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rse.getReason()).isEqualTo("Category not found");
      });

    // and budgetRepository.save should never be called
    verify(budgetRepository, never()).save(any());
    }
    
    @Test
void saveBudget_happyPath_persistsAndReturnsBudget() {
    // given
    BudgetCreationRequest request = new BudgetCreationRequest(
        new BigDecimal("250.00"),
        LocalDate.of(2025, 7, 1),
        LocalDate.of(2025, 7, 31),
        category.getId()
    );

    // stub category lookup
    when(categoryRepository.findById(category.getId()))
        .thenReturn(Optional.of(category));

    // stub save to echo back and assign an ID
    when(budgetRepository.save(any(Budget.class)))
        .thenAnswer(inv -> {
          Budget b = inv.getArgument(0);
          b.setId(77L);
          return b;
        });

    // act
    Budget saved = budgetService.saveBudget(request);

    // assert: repository was called with a Budget matching the request
    ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
    verify(budgetRepository).save(captor.capture());
    Budget toSave = captor.getValue();
    assertThat(toSave.getAmount()).isEqualByComparingTo("250.00");
    assertThat(toSave.getStartDate()).isEqualTo(request.getStartDate());
    assertThat(toSave.getEndDate()).isEqualTo(request.getEndDate());
    assertThat(toSave.getCategory()).isSameAs(category);
    assertThat(toSave.getUser()).isSameAs(stubUser);

    // assert returned object has the stub ID
    assertThat(saved.getId()).isEqualTo(77L);
    assertThat(saved).isSameAs(toSave);
    }

    @Test
    void deleteBudgetForUser_notFound_throws404() {
    // given
    Long missingId = 99L;
    // budgetRepository.findById(missingId) returns Optional.empty() by default

    // when / then
    assertThatThrownBy(() -> budgetService.deleteBudgetForUser(missingId))
      .isInstanceOf(ResponseStatusException.class)
      .satisfies(ex -> {
        ResponseStatusException rse = (ResponseStatusException) ex;
        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rse.getReason()).isEqualTo("Budget not found");
      });

    // and delete should never be called
    verify(budgetRepository, never()).delete(any(Budget.class));
    }

    @Test
    void deleteBudgetForUser_happyPath_deletesAndReturnsTrue() {
    // given
    Long id = budget.getId();  // 5L from your @BeforeEach
    when(budgetRepository.findById(id))
      .thenReturn(Optional.of(budget));

    // when
    boolean result = budgetService.deleteBudgetForUser(id);

    // then
    assertThat(result).isTrue();
    verify(budgetRepository).delete(budget);
    }
    
    @Test
    void updateBudgetAmount_notFound_throws404() {
    Long missingId = 123L;
    // findById(anyLong()) returns Optional.empty() by default

    assertThatThrownBy(() -> budgetService.updateBudgetAmount(missingId,
            new UpdateBudgetAmountDto(new BigDecimal("500.00"))))
      .isInstanceOf(ResponseStatusException.class)
      .satisfies(ex -> {
        ResponseStatusException rse = (ResponseStatusException) ex;
        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rse.getReason()).isEqualTo("Budget not found");
      });

    verify(budgetRepository, never()).save(any(Budget.class));
}

    @Test
    void updateBudgetAmount_happyPath_savesNewAmount() {
    Long id = budget.getId();  // from your @BeforeEach
    BigDecimal newAmt = new BigDecimal("250.00");
    UpdateBudgetAmountDto req = new UpdateBudgetAmountDto(newAmt);

    // stub findById to return the existing budget
    when(budgetRepository.findById(id)).thenReturn(Optional.of(budget));

    // stub save to return its argument
    when(budgetRepository.save(any(Budget.class)))
      .thenAnswer(inv -> inv.getArgument(0));

    // act
    Budget updated = budgetService.updateBudgetAmount(id, req);

    // assert that the returned budget has its amount updated
    assertThat(updated.getAmount()).isEqualByComparingTo(newAmt);

    // verify save was called with the budget whose amount was changed
    ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
    verify(budgetRepository).save(captor.capture());
    Budget savedArg = captor.getValue();
    assertThat(savedArg.getId()).isEqualTo(id);
    assertThat(savedArg.getAmount()).isEqualByComparingTo(newAmt);
    }
    
    @Test
    void updateMonthlyStatus_notFound_throws404() {
    Long missingId = 999L;
    // budgetRepository.findById(missingId) returns Optional.empty() by default

    assertThatThrownBy(() -> budgetService.updateMonthlyStatus(missingId, true))
      .isInstanceOf(ResponseStatusException.class)
      .satisfies(ex -> {
        ResponseStatusException rse = (ResponseStatusException) ex;
        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rse.getReason()).isEqualTo("Budget not found");
      });

    verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void updateMonthlyStatus_happyPath_savesAndReturnsUpdatedBudget() {
    Long id = budget.getId();  // 5L from @BeforeEach
    // given an existing budget
    when(budgetRepository.findById(id)).thenReturn(Optional.of(budget));
    // stub save to return the same instance
    when(budgetRepository.save(any(Budget.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    // when
    Budget result = budgetService.updateMonthlyStatus(id, true);

    // then
    // • the returned budget is the same instance
    assertThat(result).isSameAs(budget);
    // • its monthly flag was updated
    assertThat(result.getMonthly()).isTrue();

    // • and save was called with that budget
    ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
    verify(budgetRepository).save(captor.capture());
    Budget saved = captor.getValue();
    assertThat(saved.getId()).isEqualTo(id);
    assertThat(saved.getMonthly()).isTrue();
    }
    
    @Test
    void createNextMonthBudgets_noCurrentBudgets_doesNothing() {
    // today
    LocalDate today = LocalDate.now();

    // stub no active monthly budgets
    when(budgetRepository
      .findByMonthlyTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(today, today))
    .thenReturn(Collections.emptyList());

    // act
    budgetService.createNextMonthBudgets();

    // verify no save
    verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void createNextMonthBudgets_skipsAlreadyCreatedNextMonth() {
    LocalDate today = LocalDate.now();
    LocalDate endOfCurrent = today.with(TemporalAdjusters.lastDayOfMonth());
    LocalDate startOfNext   = endOfCurrent.plusDays(1);

    // make one “current” budget
    Budget current = new Budget(
    BigDecimal.valueOf(100),
    today.minusDays(5),
    today.plusDays(5),
    category,
    stubUser
    );
    current.setMonthly(true);

    when(budgetRepository
      .findByMonthlyTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(today, today))
    .thenReturn(List.of(current));

    // stub "exists" to say the next‐month budget already exists
    when(budgetRepository
      .existsByUserAndCategoryAndStartDateAndMonthlyTrue(
        stubUser,
        category,
        startOfNext))
    .thenReturn(true);

    // act
    budgetService.createNextMonthBudgets();

    // verify we never saved anything
    verify(budgetRepository, never()).save(any(Budget.class));
    }   

    @Test
    void createNextMonthBudgets_createsMissingNextMonthBudgets() {
    LocalDate today = LocalDate.now();
    LocalDate endOfCurrent = today.with(TemporalAdjusters.lastDayOfMonth());
    LocalDate startOfNext   = endOfCurrent.plusDays(1);
    LocalDate endOfNext     = startOfNext.with(TemporalAdjusters.lastDayOfMonth());

    // one “current” budget
    Budget current = new Budget(
    BigDecimal.valueOf(200),
    today.minusDays(3),
    today.plusDays(10),
    category,
    stubUser
    );
    current.setMonthly(true);

    when(budgetRepository
      .findByMonthlyTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(today, today))
    .thenReturn(List.of(current));

    // stub “exists” => missing
    when(budgetRepository
      .existsByUserAndCategoryAndStartDateAndMonthlyTrue(
        stubUser,
        category,
        startOfNext))
    .thenReturn(false);

    // capture the save()
    ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);

    // act
    budgetService.createNextMonthBudgets();

    // assert
    verify(budgetRepository).save(captor.capture());
    Budget next = captor.getValue();

    assertThat(next.getAmount()).isEqualByComparingTo("200");
    assertThat(next.getCategory()).isSameAs(category);
    assertThat(next.getUser()).isSameAs(stubUser);
    assertThat(next.getMonthly()).isTrue();

    // dates correct:
    assertThat(next.getStartDate()).isEqualTo(startOfNext);
    assertThat(next.getEndDate()).isEqualTo(endOfNext);
}




}
