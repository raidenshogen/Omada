package com.omada.realtime.entity;

import java.util.Locale;

/** Supported document block kinds. Stored lower-case in Mongo. */
public enum BlockType {
    PARAGRAPH, HEADING, TODO, LIST;

    /** Parses a client-supplied type, defaulting to PARAGRAPH. */
    public static String normalize(String value) {
        if (value == null || value.isBlank()) return PARAGRAPH.name().toLowerCase(Locale.ROOT);
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT)).name().toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            return PARAGRAPH.name().toLowerCase(Locale.ROOT);
        }
    }
}
