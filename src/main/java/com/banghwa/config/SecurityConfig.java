package com.banghwa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration // 📌 스프링 설정 클래스라는 표시
@EnableWebSecurity // 📌 스프링 시큐리티를 사용하겠다는 선언
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/css/**", "/images/**").permitAll()
                        .requestMatchers("/api/posts/**").permitAll()
                        .requestMatchers("/api/bible-practice/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/posts").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/posts/**").permitAll()
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
        // ✅ 비밀번호 암호화를 위한 설정 (BCrypt는 강력하고 안전한 알고리즘)
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder passwordEncoder) {
        // ✅ 테스트용 관리자 계정 생성
        UserDetails admin = User.builder()
                .username("admin") // 로그인 아이디
                .password(passwordEncoder.encode("1234")) // 로그인 비밀번호 (암호화 적용됨)
                .roles("ADMIN") // 관리자 권한
                .build();

        // ✅ 일반 교인 계정 생성
        UserDetails member = User.builder()
                .username("member") // 로그인 아이디
                .password(passwordEncoder.encode("1234")) // 로그인 비밀번호
                .roles("USER") // 교인 권한
                .build();

        // ✅ 메모리에 두 계정을 등록
        return new InMemoryUserDetailsManager(admin, member);
    }


}
