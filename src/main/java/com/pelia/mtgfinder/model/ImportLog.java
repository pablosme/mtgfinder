package com.pelia.mtgfinder.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "import_logs")
public class ImportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "import_id")
    private UUID importId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    public Long getId() { return id; }
    public UUID getImportId() { return importId; }
    public void setImportId(UUID importId) { this.importId = importId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
