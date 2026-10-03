package com.pelia.mtgfinder.repository;

import java.util.UUID;

public interface OracleSearchMatch {
    UUID getCardIdentityId();
    Double getSimilarityScore();
}
