package com.pelia.mtgfinder.model;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.BatchSize;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

@Entity
@Table(name = "card_oracle")
@BatchSize(size = 100)
public class CardOracle {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "oracle_id")
    private UUID oracleId;

    @Column(name = "fallback_print_id")
    private UUID fallbackPrintId;

    @Column(name = "name", columnDefinition = "text", nullable = false)
    private String name;

    @Column(name = "layout", columnDefinition = "text", nullable = false)
    private String layout;

    @Column(name = "mana_cost", columnDefinition = "text")
    private String manaCost;

    @Column(name = "cmc", columnDefinition = "numeric")
    private BigDecimal cmc;

    @Column(name = "type_line", columnDefinition = "text")
    private String typeLine;

    @Column(name = "oracle_text", columnDefinition = "text")
    private String oracleText;

    @Column(name = "power", columnDefinition = "text")
    private String power;

    @Column(name = "toughness", columnDefinition = "text")
    private String toughness;

    @Column(name = "loyalty", columnDefinition = "text")
    private String loyalty;

    @Column(name = "defense", columnDefinition = "text")
    private String defense;

    @Column(name = "hand_modifier", columnDefinition = "text")
    private String handModifier;

    @Column(name = "life_modifier", columnDefinition = "text")
    private String lifeModifier;

    @Column(name = "colors", columnDefinition = "text[]")
    private String[] colors;

    @Column(name = "color_identity", columnDefinition = "text[]", nullable = false)
    private String[] colorIdentity;

    @Column(name = "color_indicator", columnDefinition = "text[]")
    private String[] colorIndicator;

    @Column(name = "keywords", columnDefinition = "text[]", nullable = false)
    private String[] keywords;

    @Column(name = "produced_mana", columnDefinition = "text[]")
    private String[] producedMana;

    @Column(name = "reserved", nullable = false)
    private Boolean reserved;

    @Column(name = "game_changer")
    private Boolean gameChanger;

    @Column(name = "edhrec_rank")
    private Integer edhrecRank;

    @Column(name = "penny_rank")
    private Integer pennyRank;

    @Column(name = "prints_search_uri", columnDefinition = "text")
    private String printsSearchUri;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "legalities", columnDefinition = "jsonb", nullable = false)
    private JsonNode legalities;

    public UUID getId() { return id; }
    public void setId(UUID value) { id = value; }
    public UUID getOracleId() { return oracleId; }
    public void setOracleId(UUID value) { oracleId = value; }
    public UUID getFallbackPrintId() { return fallbackPrintId; }
    public void setFallbackPrintId(UUID value) { fallbackPrintId = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
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
    public String getPower() { return power; }
    public void setPower(String value) { power = value; }
    public String getToughness() { return toughness; }
    public void setToughness(String value) { toughness = value; }
    public String getLoyalty() { return loyalty; }
    public void setLoyalty(String value) { loyalty = value; }
    public String getDefense() { return defense; }
    public void setDefense(String value) { defense = value; }
    public String getHandModifier() { return handModifier; }
    public void setHandModifier(String value) { handModifier = value; }
    public String getLifeModifier() { return lifeModifier; }
    public void setLifeModifier(String value) { lifeModifier = value; }
    public String[] getColors() { return colors; }
    public void setColors(String[] value) { colors = value; }
    public String[] getColorIdentity() { return colorIdentity; }
    public void setColorIdentity(String[] value) { colorIdentity = value; }
    public String[] getColorIndicator() { return colorIndicator; }
    public void setColorIndicator(String[] value) { colorIndicator = value; }
    public String[] getKeywords() { return keywords; }
    public void setKeywords(String[] value) { keywords = value; }
    public String[] getProducedMana() { return producedMana; }
    public void setProducedMana(String[] value) { producedMana = value; }
    public Boolean getReserved() { return reserved; }
    public void setReserved(Boolean value) { reserved = value; }
    public Boolean getGameChanger() { return gameChanger; }
    public void setGameChanger(Boolean value) { gameChanger = value; }
    public Integer getEdhrecRank() { return edhrecRank; }
    public void setEdhrecRank(Integer value) { edhrecRank = value; }
    public Integer getPennyRank() { return pennyRank; }
    public void setPennyRank(Integer value) { pennyRank = value; }
    public String getPrintsSearchUri() { return printsSearchUri; }
    public void setPrintsSearchUri(String value) { printsSearchUri = value; }
    public JsonNode getLegalities() { return legalities; }
    public void setLegalities(JsonNode value) { legalities = value; }
}
