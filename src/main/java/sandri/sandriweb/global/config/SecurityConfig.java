package sandri.sandriweb.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // OPTIONS 요청은 CORS preflight를 위해 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Swagger UI 경로 허용
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-resources/**", "/webjars/**").permitAll()
                        // 인증 관련 API는 모두 허용
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/", "/api/user/profile/**").permitAll()
                        .requestMatchers("/api/common/**").permitAll()
                        // 리뷰 작성은 인증 필요 (더 구체적인 패턴을 먼저 선언)
                        .requestMatchers(HttpMethod.POST, "/api/places/*/reviews").authenticated()
                        .requestMatchers("/api/places/**").permitAll() // 나머지 장소 관련 API는 공개
                        .requestMatchers("/api/reviews/**").permitAll()
                        .requestMatchers("/api/advertise/**").permitAll() // 광고 조회는 인증 없이 가능
                        .requestMatchers("/api/magazines/**").permitAll() // 매거진 조회는 인증 없이 가능
                        .requestMatchers("/api/admin/**").permitAll() // 관리자 API는 인증 없이 가능
                        .requestMatchers("/api/me/**").authenticated() // 마이페이지 관련 API는 인증 필요
                        .requestMatchers("/api/routes/share/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 명시적으로 허용할 origin 목록 (배포 환경 및 개발 환경 모두 고려)
        configuration.setAllowedOrigins(List.of(
                "http://13.125.26.64:8080",         // 현재 배포 서버 (IP 주소)
                "https://api.sandri.com",           // 운영 서버 (도메인 적용 후)
                "http://localhost:8080",            // 로컬 개발 서버
                "http://127.0.0.1:8080",            // 로컬 개발 서버 (대체)
                "http://localhost:3000",            // 프론트엔드 개발 서버 (필요시)
                "https://sandri.com",               // 프론트엔드 도메인 (필요시)
                "https://www.sandri.com"            // 프론트엔드 도메인 (필요시)
        ));
        
        // 모든 HTTP 메서드 허용
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // 모든 헤더 허용
        configuration.setAllowedHeaders(List.of("*"));
        
        // 인증 정보 포함 허용 (필요한 경우)
        configuration.setAllowCredentials(true);
        
        // 응답 헤더 노출
        configuration.setExposedHeaders(List.of("*"));
        
        // preflight 요청 캐시 시간 (1시간)
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
