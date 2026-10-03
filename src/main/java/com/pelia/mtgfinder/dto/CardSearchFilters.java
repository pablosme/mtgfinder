package com.pelia.mtgfinder.dto;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Optional filters shared by ordinary, fuzzy and semantic searches. */
public record CardSearchFilters(
        String type,
        String keyword,
        String colors,
        String colorIdentity,
        String legalityFormat,
        String legalityStatus,
        String rarity,
        String set,
        String collection,
        BigDecimal cmc,
        String layout,
        String power,
        String toughness,
        String loyalty) {

    public static CardSearchFilters from(Map<String, String> parameters) {
        String legality = text(parameters.get("legality"));
        String format = null;
        String status = null;
        if (legality != null) {
            String[] parts = legality.toLowerCase(Locale.ROOT).split(":", -1);
            format = parts[0].strip();
            status = parts.length == 1 ? "legal" : parts[1].strip();
            if (parts.length > 2 || !format.matches("[a-z0-9_]+")
                    || !Set.of("legal", "not_legal", "restricted", "banned").contains(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Legality must be a format or format:legal, format:not_legal, format:restricted or format:banned");
            }
        }

        String rarity = text(parameters.get("rarity"));
        if (rarity != null) {
            rarity = rarity.toLowerCase(Locale.ROOT);
            if (!Set.of("common", "uncommon", "rare", "mythic", "special", "bonus").contains(rarity)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid rarity");
            }
        }

        BigDecimal cmc = null;
        String manaValue = text(parameters.get("cmc"));
        if (manaValue != null) {
            try {
                cmc = new BigDecimal(manaValue);
                if (cmc.signum() < 0) throw new NumberFormatException();
            } catch (NumberFormatException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CMC must be a nonnegative number");
            }
        }

        return new CardSearchFilters(
                text(parameters.get("type")), text(parameters.get("keyword")),
                colors(parameters.get("colors")), colors(parameters.get("color_identity")),
                format, status, rarity, text(parameters.get("set")), text(parameters.get("collection")),
                cmc, text(parameters.get("layout")), text(parameters.get("power")),
                text(parameters.get("toughness")), text(parameters.get("loyalty")));
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String colors(String value) {
        String normalized = text(value);
        if (normalized == null) return null;
        normalized = normalized.toUpperCase(Locale.ROOT).replaceAll("[\\s,]", "");
        if (normalized.equals("C")) return normalized;
        if (!normalized.matches("[WUBRG]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Colors must contain W, U, B, R, G or C alone for colorless");
        }
        return normalized.chars().distinct().mapToObj(color -> String.valueOf((char) color))
                .collect(Collectors.joining(","));
    }
}
