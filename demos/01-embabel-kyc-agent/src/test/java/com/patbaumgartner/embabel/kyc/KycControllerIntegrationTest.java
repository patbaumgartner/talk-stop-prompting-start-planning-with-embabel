package com.patbaumgartner.embabel.kyc;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patbaumgartner.embabel.kyc.KycModels.KycAssessment;
import com.patbaumgartner.embabel.kyc.KycModels.KycRequest;
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

@WebMvcTest(KycController.class)
class KycControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AgentPlatform agentPlatform;

	@Test
	void verifyReturnsAssessmentFromAgentInvocation() throws Exception {
		var assessment = new KycAssessment("Jane Doe", "LOW", "APPROVE", "No risk indicators found.");
		@SuppressWarnings("rawtypes")
		AgentInvocation invocation = mock(AgentInvocation.class);
		when(invocation.invoke(any(), any(Object[].class))).thenReturn(assessment);

		try (MockedStatic<AgentInvocation> mocked = Mockito.mockStatic(AgentInvocation.class)) {
			mocked.when(() -> AgentInvocation.create(agentPlatform, KycAssessment.class)).thenReturn(invocation);

			mockMvc
				.perform(post("/api/kyc/verify").contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new KycModels.ApiKycRequest("cust-1", "Jane Doe",
							"1990-01-01", "CH", "Engineer", "Salary"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName").value("Jane Doe"))
				.andExpect(jsonPath("$.riskLevel").value("LOW"))
				.andExpect(jsonPath("$.recommendation").value("APPROVE"));

			ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
			verify(invocation).invoke(captor.capture(), any(Object[].class));
			KycRequest request = (KycRequest) captor.getValue();
			assertEquals("cust-1", request.customerId());
			assertEquals("Jane Doe", request.fullName());
		}
	}

	@Test
	void verifyRejectsBlankRequiredFields() throws Exception {
		mockMvc.perform(post("/api/kyc/verify").contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "customerId": "",
				  "fullName": "",
				  "dateOfBirth": "",
				  "nationality": ""
				}
				""")).andExpect(status().isBadRequest());
	}

}
