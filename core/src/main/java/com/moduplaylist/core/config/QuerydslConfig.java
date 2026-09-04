package com.moduplaylist.core.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * QueryDSL에서 사용할 JPAQueryFactory를 Bean으로 등록한다.
 * 동적 검색 및 복잡한 조회 쿼리 작성 시 사용한다.
 */
@Configuration
public class QuerydslConfig {

    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }
}