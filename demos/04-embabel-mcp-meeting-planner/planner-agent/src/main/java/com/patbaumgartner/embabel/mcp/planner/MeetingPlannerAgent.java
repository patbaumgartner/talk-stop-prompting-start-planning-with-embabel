package com.patbaumgartner.embabel.mcp.planner;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.patbaumgartner.embabel.mcp.planner.PlannerModels.BookingResult;
import com.patbaumgartner.embabel.mcp.planner.PlannerModels.Location;
import com.patbaumgartner.embabel.mcp.planner.PlannerModels.RoomRequest;
import com.patbaumgartner.embabel.mcp.planner.PlannerModels.SuggestedRoom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Meeting Planner Agent — 3 actions, each using MCP tools via withToolGroups().
 *
 * Plan: RoomRequest → findLocation → Location → findRoomAtLocation → SuggestedRoom →
 * bookRoom → BookingResult
 *
 * Key demo points: - withToolGroups("location") connects the prompt runner to the MCP
 * tools - The agent doesn't know HOW the tools work — it only sees tool descriptions -
 * MCP provides the "hands", Embabel provides the "brain"
 */
@Agent(name = "MeetingPlannerAgent", description = "Plans and books meeting rooms using location services via MCP.",
		version = "1.0.0")
public class MeetingPlannerAgent {

	private static final Logger log = LoggerFactory.getLogger(MeetingPlannerAgent.class);

	/**
	 * Step 1: Find the best location based on meeting requirements.
	 */
	@Action(description = "Find the best matching location for a meeting based on the provided description.")
	public Location findLocation(RoomRequest request, OperationContext context) {
		log.info("Finding location for meeting in {}", request.preferredCity());

		var prompt = """
				Find the best meeting location for the following request:
				- Preferred city: %s
				- Number of people: %d
				- Needs video conference: %s
				- Meeting description: %s

				Use the all-locations tool to get available locations.
				Pick the location in or closest to the preferred city.
				""".formatted(request.preferredCity(), request.numberOfPeople(), request.needsVideoConference(),
				request.meetingDescription());

		return context.ai().withDefaultLlm().withToolGroups("location").creating(Location.class).fromPrompt(prompt);
	}

	/**
	 * Step 2: Check room availability at the selected location.
	 */
	@Action(description = "Check availability of a room at the preferred location.")
	public SuggestedRoom findRoomAtLocation(Location location, RoomRequest request, OperationContext context) {
		log.info("Checking room availability at {}", location.locationName());

		var prompt = """
				Check room availability at location '%s' (ID: %s):
				- Date: %s
				- Time: %s
				- Duration: %d minutes
				- Number of people: %d

				Use the check-room-availability tool.
				Return the suggested room details.
				""".formatted(location.locationName(), location.locationId(), request.date(), request.time(),
				request.durationMinutes(), request.numberOfPeople());

		return context.ai()
			.withDefaultLlm()
			.withToolGroups("location")
			.creating(SuggestedRoom.class)
			.fromPrompt(prompt);
	}

	/**
	 * Step 3: Book the available room.
	 */
	@AchievesGoal(description = "Book a meeting room at a location for the specified number of people.")
	@Action(description = "Book the available room at the selected location.")
	public BookingResult bookRoom(SuggestedRoom room, OperationContext context) {
		log.info("Booking room {} at {}", room.roomName(), room.locationName());

		var prompt = """
				Book the following room:
				- Location ID: %s
				- Room ID: %s
				- Date: %s
				- Time: %s
				- Duration: %d minutes

				Use the book-room tool to confirm the booking.
				""".formatted(room.locationId(), room.roomId(), room.date(), room.time(), room.durationMinutes());

		return context.ai()
			.withDefaultLlm()
			.withToolGroups("location")
			.creating(BookingResult.class)
			.fromPrompt(prompt);
	}

}
