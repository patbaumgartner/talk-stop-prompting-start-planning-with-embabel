package com.patbaumgartner.embabel.guardrails;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.patbaumgartner.embabel.guardrails.GuardrailModels.ApiSummarizeRequest;
import com.patbaumgartner.embabel.guardrails.GuardrailModels.Summary;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/summarize")
public class GuardrailController {

	private final AgentPlatform agentPlatform;

	public GuardrailController(AgentPlatform agentPlatform) {
		this.agentPlatform = agentPlatform;
	}

	/**
	 * Summarize text WITH PII guard rails. Routes to TextSummarizationAgent.summarize()
	 * which applies PIIUserInputGuardRail. If PII is detected, the request is blocked
	 * before the LLM sees it.
	 */
	@PostMapping("/safe")
	public Summary summarizeSafe(@Valid @RequestBody ApiSummarizeRequest request) {
		return AgentInvocation.create(agentPlatform, Summary.class).invoke(request.toDomain());
	}

	/**
	 * Summarize text WITHOUT guard rails — for comparison in the demo. Routes to
	 * TextSummarizationAgent.summarizeUnsafe() which skips PII checking.
	 */
	@PostMapping("/unsafe")
	public Summary summarizeUnsafe(@Valid @RequestBody ApiSummarizeRequest request) {
		return AgentInvocation.create(agentPlatform, Summary.class).invoke(request.toUnsafeDomain());
	}

}
