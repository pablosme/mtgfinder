package com.pelia.mtgfinder.dto;

import java.util.UUID;

public record PendingCardEmbedding(UUID cardIdentityId, String lang, UUID cardPrintId, String contextText) {}
