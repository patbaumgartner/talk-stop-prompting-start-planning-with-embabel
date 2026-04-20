package com.patbaumgartner.embabel.mcp.planner;

import com.patbaumgartner.embabel.mcp.planner.PlannerModels.ApiMeetingRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlannerModelsTest {

	@Test
	void apiRequestToDomainMapsAllFields() {
		var api = new ApiMeetingRequest("Quarterly planning", 8, "Basel", "2026-04-21", "10:00", 90, true);

		var domain = api.toDomain();

		assertEquals("Quarterly planning", domain.meetingDescription());
		assertEquals(8, domain.numberOfPeople());
		assertEquals("Basel", domain.preferredCity());
		assertEquals("2026-04-21", domain.date());
		assertEquals("10:00", domain.time());
		assertEquals(90, domain.durationMinutes());
		assertTrue(domain.needsVideoConference());
	}

}
