package sandri.sandriweb.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI openAPI() {
        SecurityScheme cookieAuth = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name("JSESSIONID")
                .description("세션 쿠키 (로그인 후 자동 설정됨)");
        
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("cookieAuth");
        
        return new OpenAPI()
                .info(new Info()
                        .title("Sandri API")
                        .description("함께 하는 경산 루트 API 문서")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Sandri Team")
                                .email("sandri@example.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("개발 서버"),
                        new Server()
                                .url("http://13.125.26.64:8080")
                                .description("배포 서버 (현재)"),
                        new Server()
                                .url("https://sandri.site")
                                .description("운영 서버 (도메인 적용 후)")
                ))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("cookieAuth", cookieAuth))
                .addSecurityItem(securityRequirement);
    }
}
