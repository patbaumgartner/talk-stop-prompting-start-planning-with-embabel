package com.patbaumgartner.embabel.guardrails;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.patbaumgartner.embabel.guardrails.GuardrailModels.SummarizeRequest;
import com.patbaumgartner.embabel.guardrails.GuardrailModels.Summary;
import com.patbaumgartner.embabel.guardrails.GuardrailModels.UnsafeSummarizeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Text Summarization Agent with PII guard rails.
 *
 * Two actions: - summarize: applies PIIUserInputGuardRail via
 * PromptRunner.withGuardRails() - summarizeUnsafe: no guard rails — for comparison in the
 * demo
 *
 * Plan: SummarizeRequest → summarize → Summary UnsafeSummarizeRequest → summarizeUnsafe →
 * Summary
 */
@Agent(name = "TextSummarizationAgent", description = "Summarizes text content with PII protection guard rails.",
		version = "1.0.0")
public class TextSummarizationAgent {

	private static final Logger log = LoggerFactory.getLogger(TextSummarizationAgent.class);

	private final PresidioAnalyzerClient presidioAnalyzerClient;

	private final PresidioProperties presidioProperties;

	public TextSummarizationAgent(PresidioAnalyzerClient presidioAnalyzerClient,
			PresidioProperties presidioProperties) {
		this.presidioAnalyzerClient = presidioAnalyzerClient;
		this.presidioProperties = presidioProperties;
	}

	@AchievesGoal(description = "Produce a PII-safe summary of the input text.")
	@Action(description = "Summarize the provided text, blocking PII via guard rail.")
	public Summary summarize(SummarizeRequest request, OperationContext context) {
		log.info("Summarizing text with PII guard rail ({} chars)", request.text().length());

		var prompt = """
				Summarize the following text into 2-3 concise sentences.
				Focus on the key facts and main message.

				Text:
				%s
				""".formatted(request.text());

		return context.ai()
			.withDefaultLlm()
			.withGuardRails(new PIIUserInputGuardRail(presidioAnalyzerClient, presidioProperties.piiTypes()))
			.creating(Summary.class)
			.fromPrompt(prompt);
	}

	@AchievesGoal(description = "Produce a summary of the input text without PII protection.")
	@Action(description = "Summarize the provided text without any guard rails.")
	public Summary summarizeUnsafe(UnsafeSummarizeRequest request, OperationContext context) {
		log.info("Summarizing text WITHOUT guard rail ({} chars)", request.text().length());

		var prompt = """
				Summarize the following text into 2-3 concise sentences.
				Focus on the key facts and main message.

				Text:
				%s
				""".formatted(request.text());

		return context.ai().withDefaultLlm().creating(Summary.class).fromPrompt(prompt);
	}

}
