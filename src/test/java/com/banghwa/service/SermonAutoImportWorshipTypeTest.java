package com.banghwa.service;

import com.banghwa.model.WorshipType;
import com.banghwa.repository.SermonVideoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SermonAutoImportWorshipTypeTest {
    private final SermonAutoImportService service = new SermonAutoImportService(
            mock(YoutubeService.class), mock(SermonVideoRepository.class), new ObjectMapper());

    @Test
    void parsesBothWorshipTypesWithoutExternalApiCalls() throws Exception {
        for (WorshipType type : WorshipType.values()) {
            var info = service.parseSermonInfo("{\"isSermon\":true,\"worshipType\":\"" + type + "\"}");
            assertThat(info.isSermon()).isTrue();
            assertThat(info.getWorshipType()).isEqualTo(type);
        }
    }

    @Test
    void ambiguousClassificationIsNotAssumedToBeSunday() throws Exception {
        assertThat(service.parseSermonInfo("{\"isSermon\":true}").getWorshipType()).isNull();
        assertThat(service.parseSermonInfo("{\"isSermon\":true,\"worshipType\":\"UNKNOWN\"}")
                .getWorshipType()).isNull();
    }
}
