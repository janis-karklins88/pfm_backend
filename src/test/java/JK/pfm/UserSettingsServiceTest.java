package JK.pfm;

import JK.pfm.dto.UserSettingsDto;
import JK.pfm.model.User;
import JK.pfm.model.UserSettings;
import JK.pfm.repository.UserSettingsRepository;
import JK.pfm.service.UserSettingsService;
import JK.pfm.util.SecurityUtil;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.mockito.quality.Strictness;
import JK.pfm.repository.UserRepository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserSettingsServiceTest {

    @Mock
    UserSettingsRepository userSettingsRepository;
    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserSettingsService userSettingsService;

    private MockedStatic<SecurityUtil> securityUtilMock;
    private User stubUser;

    @BeforeEach
    void setUp() {
        stubUser = new User();
        // open static mock
        securityUtilMock = Mockito.mockStatic(SecurityUtil.class);
        // default userId stub
        securityUtilMock.when(SecurityUtil::getUserId).thenReturn(123L);
        securityUtilMock.when(() -> SecurityUtil.getUser(userRepository)).thenReturn(stubUser);
        // default “not found”
        when(userSettingsRepository.findById(anyLong()))
            .thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    // --- getUserSettings() ---

    @Test
    void getUserSettings_notFound_throws404() {
        assertThatThrownBy(() -> userSettingsService.getUserSettings())
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> {
                ResponseStatusException rse = (ResponseStatusException) ex;
                assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(rse.getReason()).isEqualTo("User settings not found");
            });

        verify(userSettingsRepository, never()).save(any());
    }

    @Test
    void getUserSettings_happyPath_returnsDto() {
        // given
        UserSettings us = new UserSettings(SecurityUtil.getUser(userRepository), "EUR");
        when(userSettingsRepository.findById(123L))
            .thenReturn(Optional.of(us));

        // when
        UserSettingsDto dto = userSettingsService.getUserSettings();

        // then
        assertThat(dto.getCurrency()).isEqualTo("EUR");
        verify(userSettingsRepository).findById(123L);
    }

    // --- setUserCurrency() ---

    @Test
    void setUserCurrency_notFound_throws404() {
        assertThatThrownBy(() -> userSettingsService.setUserCurrency("USD"))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> {
                ResponseStatusException rse = (ResponseStatusException) ex;
                assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(rse.getReason()).isEqualTo("User settings not found");
            });

        verify(userSettingsRepository, never()).save(any());
    }

    @Test
    void setUserCurrency_happyPath_updatesAndSaves() {
        // given
        UserSettings us = new UserSettings(SecurityUtil.getUser(userRepository), "EUR");
        when(userSettingsRepository.findById(123L))
            .thenReturn(Optional.of(us));
        // capture save
        when(userSettingsRepository.save(any(UserSettings.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        // when
        userSettingsService.setUserCurrency("USD");

        // then
        assertThat(us.getCurrency()).isEqualTo("USD");
        verify(userSettingsRepository).save(us);
    }
}
