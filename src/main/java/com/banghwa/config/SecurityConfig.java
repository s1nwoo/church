package com.banghwa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
        // 📌 보안 규칙을 설정하는 부분
        http
                .authorizeHttpRequests(auth -> auth
                        // ✅ 로그인 없이 접근 허용할 경로 설정 (css, 이미지, 홈화면 등)
                        .requestMatchers("/", "/login", "/doLogin", "/css/**", "/images/**").permitAll()
                        // ✅ 그 외 모든 요청은 로그인 필요
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginPage("/login") // 로그인 폼을 보여줄 페이지
                        .loginProcessingUrl("/doLogin") // 실제 로그인 요청을 처리할 경로 (POST 요청 처리)
                        .defaultSuccessUrl("/", true) // 로그인 성공 시 이동할 경로
                        .permitAll()
                )
                .logout(logout -> logout
                        // ✅ 로그아웃 성공 시 이동할 경로
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
