package com.moduplaylist.api.watchparty.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WatchPartyServerTimeController {

    // 클라이언트가 서버 시계와의 차이(offset)를 계산할 때 쓰는 기준 시각
    @GetMapping("/api/watch-parties/server-time")
    public ResponseEntity<Map<String, Long>> getServerTime() {
        return ResponseEntity.ok(Map.of("serverTime", Instant.now().toEpochMilli()));
    }
}