package com.patbaumgartner.embabel.mcp.planner;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.patbaumgartner.embabel.mcp.planner.PlannerModels.ApiMeetingRequest;
import com.patbaumgartner.embabel.mcp.planner.PlannerModels.BookingResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

	private final AgentPlatform agentPlatform;

	public MeetingController(AgentPlatform agentPlatform) {
		this.agentPlatform = agentPlatform;
	}

	@PostMapping("/plan")
	public BookingResult planMeeting(@Valid @RequestBody ApiMeetingRequest request) {
		return AgentInvocation.create(agentPlatform, BookingResult.class).invoke(request.toDomain());
	}

}
