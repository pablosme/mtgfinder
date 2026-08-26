package com.pelia.mtgfinder.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CardId implements Serializable {

	@Column(name = "import_id", nullable = false)
	private UUID importId;

	@Column(name = "id", nullable = false)
	private UUID id;

	protected CardId() {
	}

	public CardId(UUID importId, UUID id) {
		this.importId = importId;
		this.id = id;
	}

	public UUID getImportId() {
		return importId;
	}

	public UUID getId() {
		return id;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (!(other instanceof CardId cardId)) return false;
		return Objects.equals(importId, cardId.importId) && Objects.equals(id, cardId.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(importId, id);
	}
}
