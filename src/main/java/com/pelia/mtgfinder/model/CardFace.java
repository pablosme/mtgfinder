package com.pelia.mtgfinder.model;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import tools.jackson.databind.JsonNode;

@Entity
@Table(name = "card_faces")
public class CardFace {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(name = "import_id", nullable = false)
	private UUID importId;
	@Column(name = "card_id", nullable = false)
	private UUID cardId;
	@Column(name = "face_index", nullable = false)
	private short faceIndex;
	@Column(nullable = false, length = 255)
	private String name;
	@Column(name = "oracle_id")
	private UUID oracleId;
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
	@Column(length = 30)
	private String power;
	@Column(length = 30)
	private String toughness;
	@Column(length = 30)
	private String loyalty;
	@Column(length = 30)
	private String defense;
	@Column(name = "flavor_text", columnDefinition = "text")
	private String flavorText;
	@Column(length = 255)
	private String artist;
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "image_uris", columnDefinition = "jsonb")
	private JsonNode imageUris;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumns({
		@JoinColumn(name = "import_id", referencedColumnName = "import_id", insertable = false, updatable = false),
		@JoinColumn(name = "card_id", referencedColumnName = "id", insertable = false, updatable = false)
	})
	private Card card;

	public Long getId() { return id; }
	public UUID getImportId() { return importId; }
	public void setImportId(UUID value) { importId = value; }
	public UUID getCardId() { return cardId; }
	public void setCardId(UUID value) { cardId = value; }
	public short getFaceIndex() { return faceIndex; }
	public void setFaceIndex(short value) { faceIndex = value; }
	public String getName() { return name; }
	public void setName(String value) { name = value; }
	public UUID getOracleId() { return oracleId; }
	public void setOracleId(UUID value) { oracleId = value; }
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
	public String getPower() { return power; }
	public void setPower(String value) { power = value; }
	public String getToughness() { return toughness; }
	public void setToughness(String value) { toughness = value; }
	public String getLoyalty() { return loyalty; }
	public void setLoyalty(String value) { loyalty = value; }
	public String getDefense() { return defense; }
	public void setDefense(String value) { defense = value; }
	public String getFlavorText() { return flavorText; }
	public void setFlavorText(String value) { flavorText = value; }
	public String getArtist() { return artist; }
	public void setArtist(String value) { artist = value; }
	public JsonNode getImageUris() { return imageUris; }
	public void setImageUris(JsonNode value) { imageUris = value; }
	public Card getCard() { return card; }
}
