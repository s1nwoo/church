package com.banghwa.controller.api.admin;

import com.banghwa.service.SermonAutoImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class YoutubeImportController {

    private final SermonAutoImportService sermonAutoImportService;

    public YoutubeImportController(SermonAutoImportService sermonAutoImportService) {
        this.sermonAutoImportService = sermonAutoImportService;
    }

    /**
     * 유튜브 설교 수동 임포트 (테스트용)
     */
    @PostMapping("/sermons/import")
    public ResponseEntity<String> importSermons() {
        int count = sermonAutoImportService.importLatestSermons();
        return ResponseEntity.ok("자동 등록 완료: " + count + "개");
    }
}