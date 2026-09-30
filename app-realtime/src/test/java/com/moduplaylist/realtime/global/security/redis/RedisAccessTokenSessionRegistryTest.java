package com.moduplaylist.realtime.global.security.redis;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

@SuppressWarnings("unchecked")
class RedisAccessTokenSessionRegistryTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RedisAccessTokenSessionRegistry registry =
            new RedisAccessTokenSessionRegistry(redisTemplate, objectMapper);

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void readsTheExistingApiJwtInformationJsonWithoutSharingItsJavaType() {
        UUID userId = UUID.randomUUID();
        GenericJackson2JsonRedisSerializer apiSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);
        byte[] serialized = apiSerializer.serialize(
                new ApiJwtInformationFixture("active-jti", "refresh-jti"));
        when(valueOperations.get(RedisAccessTokenSessionRegistry.KEY_PREFIX + userId))
                .thenReturn(new String(serialized, UTF_8));

        assertThat(registry.isAccessTokenActive(userId, "active-jti")).isTrue();
        assertThat(registry.isAccessTokenActive(userId, "different-jti")).isFalse();
    }

    @Test
    void returnsFalseWhenTheSessionDoesNotExist() {
        UUID userId = UUID.randomUUID();

        assertThat(registry.isAccessTokenActive(userId, "active-jti")).isFalse();
    }

    @Test
    void exposesMalformedStoredDataAsAContractFailure() {
        UUID userId = UUID.randomUUID();
        when(valueOperations.get(RedisAccessTokenSessionRegistry.KEY_PREFIX + userId))
                .thenReturn("not-json");

        assertThatThrownBy(() -> registry.isAccessTokenActive(userId, "active-jti"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Redis JWT session contract");
    }

    private record ApiJwtInformationFixture(String accessTokenId, String refreshTokenId) {
    }
}
