package com.resumeai.resume.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures legacy resume columns exist for backward compatibility.
 */
@Component
public class ResumeSchemaMigrator {

    private static final Logger log = LoggerFactory.getLogger(ResumeSchemaMigrator.class);

    private final JdbcTemplate jdbcTemplate;

    public ResumeSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Adds missing columns on startup when running against existing schemas.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        final String TYPE_VARCHAR = "VARCHAR(255)";
        final String TYPE_LONGTEXT = "LONGTEXT";

        List<ColumnDef> columns = List.of(
                new ColumnDef("name", TYPE_VARCHAR),
                new ColumnDef("email", TYPE_VARCHAR),
                new ColumnDef("phone", TYPE_VARCHAR),
                new ColumnDef("location", TYPE_VARCHAR),
                new ColumnDef("summary", TYPE_LONGTEXT),
                new ColumnDef("skills", TYPE_LONGTEXT),
                new ColumnDef("experience", TYPE_LONGTEXT),
                new ColumnDef("education", TYPE_LONGTEXT),
                new ColumnDef("projects", TYPE_LONGTEXT));

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