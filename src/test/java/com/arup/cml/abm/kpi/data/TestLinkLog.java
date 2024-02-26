package com.arup.cml.abm.kpi.data;

import com.arup.cml.abm.kpi.data.exceptions.LinkLogPassengerConsistencyException;
import com.google.common.collect.Table;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestLinkLog {


    @Test
    public void defaultsToUnknownModeForNewLinkLogEntryWithUnrecordedMode() {
        LinkLog linkLog = new LinkLog();
        assertThat(linkLog.getLinkLogData().isEmpty()).isTrue().as("Link log table should be empty initially");

        linkLog.createLinkLogEntry("someVehicle", "someLink", 12.0);

        Table<Long, String, Object> linkLogTable = linkLog.getLinkLogData();
        assertThat(linkLogTable.rowMap().size())
                .isEqualTo(1)
                .as("Link log table should contain a single row");
        assertThat(linkLogTable.row(Long.valueOf(0)).get("mode"))
                .isEqualTo("unknown")
                .as("Vehicle mode should default to 'unknown'");
    }

    @Test
    public void usesRecordedModeWhenAvailableForNewLinkLogEntry() {
        LinkLog linkLog = new LinkLog();
        assertThat(linkLog.getLinkLogData().isEmpty()).isTrue().as("Link log table should be empty initially");

        linkLog.recordVehicleMode("someVehicle", "someMode");
        linkLog.createLinkLogEntry("someVehicle", "someLink", 12.0);

        Table<Long, String, Object> linkLogTable = linkLog.getLinkLogData();
        assertThat(linkLogTable.rowMap().size())
                .isEqualTo(1)
                .as("Link log table should contain a single row");
        assertThat(linkLogTable.row(Long.valueOf(0)).get("mode"))
                .isEqualTo("someMode")
                .as("Vehicle mode should have been recorded as 'someMode'");
    }

    @Test
    public void completeLinkLogEntryHasEndTimeAndTracksPassengerNumber() {
        LinkLog linkLog = new LinkLog();
        linkLog.personBoardsVehicle("someVehicle", "somePerson");
        linkLog.createLinkLogEntry("someVehicle", "someLink", 12.0);
        linkLog.completeLinkLogEntry("someVehicle", 24.0);

        Table<Long, String, Object> linkLogTable = linkLog.getLinkLogData();
        assertThat(linkLogTable.rowMap().size())
                .isEqualTo(1)
                .as("Link log table should contain a single row");
        assertThat(linkLogTable.row(Long.valueOf(0)).get("endTime"))
                .isEqualTo(24.0)
                .as("Finish time on the link should have been recorded as `24.0`");
        assertThat(linkLogTable.row(Long.valueOf(0)).get("numberOfPeople"))
                .isEqualTo(1)
                .as("Number of people in vehicle should have been recorded as `1`");
    }


    @Test
    public void completeLinkLogEntryTracksPassengerIds() {
        LinkLog linkLog = new LinkLog();
        linkLog.personBoardsVehicle("someVehicle", "somePerson");
        linkLog.createLinkLogEntry("someVehicle", "someLink", 12.0);
        linkLog.completeLinkLogEntry("someVehicle", 24.0);

        Table<Long, String, Object> linkLogVehicleOccupantsTable = linkLog.getVehicleOccupantsData();
        assertThat(linkLogVehicleOccupantsTable.rowMap().size())
                .isEqualTo(1)
                .as("Vehicle Occupant table should contain a single row");
        assertThat(linkLogVehicleOccupantsTable.row(Long.valueOf(0)).get("agentId"))
                .isEqualTo("somePerson")
                .as("Agent in the vehicle should have been recorded as 'somePerson'");
    }

    @Test
    public void personCanEnterVehicleThatIsAlreadyMoving() {
        LinkLog linkLog = new LinkLog();
        linkLog.createLinkLogEntry("someVehicle", "someLink", 12.0);
        linkLog.personBoardsVehicle("someVehicle", "somePerson");
        linkLog.completeLinkLogEntry("someVehicle", 24.0);

        Table<Long, String, Object> linkLogTable = linkLog.getLinkLogData();
        assertThat(linkLogTable.rowMap().size())
                .isEqualTo(1)
                .as("Link log table should contain a single row");
        assertThat(linkLogTable.row(Long.valueOf(0)).get("numberOfPeople"))
                .isEqualTo(1)
                .as("Number of people in vehicle should have been recorded as '1'");
    }

    @Test
    public void linkLogRecordsMultiplePeopleBoardingAtOnce() {
        LinkLog linkLog = new LinkLog();
        // bus enters a link
        linkLog.createLinkLogEntry("PartyBus", "someLink", 12.0);
        // time to get this party started
        linkLog.personBoardsVehicle("PartyBus", "partyPerson1");
        linkLog.personBoardsVehicle("PartyBus", "partyPerson2");
        linkLog.personBoardsVehicle("PartyBus", "partyPerson3");
        // bus completes its travel on link
        linkLog.completeLinkLogEntry("PartyBus", 24.0);

        Table<Long, String, Object> linkLogTable = linkLog.getLinkLogData();
        assertThat(linkLogTable.rowMap().size())
                .isEqualTo(1)
                .as("Link log table should contain a single row");
        assertThat(linkLogTable.row(Long.valueOf(0)).get("numberOfPeople"))
                .isEqualTo(3)
                .as("Number of people in vehicle should have been recorded as `3`");
    }

    @Test
    public void linkLogVehicleOccupantsTableRecordsMultiplePeopleBoardingAtOnce() {
        LinkLog linkLog = new LinkLog();
        // bus enters a link
        linkLog.createLinkLogEntry("PartyBus", "someLink", 12.0);
        // time to get this party started
        linkLog.personBoardsVehicle("PartyBus", "partyPerson1");
        linkLog.personBoardsVehicle("PartyBus", "partyPerson2");
        linkLog.personBoardsVehicle("PartyBus", "partyPerson3");
        // bus completes its travel on link
        linkLog.completeLinkLogEntry("PartyBus", 24.0);

        Table<Long, String, Object> linkLogVehicleOccupantsTable = linkLog.getVehicleOccupantsData();
        assertThat(linkLogVehicleOccupantsTable.rowMap().size())
                .isEqualTo(3)
                .as("Vehicle Occupant table should contain 3 rows");
        Collection<Object> allOccupants = linkLogVehicleOccupantsTable.column("agentId").values();
        for (String passenger : new String[] {"partyPerson1", "partyPerson2", "partyPerson3"}) {
            assertThat(allOccupants.contains(passenger)).isTrue()
                    .as(String.format("'%s' is expected in the 'agentId' column", passenger));
        }
        for (Long idx : new Long[] {0L, 1L, 2L}) {
            assertThat(linkLogVehicleOccupantsTable.row(idx).get("linkLogIndex"))
                    .isEqualTo(0L)
                    .as("Each row should reference the single index: '0', of the Link Log in 'linkLogIndex' column");
        }
    }

    @Test
    public void linkLogTracksPassengerBoardingAndAlighting() {
        LinkLog linkLog = new LinkLog();
        // driver hops on and bus starts
        linkLog.personBoardsVehicle("PartyBus", "driver");
        linkLog.createLinkLogEntry("PartyBus", "startLink", 0.0);
        linkLog.completeLinkLogEntry("PartyBus", 5.0);
        // gerry gets on
        linkLog.createLinkLogEntry("PartyBus", "gerryLinkBoard", 5.0);
        linkLog.personBoardsVehicle("PartyBus", "gerry");
        linkLog.completeLinkLogEntry("PartyBus", 10.0);
        // gerry leaves
        linkLog.createLinkLogEntry("PartyBus", "gerryLinkAlight", 10.0);
        linkLog.personAlightsVehicle("PartyBus", "gerry");
        linkLog.completeLinkLogEntry("PartyBus", 15.0);
        // bus completes its travel
        linkLog.createLinkLogEntry("PartyBus", "endLink", 15.0);
        linkLog.completeLinkLogEntry("PartyBus", 20.0);

        Table<Long, String, Object> linkLogTable = linkLog.getLinkLogData();
        assertThat(linkLogTable.rowMap().size())
                .isEqualTo(4)
                .as("Link log table should contain 4 rows");
        assertThat(linkLogTable.row(Long.valueOf(0)).get("numberOfPeople"))
                .isEqualTo(1)
                .as("Number of people in vehicle at index `0` should have been recorded as `1`");
        assertThat(linkLogTable.row(Long.valueOf(1)).get("numberOfPeople"))
                .isEqualTo(2)
                .as("Number of people in vehicle at index `1` should have been recorded as `2`");
        assertThat(linkLogTable.row(Long.valueOf(2)).get("numberOfPeople"))
                .isEqualTo(1)
                .as("Number of people in vehicle at index `2 should have been recorded as `1`");
        assertThat(linkLogTable.row(Long.valueOf(3)).get("numberOfPeople"))
                .isEqualTo(1)
                .as("Number of people in vehicle at index `3` should have been recorded as `1`");
    }

    @Test
    public void vehicleOccupantsTableTracksBoardingAndAlighting() {
        LinkLog linkLog = new LinkLog();
        // driver hops on and bus starts
        linkLog.personBoardsVehicle("PartyBus", "driver");
        linkLog.createLinkLogEntry("PartyBus", "startLink", 0.0);
        linkLog.completeLinkLogEntry("PartyBus", 5.0);
        // gerry gets on
        linkLog.createLinkLogEntry("PartyBus", "gerryLinkBoard", 5.0);
        linkLog.personBoardsVehicle("PartyBus", "gerry");
        linkLog.completeLinkLogEntry("PartyBus", 10.0);
        // gerry leaves
        linkLog.createLinkLogEntry("PartyBus", "gerryLinkAlight", 10.0);
        linkLog.personAlightsVehicle("PartyBus", "gerry");
        linkLog.completeLinkLogEntry("PartyBus", 15.0);
        // bus completes its travel
        linkLog.createLinkLogEntry("PartyBus", "endLink", 15.0);
        linkLog.completeLinkLogEntry("PartyBus", 20.0);

        Table<Long, String, Object> linkLogVehicleOccupantsTable = linkLog.getVehicleOccupantsData();
        assertThat(linkLogVehicleOccupantsTable.rowMap().size())
                .isEqualTo(5)
                .as("Vehicle Occupant table should contain 5 rows");
        Map<Long, Map<String, Object>> expectedOccupantsTable = Map.of(
                0L, Map.of("agentId", "driver", "linkLogIndex", 0L),
                1L, Map.of("agentId", "driver", "linkLogIndex", 1L),
                2L, Map.of("agentId", "gerry", "linkLogIndex", 1L),
                3L, Map.of("agentId", "driver", "linkLogIndex", 2L),
                4L, Map.of("agentId", "driver", "linkLogIndex", 3L)
        );
        for (Long idx : new Long[] {0L, 1L, 2L, 3L, 4L}) {
            assertThat(linkLogVehicleOccupantsTable.row(idx).get("agentId"))
                    .isEqualTo(expectedOccupantsTable.get(idx).get("agentId"))
                    .as(String.format("'%s' is expected in the 'agentId' column at index `%d`",
                            expectedOccupantsTable.get(idx).get("agentId"), idx));
            assertThat(linkLogVehicleOccupantsTable.row(idx).get("linkLogIndex"))
                    .isEqualTo(expectedOccupantsTable.get(idx).get("linkLogIndex"))
                    .as(String.format("`%d` is expected in the 'linkLogIndex' column at index `%d`",
                            expectedOccupantsTable.get(idx).get("linkLogIndex"), idx));
        }
    }

    @Test
    public void defaultsToZeroPassengersWhenTryingToCompleteLogOfUnrecordedVehicle() {
        LinkLog linkLog = new LinkLog();
        linkLog.createLinkLogEntry("someVehicle", "someLink", 12.0);
        linkLog.completeLinkLogEntry("someVehicle", 24.0);

        Table<Long, String, Object> linkLogTable = linkLog.getLinkLogData();
        assertThat(linkLogTable.rowMap().size())
                .isEqualTo(1)
                .as("Link log table should contain a single row");
        assertThat(linkLogTable.row(Long.valueOf(0)).get("numberOfPeople"))
                .isEqualTo(0)
                .as("Number of people in vehicle at index `0` should have been recorded as `0`");
    }

    @Test(expected = LinkLogPassengerConsistencyException.class)
    public void throwsExceptionWhenPassengerWantsToLeaveUnrecordedVehicle() {
        LinkLog linkLog = new LinkLog();
        linkLog.personAlightsVehicle("someVehicle", "someDude");
    }

    @Test(expected = LinkLogPassengerConsistencyException.class)
    public void throwsExceptionWhenPersonWantsToLeaveVehicleTheyDidntBoard() {
        LinkLog linkLog = new LinkLog();
        linkLog.personBoardsVehicle("someVehicle", "someDude");
        linkLog.personAlightsVehicle("someVehicle", "someRandomDude");
    }
}
