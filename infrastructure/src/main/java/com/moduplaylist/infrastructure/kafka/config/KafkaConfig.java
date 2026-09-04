package com.moduplaylist.infrastructure.kafka.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

// spring-kafka 사용을 위한 최소 설정 (직렬화 등은 application.yml에서 처리), prod 전용
// Kafka Consumer(@KafkaListener) 사용을 활성화한다.

// Producer/Consumer의 bootstrap server, serializer 등의 상세 설정은
// 각 실행 모듈의 application.yml에서 관리한다.

@EnableKafka
@Configuration
public class KafkaConfig {
}