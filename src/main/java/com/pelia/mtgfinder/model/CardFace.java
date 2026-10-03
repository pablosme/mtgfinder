package com.pelia.mtgfinder.model;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.IdClass;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.BatchSize;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

@Entity
@Table(name = "card_face")
@BatchSize(size = 100)
@IdClass(CardFaceId.class)
public class CardFace {

    @Id
    @Column(name = "card_print_id", nullable = false)
    private UUID cardPrintId;

    @Id
    @Column(name = "face_index", nullable = false)
    private short faceIndex;

    @Column(name = "name", columnDefinition = "text", nullable = false)
    private String name;

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

    @Column(name = "colors", columnDefinition = "text[]")
    private String[] colors;

    @Column(name = "color_indicator", columnDefinition = "text[]")
    private String[] colorIndicator;

    @Column(name = "oracle_id")
    private UUID oracleId;

    @Column(name = "layout", columnDefinition = "text")
    private String layout;

    @Column(name = "printed_name", columnDefinition = "text")
    private String printedName;

    @Column(name = "printed_type_line", columnDefinition = "text")
    private String printedTypeLine;

    @Column(name = "printed_text", columnDefinition = "text")
    private String printedText;

    @Column(name = "flavor_name", columnDefinition = "text")
    private String flavorName;

    @Column(name = "flavor_text", columnDefinition = "text")
    private String flavorText;

    @Column(name = "artist", columnDefinition = "text")
    private String artist;

    @Column(name = "artist_id")
    private UUID artistId;

    @Column(name = "illustration_id")
    private UUID illustrationId;

    @Column(name = "watermark", columnDefinition = "text")
    private String watermark;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_uris", columnDefinition = "jsonb")
    private JsonNode imageUris;

    public UUID getCardPrintId() { return cardPrintId; }
    public void setCardPrintId(UUID value) { cardPrintId = value; }
    public short getFaceIndex() { return faceIndex; }
    public void setFaceIndex(short value) { faceIndex = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
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
    public String[] getColors() { return colors; }
    public void setColors(String[] value) { colors = value; }
    public String[] getColorIndicator() { return colorIndicator; }
    public void setColorIndicator(String[] value) { colorIndicator = value; }
    public UUID getOracleId() { return oracleId; }
    public void setOracleId(UUID value) { oracleId = value; }
    public String getLayout() { return layout; }
    public void setLayout(String value) { layout = value; }
    public String getPrintedName() { return printedName; }
    public void setPrintedName(String value) { printedName = value; }
    public String getPrintedTypeLine() { return printedTypeLine; }
    public void setPrintedTypeLine(String value) { printedTypeLine = value; }
    public String getPrintedText() { return printedText; }
    public void setPrintedText(String value) { printedText = value; }
    public String getFlavorName() { return flavorName; }
    public void setFlavorName(String value) { flavorName = value; }
    public String getFlavorText() { return flavorText; }
    public void setFlavorText(String value) { flavorText = value; }
    public String getArtist() { return artist; }
    public void setArtist(String value) { artist = value; }
    public UUID getArtistId() { return artistId; }
    public void setArtistId(UUID value) { artistId = value; }
    public UUID getIllustrationId() { return illustrationId; }
    public void setIllustrationId(UUID value) { illustrationId = value; }
    public String getWatermark() { return watermark; }
    public void setWatermark(String value) { watermark = value; }
    public JsonNode getImageUris() { return imageUris; }
    public void setImageUris(JsonNode value) { imageUris = value; }
}
