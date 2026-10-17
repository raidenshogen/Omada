package com.omada.realtime.config;
import com.omada.realtime.security.JwtAuthenticationFilter;
import com.omada.realtime.security.RateLimitingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;
/**
 * Spring Security configuration with CORS for localhost:5173
 * and JWT filter chain. @EnableMethodSecurity turns on @PreAuthorize
 * enforcement (without it the annotations are silently ignored).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter;
    private final RateLimitingFilter rateLimitingFilter;
    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          RateLimitingFilter rateLimitingFilter){
        this.jwtFilter = jwtFilter;
        this.rateLimitingFilter = rateLimitingFilter;
    }

    @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**","/ws/**","/actuator/health","/v3/api-docs/**","/swagger-ui/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(rateLimitingFilter, JwtAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
