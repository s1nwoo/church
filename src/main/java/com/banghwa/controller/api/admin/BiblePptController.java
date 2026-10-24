package com.banghwa.controller.api.admin;

import com.banghwa.bibleppt.BiblePptService;
import com.banghwa.bibleppt.BiblePptService.*;
import com.banghwa.bibleppt.model.SlideContent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/bible-ppt")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class BiblePptController {
    private final BiblePptService service;

    @GetMapping("/metadata")
    public Metadata metadata() { return service.metadata(); }

    @GetMapping("/books/{abbreviation}/ranges")
    public Map<Integer, List<Integer>> ranges(@PathVariable String abbreviation) {
        return service.ranges(abbreviation);
    }

    @PostMapping("/slides")
    public List<SlideContent> slides(@RequestBody BuildRequest request) { return service.build(request); }

    @PostMapping(value = "/preview", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> preview(@RequestBody Slide slide) throws IOException {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.preview(slide));
    }

    @PostMapping("/download")
    public ResponseEntity<byte[]> download(@RequestBody Deck deck) throws IOException {
        var filename = "성경말씀_" + LocalDate.now(ZoneId.of("Asia/Seoul")) + ".pptx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8).build().toString())
                .cacheControl(CacheControl.noStore()).body(service.download(deck));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
    }
}
