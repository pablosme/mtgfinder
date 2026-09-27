package com.pelia.mtgfinder.model;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Immutable
@Table(name = "cards")
public class ActiveCard extends BaseCardData {

	@Id
	private UUID id;

	public UUID getId() { return id; }
}
