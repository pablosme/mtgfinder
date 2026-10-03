package com.pelia.mtgfinder.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class CardFaceId implements Serializable {
    private UUID cardPrintId;
    private short faceIndex;

    public CardFaceId() {}

    public CardFaceId(UUID cardPrintId, short faceIndex) {
        this.cardPrintId = cardPrintId;
        this.faceIndex = faceIndex;
    }

    public UUID getCardPrintId() { return cardPrintId; }
    public short getFaceIndex() { return faceIndex; }

    @Override
    public boolean equals(Object other) {
        return other instanceof CardFaceId that
                && Objects.equals(cardPrintId, that.cardPrintId) && faceIndex == that.faceIndex;
    }

    @Override
    public int hashCode() { return Objects.hash(cardPrintId, faceIndex); }
}
