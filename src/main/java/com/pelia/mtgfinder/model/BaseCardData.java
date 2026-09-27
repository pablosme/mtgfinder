package com.pelia.mtgfinder.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

@MappedSuperclass
public abstract class BaseCardData {

	@Column(name = "oracle_id")
	private UUID oracleId;
	@Column(name = "import_id", nullable = false)
	private UUID importId;
	@Column(name = "metadata_hash", length = 64)
	private String metadataHash;
	@Column(nullable = false, length = 255)
	private String name;
	@Column(nullable = false, length = 10)
	private String lang = "en";
	@Column(nullable = false, length = 50)
	private String layout;
	@Column(name = "mana_cost", length = 100)
	private String manaCost;
	@Column(precision = 12, scale = 2)
	private BigDecimal cmc;
	@Column(name = "type_line", length = 255)
	private String typeLine;
	@Column(name = "oracle_text", columnDefinition = "text")
	private String oracleText;
	@Column(columnDefinition = "varchar(5)[]")
	private String[] colors;
	@Column(name = "color_identity", nullable = false, columnDefinition = "varchar(5)[]")
	private String[] colorIdentity;
	@Column(nullable = false, columnDefinition = "text[]")
	private String[] keywords = new String[0];
	@Column(name = "produced_mana", columnDefinition = "varchar(5)[]")
	private String[] producedMana;
	@Column(length = 30)
	private String power;
	@Column(length = 30)
	private String toughness;
	@Column(length = 30)
	private String loyalty;
	@Column(length = 30)
	private String defense;
	@Column(nullable = false)
	private boolean reserved;
	@Column(nullable = false)
	private boolean digital;
	@Column(nullable = false)
	private boolean reprint;
	@Column(name = "set_code", nullable = false, length = 10)
	private String setCode;
	@Column(name = "set_name", nullable = false, length = 255)
	private String setName;
	@Column(name = "collector_number", nullable = false, length = 20)
	private String collectorNumber;
	@Convert(converter = CardRarityConverter.class)
	@Column(nullable = false, length = 20)
	private CardRarity rarity;
	@Column(name = "flavor_text", columnDefinition = "text")
	private String flavorText;
	@Column(length = 255)
	private String artist;
	@Column(name = "released_at")
	private LocalDate releasedAt;
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private JsonNode legalities = JsonNodeFactory.instance.objectNode();
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "image_uris", columnDefinition = "jsonb")
	private JsonNode imageUris;
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private JsonNode prices = JsonNodeFactory.instance.objectNode();
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "related_uris", columnDefinition = "jsonb")
	private JsonNode relatedUris;
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "all_parts", columnDefinition = "jsonb")
	private JsonNode allParts;
	@Column(name = "context_text", columnDefinition = "text")
	private String contextText;
	@JdbcTypeCode(SqlTypes.VECTOR)
	@Column(columnDefinition = "vector(1024)")
	private float[] embedding;
	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	public UUID getOracleId() { return oracleId; }
	public void setOracleId(UUID value) { oracleId = value; }
	public UUID getImportId() { return importId; }
	public void setImportId(UUID value) { importId = value; }
	public String getMetadataHash() { return metadataHash; }
	public void setMetadataHash(String value) { metadataHash = value; }
	public String getName() { return name; }
	public void setName(String value) { name = value; }
	public String getLang() { return lang; }
	public void setLang(String value) { lang = value; }
	public String getLayout() { return layout; }
	public void setLayout(String value) { layout = value; }
	public String getManaCost() { return manaCost; }
	public void setManaCost(String value) { manaCost = value; }
	public BigDecimal getCmc() { return cmc; }
	public void setCmc(BigDecimal value) { cmc = value; }
	public String getTypeLine() { return typeLine; }
	public void setTypeLine(String value) { typeLine = value; }
	public String getOracleText() { return oracleText; }
	public void setOracleText(String value) { oracleText = value; }
	public String[] getColors() { return colors; }
	public void setColors(String[] value) { colors = value; }
	public String[] getColorIdentity() { return colorIdentity; }
	public void setColorIdentity(String[] value) { colorIdentity = value; }
	public String[] getKeywords() { return keywords; }
	public void setKeywords(String[] value) { keywords = value; }
	public String[] getProducedMana() { return producedMana; }
	public void setProducedMana(String[] value) { producedMana = value; }
	public String getPower() { return power; }
	public void setPower(String value) { power = value; }
	public String getToughness() { return toughness; }
	public void setToughness(String value) { toughness = value; }
	public String getLoyalty() { return loyalty; }
	public void setLoyalty(String value) { loyalty = value; }
	public String getDefense() { return defense; }
	public void setDefense(String value) { defense = value; }
	public boolean isReserved() { return reserved; }
	public void setReserved(boolean value) { reserved = value; }
	public boolean isDigital() { return digital; }
	public void setDigital(boolean value) { digital = value; }
	public boolean isReprint() { return reprint; }
	public void setReprint(boolean value) { reprint = value; }
	public String getSetCode() { return setCode; }
	public void setSetCode(String value) { setCode = value; }
	public String getSetName() { return setName; }
	public void setSetName(String value) { setName = value; }
	public String getCollectorNumber() { return collectorNumber; }
	public void setCollectorNumber(String value) { collectorNumber = value; }
	public CardRarity getRarity() { return rarity; }
	public void setRarity(CardRarity value) { rarity = value; }
	public String getFlavorText() { return flavorText; }
	public void setFlavorText(String value) { flavorText = value; }
	public String getArtist() { return artist; }
	public void setArtist(String value) { artist = value; }
	public LocalDate getReleasedAt() { return releasedAt; }
	public void setReleasedAt(LocalDate value) { releasedAt = value; }
	public JsonNode getLegalities() { return legalities; }
	public void setLegalities(JsonNode value) { legalities = value; }
	public JsonNode getImageUris() { return imageUris; }
	public void setImageUris(JsonNode value) { imageUris = value; }
	public JsonNode getPrices() { return prices; }
	public void setPrices(JsonNode value) { prices = value; }
	public JsonNode getRelatedUris() { return relatedUris; }
	public void setRelatedUris(JsonNode value) { relatedUris = value; }
	public JsonNode getAllParts() { return allParts; }
	public void setAllParts(JsonNode value) { allParts = value; }
	public String getContextText() { return contextText; }
	public void setContextText(String value) { contextText = value; }
	public float[] getEmbedding() { return embedding; }
	public void setEmbedding(float[] value) { embedding = value; }
	public OffsetDateTime getCreatedAt() { return createdAt; }
}
