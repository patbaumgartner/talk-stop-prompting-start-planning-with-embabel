package com.patbaumgartner.embabel.ticket.triage;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.patbaumgartner.embabel.ticket.triage.TriageModels.*;

@Agent(description = "Business-oriented ticket triage agent (category, priority, routing) with replanning")
public class TicketTriageAgent {

	@Action
	public TraceLog initTrace(TriageRequest req) {
		return TraceLog.start().add("initTrace: ticketId=" + req.ticketId() + ", simulate=" + safe(req.simulate()));
	}

	@Action
	public Signals extractSignals(TriageRequest req, TraceLog trace) {
		String text = safe(req.text()).trim();
		String normalized = text.toLowerCase();

		boolean mentionsSecurity = normalized.contains("data leak") || normalized.contains("security")
				|| normalized.contains("phishing") || normalized.contains("ransomware")
				|| normalized.contains("credential") || normalized.contains("credentials compromised");

		boolean mentionsOutage = normalized.contains("down") || normalized.contains("outage")
				|| normalized.contains("not reachable") || normalized.contains("cannot reach")
				|| normalized.contains("unavailable") || normalized.contains("service unavailable");

		// Intentionally simple, deterministic keyword detection for a demo article.
		boolean mentionsVipDeadline = normalized.contains("vip") || normalized.contains("executive")
				|| normalized.contains("board") || normalized.contains("in 30 minutes")
				|| normalized.contains("in 30 mins") || normalized.contains("customer meeting")
				|| normalized.contains("deadline");

		// Reproducible replanning trigger (demo): force the uncertain path.
		if ("LOW_CONFIDENCE".equalsIgnoreCase(safe(req.simulate()))) {
			trace.add("extractSignals: SignalsNeedsDeep(reason=simulate=LOW_CONFIDENCE)");
			return new SignalsNeedsDeep(normalized, mentionsSecurity, mentionsOutage, mentionsVipDeadline,
					"simulate=LOW_CONFIDENCE");
		}

		trace.add("extractSignals: SignalsOk(security=" + mentionsSecurity + ", outage=" + mentionsOutage + ", vip="
				+ mentionsVipDeadline + ")");

		return new SignalsOk(normalized, mentionsSecurity, mentionsOutage, mentionsVipDeadline);
	}

	// Normal path: we can classify confidently right away (heuristic,
	// deterministic)
	@Action
	public ConfidentCategory quickClassifyOk(SignalsOk s, TraceLog trace) {
		String category;
		String rationale;

		if (s.mentionsSecurity()) {
			category = "SECURITY";
			rationale = "Security keywords detected.";
		} else if (s.mentionsOutage()) {
			category = "INCIDENT";
			rationale = "Outage/reachability detected.";
		} else {
			category = "SERVICE_REQUEST";
			rationale = "No outage/security; defaulting to service request.";
		}

		trace.add("quickClassify: ConfidentCategory(" + category + ", confidence=0.85)");
		return new ConfidentCategory(category, 0.85, rationale);
	}

	// Replanning path: intentionally returns an uncertain result (forces
	// deepClassify)
	@Action
	public UncertainCategory quickClassifyNeedsDeep(SignalsNeedsDeep s, TraceLog trace) {
		String bestGuess = s.mentionsSecurity() ? "SECURITY" : (s.mentionsOutage() ? "INCIDENT" : "SERVICE_REQUEST");

		trace.add("quickClassify: UncertainCategory(bestGuess=" + bestGuess + ", confidence=0.45, reason=" + s.reason()
				+ ")");

		return new UncertainCategory(bestGuess, 0.45, "Insufficient signals / forced via " + s.reason());
	}

	// Converts uncertainty -> confident via LLM (OpenAI)
	@Action
	public ConfidentCategory deepClassify(UncertainCategory u, Signals s, OperationContext context, TraceLog trace) {
		trace.add("deepClassify: invoking LLM to resolve uncertainty");

		// Intentionally structured output: no string parsing.
		record LlmCategory(String category, double confidence, String rationale) {
		}

		String normalizedText = s instanceof SignalsOk so ? so.normalizedText()
				: ((SignalsNeedsDeep) s).normalizedText();

		String prompt = """
				You are a support triage system. Classify the ticket into exactly one of these categories:
				- INCIDENT
				- SERVICE_REQUEST
				- SECURITY

				Return JSON for: { \"category\": \"...\", \"confidence\": 0.0-1.0, \"rationale\": \"...\" }

				Ticket (normalized):
				%s

				Current prediction (uncertain):
				bestGuess=%s, confidence=%s, reason=%s
				""".formatted(normalizedText, u.bestGuessCategory(), u.confidence(), u.uncertaintyReason());

		LlmCategory llm = context.ai().withDefaultLlm().creating(LlmCategory.class).fromPrompt(prompt);

		double conf = clamp01(llm.confidence());
		trace.add("deepClassify: ConfidentCategory(" + llm.category() + ", confidence=" + conf + ")");

		return new ConfidentCategory(llm.category(), conf, llm.rationale());
	}

	@Action
	public PriorityAssessment assessPriority(ConfidentCategory category, Signals s, OperationContext context,
			TraceLog trace) {

		// Deterministic fast-path for SECURITY.
		if ("SECURITY".equalsIgnoreCase(category.category())) {
			trace.add("assessPriority: SECURITY => CRITICAL (deterministic)");
			return new PriorityAssessment("CRITICAL", 0.95, "Security cases are always critical.");
		}

		boolean outage = s instanceof SignalsOk so ? so.mentionsOutage() : ((SignalsNeedsDeep) s).mentionsOutage();
		boolean vip = s instanceof SignalsOk so ? so.mentionsVipDeadline()
				: ((SignalsNeedsDeep) s).mentionsVipDeadline();

		// Deterministic fast-path for outage + VIP.
		if (outage && vip) {
			trace.add("assessPriority: outage+vip => CRITICAL (deterministic)");
			return new PriorityAssessment("CRITICAL", 0.9, "Outage + time pressure (VIP/deadline).");
		}

		// Deterministic fast-path for outage.
		if (outage) {
			trace.add("assessPriority: outage => HIGH (deterministic)");
			return new PriorityAssessment("HIGH", 0.85, "Outage/reachability issue.");
		}

		// Otherwise use the LLM for a more nuanced assessment.
		trace.add("assessPriority: invoking LLM for non-outage");

		record LlmPriority(String priority, double confidence, String rationale) {
		}

		String normalizedText = s instanceof SignalsOk so ? so.normalizedText()
				: ((SignalsNeedsDeep) s).normalizedText();

		String prompt = """
				You are a support triage system. Assess the priority as exactly one of:
				- LOW
				- MEDIUM
				- HIGH
				- CRITICAL

				Return JSON for: { \"priority\": \"...\", \"confidence\": 0.0-1.0, \"rationale\": \"...\" }

				Category: %s
				Ticket (normalized):
				%s
				""".formatted(category.category(), normalizedText);

		LlmPriority llm = context.ai().withDefaultLlm().creating(LlmPriority.class).fromPrompt(prompt);

		double conf = clamp01(llm.confidence());
		trace.add("assessPriority: " + llm.priority() + " (confidence=" + conf + ")");

		return new PriorityAssessment(llm.priority(), conf, llm.rationale());
	}

	@Action
	public RoutingDecision route(ConfidentCategory category, PriorityAssessment prio, TraceLog trace) {
		String team;
		String rationale;

		if ("SECURITY".equalsIgnoreCase(category.category())) {
			team = "security-incident";
			rationale = "Security always goes to Security Incident Response.";
		} else if ("INCIDENT".equalsIgnoreCase(category.category())) {
			team = "platform-ops";
			rationale = "Incidents go to Ops (SRE/Platform).";
		} else {
			// Service requests:
			if ("CRITICAL".equalsIgnoreCase(prio.priority()) || "HIGH".equalsIgnoreCase(prio.priority())) {
				team = "service-desk-l2";
				rationale = "High priority => L2.";
			} else {
				team = "service-desk-l1";
				rationale = "Normal => L1.";
			}
		}

		trace.add("route: targetTeam=" + team);
		return new RoutingDecision(team, rationale);
	}

	@AchievesGoal(description = "Ticket is classified, prioritized and routed; trace is included")
	@Action
	public TriageResponse finalizeResponse(TriageRequest req, ConfidentCategory cat, PriorityAssessment prio,
			RoutingDecision routing, TraceLog trace) {

		trace.add("finalizeResponse: done");

		return new TriageResponse(req.ticketId(), cat.category(), prio.priority(), routing.targetTeam(),
				// Overall confidence is conservative: min(category, priority)
				Math.min(cat.confidence(), prio.confidence()), trace.events());
	}

	private static String safe(String s) {
		return s == null ? "" : s;
	}

	private static double clamp01(double v) {
		if (v < 0.0) {
			return 0.0;
		}
		if (v > 1.0) {
			return 1.0;
		}
		return v;
	}

}
