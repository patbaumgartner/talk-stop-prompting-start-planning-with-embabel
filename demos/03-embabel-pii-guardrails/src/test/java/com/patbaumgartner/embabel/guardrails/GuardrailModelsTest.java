package com.patbaumgartner.embabel.guardrails;

import com.patbaumgartner.embabel.guardrails.GuardrailModels.ApiSummarizeRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuardrailModelsTest {

	@Test
	void apiRequestMapsToSafeAndUnsafeDomainTypes() {
		var api = new ApiSummarizeRequest("hello world");

		var safe = api.toDomain();
		var unsafe = api.toUnsafeDomain();

		assertEquals("hello world", safe.text());
		assertEquals("hello world", unsafe.text());
	}

}
