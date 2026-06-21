package com.banghwa.scheduler;

import com.banghwa.repository.SermonVideoRepository;
import com.banghwa.service.SermonAutoImportService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class YoutubeImportScheduler {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final SermonAutoImportService sermonAutoImportService;
    private final SermonVideoRepository sermonVideoRepository;

    public YoutubeImportScheduler(
            SermonAutoImportService sermonAutoImportService,
            SermonVideoRepository sermonVideoRepository
    ) {
        this.sermonAutoImportService = sermonAutoImportService;
        this.sermonVideoRepository = sermonVideoRepository;
    }

    /**
     * 매주 일요일 오후 2시에 자동 실행
     * 초 분 시 일 월 요일
     * 0 0 14 * * SUN
     */
    @Scheduled(cron = "0 0 14 * * SUN", zone = "Asia/Seoul")
    public void autoImportSermons() {
        System.out.println("===== 유튜브 설교 자동 등록 시작 (일요일 14:00, Asia/Seoul) =====");
        int count = sermonAutoImportService.importLatestSermons();
        System.out.println("===== 자동 등록 완료 (일요일 14:00): " + count + "개 =====");
    }

    /**
     * 오후 2시 실행 때 해당 일요일 설교가 없었던 경우를 위해 저녁에 한 번만 재시도한다.
     * 이미 해당 설교일 영상이 있으면 AI 분석 호출 없이 종료해 크레딧을 아낀다.
     */
    @Scheduled(cron = "0 0 20 * * SUN", zone = "Asia/Seoul")
    public void retryAutoImportSermonsIfMissing() {
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        if (sermonVideoRepository.existsBySermonDateAndDeletedFalse(today)) {
            System.out.println("===== 일요일 저녁 재시도 생략: 이미 등록된 설교가 있습니다. sermonDate=" + today + " =====");
            return;
        }

        System.out.println("===== 일요일 저녁 재시도 시작: 아직 등록된 설교가 없습니다. sermonDate=" + today + " =====");
        int count = sermonAutoImportService.importLatestSermons();
        System.out.println("===== 일요일 저녁 재시도 완료: " + count + "개 =====");
    }
}
