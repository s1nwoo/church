package com.banghwa.scheduler;

import com.banghwa.model.WorshipType;
import com.banghwa.repository.SermonVideoRepository;
import com.banghwa.service.SermonAutoImportService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class YoutubeImportSchedulerTest {
    @Test
    void retriesWhenOnlySundayHasBeenImported() {
        var repository = mock(SermonVideoRepository.class);
        var importer = mock(SermonAutoImportService.class);
        when(repository.existsBySermonDateAndWorshipTypeAndDeletedFalse(any(), eq(WorshipType.SUNDAY)))
                .thenReturn(true);
        new YoutubeImportScheduler(importer, repository).retryAutoImportSermonsIfMissing();
        verify(importer).importLatestSermons();
    }

    @Test
    void skipsWhenBothServicesHaveBeenImported() {
        var repository = mock(SermonVideoRepository.class);
        var importer = mock(SermonAutoImportService.class);
        when(repository.existsBySermonDateAndWorshipTypeAndDeletedFalse(any(), any())).thenReturn(true);
        new YoutubeImportScheduler(importer, repository).retryAutoImportSermonsIfMissing();
        verifyNoInteractions(importer);
    }
}
