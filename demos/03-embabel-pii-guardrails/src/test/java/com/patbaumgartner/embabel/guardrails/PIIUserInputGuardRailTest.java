package com.patbaumgartner.embabel.guardrails;

import com.embabel.common.core.validation.ValidationResult;
import com.patbaumgartner.embabel.guardrails.GuardrailModels.AnalyzeResult;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PIIUserInputGuardRailTest {

	@Test
	void validatePassesWhenNoHighConfidencePiiFound() throws Exception {
		PresidioAnalyzerClient client = mock(PresidioAnalyzerClient.class);
		when(client.analyze(any())).thenReturn(List.of(new AnalyzeResult("EMAIL_ADDRESS", 0, 10, 0.30)));

		var guardRail = new PIIUserInputGuardRail(client, List.of("EMAIL_ADDRESS"));

		ValidationResult result = guardRail.validate("hello world", null);

		assertTrue(invokeIsValid(result));
		assertTrue(invokeErrors(result).isEmpty());
	}

	@Test
	void validateBlocksWhenHighConfidencePiiFound() throws Exception {
		PresidioAnalyzerClient client = mock(PresidioAnalyzerClient.class);
		when(client.analyze(any())).thenReturn(List.of(new AnalyzeResult("EMAIL_ADDRESS", 0, 10, 0.95)));

		var guardRail = new PIIUserInputGuardRail(client, List.of("EMAIL_ADDRESS"));

		ValidationResult result = guardRail.validate("john@example.com", null);

		assertFalse(invokeIsValid(result));
		var errors = invokeErrors(result);
		assertEquals(1, errors.size());
		assertTrue(errors.getFirst().toString().contains("PII_DETECTED"));
	}

	private static boolean invokeIsValid(ValidationResult result) throws Exception {
		Method m = result.getClass().getMethod("isValid");
		return (boolean) m.invoke(result);
	}

	@SuppressWarnings("unchecked")
	private static List<Object> invokeErrors(ValidationResult result) throws Exception {
		Method m = result.getClass().getMethod("getErrors");
		return (List<Object>) m.invoke(result);
	}

}
