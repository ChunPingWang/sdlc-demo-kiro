package com.example.lifepremium.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Security 設定
 * JWT 由 API Gateway 驗證後，以 X-Agent-Id / X-Roles Header 傳入。
 * 本服務信任 Header，轉換為 Spring Security Authentication。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 保費試算允許匿名
                .requestMatchers(HttpMethod.POST, "/api/v1/premium/calculate").permitAll()
                // Actuator health
                .requestMatchers("/actuator/health").permitAll()
                // OpenAPI docs
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // 其餘需認證
                .anyRequest().authenticated()
            )
            .addFilterBefore(headerAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 從 X-Roles Header 建立 Spring Security Authentication。
     * 格式：X-Roles: ROLE_AGENT,ROLE_ADMIN
     */
    @Bean
    public OncePerRequestFilter headerAuthFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain) throws ServletException, IOException {

                String rolesHeader = request.getHeader("X-Roles");
                if (rolesHeader != null && !rolesHeader.isBlank()) {
                    List<SimpleGrantedAuthority> authorities = Arrays
                        .stream(rolesHeader.split(","))
                        .map(String::trim)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                    String agentId = request.getHeader("X-Agent-Id");
                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(agentId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
                filterChain.doFilter(request, response);
            }
        };
    }
}
