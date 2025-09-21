package com.library.config;

import com.library.service.JwtService;
import com.library.service.UserDetailsServiceImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.mockito.Mockito.mock;

@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // 測試時需要的公開端點
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/books/search").permitAll()
                .requestMatchers("/api/books/{id}").permitAll()
                .requestMatchers("/api/books").permitAll() // 測試時允許，實際權限由 @PreAuthorize 控制
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    public JwtService jwtService() {
        return mock(JwtService.class);
    }

    @Bean
    public UserDetailsServiceImpl userDetailsService() {
        return mock(UserDetailsServiceImpl.class);
    }
}