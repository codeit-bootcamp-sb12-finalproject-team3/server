package com.moduplaylist.batch;

import com.moduplaylist.infrastructure.ai.playlist.OpenAiPlaylistGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication(scanBasePackages = "com.moduplaylist")
@ComponentScan(basePackages = "com.moduplaylist", excludeFilters = @ComponentScan.Filter(
    type = FilterType.ASSIGNABLE_TYPE, classes = OpenAiPlaylistGenerator.class))
@EntityScan(basePackages = "com.moduplaylist.core")
@EnableJpaRepositories(basePackages = "com.moduplaylist.core")
@EnableScheduling
public class BatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }
}
