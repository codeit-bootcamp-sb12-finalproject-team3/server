package com.moduplaylist.api.global.config;

import com.moduplaylist.api.global.logging.MDCLoggingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 공통 설정.
 * 모든 HTTP 요청에 MDC 로깅 인터셉터를 적용한다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MDCLoggingInterceptor())
                .addPathPatterns("/**");
    }
}