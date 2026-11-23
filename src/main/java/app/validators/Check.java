package app.validators;

import java.time.LocalDate;
import java.util.Arrays;

public final class Check {
    private Check() {
    }

    public static void requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    public static void requireNotNull(String field, Object value) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    public static void requireMaxLength(String field, String value, int maxLength) {
        if (maxLength < 1) {
            throw new IllegalArgumentException("maxLength must be positive");
        }
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be at most " + maxLength + " characters");
        }
    }

    public static void requireNoControlChars(String field, String value) {
        if (value == null) {
            return;
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch < 0x20 || ch == 0x7F) {
                throw new IllegalArgumentException(field + " must not contain control characters");
            }
        }
    }

    public static void requireOneOf(String field, String value, String... allowed) {
        requireText(field, value);
        if (!Arrays.asList(allowed).contains(value)) {
            throw new IllegalArgumentException(field + " must be one of " + String.join(", ", allowed));
        }
    }

    public static void requireNotBefore(String field, LocalDate date, String otherField, LocalDate otherDate) {
        if (date != null && otherDate != null && date.isBefore(otherDate)) {
            throw new IllegalArgumentException(field + " must not be before " + otherField);
        }
    }

    public static void requireNotAfterToday(String field, LocalDate date) {
        if (date != null && date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(field + " must not be in the future");
        }
    }
}