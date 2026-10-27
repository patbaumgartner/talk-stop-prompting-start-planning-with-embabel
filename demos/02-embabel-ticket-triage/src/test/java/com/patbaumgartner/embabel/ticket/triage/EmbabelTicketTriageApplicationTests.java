package com.patbaumgartner.embabel.ticket.triage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Embabel fails fast without an OpenAI key; a dummy key is enough to load the context.
@SpringBootTest(properties = "embabel.agent.platform.models.openai.api-key=test-key")
class EmbabelTicketTriageApplicationTests {

	@Test
	void contextLoads() {
	}

}
