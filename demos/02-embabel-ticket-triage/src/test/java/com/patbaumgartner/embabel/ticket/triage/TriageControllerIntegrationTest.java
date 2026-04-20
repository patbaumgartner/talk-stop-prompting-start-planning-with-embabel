package com.patbaumgartner.embabel.ticket.triage;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patbaumgartner.embabel.ticket.triage.TriageModels.ApiTriageRequest;
import com.patbaumgartner.embabel.ticket.triage.TriageModels.TriageRequest;
import com.patbaumgartner.embabel.ticket.triage.TriageModels.TriageResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TriageController.class)
class TriageControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AgentPlatform agentPlatform;

	@Test
	void triageReturnsResponseFromAgentInvocation() throws Exception {
		var response = new TriageResponse("T-1", "INCIDENT", "HIGH", "platform-ops", 0.85,
				List.of("trace-1", "trace-2"));
		@SuppressWarnings("rawtypes")
		AgentInvocation invocation = mock(AgentInvocation.class);
		when(invocation.invoke(any(), any(Object[].class))).thenReturn(response);

		try (MockedStatic<AgentInvocation> mocked = Mockito.mockStatic(AgentInvocation.class)) {
			mocked.when(() -> AgentInvocation.create(agentPlatform, TriageResponse.class)).thenReturn(invocation);

			mockMvc
					.perform(post("/api/v1/triage").contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(
									new ApiTriageRequest("T-1", "System outage", "email", "NORMAL"))))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ticketId").value("T-1"))
					.andExpect(jsonPath("$.category").value("INCIDENT"))
					.andExpect(jsonPath("$.priority").value("HIGH"))
					.andExpect(jsonPath("$.targetTeam").value("platform-ops"));

			ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
			verify(invocation).invoke(captor.capture(), any(Object[].class));
			TriageRequest request = (TriageRequest) captor.getValue();
			assertEquals("T-1", request.ticketId());
			assertEquals("System outage", request.text());
			assertEquals("NORMAL", request.simulate());
		}
	}

	@Test
	void triageRejectsBlankTicketIdAndText() throws Exception {
		mockMvc.perform(post("/api/v1/triage").contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "ticketId": "",
				  "text": ""
				}
				""")).andExpect(status().isBadRequest());
	}

}
