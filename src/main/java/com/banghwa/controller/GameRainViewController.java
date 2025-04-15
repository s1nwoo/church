package com.banghwa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/game/rain")
public class GameRainViewController {

    /**
     * 난이도 선택 화면
     */
    @GetMapping
    public String selectDifficulty() {
        return "game/rain_difficulty";  // templates/game/rain_difficulty.html
    }

    /**
     * 게임 플레이 화면
     */
    @GetMapping("/play")
    public String startGame() {
        return "game/rain_bible";       // templates/game/rain_bible.html
    }
}
