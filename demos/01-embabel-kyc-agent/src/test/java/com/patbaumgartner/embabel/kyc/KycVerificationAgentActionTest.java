package com.patbaumgartner.embabel.kyc;

import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.api.common.PromptRunner;
import com.patbaumgartner.embabel.kyc.KycModels.KycAssessment;
import com.patbaumgartner.embabel.kyc.KycModels.KycRequest;
import com.patbaumgartner.embabel.kyc.KycModels.KycScreening;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KycVerificationAgentActionTest {

	@Test
	void screenCustomerUsesPromptRunnerAndFiltersCustomerId() {
		var agent = new KycVerificationAgent();
		OperationContext context = mock(OperationContext.class);
		Ai ai = mock(Ai.class);
		PromptRunner runner = mock(PromptRunner.class);
		@SuppressWarnings("rawtypes")
		PromptRunner.Creating creating = mock(PromptRunner.Creating.class);
		var expected = new KycScreening("Jane Doe", "CH", false, false, false, "No hits");

		when(context.ai()).thenReturn(ai);
		when(ai.withDefaultLlm()).thenReturn(runner);
		when(runner.creating(KycScreening.class)).thenReturn(creating);
		when(creating.withoutProperties("customerId")).thenReturn(creating);
		when(creating.fromPrompt(anyString())).thenReturn(expected);

		var result = agent
			.screenCustomer(new KycRequest("cust-1", "Jane Doe", "1990-01-01", "CH", "Engineer", "Salary"), context);

		assertEquals(expected, result);
		verify(creating).withoutProperties("customerId");
		ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
		verify(creating).fromPrompt(promptCaptor.capture());
		assertTrue(promptCaptor.getValue().contains("Jane Doe"));
		assertTrue(promptCaptor.getValue().contains("Source of Funds"));
	}

	@Test
	void assessRiskUsesPromptRunnerAndReturnsAssessment() {
		var agent = new KycVerificationAgent();
		OperationContext context = mock(OperationContext.class);
		Ai ai = mock(Ai.class);
		PromptRunner runner = mock(PromptRunner.class);
		@SuppressWarnings("rawtypes")
		PromptRunner.Creating creating = mock(PromptRunner.Creating.class);
		var expected = new KycAssessment("Jane Doe", "LOW", "APPROVE", "No hits");

		when(context.ai()).thenReturn(ai);
		when(ai.withDefaultLlm()).thenReturn(runner);
		when(runner.creating(KycAssessment.class)).thenReturn(creating);
		when(creating.fromPrompt(anyString())).thenReturn(expected);

		var result = agent.assessRisk(new KycScreening("Jane Doe", "CH", false, false, false, "No hits"), context);

		assertEquals(expected, result);
		ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
		verify(creating).fromPrompt(promptCaptor.capture());
		assertTrue(promptCaptor.getValue().contains("Risk levels: LOW, MEDIUM, HIGH, CRITICAL"));
		assertTrue(promptCaptor.getValue().contains("No hits"));
	}

}
