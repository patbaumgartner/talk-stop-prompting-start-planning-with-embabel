package com.patbaumgartner.embabel.mcp.planner;

import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.api.common.PromptRunner;
import com.patbaumgartner.embabel.mcp.planner.PlannerModels.BookingResult;
import com.patbaumgartner.embabel.mcp.planner.PlannerModels.Location;
import com.patbaumgartner.embabel.mcp.planner.PlannerModels.RoomRequest;
import com.patbaumgartner.embabel.mcp.planner.PlannerModels.SuggestedRoom;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeetingPlannerAgentActionTest {

	@SuppressWarnings("unchecked")
	private static <T> PromptRunner.Creating<T> creatingMock() {
		return mock(PromptRunner.Creating.class);
	}

	@Test
	void findLocationUsesLocationToolGroup() {
		var agent = new MeetingPlannerAgent();
		OperationContext context = mock(OperationContext.class);
		Ai ai = mock(Ai.class);
		PromptRunner runner = mock(PromptRunner.class);
		PromptRunner.Creating<Location> creating = creatingMock();
		var expected = new Location("basel-hq", "Basel Headquarters", "Basel", "Aeschenvorstadt 71");

		when(context.ai()).thenReturn(ai);
		when(ai.withDefaultLlm()).thenReturn(runner);
		when(runner.withToolGroups("location")).thenReturn(runner);
		when(runner.creating(Location.class)).thenReturn(creating);
		when(creating.fromPrompt(anyString())).thenReturn(expected);

		var result = agent
			.findLocation(new RoomRequest("Quarterly planning", 8, "Basel", "2026-04-21", "10:00", 60, true), context);

		assertEquals(expected, result);
		verify(runner).withToolGroups("location");
	}

	@Test
	void findRoomAtLocationUsesLocationToolGroup() {
		var agent = new MeetingPlannerAgent();
		OperationContext context = mock(OperationContext.class);
		Ai ai = mock(Ai.class);
		PromptRunner runner = mock(PromptRunner.class);
		PromptRunner.Creating<SuggestedRoom> creating = creatingMock();
		var expected = new SuggestedRoom("basel-hq", "Basel Headquarters", "bsl-1", "Rhine View", 12, "2026-04-21",
				"10:00", 60);

		when(context.ai()).thenReturn(ai);
		when(ai.withDefaultLlm()).thenReturn(runner);
		when(runner.withToolGroups("location")).thenReturn(runner);
		when(runner.creating(SuggestedRoom.class)).thenReturn(creating);
		when(creating.fromPrompt(anyString())).thenReturn(expected);

		var result = agent.findRoomAtLocation(
				new Location("basel-hq", "Basel Headquarters", "Basel", "Aeschenvorstadt 71"),
				new RoomRequest("Quarterly planning", 8, "Basel", "2026-04-21", "10:00", 60, true), context);

		assertEquals(expected, result);
		ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
		verify(creating).fromPrompt(promptCaptor.capture());
		assertTrue(promptCaptor.getValue().contains("check-room-availability"));
		assertTrue(promptCaptor.getValue().contains("another location with sufficient capacity"));
	}

	@Test
	void findRoomRejectsUndersizedSuggestionBeforeBooking() {
		var context = mock(OperationContext.class);
		var ai = mock(Ai.class);
		var runner = mock(PromptRunner.class);
		PromptRunner.Creating<SuggestedRoom> creating = creatingMock();
		when(context.ai()).thenReturn(ai);
		when(ai.withDefaultLlm()).thenReturn(runner);
		when(runner.withToolGroups("location")).thenReturn(runner);
		when(runner.creating(SuggestedRoom.class)).thenReturn(creating);
		when(creating.fromPrompt(anyString())).thenReturn(new SuggestedRoom("bern-hub", "Bern Innovation Hub", "brn-1",
				"Bundeshaus", 10, "2026-04-24", "09:00", 180));

		assertThrows(IllegalStateException.class,
				() -> new MeetingPlannerAgent().findRoomAtLocation(
						new Location("bern-hub", "Bern Innovation Hub", "Bern", "Bundesplatz 3"),
						new RoomRequest("Workshop", 15, "Bern", "2026-04-24", "09:00", 180, true), context));
	}

	@Test
	void bookRoomUsesLocationToolGroup() {
		var agent = new MeetingPlannerAgent();
		OperationContext context = mock(OperationContext.class);
		Ai ai = mock(Ai.class);
		PromptRunner runner = mock(PromptRunner.class);
		PromptRunner.Creating<BookingResult> creating = creatingMock();
		var expected = new BookingResult("BK-1", "Basel Headquarters", "Rhine View", "2026-04-21", "10:00", 60, true,
				"Booked");

		when(context.ai()).thenReturn(ai);
		when(ai.withDefaultLlm()).thenReturn(runner);
		when(runner.withToolGroups("location")).thenReturn(runner);
		when(runner.creating(BookingResult.class)).thenReturn(creating);
		when(creating.fromPrompt(anyString()))
			.thenReturn(new BookingResult("BK-1", null, "bsl-1", "2026-04-21", "10:00", 60, true, "Booked"));

		var result = agent.bookRoom(new SuggestedRoom("basel-hq", "Basel Headquarters", "bsl-1", "Rhine View", 12,
				"2026-04-21", "10:00", 60), context);

		assertEquals(expected, result);
		verify(runner).withToolGroups("location");
	}

}
