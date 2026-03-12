package com.banghwa.scheduler;

import com.banghwa.service.SermonAutoImportService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class YoutubeImportScheduler {

    private final SermonAutoImportService sermonAutoImportService;

    public YoutubeImportScheduler(SermonAutoImportService sermonAutoImportService) {
        this.sermonAutoImportService = sermonAutoImportService;
    }

    /**
     * 매주 일요일 오후 2시에 자동 실행
     * 초 분 시 일 월 요일
     * 0 0 14 * * SUN
     */
    @Scheduled(cron = "0 0 14 * * SUN")
    public void autoImportSermons() {
        System.out.println("===== 유튜브 설교 자동 등록 시작 =====");
        int count = sermonAutoImportService.importLatestSermons();
        System.out.println("===== 자동 등록 완료: " + count + "개 =====");
    }
}