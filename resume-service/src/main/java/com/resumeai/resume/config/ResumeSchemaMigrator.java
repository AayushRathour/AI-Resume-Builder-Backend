package com.resumeai.resume.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ResumeSchemaMigrator {

    private static final Logger log = LoggerFactory.getLogger(ResumeSchemaMigrator.class);

    private final JdbcTemplate jdbcTemplate;

    public ResumeSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        List<ColumnDef> columns = List.of(
                new ColumnDef("name", "VARCHAR(255)"),
                new ColumnDef("email", "VARCHAR(255)"),
                new ColumnDef("phone", "VARCHAR(255)"),
                new ColumnDef("location", "VARCHAR(255)"),
                new ColumnDef("summary", "LONGTEXT"),
                new ColumnDef("skills", "LONGTEXT"),
                new ColumnDef("experience", "LONGTEXT"),
                new ColumnDef("education", "LONGTEXT"),
                new ColumnDef("projects", "LONGTEXT"));

        for (ColumnDef column : columns) {
            ensureColumn("resumes", column);
        }
    }

    private void ensureColumn(String table, ColumnDef column) {
        String existsSql = "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
        Integer count = jdbcTemplate.queryForObject(existsSql, Integer.class, table, column.name);
        if (count != null && count > 0) {
            return;
        }

        String alterSql = "ALTER TABLE " + table + " ADD COLUMN " + column.name + " " + column.type;
        jdbcTemplate.execute(alterSql);
        log.info("Added column {}.{} ({})", table, column.name, column.type);
    }

    private record ColumnDef(String name, String type) {}
}