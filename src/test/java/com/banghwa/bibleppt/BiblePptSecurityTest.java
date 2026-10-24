package com.banghwa.bibleppt;

import com.banghwa.config.SecurityConfig;
import com.banghwa.controller.api.admin.BiblePptController;
import com.banghwa.repository.UserRepository;
import com.banghwa.security.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BiblePptController.class)
@Import(SecurityConfig.class)
class BiblePptSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean BiblePptService service;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserRepository users;

    @Test void anonymousCannotReadOrGenerate() throws Exception { denied(); }
    @Test @WithMockUser(roles = "USER") void memberCannotReadOrGenerate() throws Exception { denied(); }

    private void denied() throws Exception {
        mvc.perform(get("/api/admin/bible-ppt/metadata")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/bible-ppt/books/창/ranges")).andExpect(status().isForbidden());
        for (String endpoint : new String[]{"slides", "preview", "download"}) {
            mvc.perform(post("/api/admin/bible-ppt/" + endpoint).contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "ADMIN") void adminCanGenerateAndDownload() throws Exception {
        when(service.download(any())).thenReturn(new byte[]{1, 2, 3});
        mvc.perform(get("/api/admin/bible-ppt/metadata")).andExpect(status().isOk());
        mvc.perform(post("/api/admin/bible-ppt/download").contentType("application/json").content("{\"slides\":[]}"))
                .andExpect(status().isOk()).andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation"))
                .andExpect(header().exists("Content-Disposition"));
    }
}
