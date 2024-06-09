package roomescape.system.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(apiInfo());
    }

    private Info apiInfo() {
        return new Info()
                .title("방탈출 예약 API 문서")
                .description("""
                        ## API 테스트는 '1. 인증 / 인가 API' 의 '/login' 을 통해 로그인 후 사용해주세요.
                        
                        ### 테스트 계정 정보
                        - **관리자**:
                            - **email**: a@a.a
                        
                            - **password**: a
                        
                        - **회원**:
                            - **1번부터 4번까지의 회원이 존재**합니다.
                        
                            - **email**: 1@1.1, 2@2.2, 3@3.3, 4@4.4
                        
                            - **password**: 순서대로 1, 2, 3, 4
                        
                        ### 테스트시 사용할 수 있는 파라미터 정보
                        - **themeId**: 1(테스트1), 2(테스트2), 3(테스트3), 4(테스트4)
                        
                        - **timeId**: 1(15:00), 2(16:00), 3(17:00), 4(18:00)
                        
                        - **memberId**: 1(어드민), 2(회원1), 3(회원2), 4(회원3), 5(회원4)
                        
                        - **reservationId**:
                            - 1 ~ 6: 예약 및 결제 완료 상태
                        
                            - 7: 예약은 승인되었으나, 결제 대기 상태
                        
                            - 8 ~ 10: 예약 대기 상태
                        """)
                .version("1.0.0");
    }
}