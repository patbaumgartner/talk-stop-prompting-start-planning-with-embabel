package com.patbaumgartner.embabel.mcp.planner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Domain model for the Meeting Planner Agent.
 *
 * Flow: RoomRequest → [findLocation] → Location → [findRoomAtLocation] → SuggestedRoom →
 * [bookRoom] → BookingResult
 *
 * Each action uses withToolGroups("location") to access the MCP tools.
 */
public class PlannerModels {

	// ── Input ───────────────────────────────────────────────────────────

	public record RoomRequest(String meetingDescription, int numberOfPeople, String preferredCity, String date,
			String time, int durationMinutes, boolean needsVideoConference) {
	}

	// ── Intermediate ────────────────────────────────────────────────────

	public record Location(String locationId, String locationName, String city, String address) {
	}

	public record SuggestedRoom(String locationId, String locationName, String roomId, String roomName, int capacity,
			String date, String time, int durationMinutes) {
	}

	// ── Output ──────────────────────────────────────────────────────────

	public record BookingResult(String bookingId, String locationName, String roomName, String date, String time,
			int durationMinutes, boolean confirmed, String message) {
	}

	// ── REST API types ──────────────────────────────────────────────────

	public record ApiMeetingRequest(@NotBlank String meetingDescription, @Positive int numberOfPeople,
			@NotBlank String preferredCity, @NotBlank String date, @NotBlank String time, @Positive int durationMinutes,
			boolean needsVideoConference) {
		public RoomRequest toDomain() {
			return new RoomRequest(meetingDescription, numberOfPeople, preferredCity, date, time, durationMinutes,
					needsVideoConference);
		}
	}

}
