package com.patbaumgartner.embabel.mcp.location;

import com.patbaumgartner.embabel.mcp.location.LocationModels.BookRoomRequest;
import com.patbaumgartner.embabel.mcp.location.LocationModels.RoomAvailableRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocationServiceTest {

	private final LocationService service = new LocationService();

	@Test
	void getAllLocationsReturnsConfiguredLocations() {
		var response = service.getAllLocations();

		assertEquals(3, response.locations().size());
	}

	@Test
	void checkRoomAvailabilityReturnsAvailableRoomWhenCapacityMatches() {
		var request = new RoomAvailableRequest("basel-hq", "2026-04-21", "10:00", 60, 10);

		var response = service.checkRoomAvailability(request);

		assertTrue(response.available());
		assertEquals("basel-hq", response.locationId());
		assertNotNull(response.roomId());
	}

	@Test
	void checkRoomAvailabilityReturnsUnavailableWhenNoRoomFits() {
		var request = new RoomAvailableRequest("bern-hub", "2026-04-21", "10:00", 60, 99);

		var response = service.checkRoomAvailability(request);

		assertFalse(response.available());
		assertEquals("bern-hub", response.locationId());
		assertTrue(response.reason().contains("No room with sufficient capacity"));
	}

	@Test
	void bookRoomReturnsConfirmedBookingId() {
		var request = new BookRoomRequest("basel-hq", "bsl-1", "2026-04-21", "10:00", 60, "demo-user");

		var response = service.bookRoom(request);

		assertTrue(response.confirmed());
		assertTrue(response.bookingId().startsWith("BK-"));
		assertEquals("basel-hq", response.locationId());
		assertEquals("bsl-1", response.roomId());
	}

}
