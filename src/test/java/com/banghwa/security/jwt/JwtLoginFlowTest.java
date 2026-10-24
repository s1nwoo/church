package com.banghwa.security.jwt;

import com.banghwa.bibleppt.BiblePptService;
import com.banghwa.config.SecurityConfig;
import com.banghwa.controller.api.admin.BiblePptController;
import com.banghwa.controller.api.auth.AuthController;
import com.banghwa.model.User;
import com.banghwa.model.UserRole;
import com.banghwa.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercise actual token issuance, signature validation and role checking, without WithMockUser. */
@WebMvcTest({AuthController.class, BiblePptController.class})
@Import({SecurityConfig.class, JwtUtil.class})
class JwtLoginFlowTest {
    private static final Path DIRECTORY = createDirectory();
    private static final Path KEY = DIRECTORY.resolve("signing.key");
    private static Path createDirectory() {
        try { return Files.createTempDirectory("church-jwt-login-test-"); }
        catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> "");
        registry.add("jwt.secret-file", KEY::toString);
    }
    @AfterAll static void cleanup() throws Exception { Files.deleteIfExists(KEY); Files.deleteIfExists(DIRECTORY); }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtUtil jwt;
    @MockBean AuthenticationManager manager;
    @MockBean UserRepository users;
    @MockBean BiblePptService service;

    private String login(UserRole role) throws Exception {
        var account = User.builder().id(7L).username("test-account").name("테스트")
                .email("test@example.invalid").role(role).build();
        var principal = org.springframework.security.core.userdetails.User.withUsername("test-account")
                .password("unused-test-password").roles(role.name()).build();
        when(manager.authenticate(any())).thenReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(users.findByUsername("test-account")).thenReturn(Optional.of(account));
        var result = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"test-account\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test void administratorLoginCanImmediatelyCallThePptApiAndSurvivesKeyReload() throws Exception {
        String token = login(UserRole.ADMIN);
        mvc.perform(get("/api/admin/bible-ppt/metadata").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        verify(service).metadata();
        var restarted = new JwtUtil("", KEY.toString());
        assertThat(restarted.validateToken(token)).isTrue();
        String afterRestart = restarted.generateToken("test-account");
        mvc.perform(get("/api/admin/bible-ppt/metadata").header("Authorization", "Bearer " + afterRestart)).andExpect(status().isOk());
    }

    @Test void ordinaryMemberStillCannotCallThePptApi() throws Exception {
        String token = login(UserRole.USER);
        mvc.perform(get("/api/admin/bible-ppt/metadata").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test void invalidTokenIsAnAuthenticationFailureInsteadOfPermissionFailure() throws Exception {
        mvc.perform(get("/api/admin/bible-ppt/metadata").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
