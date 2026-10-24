package com.banghwa.scheduler;

import com.banghwa.repository.SermonVideoRepository;
import com.banghwa.model.WorshipType;
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
     * 주일예배 또는 오후예배가 아직 없으면 저녁에 한 번 더 조회한다.
     * 두 예배가 모두 등록되어 있으면 AI 분석 호출 없이 종료한다.
     */
    @Scheduled(cron = "0 0 20 * * SUN", zone = "Asia/Seoul")
    public void retryAutoImportSermonsIfMissing() {
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        if (sermonVideoRepository.existsBySermonDateAndWorshipTypeAndDeletedFalse(today, WorshipType.SUNDAY)
                && sermonVideoRepository.existsBySermonDateAndWorshipTypeAndDeletedFalse(today, WorshipType.AFTERNOON)) {
            System.out.println("===== 일요일 저녁 재시도 생략: 두 예배가 모두 등록되어 있습니다. sermonDate=" + today + " =====");
            return;
        }

        System.out.println("===== 일요일 저녁 재시도 시작: 미등록 예배를 확인합니다. sermonDate=" + today + " =====");
        int count = sermonAutoImportService.importLatestSermons();
        System.out.println("===== 일요일 저녁 재시도 완료: " + count + "개 =====");
    }
}
