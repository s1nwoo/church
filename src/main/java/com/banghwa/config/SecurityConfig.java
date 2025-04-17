package com.banghwa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors().and()                      // WebConfig 에 정의한 CORS 설정 활성화
                .csrf().disable()
                .authorizeHttpRequests(auth -> auth
                        // 로그인·정적 리소스
                        .requestMatchers("/", "/login", "/signup", "/css/**", "/images/**").permitAll()

                        // React SPA 라우트 허용
                        .requestMatchers(HttpMethod.GET,
                                "/bible-practice", "/bible-practice/**",
                                "/location",        "/location/**",
                                "/posts",           "/posts/**"
                        ).permitAll()

                        // API 엔드포인트
                        .requestMatchers("/api/posts/**").permitAll()
                        .requestMatchers("/api/bible-practice/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        // 그 외는 인증 필요
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginPage("/login")
                        .loginProcessingUrl("/doLogin")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder pw) {
        UserDetails admin = User.builder()
                .username("admin")
                .password(pw.encode("1234"))
                .roles("ADMIN")
                .build();

        UserDetails member = User.builder()
                .username("member")
                .password(pw.encode("1234"))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(admin, member);
    }
}
