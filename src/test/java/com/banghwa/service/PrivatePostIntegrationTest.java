package com.banghwa.service;

import com.banghwa.repository.PostRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import java.nio.file.*;
import java.io.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:postprivacy;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc @WithMockUser(roles = "ADMIN")
class PrivatePostIntegrationTest {
    static final Path DIRECTORY = tempDirectory();
    static Path tempDirectory() {
        try { return Files.createTempDirectory("church-private-post-test-"); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
    @DynamicPropertySource static void paths(DynamicPropertyRegistry registry) {
        registry.add("posts.attachments.directory", DIRECTORY::toString);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PostRepository posts;
    @Autowired JdbcTemplate jdbc;
    @SpyBean PostFileStorage storage;
    @BeforeEach void clear() throws Exception {
        posts.deleteAll();
        try (var files = Files.list(DIRECTORY)) { for (Path path : files.toList()) Files.deleteIfExists(path); }
    }
    @AfterAll static void cleanup() throws Exception {
        try (var files = Files.list(DIRECTORY)) { for (Path path : files.toList()) Files.deleteIfExists(path); }
        Files.deleteIfExists(DIRECTORY);
    }
    MockMultipartFile postPart(boolean adminOnly) {
        return new MockMultipartFile("post", "post.json", "application/json",
                ("{\"title\":\"예배자료\",\"writer\":\"관리자\",\"category\":\"공지사항\",\"content\":\"비공개 본문\",\"adminOnly\":" + adminOnly + "}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    MockMultipartFile file(String filename) { return new MockMultipartFile("files", filename, "application/octet-stream", new byte[]{1,2,3,4}); }
    JsonNode create(boolean adminOnly) throws Exception {
        var result = mvc.perform(multipart("/api/admin/posts").file(postPart(adminOnly)).file(file("예배자료.pptx")))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    Path physical(JsonNode post) { return storage.path(posts.findById(post.get("id").asLong()).orElseThrow().getAttachments().get(0).getStorageKey()); }

    @Test void privatePostsAndFilesAreHiddenFromPublicSearchAndDirectUrls() throws Exception {
        var post = create(true); long id = post.get("id").asLong(), file = post.get("attachments").get(0).get("id").asLong();
        assertThat(Files.readAllBytes(physical(post))).containsExactly(1,2,3,4);
        assertThat(post.get("attachments").get(0).has("storageKey")).isFalse();
        mvc.perform(get("/api/posts").with(anonymous()).param("keyword", "예배자료").param("adminOnly", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        for (String url : new String[]{"/api/posts/" + id, "/api/posts/" + id + "/attachments/" + file}) {
            mvc.perform(get(url).with(anonymous())).andExpect(status().isNotFound());
            mvc.perform(get(url).with(user("member").roles("USER"))).andExpect(status().isNotFound());
        }
        mvc.perform(get("/api/admin/posts")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/posts/" + id + "/attachments/" + file)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes(new byte[]{1,2,3,4}));
    }
    @Test void anonymousAndMembersCannotCreateEditOrDeletePosts() throws Exception {
        var post = create(true); long id = post.get("id").asLong();
        mvc.perform(get("/api/admin/posts").with(anonymous())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/posts").with(user("member").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/admin/posts").file(postPart(true)).with(anonymous())).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/admin/posts").file(postPart(true)).with(user("member").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/posts/" + id).with(user("member").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(put("/api/posts/" + id).contentType("application/json").content("{\"title\":\"x\"}")
                .with(user("member").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/posts/" + id).with(anonymous())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/posts").contentType("application/json").content("{\"title\":\"x\"}").with(anonymous()))
                .andExpect(status().isUnauthorized());
        assertThat(posts.findById(id)).isPresent();
    }
    @Test void deletingAPrivatePostRemovesItsDatabaseRowAndPhysicalFiles() throws Exception {
        var post = create(true); long id = post.get("id").asLong(); Path file = physical(post);
        mvc.perform(delete("/api/posts/" + id)).andExpect(status().isNoContent());
        assertThat(Files.exists(file)).isFalse(); assertThat(posts.findById(id)).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from post_attachment", Long.class)).isZero();
        mvc.perform(get("/api/posts/" + id + "/attachments/" + post.get("attachments").get(0).get("id").asLong()))
                .andExpect(status().isNotFound());
    }
    @Test void publicPostsStayPublicAndDeletingThemStillRemovesFiles() throws Exception {
        var post = create(false); long id = post.get("id").asLong(); Path file = physical(post);
        mvc.perform(get("/api/posts").with(anonymous())).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/posts/" + id + "/attachments/" + post.get("attachments").get(0).get("id").asLong()).with(anonymous()))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/posts/" + id)).andExpect(status().isNoContent());
        assertThat(Files.exists(file)).isFalse(); assertThat(posts.findById(id).orElseThrow().getDeleted()).isTrue();
        mvc.perform(get("/api/posts").with(anonymous())).andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test void editingRemovesOnlyTheSelectedAttachmentAndStoresNewOnes() throws Exception {
        var post = create(true); long id = post.get("id").asLong(); Path oldFile = physical(post);
        var remove = new MockMultipartFile("removeAttachmentIds", "remove.json", "application/json",
                ("[" + post.get("attachments").get(0).get("id").asLong() + "]").getBytes());
        var result = mvc.perform(multipart("/api/admin/posts/" + id).file(postPart(true)).file(file("새자료.pdf")).file(remove)
                .with(request -> { request.setMethod("PUT"); return request; })).andExpect(status().isOk()).andReturn();
        var updated = json.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(updated.get("attachments")).hasSize(1);
        assertThat(updated.get("attachments").get(0).get("filename").asText()).isEqualTo("새자료.pdf");
        assertThat(Files.exists(oldFile)).isFalse(); assertThat(Files.exists(physical(updated))).isTrue();
    }
    @Test void failedPhysicalDeletionDoesNotReportSuccessOrDiscardRetryMetadata() throws Exception {
        var post = create(true); long id = post.get("id").asLong(); Path file = physical(post);
        String key = posts.findById(id).orElseThrow().getAttachments().get(0).getStorageKey();
        doThrow(new UncheckedIOException("첨부파일 삭제 실패", new IOException("test failure"))).when(storage).delete(key);
        mvc.perform(delete("/api/posts/" + id)).andExpect(status().isInternalServerError());
        assertThat(Files.exists(file)).isTrue(); assertThat(posts.findById(id).orElseThrow().getAttachments()).hasSize(1);
    }
    @Test void aDatabaseRollbackDoesNotLeaveUploadedFilesBehind() throws Exception {
        var existing = create(true);
        long id = existing.get("id").asLong();
        var invalid = new MockMultipartFile("post", "post.json", "application/json",
                ("{\"title\":\"자료\",\"writer\":\"" + "x".repeat(300) + "\",\"adminOnly\":true}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThatThrownBy(() -> mvc.perform(multipart("/api/admin/posts/" + id).file(invalid).file(file("자료.pptx"))
                .with(request -> { request.setMethod("PUT"); return request; })))
                .isInstanceOf(Exception.class);
        try (var files = Files.list(DIRECTORY)) { assertThat(files.count()).isEqualTo(1); }
        assertThat(posts.findById(id).orElseThrow().getAttachments()).hasSize(1);
        assertThat(posts.findById(id).orElseThrow().getWriter()).isEqualTo("관리자");
    }
    @Test void oversizedFilesAreRejectedBeforeStorageAndPathsCannotEscape() throws Exception {
        var oversized = new MockMultipartFile("files", "big.pptx", "application/octet-stream", new byte[]{1}) {
            @Override public long getSize() { return PostServiceImpl.MAX_FILE_SIZE + 1; }
        };
        mvc.perform(multipart("/api/admin/posts").file(postPart(true)).file(oversized)).andExpect(status().isBadRequest());
        assertThatThrownBy(() -> storage.path("../outside")).isInstanceOf(IllegalArgumentException.class);
        try (var files = Files.list(DIRECTORY)) { assertThat(files.count()).isZero(); }
    }
}
