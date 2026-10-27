package com.patbaumgartner.embabel.ticket.triage;

import com.embabel.agent.core.internal.LlmOperations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "embabel.agent.platform.models.openai.api-key=test-key")
@ExtendWith(OutputCaptureExtension.class)
class TriagePlanningIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoBean
	private LlmOperations llmOperations;

	@Test
	void incidentRunsThroughRealPlannerWithoutLlm(CapturedOutput output) throws Exception {
		var response = triage("NORMAL", "Multiple employees report an outage.");

		assertEquals("INCIDENT", response.category());
		assertEquals("HIGH", response.priority());
		assertEquals("platform-ops", response.targetTeam());
		assertTrue(output.getOut().contains("formulated plan:"));
		assertTrue(output.getOut().contains("executing action"));
		assertFalse(response.trace().stream().anyMatch(event -> event.contains("deepClassify:")));
		verifyNoInteractions(llmOperations);
	}

	@Test
	void uncertainResultChangesRealPlanAndExecutesDeepClassification(CapturedOutput output) throws Exception {
		when(llmOperations.createObject(anyList(), any(), any(), any(), any())).thenAnswer(invocation -> {
			Class<?> type = invocation.getArgument(2);
			var constructor = type.getDeclaredConstructors()[0];
			constructor.setAccessible(true);
			return switch (type.getSimpleName()) {
				case "LlmCategory" -> constructor.newInstance("SERVICE_REQUEST", 0.9, "Uncertainty resolved");
				case "LlmPriority" -> constructor.newInstance("MEDIUM", 0.9, "Standard request");
				default -> throw new IllegalArgumentException("Unexpected LLM output: " + type.getName());
			};
		});

		var response = triage("LOW_CONFIDENCE", "Please investigate my intermittent login issue.");

		assertEquals("SERVICE_REQUEST", response.category());
		assertEquals("MEDIUM", response.priority());
		assertTrue(response.trace().stream().anyMatch(event -> event.contains("quickClassify: UncertainCategory")));
		assertTrue(response.trace().stream().anyMatch(event -> event.contains("deepClassify: invoking LLM")));
		assertTrue(output.getOut().contains("formulated plan:"));
		var plans = Pattern.compile("formulated plan:\\R(.*?)\\R\\s+from:", Pattern.DOTALL)
			.matcher(output.getOut())
			.results()
			.map(match -> match.group(1))
			.toList();
		assertTrue(plans.size() > 1, "Expected successive framework plans");
		assertTrue(plans.getFirst().contains(".quickClassifyOk"));
		assertFalse(plans.getFirst().contains(".deepClassify"));
		assertTrue(
				plans.stream()
					.skip(1)
					.anyMatch(plan -> plan.contains(".quickClassifyNeedsDeep") && plan.contains(".deepClassify")),
				"New facts must replace the initial optimistic plan with the deep-classification path");
		assertTrue(output.getOut()
			.contains("executing action com.patbaumgartner.embabel.ticket.triage.TicketTriageAgent.deepClassify"),
				output.getOut());
		assertTrue(output.getOut().contains("finalizeResponse achieved"));
	}

	private TriageModels.TriageResponse triage(String simulation, String text) throws Exception {
		var payload = new TriageModels.ApiTriageRequest("PLANNING-TEST", text, "web", simulation);
		var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/triage"))
			.timeout(Duration.ofSeconds(30))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(payload)))
			.build();
		try (var client = HttpClient.newHttpClient()) {
			var response = client.send(request, HttpResponse.BodyHandlers.ofString());
			assertEquals(200, response.statusCode(), response.body());
			return jsonMapper.readValue(response.body(), TriageModels.TriageResponse.class);
		}
	}

}
