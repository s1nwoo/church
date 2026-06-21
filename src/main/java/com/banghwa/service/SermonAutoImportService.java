package com.banghwa.service;

import com.banghwa.model.SermonVideo;
import com.banghwa.repository.SermonVideoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SermonAutoImportService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final Pattern TITLE_DATE_PATTERN = Pattern.compile("(\\d{2})/(\\d{2})/(\\d{2})");
    private static final DateTimeFormatter TITLE_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
            .appendLiteral('/')
            .appendPattern("MM/dd")
            .toFormatter(Locale.KOREA)
            .withResolverStyle(ResolverStyle.STRICT);

    private final YoutubeService youtubeService;
    private final SermonVideoRepository sermonVideoRepository;
    private final ObjectMapper objectMapper;

    @Value("${anthropic.api.key}")
    private String anthropicApiKey;

    public SermonAutoImportService(
            YoutubeService youtubeService,
            SermonVideoRepository sermonVideoRepository,
            ObjectMapper objectMapper
    ) {
        this.youtubeService = youtubeService;
        this.sermonVideoRepository = sermonVideoRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 최신 유튜브 영상을 가져와서 DB에 자동 저장
     */
    public int importLatestSermons() {
        int importedCount = 0;

        // 1. 최신 영상 5개 조회
        List<YoutubeService.YoutubeVideoInfo> videos = youtubeService.getLatestVideos(5);

        for (YoutubeService.YoutubeVideoInfo video : videos) {

            // 2. 이미 등록된 영상인지 중복 체크
            boolean exists = sermonVideoRepository.existsByYoutubeUrl(video.getYoutubeUrl());
            if (exists) {
                System.out.println("이미 등록된 영상 건너뜀: " + video.getYoutubeUrl());
                continue;
            }

            try {
                // 3. 고화질 썸네일 다운로드
                String base64Image = downloadBestThumbnail(video.getVideoId());

                // 4. Claude Vision API로 썸네일 분석
                SermonInfo sermonInfo = extractSermonInfoFromThumbnail(base64Image);

                // 5. 설교 영상이 아니면 건너뜀
                if (!sermonInfo.isSermon()) {
                    System.out.println("설교 영상 아님, 건너뜀: " + video.getYoutubeUrl());
                    continue;
                }

                // 6. DB에 저장 (title 제거 - content를 메인 제목으로 사용)
                SermonVideo sermon = new SermonVideo();
                sermon.setYoutubeUrl(video.getYoutubeUrl());
                sermon.setContent(sermonInfo.getTitle() != null ? sermonInfo.getTitle() : "제목 미확인");
                sermon.setPreacher(sermonInfo.getPreacher() != null ? sermonInfo.getPreacher() : "설교자 미확인");
                sermon.setBibleText(sermonInfo.getBibleVerse() != null ? sermonInfo.getBibleVerse() : "");
                sermon.setSermonDate(resolveSermonDate(video));
                sermon.setDeleted(false);

                sermonVideoRepository.save(sermon);
                importedCount++;
                System.out.println("새 설교 자동 등록 완료: " + sermonInfo.getTitle());

            } catch (Exception e) {
                System.err.println("영상 처리 실패: " + video.getYoutubeUrl() + " / " + e.getMessage());
            }
        }

        return importedCount;
    }

    private LocalDate resolveSermonDate(YoutubeService.YoutubeVideoInfo video) {
        LocalDate titleDate = parseSermonDateFromTitle(video.getTitle());
        if (titleDate != null) {
            return titleDate;
        }

        try {
            return Instant.parse(video.getPublishedAt()).atZone(SEOUL_ZONE).toLocalDate();
        } catch (Exception e) {
            System.err.println("영상 공개일 파싱 실패, 오늘 날짜로 저장합니다. publishedAt="
                    + video.getPublishedAt() + " / " + e.getMessage());
            return LocalDate.now(SEOUL_ZONE);
        }
    }

    private LocalDate parseSermonDateFromTitle(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }

        Matcher matcher = TITLE_DATE_PATTERN.matcher(title);
        if (!matcher.find()) {
            return null;
        }

        String year = matcher.group(1);
        String month = matcher.group(2);
        String day = matcher.group(3);
        try {
            return LocalDate.parse(year + "/" + month + "/" + day, TITLE_DATE_FORMATTER);
        } catch (Exception e) {
            System.err.println("영상 제목 날짜 파싱 실패, 공개일을 사용합니다. title=" + title + " / " + e.getMessage());
            return null;
        }
    }

    /**
     * 가장 고화질 썸네일을 찾아서 Base64로 반환
     * maxresdefault → sddefault → hqdefault 순으로 시도
     */
    private String downloadBestThumbnail(String videoId) throws Exception {
        String[] qualities = {
                "maxresdefault",  // 1280x720
                "sddefault",      // 640x480
                "hqdefault"       // 480x360
        };

        HttpClient client = HttpClient.newHttpClient();

        for (String quality : qualities) {
            String url = "https://img.youtube.com/vi/" + videoId + "/" + quality + ".jpg";
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

                // 유튜브는 없는 썸네일을 120x90 기본 이미지로 반환함
                // 실제 썸네일은 보통 10KB 이상
                if (response.statusCode() == 200 && response.body().length > 10000) {
                    System.out.println("썸네일 다운로드 성공 (" + quality + "): " + url);
                    return Base64.getEncoder().encodeToString(response.body());
                }
            } catch (Exception e) {
                System.out.println("썸네일 시도 실패 (" + quality + "): " + e.getMessage());
            }
        }

        throw new RuntimeException("모든 화질의 썸네일 다운로드 실패: videoId=" + videoId);
    }

    /**
     * Claude Vision API로 썸네일에서 설교 정보 추출
     */
    private SermonInfo extractSermonInfoFromThumbnail(String base64Image) throws Exception {

        Map<String, Object> requestBody = Map.of(
                "model", "claude-sonnet-4-20250514",
                "max_tokens", 500,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", List.of(
                                        Map.of(
                                                "type", "image",
                                                "source", Map.of(
                                                        "type", "base64",
                                                        "media_type", "image/jpeg",
                                                        "data", base64Image
                                                )
                                        ),
                                        Map.of(
                                                "type", "text",
                                                "text", """
                                이 교회 영상 썸네일에서 텍스트를 정확하게 읽어서 정보를 추출해주세요.
                                한국어 텍스트를 매우 주의깊게 읽어주세요. 글자를 추측하지 말고 보이는 그대로 읽어주세요.
                                반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요.
                                
                                {
                                  "isSermon": true,
                                  "title": "설교 제목 (보이는 텍스트 그대로)",
                                  "preacher": "설교자 이름 (예: 김성휘 목사님)",
                                  "bibleVerse": "성경구절 약어 (예: 행8:26-40, 겔47:1-12, 요3:16)"
                                }
                                
                                isSermon 판단 기준:
                                - 썸네일에 "주일예배" 텍스트가 있으면 true
                                - 공동체영상, 찬양, 행사 등이면 false
                                
                                bibleVerse 형식 규칙:
                                - 반드시 약어 사용 (행, 창, 요, 겔, 삼하, 에스겔X → 겔O)
                                - 올바른 예시: 행8:26-40, 겔47:1-12, 요3:16
                                - 잘못된 예시: 사도행전8:26-40, 에스겔47:1-12
                                
                                정보가 없으면 null로 반환하세요.
                                """
                                        )
                                )
                        )
                )
        );

        // Claude API 호출
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.anthropic.com/v1/messages"))
                .header("Content-Type", "application/json")
                .header("x-api-key", anthropicApiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 응답 파싱
        String responseBody = response.body();
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String reason = switch (response.statusCode()) {
                case 402 -> "Anthropic 크레딧 또는 결제 상태를 확인해야 합니다.";
                case 429 -> "Anthropic 요청 한도 또는 사용량 제한에 도달했습니다.";
                default -> "Anthropic API 호출이 실패했습니다.";
            };
            System.err.println(reason + " status=" + response.statusCode() + " body=" + responseBody);
            throw new RuntimeException(reason + " status=" + response.statusCode());
        }

        System.out.println("Claude API 응답: " + responseBody);

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode contentArray = root.path("content");

        if (contentArray.isEmpty()) {
            throw new RuntimeException("Claude API 응답이 비어있음: " + responseBody);
        }

        String content = contentArray.get(0).path("text").asText();
        System.out.println("추출된 텍스트: " + content);

        // 백틱 제거 (Claude가 ```json ... ``` 형식으로 반환할 때 대비)
        content = content.replaceAll("```json", "").replaceAll("```", "").trim();

        // JSON 파싱
        JsonNode infoNode = objectMapper.readTree(content);
        SermonInfo info = new SermonInfo();
        info.setIsSermon(infoNode.path("isSermon").asBoolean(false));
        info.setTitle(infoNode.path("title").asText(null));
        info.setPreacher(infoNode.path("preacher").asText(null));
        info.setBibleVerse(infoNode.path("bibleVerse").asText(null));

        return info;
    }

    /**
     * 썸네일에서 추출한 설교 정보 DTO
     */
    public static class SermonInfo {
        private boolean isSermon;
        private String title;
        private String preacher;
        private String bibleVerse;

        public boolean isSermon() { return isSermon; }
        public void setIsSermon(boolean isSermon) { this.isSermon = isSermon; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getPreacher() { return preacher; }
        public void setPreacher(String preacher) { this.preacher = preacher; }
        public String getBibleVerse() { return bibleVerse; }
        public void setBibleVerse(String bibleVerse) { this.bibleVerse = bibleVerse; }
    }
}
