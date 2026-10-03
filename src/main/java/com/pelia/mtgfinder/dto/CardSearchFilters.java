package com.pelia.mtgfinder.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Optional search filters. Color selections require all values; other lists contain alternatives. */
public record CardSearchFilters(
        List<String> type,
        List<String> keyword,
        List<String> colors,
        List<String> colorIdentity,
        List<String> legality,
        List<String> rarity,
        List<String> set,
        List<String> collection,
        List<BigDecimal> cmc,
        List<String> layout,
        List<String> power,
        List<String> toughness,
        List<String> loyalty) {

    public static CardSearchFilters from(Map<String, List<String>> parameters) {
        List<String> legalities = text(parameters.get("legality")).stream().map(value -> {
            String[] parts = value.toLowerCase(Locale.ROOT).split(":", -1);
            String format = parts[0].strip();
            String status = parts.length == 1 ? "legal" : parts[1].strip();
            if (parts.length > 2 || !format.matches("[a-z0-9_]+")
                    || !Set.of("legal", "not_legal", "restricted", "banned").contains(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Legality must be a format or format:legal, format:not_legal, format:restricted or format:banned");
            }
            return format + ":" + status;
        }).distinct().toList();

        List<String> rarities = text(parameters.get("rarity")).stream().map(value -> {
            String rarity = value.toLowerCase(Locale.ROOT);
            if (!Set.of("common", "uncommon", "rare", "mythic", "special", "bonus").contains(rarity)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid rarity");
            }
            return rarity;
        }).distinct().toList();

        List<BigDecimal> manaValues = text(parameters.get("cmc")).stream().map(value -> {
            try {
                BigDecimal cmc = new BigDecimal(value);
                if (cmc.signum() < 0) throw new NumberFormatException();
                return cmc;
            } catch (NumberFormatException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CMC must be a nonnegative number");
            }
        }).distinct().toList();

        return new CardSearchFilters(
                text(parameters.get("type")), text(parameters.get("keyword")),
                colors(parameters.get("colors")), colors(parameters.get("color_identity")),
                legalities, rarities, text(parameters.get("set")), text(parameters.get("collection")),
                manaValues, text(parameters.get("layout")), text(parameters.get("power")),
                text(parameters.get("toughness")), text(parameters.get("loyalty")));
    }

    private static List<String> text(List<String> values) {
        if (values == null) return List.of();
        return values.stream().filter(Objects::nonNull).map(String::strip)
                .filter(value -> !value.isEmpty()).distinct().toList();
    }

    private static List<String> colors(List<String> values) {
        List<String> selected = text(values).stream().map(value -> {
            String normalized = value.toUpperCase(Locale.ROOT).replaceAll("[\\s,]", "");
            if (normalized.equals("C")) return normalized;
            if (!normalized.matches("[WUBRG]+")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Colors must contain W, U, B, R, G or C alone for colorless");
            }
            return normalized;
        }).flatMap(value -> value.chars().mapToObj(color -> String.valueOf((char) color)))
                .distinct().toList();
        if (selected.contains("C") && selected.size() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Colorless C cannot be combined with other colors");
        }
        return selected;
    }
}
