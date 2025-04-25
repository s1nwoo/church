package com.banghwa.controller.api.sermon;

import com.banghwa.model.SermonVideo;
import com.banghwa.service.SermonVideoService;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sermons")
@RequiredArgsConstructor
public class SermonVideoController {

    private final SermonVideoService sermonVideoService;

    /**
     * 검색 + 페이징(최신순)
     * keyword, page, size 파라미터 이름을 명시적으로 지정
     */
    @GetMapping
    public Page<SermonVideo> listSermons(
            @RequestParam(name = "keyword",        defaultValue = "")      String keyword,
            @RequestParam(name = "page",           defaultValue = "0")     int page,
            @RequestParam(name = "size",           defaultValue = "10")    int size,
            @RequestParam(name = "includeDeleted", defaultValue = "false") boolean includeDeleted
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return sermonVideoService.searchSermons(keyword, pageable, includeDeleted);
    }

    @GetMapping("/{id}")
    public SermonVideo getSermon(@PathVariable("id") Long id) {
        return sermonVideoService.getSermon(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 설교 영상이 존재하지 않습니다."));
    }

    @PostMapping
    @RolesAllowed("ADMIN")
    public SermonVideo create(@RequestBody SermonVideo sermon) {
        return sermonVideoService.createSermon(sermon);
    }

    @PutMapping("/{id}")
    @RolesAllowed("ADMIN")
    public SermonVideo update(
            @PathVariable("id") Long id,
            @RequestBody SermonVideo sermon
    ) {
        return sermonVideoService.updateSermon(id, sermon);
    }

    @DeleteMapping("/{id}")
    @RolesAllowed("ADMIN")
    public void delete(@PathVariable("id") Long id) {
        sermonVideoService.deleteSermon(id);
    }
}
