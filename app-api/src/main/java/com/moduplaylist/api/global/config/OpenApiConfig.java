package com.moduplaylist.api.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger(OpenAPI) 문서 설정.
 * /api/** 경로의 REST API를 v1 그룹으로 문서화한다.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ModuPlaylist API")
                        .version("v1.0.0")
                        .description("ModuPlaylist API 문서"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("로컬 API 서버")
                ));
    }

    @Bean
    public GroupedOpenApi v1Api() {
        return GroupedOpenApi.builder()
                .group("v1")
                .pathsToMatch("/api/**")
                .build();
    }
}