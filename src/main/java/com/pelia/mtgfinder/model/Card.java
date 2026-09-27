package com.pelia.mtgfinder.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "cards")
public class Card extends BaseCardData {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "import_id", insertable = false, updatable = false)
	private BulkImport bulkImport;

	@OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<CardFace> faces = new ArrayList<>();

	public UUID getId() { return id; }
	public void setId(UUID id) { this.id = id; }
	public BulkImport getBulkImport() { return bulkImport; }
	public List<CardFace> getFaces() { return faces; }
}
