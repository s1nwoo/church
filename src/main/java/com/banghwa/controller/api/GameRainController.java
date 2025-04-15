package com.banghwa.controller.api;

import com.banghwa.service.GameRainService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game/rain")
public class GameRainController {

    private final GameRainService gameRainService;

    public GameRainController(GameRainService gameRainService) {
        this.gameRainService = gameRainService;
    }

    @GetMapping("/verse")
    public String getRandomVerse(@RequestParam(value = "level", defaultValue = "easy") String level) {
        return gameRainService.getVerseByDifficulty(level);
    }
}
