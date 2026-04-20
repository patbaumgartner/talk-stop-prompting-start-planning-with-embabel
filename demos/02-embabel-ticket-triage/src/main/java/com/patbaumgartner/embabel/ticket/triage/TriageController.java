package com.patbaumgartner.embabel.ticket.triage;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.patbaumgartner.embabel.ticket.triage.TriageModels.ApiTriageRequest;
import com.patbaumgartner.embabel.ticket.triage.TriageModels.TriageResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/triage")
@Validated
public class TriageController {

	private final AgentPlatform agentPlatform;

	public TriageController(AgentPlatform agentPlatform) {
		this.agentPlatform = agentPlatform;
	}

	@PostMapping
	public TriageResponse triage(@RequestBody @Valid ApiTriageRequest req) {
		// AgentInvocation is a type-safe way to perform an invocation.
		AgentInvocation<TriageResponse> inv = AgentInvocation.create(agentPlatform, TriageResponse.class);
		return inv.invoke(req.toDomain());
	}

}
