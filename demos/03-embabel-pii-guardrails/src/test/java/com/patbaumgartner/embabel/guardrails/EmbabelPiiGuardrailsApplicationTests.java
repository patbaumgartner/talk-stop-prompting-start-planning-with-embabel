package com.patbaumgartner.embabel.guardrails;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
		properties = { "embabel.agent.platform.models.openai.api-key=test-key", "spring.docker.compose.enabled=false" })
class EmbabelPiiGuardrailsApplicationTests {

	@Test
	void contextLoads() {
	}

}
