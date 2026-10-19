package com.practice.remopl.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        // 실제 오류가 인증 오류로 가려지지 않도록 허용
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .anyRequest().authenticated()
                )
                // 회원가입 API의 Postman 테스트를 위한 임시 예외
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(request ->
                                "POST".equals(request.getMethod())
                                        && "/api/users".equals(request.getServletPath())
                        )
                );

        return http.build();
    }
}
