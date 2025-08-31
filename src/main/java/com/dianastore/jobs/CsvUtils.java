package com.dianastore.jobs;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Single-format CSV utilities.
 * Canonical timestamp format: dd/MM/yyyy HH:mm  (e.g. 25/08/2025 10:00)
 */
public final class CsvUtils {

    // === SINGLE FORMAT ===
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private CsvUtils() {}

    /**
     * Parse into LocalDateTime using the single canonical format.
     * Returns null if blank or parse fails.
     */
    public static LocalDateTime parseTimestamp(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim();
        try {
            return LocalDateTime.parse(s, DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            System.err.println("⚠️ Failed to parse timestamp: " + raw + " (expected dd/MM/yyyy HH:mm)");
            return null;
        }
    }

    /**
     * Parse directly into java.sql.Timestamp (null on failure).
     */
    public static Timestamp parseSqlTimestamp(String raw) {
        LocalDateTime ldt = parseTimestamp(raw);
        return (ldt == null) ? null : Timestamp.valueOf(ldt);
    }

    /**
     * Convert LocalDateTime to java.sql.Timestamp (null-safe).
     */
    public static Timestamp toSqlTimestamp(LocalDateTime ldt) {
        return (ldt == null) ? null : Timestamp.valueOf(ldt);
    }

    /**
     * Current time helpers.
     */
    public static LocalDateTime nowLocal() {
        return LocalDateTime.now();
    }

    public static Timestamp nowSql() {
        return Timestamp.from(Instant.now());
    }

    /**
     * Integer parsing (null-safe).
     */
    public static Integer parseInteger(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            System.err.println("⚠️ Failed to parse integer: " + raw);
            return null;
        }
    }

    /**
     * Double parsing (null-safe).
     */
    public static Double parseDouble(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException ex) {
            System.err.println("⚠️ Failed to parse double: " + raw);
            return null;
        }
    }

    /**
     * Boolean parsing (null-safe). Accepts true/false, 1/0, yes/no.
     * Returns null if unknown/blank.
     */
    public static Boolean parseBoolean(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String v = raw.trim().toLowerCase();
        if (v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("y")) return Boolean.TRUE;
        if (v.equals("false") || v.equals("0") || v.equals("no") || v.equals("n")) return Boolean.FALSE;
        System.err.println("⚠️ Failed to parse boolean: " + raw);
        return null;
    }
}
