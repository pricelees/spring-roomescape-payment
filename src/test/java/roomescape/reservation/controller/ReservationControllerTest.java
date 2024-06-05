package roomescape.reservation.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import roomescape.member.domain.Member;
import roomescape.member.domain.Role;
import roomescape.member.domain.repository.MemberRepository;
import roomescape.payment.PaymentRequest;
import roomescape.payment.PaymentResponse;
import roomescape.payment.TossPaymentClient;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.domain.ReservationTime;
import roomescape.reservation.domain.repository.ReservationRepository;
import roomescape.reservation.domain.repository.ReservationTimeRepository;
import roomescape.theme.domain.Theme;
import roomescape.theme.domain.repository.ThemeRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/truncate.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
public class ReservationControllerTest {

    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private ReservationTimeRepository reservationTimeRepository;
    @Autowired
    private ThemeRepository themeRepository;
    @Autowired
    private MemberRepository memberRepository;

    @MockBean
    private TossPaymentClient tossPaymentClient;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("처음으로 등록하는 예약의 id는 1이다.")
    void firstPost() {
        String accessTokenCookie = getAdminAccessTokenCookieByLogin("admin@admin.com", "12341234");

        reservationTimeRepository.save(new ReservationTime(LocalTime.of(17, 30)));
        themeRepository.save(new Theme("테마명", "설명", "썸네일URL"));

        Map<String, String> reservationParams = Map.of(
                "name", "썬",
                "date", LocalDate.now().plusDays(1L).toString(),
                "timeId", "1",
                "themeId", "1"
        );

        when(tossPaymentClient.confirmPayment(any(PaymentRequest.class))).thenReturn(
                new PaymentResponse("pk", "oi", 1000L));

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .port(port)
                .header("Cookie", accessTokenCookie)
                .body(reservationParams)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(201)
                .body("data.id", is(1))
                .header("Location", "/reservations/1");
    }

    @Test
    @DisplayName("관리자 권한이 있으면 전체 예약정보를 조회할 수 있다.")
    void readEmptyReservations() {
        // given
        String accessTokenCookie = getAdminAccessTokenCookieByLogin("admin@admin.com", "12341234");

        ReservationTime reservationTime = reservationTimeRepository.save(new ReservationTime(LocalTime.of(17, 30)));
        Theme theme = themeRepository.save(new Theme("테마명", "설명", "썸네일URL"));
        Member member = memberRepository.save(new Member("name", "email@email.com", "password", Role.MEMBER));

        // when
        reservationRepository.save(new Reservation(LocalDate.now(), reservationTime, theme, member));
        reservationRepository.save(new Reservation(LocalDate.now().plusDays(1), reservationTime, theme, member));
        reservationRepository.save(new Reservation(LocalDate.now().plusDays(2), reservationTime, theme, member));

        // then
        RestAssured.given().log().all()
                .port(port)
                .header(new Header("Cookie", accessTokenCookie))
                .when().get("/reservations")
                .then().log().all()
                .statusCode(200)
                .body("data.reservations.size()", is(3));
    }

    @Test
    @DisplayName("본인의 예약 정보를 삭제할 수 있다.")
    void canRemoveMyReservation() {
        // given
        Member member = memberRepository.save(new Member("name", "email@email.com", "password", Role.MEMBER));
        String accessTokenCookie = getAccessTokenCookieByLogin(member.getEmail(), member.getPassword());

        ReservationTime reservationTime = reservationTimeRepository.save(new ReservationTime(LocalTime.of(17, 30)));
        Theme theme = themeRepository.save(new Theme("테마명", "설명", "썸네일URL"));
        Reservation reservation = reservationRepository.save(
                new Reservation(LocalDate.now(), reservationTime, theme, member));

        // when & then
        RestAssured.given().log().all()
                .port(port)
                .header("Cookie", accessTokenCookie)
                .when().delete("/reservations/" + reservation.getId())
                .then().log().all()
                .statusCode(204);
    }

    @Test
    @DisplayName("본인의 예약이 아니면 예약 정보를 삭제할 수 없으며 403 Forbidden 을 Response 받는다.")
    void canRemoveAnotherReservation() {
        // given
        Member member = memberRepository.save(new Member("name", "member1@email.com", "password", Role.MEMBER));
        String accessTokenCookie = getAccessTokenCookieByLogin(member.getEmail(), member.getPassword());

        Member anotherMember = memberRepository.save(new Member("name1", "member2@email.com", "password", Role.MEMBER));
        ReservationTime reservationTime = reservationTimeRepository.save(new ReservationTime(LocalTime.of(17, 30)));
        Theme theme = themeRepository.save(new Theme("테마명", "설명", "썸네일URL"));

        Reservation reservation = reservationRepository.save(
                new Reservation(LocalDate.now(), reservationTime, theme, anotherMember));

        // when & then
        RestAssured.given().log().all()
                .port(port)
                .header("Cookie", accessTokenCookie)
                .when().delete("/reservations/" + reservation.getId())
                .then().log().all()
                .statusCode(403);
    }

    @Test
    @DisplayName("본인의 예약이 아니더라도 관리자 권한이 있으면 예약 정보를 삭제할 수 있다.")
    void readReservationsSizeAfterPostAndDelete() {
        // given
        Member member = memberRepository.save(new Member("name", "admin@admin.com", "password", Role.ADMIN));
        String accessTokenCookie = getAccessTokenCookieByLogin(member.getEmail(), member.getPassword());

        ReservationTime reservationTime = reservationTimeRepository.save(new ReservationTime(LocalTime.of(17, 30)));
        Theme theme = themeRepository.save(new Theme("테마명", "설명", "썸네일URL"));
        Member anotherMember = memberRepository.save(new Member("name", "email@email.com", "password", Role.MEMBER));

        Reservation reservation = reservationRepository.save(
                new Reservation(LocalDate.now(), reservationTime, theme, anotherMember));

        // when & then
        RestAssured.given().log().all()
                .port(port)
                .header("Cookie", accessTokenCookie)
                .when().delete("/reservations/" + reservation.getId())
                .then().log().all()
                .statusCode(204);
    }

    @ParameterizedTest
    @MethodSource("requestValidateSource")
    @DisplayName("예약 생성 시, 요청 값에 공백 또는 null이 포함되어 있으면 400 에러를 발생한다.")
    void validateBlankRequest(Map<String, String> invalidRequestBody) {
        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .port(port)
                .body(invalidRequestBody)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(400);
    }

    private static Stream<Map<String, String>> requestValidateSource() {
        return Stream.of(
                Map.of("timeId", "1",
                        "themeId", "1"),

                Map.of("date", LocalDate.now().plusDays(1L).toString(),
                        "themeId", "1"),

                Map.of("date", LocalDate.now().plusDays(1L).toString(),
                        "timeId", "1"),

                Map.of("date", " ",
                        "timeId", "1",
                        "themeId", "1"),

                Map.of("date", LocalDate.now().plusDays(1L).toString(),
                        "timeId", " ",
                        "themeId", "1"),

                Map.of("date", LocalDate.now().plusDays(1L).toString(),
                        "timeId", "1",
                        "themeId", " ")
        );
    }

    @Test
    @DisplayName("예약 생성 시, 정수 요청 데이터에 문자가 입력되어오면 400 에러를 발생한다.")
    void validateRequestDataFormat() {
        Map<String, String> invalidTypeRequestBody = Map.of(
                "date", LocalDate.now().plusDays(1L).toString(),
                "timeId", "1",
                "themeId", "한글"
        );

        RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .port(port)
                .body(invalidTypeRequestBody)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(400);
    }

    private String getAdminAccessTokenCookieByLogin(final String email, final String password) {
        memberRepository.save(new Member("이름", email, password, Role.ADMIN));

        Map<String, String> loginParams = Map.of(
                "email", email,
                "password", password
        );

        String accessToken = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .port(port)
                .body(loginParams)
                .when().post("/login")
                .then().log().all().extract().cookie("accessToken");

        return "accessToken=" + accessToken;
    }

    private String getAccessTokenCookieByLogin(final String email, final String password) {
        Map<String, String> loginParams = Map.of(
                "email", email,
                "password", password
        );

        String accessToken = RestAssured.given().log().all()
                .contentType(ContentType.JSON)
                .port(port)
                .body(loginParams)
                .when().post("/login")
                .then().log().all().extract().cookie("accessToken");

        return "accessToken=" + accessToken;
    }
}
