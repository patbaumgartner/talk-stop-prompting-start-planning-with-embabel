package com.patbaumgartner.embabel.ticket.triage;

import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.api.common.PromptRunner;
import com.patbaumgartner.embabel.ticket.triage.TriageModels.ConfidentCategory;
import com.patbaumgartner.embabel.ticket.triage.TriageModels.PriorityAssessment;
import com.patbaumgartner.embabel.ticket.triage.TriageModels.RoutingDecision;
import com.patbaumgartner.embabel.ticket.triage.TriageModels.Signals;
import com.patbaumgartner.embabel.ticket.triage.TriageModels.SignalsNeedsDeep;
import com.patbaumgartner.embabel.ticket.triage.TriageModels.SignalsOk;
import com.patbaumgartner.embabel.ticket.triage.TriageModels.TraceLog;
import com.patbaumgartner.embabel.ticket.triage.TriageModels.TriageRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TicketTriageAgentFlowTest {

	@Test
	void normalSecurityFlowStaysDeterministicAndProducesFinalResponse() {
		var agent = new TicketTriageAgent();
		var request = new TriageRequest("T-1", "Possible data leak and compromised credentials", "email", "NORMAL");

		TraceLog trace = agent.initTrace(request);
		Signals signals = agent.extractSignals(request, trace);
		assertInstanceOf(SignalsOk.class, signals);

		ConfidentCategory category = agent.quickClassifyOk((SignalsOk) signals, trace);
		PriorityAssessment priority = agent.assessPriority(category, signals, mock(OperationContext.class), trace);
		RoutingDecision routing = agent.route(category, priority, trace);
		var response = agent.finalizeResponse(request, category, priority, routing, trace);

		assertEquals("SECURITY", response.category());
		assertEquals("CRITICAL", response.priority());
		assertEquals("security-incident", response.targetTeam());
		assertTrue(response.trace().stream().anyMatch(it -> it.contains("finalizeResponse: done")));
	}

	@Test
	void lowConfidenceFlowUsesLlmBackedStepsAndRoutesResult() throws Exception {
		var agent = new TicketTriageAgent();
		var request = new TriageRequest("T-2", "Need help creating a shared mailbox", "portal", "LOW_CONFIDENCE");
		TraceLog trace = agent.initTrace(request);
		Signals signals = agent.extractSignals(request, trace);
		assertInstanceOf(SignalsNeedsDeep.class, signals);

		OperationContext context = mock(OperationContext.class);
		Ai ai = mock(Ai.class);
		PromptRunner runner = mock(PromptRunner.class);
		when(context.ai()).thenReturn(ai);
		when(ai.withDefaultLlm()).thenReturn(runner);
		when(runner.creating(any())).thenAnswer(invocation -> {
			Class<?> targetType = invocation.getArgument(0);
			@SuppressWarnings("rawtypes")
			PromptRunner.Creating creating = mock(PromptRunner.Creating.class);
			Object result = instantiateLocalRecord(targetType);
			when(creating.fromPrompt(anyString())).thenReturn(result);
			return creating;
		});

		var uncertain = agent.quickClassifyNeedsDeep((SignalsNeedsDeep) signals, trace);
		var category = agent.deepClassify(uncertain, signals, context, trace);
		var priority = agent.assessPriority(category, signals, context, trace);
		var routing = agent.route(category, priority, trace);
		var response = agent.finalizeResponse(request, category, priority, routing, trace);

		assertEquals("SERVICE_REQUEST", response.category());
		assertEquals("MEDIUM", response.priority());
		assertEquals("service-desk-l1", response.targetTeam());
		assertTrue(response.trace().stream().anyMatch(it -> it.contains("deepClassify: invoking LLM")));
	}

	private static Object instantiateLocalRecord(Class<?> targetType) throws Exception {
		Constructor<?> constructor = targetType.getDeclaredConstructors()[0];
		constructor.setAccessible(true);
		return switch (targetType.getSimpleName()) {
			case "LlmCategory" -> constructor.newInstance("SERVICE_REQUEST", 0.66, "LLM resolved uncertainty");
			case "LlmPriority" -> constructor.newInstance("MEDIUM", 0.70, "Standard priority");
			default -> throw new IllegalArgumentException("Unexpected target type: " + targetType.getName());
		};
	}

}
