package com.patbaumgartner.embabel.guardrails;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patbaumgartner.embabel.guardrails.GuardrailModels.SummarizeRequest;
import com.patbaumgartner.embabel.guardrails.GuardrailModels.Summary;
import com.patbaumgartner.embabel.guardrails.GuardrailModels.UnsafeSummarizeRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GuardrailController.class)
class GuardrailControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AgentPlatform agentPlatform;

	@Test
	void safeEndpointInvokesSafeDomainType() throws Exception {
		var response = new Summary("Safe summary", false, null);
		@SuppressWarnings("rawtypes")
		AgentInvocation invocation = mock(AgentInvocation.class);
		when(invocation.invoke(any(), any(Object[].class))).thenReturn(response);

		try (MockedStatic<AgentInvocation> mocked = Mockito.mockStatic(AgentInvocation.class)) {
			mocked.when(() -> AgentInvocation.create(agentPlatform, Summary.class)).thenReturn(invocation);

			mockMvc
				.perform(post("/api/summarize/safe").contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper
						.writeValueAsString(new GuardrailModels.ApiSummarizeRequest("Quarterly results"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.summary").value("Safe summary"));

			ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
			verify(invocation).invoke(captor.capture(), any(Object[].class));
			assertInstanceOf(SummarizeRequest.class, captor.getValue());
		}
	}

	@Test
	void unsafeEndpointInvokesUnsafeDomainType() throws Exception {
		var response = new Summary("Unsafe summary", true, "EMAIL_ADDRESS");
		@SuppressWarnings("rawtypes")
		AgentInvocation invocation = mock(AgentInvocation.class);
		when(invocation.invoke(any(), any(Object[].class))).thenReturn(response);

		try (MockedStatic<AgentInvocation> mocked = Mockito.mockStatic(AgentInvocation.class)) {
			mocked.when(() -> AgentInvocation.create(agentPlatform, Summary.class)).thenReturn(invocation);

			mockMvc
				.perform(post("/api/summarize/unsafe").contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper
						.writeValueAsString(new GuardrailModels.ApiSummarizeRequest("john@example.com"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.summary").value("Unsafe summary"));

			ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
			verify(invocation).invoke(captor.capture(), any(Object[].class));
			assertInstanceOf(UnsafeSummarizeRequest.class, captor.getValue());
		}
	}

	@Test
	void summarizeRejectsBlankText() throws Exception {
		mockMvc.perform(post("/api/summarize/safe").contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"\"}"))
			.andExpect(status().isBadRequest());
	}

}
