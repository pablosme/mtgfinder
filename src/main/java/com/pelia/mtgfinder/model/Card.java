package com.pelia.mtgfinder.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.BatchSize;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

@Entity
@Table(name = "card_print")
@BatchSize(size = 100)
public class Card {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "card_identity_id", nullable = false)
    private UUID cardIdentityId;

    @Column(name = "import_id", nullable = false)
    private UUID importId;

    @Column(name = "metadata_hash")
    private String metadataHash;

    @Column(name = "context_text", columnDefinition = "text")
    private String contextText;

    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "lang", columnDefinition = "text", nullable = false)
    private String lang;

    @Column(name = "released_at")
    private LocalDate releasedAt;

    @Column(name = "multiverse_ids", columnDefinition = "bigint[]")
    private Long[] multiverseIds;

    @Column(name = "mtgo_id")
    private Long mtgoId;

    @Column(name = "mtgo_foil_id")
    private Long mtgoFoilId;

    @Column(name = "arena_id")
    private Long arenaId;

    @Column(name = "tcgplayer_id")
    private Long tcgplayerId;

    @Column(name = "tcgplayer_etched_id")
    private Long tcgplayerEtchedId;

    @Column(name = "cardmarket_id")
    private Long cardmarketId;

    @Column(name = "resource_id", columnDefinition = "text")
    private String resourceId;

    @Column(name = "uri", columnDefinition = "text")
    private String uri;

    @Column(name = "scryfall_uri", columnDefinition = "text")
    private String scryfallUri;

    @Column(name = "rulings_uri", columnDefinition = "text")
    private String rulingsUri;

    @Column(name = "set_id")
    private UUID setId;

    @Column(name = "set_code", columnDefinition = "text", nullable = false)
    private String setCode;

    @Column(name = "set_name", columnDefinition = "text", nullable = false)
    private String setName;

    @Column(name = "set_type", columnDefinition = "text")
    private String setType;

    @Column(name = "set_uri", columnDefinition = "text")
    private String setUri;

    @Column(name = "set_search_uri", columnDefinition = "text")
    private String setSearchUri;

    @Column(name = "scryfall_set_uri", columnDefinition = "text")
    private String scryfallSetUri;

    @Column(name = "collector_number", columnDefinition = "text", nullable = false)
    private String collectorNumber;

    @Convert(converter = CardRarityConverter.class)
    @Column(name = "rarity", columnDefinition = "text", nullable = false)
    private CardRarity rarity;

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

    @Column(name = "artist_ids", columnDefinition = "uuid[]")
    private UUID[] artistIds;

    @Column(name = "illustration_id")
    private UUID illustrationId;

    @Column(name = "watermark", columnDefinition = "text")
    private String watermark;

    @Column(name = "card_back_id")
    private UUID cardBackId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_uris", columnDefinition = "jsonb")
    private JsonNode imageUris;

    @Column(name = "highres_image")
    private Boolean highresImage;

    @Column(name = "image_status", columnDefinition = "text")
    private String imageStatus;

    @Column(name = "image_updated_at")
    private OffsetDateTime imageUpdatedAt;

    @Column(name = "games", columnDefinition = "text[]")
    private String[] games;

    @Column(name = "finishes", columnDefinition = "text[]")
    private String[] finishes;

    @Column(name = "foil")
    private Boolean foil;

    @Column(name = "nonfoil")
    private Boolean nonfoil;

    @Column(name = "oversized")
    private Boolean oversized;

    @Column(name = "promo")
    private Boolean promo;

    @Column(name = "promo_types", columnDefinition = "text[]")
    private String[] promoTypes;

    @Column(name = "reprint")
    private Boolean reprint;

    @Column(name = "variation")
    private Boolean variation;

    @Column(name = "variation_of")
    private UUID variationOf;

    @Column(name = "digital")
    private Boolean digital;

    @Column(name = "border_color", columnDefinition = "text")
    private String borderColor;

    @Column(name = "frame", columnDefinition = "text")
    private String frame;

    @Column(name = "frame_effects", columnDefinition = "text[]")
    private String[] frameEffects;

    @Column(name = "security_stamp", columnDefinition = "text")
    private String securityStamp;

    @Column(name = "full_art")
    private Boolean fullArt;

    @Column(name = "textless")
    private Boolean textless;

    @Column(name = "booster")
    private Boolean booster;

    @Column(name = "story_spotlight")
    private Boolean storySpotlight;

    @Column(name = "content_warning")
    private Boolean contentWarning;

    @Column(name = "attraction_lights", columnDefinition = "smallint[]")
    private Short[] attractionLights;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "all_parts", columnDefinition = "jsonb")
    private JsonNode allParts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prices", columnDefinition = "jsonb")
    private JsonNode prices;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "related_uris", columnDefinition = "jsonb")
    private JsonNode relatedUris;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "purchase_uris", columnDefinition = "jsonb")
    private JsonNode purchaseUris;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preview", columnDefinition = "jsonb")
    private JsonNode preview;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_identity_id", insertable = false, updatable = false)
    private CardOracle oracle;

    public UUID getId() { return id; }
    public void setId(UUID value) { id = value; }
    public UUID getCardIdentityId() { return cardIdentityId; }
    public void setCardIdentityId(UUID value) { cardIdentityId = value; }
    public UUID getImportId() { return importId; }
    public void setImportId(UUID value) { importId = value; }
    public String getMetadataHash() { return metadataHash; }
    public void setMetadataHash(String value) { metadataHash = value; }
    public String getContextText() { return contextText; }
    public void setContextText(String value) { contextText = value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public String getLang() { return lang; }
    public void setLang(String value) { lang = value; }
    public LocalDate getReleasedAt() { return releasedAt; }
    public void setReleasedAt(LocalDate value) { releasedAt = value; }
    public Long[] getMultiverseIds() { return multiverseIds; }
    public void setMultiverseIds(Long[] value) { multiverseIds = value; }
    public Long getMtgoId() { return mtgoId; }
    public void setMtgoId(Long value) { mtgoId = value; }
    public Long getMtgoFoilId() { return mtgoFoilId; }
    public void setMtgoFoilId(Long value) { mtgoFoilId = value; }
    public Long getArenaId() { return arenaId; }
    public void setArenaId(Long value) { arenaId = value; }
    public Long getTcgplayerId() { return tcgplayerId; }
    public void setTcgplayerId(Long value) { tcgplayerId = value; }
    public Long getTcgplayerEtchedId() { return tcgplayerEtchedId; }
    public void setTcgplayerEtchedId(Long value) { tcgplayerEtchedId = value; }
    public Long getCardmarketId() { return cardmarketId; }
    public void setCardmarketId(Long value) { cardmarketId = value; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String value) { resourceId = value; }
    public String getUri() { return uri; }
    public void setUri(String value) { uri = value; }
    public String getScryfallUri() { return scryfallUri; }
    public void setScryfallUri(String value) { scryfallUri = value; }
    public String getRulingsUri() { return rulingsUri; }
    public void setRulingsUri(String value) { rulingsUri = value; }
    public UUID getSetId() { return setId; }
    public void setSetId(UUID value) { setId = value; }
    public String getSetCode() { return setCode; }
    public void setSetCode(String value) { setCode = value; }
    public String getSetName() { return setName; }
    public void setSetName(String value) { setName = value; }
    public String getSetType() { return setType; }
    public void setSetType(String value) { setType = value; }
    public String getSetUri() { return setUri; }
    public void setSetUri(String value) { setUri = value; }
    public String getSetSearchUri() { return setSearchUri; }
    public void setSetSearchUri(String value) { setSearchUri = value; }
    public String getScryfallSetUri() { return scryfallSetUri; }
    public void setScryfallSetUri(String value) { scryfallSetUri = value; }
    public String getCollectorNumber() { return collectorNumber; }
    public void setCollectorNumber(String value) { collectorNumber = value; }
    public CardRarity getRarity() { return rarity; }
    public void setRarity(CardRarity value) { rarity = value; }
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
    public UUID[] getArtistIds() { return artistIds; }
    public void setArtistIds(UUID[] value) { artistIds = value; }
    public UUID getIllustrationId() { return illustrationId; }
    public void setIllustrationId(UUID value) { illustrationId = value; }
    public String getWatermark() { return watermark; }
    public void setWatermark(String value) { watermark = value; }
    public UUID getCardBackId() { return cardBackId; }
    public void setCardBackId(UUID value) { cardBackId = value; }
    public JsonNode getImageUris() { return imageUris; }
    public void setImageUris(JsonNode value) { imageUris = value; }
    public Boolean getHighresImage() { return highresImage; }
    public void setHighresImage(Boolean value) { highresImage = value; }
    public String getImageStatus() { return imageStatus; }
    public void setImageStatus(String value) { imageStatus = value; }
    public OffsetDateTime getImageUpdatedAt() { return imageUpdatedAt; }
    public void setImageUpdatedAt(OffsetDateTime value) { imageUpdatedAt = value; }
    public String[] getGames() { return games; }
    public void setGames(String[] value) { games = value; }
    public String[] getFinishes() { return finishes; }
    public void setFinishes(String[] value) { finishes = value; }
    public Boolean getFoil() { return foil; }
    public void setFoil(Boolean value) { foil = value; }
    public Boolean getNonfoil() { return nonfoil; }
    public void setNonfoil(Boolean value) { nonfoil = value; }
    public Boolean getOversized() { return oversized; }
    public void setOversized(Boolean value) { oversized = value; }
    public Boolean getPromo() { return promo; }
    public void setPromo(Boolean value) { promo = value; }
    public String[] getPromoTypes() { return promoTypes; }
    public void setPromoTypes(String[] value) { promoTypes = value; }
    public Boolean getReprint() { return reprint; }
    public void setReprint(Boolean value) { reprint = value; }
    public Boolean getVariation() { return variation; }
    public void setVariation(Boolean value) { variation = value; }
    public UUID getVariationOf() { return variationOf; }
    public void setVariationOf(UUID value) { variationOf = value; }
    public Boolean getDigital() { return digital; }
    public void setDigital(Boolean value) { digital = value; }
    public String getBorderColor() { return borderColor; }
    public void setBorderColor(String value) { borderColor = value; }
    public String getFrame() { return frame; }
    public void setFrame(String value) { frame = value; }
    public String[] getFrameEffects() { return frameEffects; }
    public void setFrameEffects(String[] value) { frameEffects = value; }
    public String getSecurityStamp() { return securityStamp; }
    public void setSecurityStamp(String value) { securityStamp = value; }
    public Boolean getFullArt() { return fullArt; }
    public void setFullArt(Boolean value) { fullArt = value; }
    public Boolean getTextless() { return textless; }
    public void setTextless(Boolean value) { textless = value; }
    public Boolean getBooster() { return booster; }
    public void setBooster(Boolean value) { booster = value; }
    public Boolean getStorySpotlight() { return storySpotlight; }
    public void setStorySpotlight(Boolean value) { storySpotlight = value; }
    public Boolean getContentWarning() { return contentWarning; }
    public void setContentWarning(Boolean value) { contentWarning = value; }
    public Short[] getAttractionLights() { return attractionLights; }
    public void setAttractionLights(Short[] value) { attractionLights = value; }
    public JsonNode getAllParts() { return allParts; }
    public void setAllParts(JsonNode value) { allParts = value; }
    public JsonNode getPrices() { return prices; }
    public void setPrices(JsonNode value) { prices = value; }
    public JsonNode getRelatedUris() { return relatedUris; }
    public void setRelatedUris(JsonNode value) { relatedUris = value; }
    public JsonNode getPurchaseUris() { return purchaseUris; }
    public void setPurchaseUris(JsonNode value) { purchaseUris = value; }
    public JsonNode getPreview() { return preview; }
    public void setPreview(JsonNode value) { preview = value; }

    public CardOracle getOracle() { return oracle; }
    public UUID getOracleId() { return oracle.getOracleId(); }
    public String getName() { return oracle.getName(); }
    public String getLayout() { return oracle.getLayout(); }
    public String getManaCost() { return oracle.getManaCost(); }
    public BigDecimal getCmc() { return oracle.getCmc(); }
    public String getTypeLine() { return oracle.getTypeLine(); }
    public String getOracleText() { return oracle.getOracleText(); }
    public String getPower() { return oracle.getPower(); }
    public String getToughness() { return oracle.getToughness(); }
    public String getLoyalty() { return oracle.getLoyalty(); }
    public String getDefense() { return oracle.getDefense(); }
    public String getHandModifier() { return oracle.getHandModifier(); }
    public String getLifeModifier() { return oracle.getLifeModifier(); }
    public String[] getColors() { return oracle.getColors(); }
    public String[] getColorIdentity() { return oracle.getColorIdentity(); }
    public String[] getColorIndicator() { return oracle.getColorIndicator(); }
    public String[] getKeywords() { return oracle.getKeywords(); }
    public String[] getProducedMana() { return oracle.getProducedMana(); }
    public Boolean getReserved() { return oracle.getReserved(); }
    public Boolean getGameChanger() { return oracle.getGameChanger(); }
    public Integer getEdhrecRank() { return oracle.getEdhrecRank(); }
    public Integer getPennyRank() { return oracle.getPennyRank(); }
    public String getPrintsSearchUri() { return oracle.getPrintsSearchUri(); }
    public JsonNode getLegalities() { return oracle.getLegalities(); }
}
