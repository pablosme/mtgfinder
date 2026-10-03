package com.pelia.mtgfinder.repository;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

// Encodes one PostgreSQL text COPY row; each append represents exactly one column.
final class PostgresCopyRow {

    private final StringBuilder row = new StringBuilder();
    private int columns;

    void append(String value) {
        if (columns++ > 0) {
            row.append('\t');
        }
        if (value == null) {
            row.append("\\N");
        } else {
            row.append(value.replace("\\", "\\\\")
                    .replace("\t", "\\t")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r"));
        }
    }

    <T> void append(T value, Function<T, String> format) {
        append(value == null ? null : format.apply(value));
    }

    <T> void appendArray(List<T> values, Function<T, String> format) {
        if (values == null) {
            append(null);
            return;
        }
        // Array escaping happens before COPY escaping; null elements remain unquoted.
        append(values.stream()
                .map(value -> value == null ? "NULL"
                        : "\"" + format.apply(value).replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",", "{", "}")));
    }

    String finish() {
        return row.append('\n').toString();
    }
}
