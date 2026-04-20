package com.patbaumgartner.embabel.guardrails;

import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.api.common.PromptRunner;
import com.patbaumgartner.embabel.guardrails.GuardrailModels.SummarizeRequest;
import com.patbaumgartner.embabel.guardrails.GuardrailModels.Summary;
import com.patbaumgartner.embabel.guardrails.GuardrailModels.UnsafeSummarizeRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TextSummarizationAgentActionTest {

	@Test
	void summarizeAddsGuardRailBeforeCallingPromptRunner() {
		PresidioAnalyzerClient client = mock(PresidioAnalyzerClient.class);
		var properties = new PresidioProperties("http://localhost:3000", java.util.List.of("EMAIL_ADDRESS"));
		var agent = new TextSummarizationAgent(client, properties);
		OperationContext context = mock(OperationContext.class);
		Ai ai = mock(Ai.class);
		PromptRunner runner = mock(PromptRunner.class);
		@SuppressWarnings("rawtypes")
		PromptRunner.Creating creating = mock(PromptRunner.Creating.class);
		var expected = new Summary("Summary text", false, null);

		when(context.ai()).thenReturn(ai);
		when(ai.withDefaultLlm()).thenReturn(runner);
		when(runner.withGuardRails(any())).thenReturn(runner);
		when(runner.creating(Summary.class)).thenReturn(creating);
		when(creating.fromPrompt(anyString())).thenReturn(expected);

		var result = agent.summarize(new SummarizeRequest("Quarterly results were strong."), context);

		assertEquals(expected, result);
		verify(runner).withGuardRails(any());
		verify(creating).fromPrompt(anyString());
	}

	@Test
	void summarizeUnsafeSkipsGuardRails() {
		PresidioAnalyzerClient client = mock(PresidioAnalyzerClient.class);
		var properties = new PresidioProperties("http://localhost:3000", java.util.List.of("EMAIL_ADDRESS"));
		var agent = new TextSummarizationAgent(client, properties);
		OperationContext context = mock(OperationContext.class);
		Ai ai = mock(Ai.class);
		PromptRunner runner = mock(PromptRunner.class);
		@SuppressWarnings("rawtypes")
		PromptRunner.Creating creating = mock(PromptRunner.Creating.class);
		var expected = new Summary("Unsafe summary", true, "EMAIL_ADDRESS");

		when(context.ai()).thenReturn(ai);
		when(ai.withDefaultLlm()).thenReturn(runner);
		when(runner.creating(Summary.class)).thenReturn(creating);
		when(creating.fromPrompt(anyString())).thenReturn(expected);

		var result = agent.summarizeUnsafe(new UnsafeSummarizeRequest("john@example.com"), context);

		assertEquals(expected, result);
		verify(runner, never()).withGuardRails(any());
		verify(creating).fromPrompt(anyString());
	}

}
