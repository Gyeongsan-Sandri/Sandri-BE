package sandri.sandriweb.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import sandri.sandriweb.domain.user.repository.UserRepository;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final UserRepository userRepository;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authenticationProvider);
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
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/api/common/**").permitAll()
                        // 리뷰 작성은 인증 필요 (더 구체적인 패턴을 먼저 선언)
                        .requestMatchers(HttpMethod.POST, "/api/places/*/reviews").authenticated()
                        .requestMatchers("/api/places/**").permitAll() // 나머지 장소 관련 API는 공개
                        .requestMatchers("/api/reviews/**").permitAll()
                        .requestMatchers("/api/advertise/**").permitAll() // 광고 조회는 인증 없이 가능
                        .requestMatchers("/api/magazines/**").permitAll() // 매거진 조회는 인증 없이 가능
                        .requestMatchers("/api/admin/**").permitAll() // 관리자 API는 인증 없이 가능
                        // 루트 관련 API - 공유 링크는 인증 없이 가능 (더 구체적인 패턴을 먼저 선언)
                        .requestMatchers("/api/routes/share/**").permitAll() // 공유 링크는 인증 없이 가능
                        .requestMatchers("/api/routes/search").permitAll() // 루트 검색은 인증 없이 가능
                        .requestMatchers("/api/routes/**").authenticated() // 나머지 루트 관련 API는 인증 필요
                        // 검색 관련 API
                        .requestMatchers("/api/popular-searches").permitAll() // 인기 검색어는 인증 없이 가능
                        .requestMatchers("/api/categories").permitAll() // 카테고리 목록은 인증 없이 가능
                        // 사용자 관련 API - 닉네임으로 프로필 조회는 인증 없이 가능 (더 구체적인 패턴을 먼저 선언)
                        .requestMatchers("/api/user/profile/*").permitAll() // 닉네임으로 프로필 조회는 인증 없이 가능
                        .requestMatchers("/api/user/profile").authenticated() // 현재 사용자 프로필 조회는 인증 필요
                        .requestMatchers("/api/user/**").authenticated() // 나머지 사용자 관련 API는 인증 필요
                        .requestMatchers("/api/me/**").authenticated() // 마이페이지 관련 API는 인증 필요
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
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

        // 허용할 Origin 명시적으로 등록
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",      // 로컬 프론트
                "https://localhost:5173",     // 로컬 프론트 (HTTPS)
                "http://13.125.26.64:8080",   // Swagger 테스트
                "https://sandri.site",        // 실제 배포
                "https://www.sandri.site",        // 실제 배포
                "https://api.sandri.site"     // 최종 배포 서버 (Swagger)
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("*"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("JSESSIONID");
        serializer.setSameSite("None");
        serializer.setUseSecureCookie(true); // HTTPS 환경에서만 쿠키 전송
        serializer.setUseHttpOnlyCookie(true); // XSS 공격 방지
        return serializer;
    }
}
