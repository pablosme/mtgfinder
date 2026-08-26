package com.pelia.mtgfinder.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class ScryfallCardTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void deserializesMalformedPreviewSourceUriAsText() {
		String sourceUri = "https://www.facebook.com/photo/?fbid=1&set=invalid Card name";
		String json = """
				{
				  "preview": {
				    "source_uri": "%s",
				    "source": "Facebook"
				  }
				}
				""".formatted(sourceUri);

		ScryfallCard card = objectMapper.readValue(json, ScryfallCard.class);

		assertThat(card.preview().sourceUri()).isEqualTo(sourceUri);
	}
}
