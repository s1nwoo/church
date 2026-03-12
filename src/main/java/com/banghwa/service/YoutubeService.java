package com.banghwa.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class YoutubeService {

    // application.yml에서 API 키 주입
    @Value("${youtube.api.key}")
    private String apiKey;

    // 채널 ID (방화침례교회)
    private static final String CHANNEL_ID = "UCdaX9-QoZXozLBUMWmQnCeQ";

    /**
     * 채널의 최신 영상 목록 조회
     * @param maxResults 가져올 영상 수
     * @return 영상 정보 리스트
     */
    public List<YoutubeVideoInfo> getLatestVideos(long maxResults) {
        List<YoutubeVideoInfo> result = new ArrayList<>();

        try {
            // YouTube API 클라이언트 생성
            YouTube youtube = new YouTube.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    request -> {}
            ).setApplicationName("church-website").build();

            // 1. 채널의 업로드 플레이리스트 ID 조회
            ChannelListResponse channelResponse = youtube.channels()
                    .list(List.of("contentDetails"))
                    .setId(List.of(CHANNEL_ID))
                    .setKey(apiKey)
                    .execute();

            if (channelResponse.getItems().isEmpty()) return result;

            String uploadsPlaylistId = channelResponse.getItems().get(0)
                    .getContentDetails().getRelatedPlaylists().getUploads();

            // 2. 플레이리스트에서 최신 영상 목록 조회
            PlaylistItemListResponse playlistResponse = youtube.playlistItems()
                    .list(List.of("snippet"))
                    .setPlaylistId(uploadsPlaylistId)
                    .setMaxResults(maxResults)
                    .setKey(apiKey)
                    .execute();

            // 3. 영상 정보 추출
            for (PlaylistItem item : playlistResponse.getItems()) {
                YoutubeVideoInfo info = new YoutubeVideoInfo();
                String videoId = item.getSnippet().getResourceId().getVideoId();
                info.setVideoId(videoId);
                info.setYoutubeUrl("https://www.youtube.com/watch?v=" + videoId);
                info.setThumbnailUrl("https://img.youtube.com/vi/" + videoId + "/maxresdefault.jpg");
                info.setPublishedAt(item.getSnippet().getPublishedAt().toString());
                result.add(info);
            }

        } catch (Exception e) {
            System.err.println("YouTube API 호출 실패: " + e.getMessage());
        }

        return result;
    }

    /**
     * 유튜브 영상 정보 DTO
     */
    public static class YoutubeVideoInfo {
        private String videoId;
        private String youtubeUrl;
        private String thumbnailUrl;
        private String publishedAt;

        // Getters & Setters
        public String getVideoId() { return videoId; }
        public void setVideoId(String videoId) { this.videoId = videoId; }
        public String getYoutubeUrl() { return youtubeUrl; }
        public void setYoutubeUrl(String youtubeUrl) { this.youtubeUrl = youtubeUrl; }
        public String getThumbnailUrl() { return thumbnailUrl; }
        public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
        public String getPublishedAt() { return publishedAt; }
        public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }
    }
}