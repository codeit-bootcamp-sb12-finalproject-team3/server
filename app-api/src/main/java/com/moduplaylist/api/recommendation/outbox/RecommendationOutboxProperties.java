package com.moduplaylist.api.recommendation.outbox;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mopl.recommendation.outbox")
public class RecommendationOutboxProperties {

    private boolean workerEnabled = true;
    private long pollingIntervalMs = 3000;
    private int claimLimit = 10;
    private Duration processingTimeout = Duration.ofMinutes(10);
}
