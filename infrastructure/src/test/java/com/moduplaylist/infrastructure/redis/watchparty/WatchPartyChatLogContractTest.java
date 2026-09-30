package com.moduplaylist.infrastructure.redis.watchparty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.core.watchparty.repository.WatchPartyChatMessage;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * app-realtime(쓰기)과 app-api(읽기)의 chat:log 저장 규격이 호환되는지 검증한다.
 * - 쓰기: realtime RedisWatchPartyChatLogRegistry — StringRedisTemplate으로 순수 JSON 문자열 저장
 * - 읽기: infrastructure RedisWatchPartyChatLogRegistry — GenericJackson2JsonRedisSerializer로 읽은 뒤 convertValue
 */
class WatchPartyChatLogContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GenericJackson2JsonRedisSerializer serializer =
            new GenericJackson2JsonRedisSerializer(objectMapper); // RedisConfig와 같은 생성 방식

    @Test
    void realtime이_저장한_JSON을_api쪽_직렬화_설정으로_읽을_수_있다() {
        UUID senderId = UUID.randomUUID();
        String storedByRealtime =
                "{\"senderId\":\"" + senderId + "\",\"content\":\"안녕하세요\",\"sentAt\":1759734000000}";

        Object raw = serializer.deserialize(storedByRealtime.getBytes(StandardCharsets.UTF_8));
        WatchPartyChatMessage message = objectMapper.convertValue(raw, WatchPartyChatMessage.class);

        assertThat(message.getSenderId()).isEqualTo(senderId);
        assertThat(message.getContent()).isEqualTo("안녕하세요");
        assertThat(message.getSentAt()).isEqualTo(1_759_734_000_000L);
    }

    @Test
    void 키_형식이_realtime과_같다() {
        UUID partyId = UUID.randomUUID();

        // realtime 쪽: KEY_PREFIX("watchparty:") + partyId + KEY_SUFFIX(":chat:log")
        assertThat(WatchPartyRedisKey.chatLog(partyId))
                .isEqualTo("watchparty:" + partyId + ":chat:log");
    }
}