package com.patbaumgartner.embabel.guardrails;

import jakarta.validation.constraints.NotBlank;

/**
 * Domain model for PII guardrails demo.
 *
 * Flow: SummarizeRequest → [summarize with guardrails] → Summary
 *
 * The guard rail intercepts user input BEFORE it reaches the LLM, detects PII via
 * Presidio, and blocks the request if PII is found.
 */
public class GuardrailModels {

	// Input

	public record SummarizeRequest(String text) {
	}

	// Output

	public record Summary(String summary, boolean piiDetected, String piiDetails) {
	}

	// Presidio payloads

	public record AnalyzeRequest(String text, String language, java.util.List<String> entities) {
	}

	public record AnalyzeResult(String entity_type, int start, int end, double score) {
	}

	// REST API types

	public record ApiSummarizeRequest(@NotBlank String text) {
		public SummarizeRequest toDomain() {
			return new SummarizeRequest(text);
		}

		public UnsafeSummarizeRequest toUnsafeDomain() {
			return new UnsafeSummarizeRequest(text);
		}
	}

	public record UnsafeSummarizeRequest(String text) {
	}

}
