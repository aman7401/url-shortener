package com.shortener.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SequenceRepository {

    private final JdbcTemplate jdbcTemplate;

    public SequenceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long nextValue() {
        Long value = jdbcTemplate.queryForObject("SELECT nextval('url_code_seq')", Long.class);
        if (value == null) throw new IllegalStateException("Failed to get next sequence value");
        return value;
    }
}
