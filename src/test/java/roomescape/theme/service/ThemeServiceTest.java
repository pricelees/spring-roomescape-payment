package roomescape.theme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;
import roomescape.system.exception.RoomEscapeException;
import roomescape.theme.domain.Theme;
import roomescape.theme.domain.repository.ThemeRepository;
import roomescape.theme.dto.ThemeRequest;
import roomescape.theme.dto.ThemeResponse;
import roomescape.theme.dto.ThemesResponse;

@DataJpaTest
@Sql(scripts = "/truncate.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ThemeServiceTest {

    @Autowired
    private ThemeRepository themeRepository;

    private ThemeService themeService;

    @BeforeEach
    void setUp() {
        themeService = new ThemeService(themeRepository);
    }

    @Test
    @DisplayName("테마를 조회한다.")
    void findThemeById() {
        // given
        Theme theme = themeRepository.save(new Theme("name", "description", "thumbnail"));

        // when
        Theme foundTheme = themeService.findThemeById(theme.getId());

        // then
        assertThat(foundTheme).isEqualTo(theme);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 테마를 조회하면 예외가 발생한다.")
    void findThemeByNotExistId() {
        // given
        Theme theme = themeRepository.save(new Theme("name", "description", "thumbnail"));

        // when
        Long notExistId = theme.getId() + 1;

        // then
        assertThatThrownBy(() -> themeService.findThemeById(notExistId))
                .isInstanceOf(RoomEscapeException.class);
    }

    @Test
    @DisplayName("모든 테마를 조회한다.")
    void findAllThemes() {
        // given
        Theme theme = themeRepository.save(new Theme("name", "description", "thumbnail"));
        Theme theme1 = themeRepository.save(new Theme("name1", "description1", "thumbnail1"));

        // when
        ThemesResponse found = themeService.findAllThemes();

        // then
        assertThat(found.themes()).extracting("id").containsExactly(theme.getId(), theme1.getId());
    }

    @Test
    @DisplayName("예약 수 상위 10개 테마를 조회했을 때 내림차순으로 정렬된다. 만약 예약 수가 같다면, id 순으로 오름차순 정렬된다.")
    @Sql({"/truncate.sql", "/reservationData.sql"})
    void getTop10Themes() {
        // given
        LocalDate today = LocalDate.now();

        // when
        List<ThemeResponse> found = themeService.getTop10Themes(today).themes();

        // then : 11번 테마는 조회되지 않아야 한다.
        assertThat(found).extracting("id").containsExactly(1L, 4L, 2L, 6L, 3L, 5L, 7L, 8L, 9L, 10L);
    }

    @Test
    @DisplayName("테마를 추가한다.")
    void addTheme() {
        // given
        ThemeResponse themeResponse = themeService.addTheme(new ThemeRequest("name", "description", "thumbnail"));

        // when
        Theme found = themeRepository.findById(themeResponse.id()).orElse(null);

        // then
        assertThat(found).isNotNull();
    }

    @Test
    @DisplayName("테마를 추가할 때 같은 이름의 테마가 존재하면 예외가 발생한다. ")
    void addDuplicateTheme() {
        // given
        ThemeResponse themeResponse = themeService.addTheme(new ThemeRequest("name", "description", "thumbnail"));

        // when
        ThemeRequest invalidRequest = new ThemeRequest(themeResponse.name(), "description", "thumbnail");

        // then
        assertThatThrownBy(() -> themeService.addTheme(invalidRequest))
                .isInstanceOf(RoomEscapeException.class);
    }

    @Test
    @DisplayName("테마를 삭제한다.")
    void removeThemeById() {
        // given
        Theme theme = themeRepository.save(new Theme("name", "description", "thumbnail"));

        // when
        themeService.removeThemeById(theme.getId());

        // then
        assertThat(themeRepository.findById(theme.getId())).isEmpty();
    }
}
