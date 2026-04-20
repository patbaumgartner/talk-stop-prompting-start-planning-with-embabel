package com.patbaumgartner.embabel.mcp.location;

import java.util.List;

/**
 * Domain types for the Location MCP server.
 */
public class LocationModels {

	public record Location(String id, String name, String city, String address, int totalRooms) {
	}

	public record Room(String roomId, String name, int capacity, boolean hasVideoConference, boolean hasWhiteboard) {
	}

	public record RoomAvailableRequest(String locationId, String date, String time, int durationMinutes,
			int numberOfPeople) {
	}

	public record RoomAvailableResponse(String locationId, String roomId, String roomName, boolean available,
			String reason) {
	}

	public record BookRoomRequest(String locationId, String roomId, String date, String time, int durationMinutes,
			String bookedBy) {
	}

	public record BookRoomResponse(String bookingId, String locationId, String roomId, String date, String time,
			boolean confirmed, String message) {
	}

	public record LocationResponse(List<Location> locations) {
	}

}
