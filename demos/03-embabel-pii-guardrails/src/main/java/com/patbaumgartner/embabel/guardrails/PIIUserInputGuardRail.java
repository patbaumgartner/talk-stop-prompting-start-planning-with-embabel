package com.patbaumgartner.embabel.guardrails;

import com.embabel.agent.api.validation.guardrails.UserInputGuardRail;
import com.embabel.agent.core.Blackboard;
import com.embabel.common.core.validation.ValidationError;
import com.embabel.common.core.validation.ValidationResult;
import com.embabel.common.core.validation.ValidationSeverity;
import com.patbaumgartner.embabel.guardrails.GuardrailModels.AnalyzeRequest;
import com.patbaumgartner.embabel.guardrails.GuardrailModels.AnalyzeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Guard rail that intercepts user input BEFORE it reaches the LLM.
 *
 * Calls Microsoft Presidio to detect PII entities (names, emails, phone numbers, etc.).
 * If PII is detected with high confidence, the request is BLOCKED (CRITICAL severity).
 *
 * Key demo point: - Guard rails run BEFORE the LLM call — PII never leaves the system -
 * Uses an external service (Presidio) — not regex, not the LLM itself - Plugged into the
 * PromptRunner with .withGuardRails() — composable
 */
public class PIIUserInputGuardRail implements UserInputGuardRail {

	private static final Logger log = LoggerFactory.getLogger(PIIUserInputGuardRail.class);

	private static final double CONFIDENCE_THRESHOLD = 0.7;

	private final PresidioAnalyzerClient presidioAnalyzerClient;

	private final List<String> piiTypes;

	public PIIUserInputGuardRail(PresidioAnalyzerClient presidioAnalyzerClient, List<String> piiTypes) {
		this.presidioAnalyzerClient = presidioAnalyzerClient;
		this.piiTypes = piiTypes;
	}

	@Override
	public String getName() {
		return "PIIGuardRail";
	}

	@Override
	public String getDescription() {
		return "Blocks requests containing PII detected by Presidio";
	}

	@Override
	public ValidationResult validate(String userInput, Blackboard blackboard) {
		log.info("PII Guard Rail: scanning input ({} chars)", userInput.length());

		List<AnalyzeResult> results = presidioAnalyzerClient.analyze(new AnalyzeRequest(userInput, "en", piiTypes));

		List<AnalyzeResult> highConfidence = results.stream().filter(r -> r.score() >= CONFIDENCE_THRESHOLD).toList();

		if (highConfidence.isEmpty()) {
			log.info("PII Guard Rail: no PII detected — request passes");
			return new ValidationResult(true, Collections.emptyList());
		}

		String detected = highConfidence.stream()
			.map(r -> "%s (score=%.2f)".formatted(r.entity_type(), r.score()))
			.collect(Collectors.joining(", "));

		log.warn("PII Guard Rail: BLOCKED — detected: {}", detected);

		return new ValidationResult(false, List.of(new ValidationError("PII_DETECTED",
				"PII detected in input: " + detected + ". Request blocked.", ValidationSeverity.CRITICAL)));
	}

}
