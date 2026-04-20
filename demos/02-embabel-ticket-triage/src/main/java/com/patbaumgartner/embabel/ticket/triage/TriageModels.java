package com.patbaumgartner.embabel.ticket.triage;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class TriageModels {

	private TriageModels() {
	}

	// -------- API --------

	public record TriageRequest(String ticketId, String text, String channel,
			// Demonstratively controls the flow:
			// "NORMAL" | "LOW_CONFIDENCE"
			String simulate) {
	}

	public record TriageResponse(String ticketId, String category, String priority, String targetTeam,
			double confidence, List<String> trace) {
	}

	// -------- REST API types --------

	public record ApiTriageRequest(@NotBlank String ticketId, @NotBlank String text, String channel, String simulate) {
		public TriageRequest toDomain() {
			return new TriageRequest(ticketId, text, channel, simulate);
		}
	}

	// -------- Agent Internal Types --------

	public record TraceLog(List<String> events) {
		public static TraceLog start() {
			return new TraceLog(new ArrayList<>());
		}

		public TraceLog add(String event) {
			events.add(Instant.now() + " | " + event);
			return this;
		}
	}

	// Signals extracted from the ticket
	public sealed interface Signals permits SignalsOk, SignalsNeedsDeep {

	}

	public record SignalsOk(String normalizedText, boolean mentionsSecurity, boolean mentionsOutage,
			boolean mentionsVipDeadline) implements Signals {
	}

	// Marker type: forces an additional replanning step
	public record SignalsNeedsDeep(String normalizedText, boolean mentionsSecurity, boolean mentionsOutage,
			boolean mentionsVipDeadline, String reason) implements Signals {
	}

	// Category assessments
	public sealed interface CategoryAssessment permits ConfidentCategory, UncertainCategory {

	}

	public record ConfidentCategory(String category, double confidence,
			String rationale) implements CategoryAssessment {
	}

	public record UncertainCategory(String bestGuessCategory, double confidence,
			String uncertaintyReason) implements CategoryAssessment {
	}

	public record PriorityAssessment(String priority, double confidence, String rationale) {
	}

	public record RoutingDecision(String targetTeam, String rationale) {
	}

}
