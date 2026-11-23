package com.spacedvocab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SpacedVocabApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoadsAndFlywayAppliesBaseline() {
        String baseline = jdbcTemplate.queryForObject(
                "SELECT meta_value FROM app_metadata WHERE meta_key = 'schema_baseline'",
                String.class);

        assertThat(baseline).isEqualTo("M0 - project setup");
    }
}
