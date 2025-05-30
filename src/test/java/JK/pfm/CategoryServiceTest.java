
package JK.pfm;

import JK.pfm.model.Category;
import JK.pfm.model.User;
import JK.pfm.model.UserCategoryPreference;
import JK.pfm.repository.CategoryRepository;
import JK.pfm.repository.UserCategoryPreferenceRepository;
import JK.pfm.repository.UserRepository;
import JK.pfm.service.CategoryService;
import JK.pfm.util.SecurityUtil;
import java.util.HashSet;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CategoryServiceTest {

    @Mock CategoryRepository                   categoryRepository;
    @Mock UserRepository                       userRepository;
    @Mock UserCategoryPreferenceRepository     userCategoryPreferenceRepository;
    @InjectMocks CategoryService               categoryService;

    private MockedStatic<SecurityUtil>         securityUtilMock;
    private User                               stubUser;

    @BeforeEach
    void setUp() {
      // static SecurityUtil
      securityUtilMock = Mockito.mockStatic(SecurityUtil.class);
      stubUser = new User();
      stubUser.setId(1L);
      stubUser.setCategoryPreferences(new HashSet<>());

      securityUtilMock
        .when(() -> SecurityUtil.getUser(userRepository))
        .thenReturn(stubUser);
      securityUtilMock
        .when(SecurityUtil::getUserId)
        .thenReturn(1L);

      // By default no existing preference
      Mockito.lenient().when(
        userCategoryPreferenceRepository
          .existsByUserAndCategory_NameIgnoreCase(any(), anyString())
      ).thenReturn(false);

      // Echo back saves
      Mockito.lenient().when(categoryRepository.save(any(Category.class)))
        .thenAnswer(inv -> inv.getArgument(0));
      Mockito.lenient().when(userRepository.save(any(User.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
      securityUtilMock.close();
    }

    // --- saveCategory(...) tests ---

    @Test
    void saveCategory_reservedName_throwsConflict() {
      assertThatThrownBy(() -> categoryService.saveCategory("Savings"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
          var rse = (ResponseStatusException) ex;
          assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
          assertThat(rse.getReason()).isEqualTo("Choose different name");
        });

      verify(categoryRepository, never()).save(any());
      verify(userRepository, never()).save(any());
    }

    @Test
    void saveCategory_alreadyExists_throwsConflict() {
      String name = "Groceries";
      when(userCategoryPreferenceRepository
            .existsByUserAndCategory_NameIgnoreCase(stubUser, name))
        .thenReturn(true);

      assertThatThrownBy(() -> categoryService.saveCategory(name))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
          var rse = (ResponseStatusException) ex;
          assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
          assertThat(rse.getReason()).isEqualTo("Category already exists");
        });

      verify(categoryRepository, never()).save(any());
      verify(userRepository, never()).save(any());
    }

    @Test
    void saveCategory_happyPath_savesCategoryAndUserPref() {
      String name = "Travel";
      // ensure no conflict
      when(userCategoryPreferenceRepository
            .existsByUserAndCategory_NameIgnoreCase(stubUser, name))
        .thenReturn(false);

      Category created = categoryService.saveCategory(name);

      // saved category has correct name
      assertThat(created.getName()).isEqualTo(name);

      // verify repository interactions
      verify(categoryRepository).save(argThat(c -> c.getName().equals(name)));
      verify(userRepository).save(stubUser);

      // and the stubUser now has exactly one preference pointing at that category
      assertThat(stubUser.getCategoryPreferences())
        .extracting("category")
        .containsExactly(created);
    }

    // --- updateCategoryVisibility(...) tests ---

    @Test
    void updateCategoryVisibility_notFound_throws404() {
      Long catId = 77L;
      when(userCategoryPreferenceRepository
            .findByUserIdAndCategoryId(1L, catId))
        .thenReturn(Optional.empty());

      assertThatThrownBy(() -> categoryService.updateCategoryVisibility(catId, false))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
          var rse = (ResponseStatusException) ex;
          assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
          assertThat(rse.getReason()).isEqualTo("Category not found");
        });

      verify(userCategoryPreferenceRepository, never()).save(any());
    }

    @Test
    void updateCategoryVisibility_happyPath_updatesAndSavesPref() {
      Long catId = 42L;
      UserCategoryPreference pref = new UserCategoryPreference(stubUser, new Category("X"));
      pref.setActive(false);

      when(userCategoryPreferenceRepository
            .findByUserIdAndCategoryId(1L, catId))
        .thenReturn(Optional.of(pref));
      when(userCategoryPreferenceRepository.save(any(UserCategoryPreference.class)))
        .thenAnswer(inv -> inv.getArgument(0));

      // Act: switch it on
      categoryService.updateCategoryVisibility(catId, true);

      // Assert: it updated the flag
      assertThat(pref.getActive()).isTrue();
      // ...and saved the updated preference
      verify(userCategoryPreferenceRepository).save(pref);
    }
}
