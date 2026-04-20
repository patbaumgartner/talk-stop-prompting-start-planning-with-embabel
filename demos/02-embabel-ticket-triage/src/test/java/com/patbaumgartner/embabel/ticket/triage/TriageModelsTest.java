package com.patbaumgartner.embabel.ticket.triage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriageModelsTest {

	@Test
	void traceLogStartCreatesEmptyEventsList() {
		var trace = TriageModels.TraceLog.start();

		assertTrue(trace.events().isEmpty());
	}

	@Test
	void traceLogAddAppendsEventAndReturnsSameInstance() {
		var trace = TriageModels.TraceLog.start();

		var returned = trace.add("normalized ticket");

		assertSame(trace, returned);
		assertEquals(1, trace.events().size());
		assertTrue(trace.events().getFirst().contains("normalized ticket"));
	}

}
