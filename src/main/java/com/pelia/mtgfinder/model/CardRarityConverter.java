package com.pelia.mtgfinder.model;

import java.util.Locale;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CardRarityConverter implements AttributeConverter<CardRarity, String> {

	@Override
	public String convertToDatabaseColumn(CardRarity rarity) {
		return rarity == null ? null : rarity.name().toLowerCase(Locale.ROOT);
	}

	@Override
	public CardRarity convertToEntityAttribute(String value) {
		return value == null ? null : CardRarity.valueOf(value.toUpperCase(Locale.ROOT));
	}
}
