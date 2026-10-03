package com.pelia.mtgfinder.repository;

import java.sql.Connection;
import java.sql.SQLException;

// Serializes import promotion and vector persistence, never model inference.
public final class CardWriteLock {
    private CardWriteLock() {}

    public static void acquire(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute("SELECT pg_advisory_xact_lock(77271901)");
        }
    }
}
