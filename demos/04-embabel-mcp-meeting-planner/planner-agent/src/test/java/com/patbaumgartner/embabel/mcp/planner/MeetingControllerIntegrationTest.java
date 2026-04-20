package com.patbaumgartner.embabel.mcp.planner;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patbaumgartner.embabel.mcp.planner.PlannerModels.BookingResult;
import com.patbaumgartner.embabel.mcp.planner.PlannerModels.RoomRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeetingController.class)
class MeetingControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AgentPlatform agentPlatform;

	@Test
	void planMeetingReturnsBookingResultFromAgentInvocation() throws Exception {
		var result = new BookingResult("BK-1234", "Basel Headquarters", "Rhine View", "2026-04-21", "10:00", 60, true,
				"Booked");
		@SuppressWarnings("rawtypes")
		AgentInvocation invocation = mock(AgentInvocation.class);
		when(invocation.invoke(any(), any(Object[].class))).thenReturn(result);

		try (MockedStatic<AgentInvocation> mocked = Mockito.mockStatic(AgentInvocation.class)) {
			mocked.when(() -> AgentInvocation.create(agentPlatform, BookingResult.class)).thenReturn(invocation);

			mockMvc
				.perform(post("/api/meetings/plan").contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new PlannerModels.ApiMeetingRequest("Quarterly planning",
							8, "Basel", "2026-04-21", "10:00", 60, true))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bookingId").value("BK-1234"))
				.andExpect(jsonPath("$.confirmed").value(true));

			ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
			verify(invocation).invoke(captor.capture(), any(Object[].class));
			RoomRequest request = (RoomRequest) captor.getValue();
			assertEquals("Quarterly planning", request.meetingDescription());
			assertEquals("Basel", request.preferredCity());
		}
	}

	@Test
	void planMeetingRejectsInvalidPayload() throws Exception {
		mockMvc.perform(post("/api/meetings/plan").contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "meetingDescription": "",
				  "numberOfPeople": 0,
				  "preferredCity": "",
				  "date": "",
				  "time": "",
				  "durationMinutes": 0
				}
				""")).andExpect(status().isBadRequest());
	}

}
