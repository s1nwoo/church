package com.banghwa.controller.api.sermon;

import com.banghwa.model.WorshipType;
import com.banghwa.service.SermonVideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SermonVideoControllerTest {
    private SermonVideoService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(SermonVideoService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SermonVideoController(service)).build();
    }

    @Test
    void passesCategoryAndSearchToService() throws Exception {
        when(service.searchSermons(eq("은혜"), any(Pageable.class), eq(false), eq(WorshipType.AFTERNOON)))
                .thenReturn(Page.empty(PageRequest.of(0, 10)));
        mvc.perform(get("/api/sermons").param("worshipType", "AFTERNOON").param("keyword", "은혜"))
                .andExpect(status().isOk());
        verify(service).searchSermons(eq("은혜"), any(Pageable.class), eq(false), eq(WorshipType.AFTERNOON));
    }

    @Test
    void rejectsUnknownFilterInsteadOfReturningMixedWorshipTypes() throws Exception {
        mvc.perform(get("/api/sermons").param("worshipType", "UNKNOWN"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsUnknownWorshipTypeInWriteRequest() throws Exception {
        mvc.perform(post("/api/sermons").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"worshipType\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
