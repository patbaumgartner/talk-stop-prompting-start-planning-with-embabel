package com.patbaumgartner.embabel.mcp.location;

import com.patbaumgartner.embabel.mcp.location.LocationModels.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Location service exposing 3 MCP tools via @Tool annotations.
 *
 * In production, these would call a room booking system / calendar API. For the demo, we
 * use in-memory data to keep the focus on MCP integration.
 */
@Service
public class LocationService {

	private static final Logger log = LoggerFactory.getLogger(LocationService.class);

	private static final Map<String, Location> LOCATIONS = Map.of("basel-hq",
			new Location("basel-hq", "Basel Headquarters", "Basel", "Aeschenvorstadt 71, 4051 Basel", 5),
			"zurich-office",
			new Location("zurich-office", "Zürich Office", "Zürich", "Bahnhofstrasse 10, 8001 Zürich", 3), "bern-hub",
			new Location("bern-hub", "Bern Innovation Hub", "Bern", "Bundesplatz 3, 3011 Bern", 2));

	private static final Map<String, List<Room>> ROOMS = Map.of("basel-hq",
			List.of(new Room("bsl-1", "Rhine View", 12, true, true), new Room("bsl-2", "Münster", 6, true, false),
					new Room("bsl-3", "Dreiländereck", 20, true, true)),
			"zurich-office",
			List.of(new Room("zrh-1", "Limmat", 8, true, true), new Room("zrh-2", "Uetliberg", 4, false, true)),
			"bern-hub", List.of(new Room("brn-1", "Bundeshaus", 10, true, true)));

	@Tool(name = "all-locations", description = "Get all available meeting locations.")
	public LocationResponse getAllLocations() {
		log.info("MCP Tool: all-locations");
		return new LocationResponse(List.copyOf(LOCATIONS.values()));
	}

	@Tool(name = "check-room-availability",
			description = "Check availability of a room at a specific location, date, time, duration and number of people.")
	public RoomAvailableResponse checkRoomAvailability(RoomAvailableRequest request) {
		log.info("MCP Tool: check-room-availability at {} on {}", request.locationId(), request.date());

		List<Room> rooms = ROOMS.getOrDefault(request.locationId(), List.of());
		Room match = rooms.stream()
			.filter(room -> room.capacity() >= request.numberOfPeople())
			.findFirst()
			.orElse(null);

		if (match == null) {
			return new RoomAvailableResponse(request.locationId(), null, null, false,
					"No room with sufficient capacity (%d people) at this location."
						.formatted(request.numberOfPeople()));
		}

		return new RoomAvailableResponse(request.locationId(), match.roomId(), match.name(), true,
				"Room '%s' is available (capacity: %d).".formatted(match.name(), match.capacity()));
	}

	@Tool(name = "book-room", description = "Book a room at a specific location on a day and time.")
	public BookRoomResponse bookRoom(BookRoomRequest request) {
		log.info("MCP Tool: book-room {} at {} on {}", request.roomId(), request.locationId(), request.date());

		String bookingId = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

		return new BookRoomResponse(bookingId, request.locationId(), request.roomId(), request.date(), request.time(),
				true, "Room booked successfully. Booking ID: " + bookingId);
	}

}
