package com.patbaumgartner.embabel.guardrails;

import com.patbaumgartner.embabel.guardrails.GuardrailModels.AnalyzeResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "embabel.agent.platform.models.openai.api-key=test-key", "spring.docker.compose.enabled=false" })
class GuardrailRejectionIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private JsonMapper jsonMapper;

	@MockitoBean
	private PresidioAnalyzerClient presidioAnalyzerClient;

	@Test
	void blockedInputReturns422WithoutRetryingTheAction() throws Exception {
		when(presidioAnalyzerClient.analyze(any())).thenReturn(List.of(new AnalyzeResult("EMAIL_ADDRESS", 0, 16, 1.0)));
		var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/summarize/safe"))
			.timeout(Duration.ofSeconds(5))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString("{\"text\":\"demo@example.com\"}"))
			.build();

		try (var client = HttpClient.newHttpClient()) {
			var response = client.send(request, HttpResponse.BodyHandlers.ofString());

			assertEquals(422, response.statusCode(), response.body());
			var problem = jsonMapper.readTree(response.body());
			assertEquals("GUARDRAIL_VIOLATION", problem.get("code").asString());
			assertEquals("PIIGuardRail", problem.get("guard").asString());
			assertTrue(problem.get("detail").asString().contains("EMAIL_ADDRESS"));
		}
		verify(presidioAnalyzerClient, times(1)).analyze(any());
	}

}
