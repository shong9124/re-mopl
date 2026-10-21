package com.practice.remopl.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {

    private static final RequestMatcher USER_REGISTRATION =
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/users");

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        // 실제 오류가 인증 오류로 가려지지 않도록 허용
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(USER_REGISTRATION).permitAll()
                        .anyRequest().authenticated()
                )
                // 회원가입 API의 Postman 테스트를 위한 임시 예외
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(USER_REGISTRATION)
                );

        return http.build();
    }
}
