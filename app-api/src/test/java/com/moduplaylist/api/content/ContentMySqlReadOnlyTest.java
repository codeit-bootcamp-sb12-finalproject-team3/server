package com.moduplaylist.api.content;

import com.moduplaylist.api.ApiApplication;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.moduplaylist.core.content.repository.ContentRelationRepository;
import static com.moduplaylist.core.content.repository.ContentQueryRepository.uuid;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ApiApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "aws.access-key=audit-unused",
        "aws.secret-key=audit-unused",
        "aws.s3.region=ap-northeast-2"
})
@AutoConfigureMockMvc
@Transactional(readOnly = true)
@EnabledIfEnvironmentVariable(named = "CODEX_MYSQL_AUDIT", matches = "true")
class ContentMySqlReadOnlyTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        // 연결 자체를 READ ONLY로 설정하며 실제 DB에서 쓰기나 DDL을 실행하지 않는다.
        registry.add("spring.datasource.url", () -> System.getenv("CODEX_MYSQL_AUDIT_URL"));
        registry.add("spring.datasource.username", () -> System.getenv("CODEX_MYSQL_AUDIT_USER"));
        registry.add("spring.datasource.password", () -> System.getenv("CODEX_MYSQL_AUDIT_PASSWORD"));
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ContentRelationRepository relations;

    @Test
    void allContentTypesAndReviewCanBeReadWithRealDdl() throws Exception {
        var ids = jdbc.queryForList("SELECT id FROM contents ORDER BY type,id LIMIT 40", byte[].class);
        assumeFalse(ids.isEmpty());
        for (byte[] id : ids) {
            mvc.perform(get("/api/contents/" + uuid(id))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(uuid(id).toString()));
        }
        var reviewIds = jdbc.queryForList("SELECT id FROM reviews LIMIT 1", byte[].class);
        for (byte[] id : reviewIds) {
            mvc.perform(get("/api/reviews/" + uuid(id))).andExpect(status().isOk());
        }
    }

    @Test
    void nativeDatetimeIsMappedForWatchParties() {
        List<java.util.Map<String,Object>> rows = jdbc.queryForList(
                "SELECT content_id,scheduled_at FROM watch_parties WHERE status <> 'ENDED' AND ended_at IS NULL LIMIT 1");
        assumeFalse(rows.isEmpty());
        var row = rows.get(0);
        Instant scheduled = jdbc.queryForObject("SELECT scheduled_at FROM watch_parties WHERE content_id=? ORDER BY scheduled_at LIMIT 1",
                (rs, i) -> rs.getTimestamp(1, java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))).toInstant(),
                row.get("content_id"));
        assertThat(relations.watchParties(uuid(row.get("content_id")), scheduled)).isNotEmpty();
    }
}
