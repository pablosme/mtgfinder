package com.pelia.mtgfinder.model;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Immutable
@Table(name = "cards_active_v")
public class ActiveCard extends BaseCardData {

	@EmbeddedId
	private CardId id;

	public CardId getId() { return id; }
}
