package com.pelia.mtgfinder.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bulk_imports")
public class BulkImport {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "scryfall_bulk_type", nullable = false, length = 50)
	private String scryfallBulkType;

	@Column(name = "scryfall_updated_at")
	private OffsetDateTime scryfallUpdatedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ImportStatus status = ImportStatus.PROCESSING;

	@Column(name = "total_cards")
	private Integer totalCards = 0;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "activated_at")
	private OffsetDateTime activatedAt;

	@Column(name = "created_cards")
	private Integer createdCards = 0;

	@Column(name = "updated_cards")
	private Integer updatedCards = 0;

	public UUID getId() { return id; }
	public void setId(UUID id) { this.id = id; }
	public String getScryfallBulkType() { return scryfallBulkType; }
	public void setScryfallBulkType(String value) { this.scryfallBulkType = value; }
	public OffsetDateTime getScryfallUpdatedAt() { return scryfallUpdatedAt; }
	public void setScryfallUpdatedAt(OffsetDateTime value) { this.scryfallUpdatedAt = value; }
	public ImportStatus getStatus() { return status; }
	public void setStatus(ImportStatus status) { this.status = status; }
	public Integer getTotalCards() { return totalCards; }
	public void setTotalCards(Integer totalCards) { this.totalCards = totalCards; }
	public OffsetDateTime getCreatedAt() { return createdAt; }
	public OffsetDateTime getActivatedAt() { return activatedAt; }
	public void setActivatedAt(OffsetDateTime activatedAt) { this.activatedAt = activatedAt; }
	public Integer getCreatedCards() { return createdCards; }
	public void setCreatedCards(Integer createdCards) { this.createdCards = createdCards; }
	public Integer getUpdatedCards() { return updatedCards; }
	public void setUpdatedCards(Integer updatedCards) { this.updatedCards = updatedCards; }
}
