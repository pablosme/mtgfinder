package com.pelia.mtgfinder.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.pelia.mtgfinder.dto.ScryfallCard;

@Component
public class CardContextService {

    // Shared fields whose changes require refreshing context on retained prints.
    static final List<String> ORACLE_CONTEXT_FIELDS = List.of(
            "name",
            "mana_cost",
            "type_line",
            "oracle_text",
            "colors",
            "color_identity",
            "keywords",
            "produced_mana",
            "power",
            "toughness",
            "loyalty",
            "defense");

    public String build(ScryfallCard card) {
        StringBuilder context = new StringBuilder();
        appendText(context, "Name", card.name());
        appendText(context, "Printed name", card.printedName());
        appendText(context, "Language", card.lang());
        appendText(context, "Mana cost", card.manaCost());
        appendText(context, "Type", card.typeLine());
        appendText(context, "Printed type", card.printedTypeLine());
        appendText(context, "Rules", card.oracleText());
        appendText(context, "Printed rules", card.printedText());
        appendList(context, "Colors", card.colors());
        appendList(context, "Color identity", card.colorIdentity());
        appendList(context, "Keywords", card.keywords());
        appendList(context, "Produced mana", card.producedMana());
        appendText(context, "Power", card.power());
        appendText(context, "Toughness", card.toughness());
        appendText(context, "Loyalty", card.loyalty());
        appendText(context, "Defense", card.defense());
        appendText(context, "Set", card.setName());
        appendText(context, "Rarity", card.rarity());
        appendText(context, "Flavor text", card.flavorText());

        if (card.cardFaces() != null) {
            for (int index = 0; index < card.cardFaces().size(); index++) {
                ScryfallCard.CardFace face = card.cardFaces().get(index);
                context.append("Face ").append(index + 1).append(":\n");
                appendText(context, "Name", face.name());
                appendText(context, "Printed name", face.printedName());
                appendText(context, "Mana cost", face.manaCost());
                appendText(context, "Type", face.typeLine());
                appendText(context, "Printed type", face.printedTypeLine());
                appendText(context, "Rules", face.oracleText());
                appendText(context, "Printed rules", face.printedText());
                appendList(context, "Colors", face.colors());
                appendText(context, "Power", face.power());
                appendText(context, "Toughness", face.toughness());
                appendText(context, "Loyalty", face.loyalty());
                appendText(context, "Defense", face.defense());
                appendText(context, "Flavor text", face.flavorText());
            }
        }
        return context.toString().stripTrailing();
    }

    private void appendText(StringBuilder context, String label, String value) {
        if (value != null && !value.isBlank()) {
            context.append(label).append(": ").append(value.strip()).append('\n');
        }
    }

    private void appendList(StringBuilder context, String label, List<String> values) {
        if (values == null) {
            return;
        }
        String text = values.stream()
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(", "));
        if (!text.isBlank()) {
            context.append(label).append(": ").append(text).append('\n');
        }
    }
}
