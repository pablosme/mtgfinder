package com.pelia.mtgfinder.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScryfallBulkDataResponse(
		String object,
		String id,
		String type,
		@JsonProperty("updated_at") OffsetDateTime updatedAt,
		String uri,
		String name,
		String description,
		@JsonProperty("jsonl_download_uri") String jsonlDownloadUri,
		@JsonProperty("compressed_size") Long compressedSize) {
}
