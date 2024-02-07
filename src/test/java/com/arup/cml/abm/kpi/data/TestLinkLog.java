package com.arup.cml.abm.kpi.data;

import com.arup.cml.abm.kpi.data.exceptions.LinkLogPassengerConsistencyException;
import com.google.common.collect.RowSortedTable;
import com.google.common.collect.TreeBasedTable;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;

public class TestLinkLog {


    @Test
    public void testLinkLogEntryDefaultsMode() {
        RowSortedTable<Long, String, Object> expectedTable = new LinkLogTableBuilder()
                .withInitialEntry(0L, "someLink", "someVehicle", "unknown", 12.0)
                .build();

        LinkLog linkLog = new LinkLog();
        linkLog.newLinkLogEntry("someVehicle", "someLink", 12.0);

        assertThat(
                linkLog.getLinkLogData())
                .isEqualTo(expectedTable);
    }

    @Test
    public void testLinkLogEntryUsesRecordedVehicleMode() {
        RowSortedTable<Long, String, Object> expectedTable = new LinkLogTableBuilder()
                .withInitialEntry(0L, "someLink", "someVehicle", "someMode", 12.0)
                .build();

        LinkLog linkLog = new LinkLog();
        linkLog.recordVehicleMode("someVehicle", "someMode");
        linkLog.newLinkLogEntry("someVehicle", "someLink", 12.0);

        assertThat(
                linkLog.getLinkLogData())
                .isEqualTo(expectedTable);
    }

    @Test
    public void testCompleteLinkLogEntry() {
        RowSortedTable<Long, String, Object> expectedLinkLogTable = new LinkLogTableBuilder()
                .withCompleteEntry(0L, "someLink", "someVehicle", "unknown", 12.0, 24.0, 1)
                .build();
        RowSortedTable<Long, String, Object> expectedOccupancyTable = new LinkLogVehicleOccupancyTableBuilder()
                .withEntry(0L, "somePerson")
                .build();

        LinkLog linkLog = new LinkLog();
        linkLog.personBoardsVehicle("someVehicle", "somePerson");
        linkLog.newLinkLogEntry("someVehicle", "someLink", 12.0);
        linkLog.completeLinkLogEntry("someVehicle", 24.0);

        assertThat(
                linkLog.getLinkLogData())
                .isEqualTo(expectedLinkLogTable);
        assertThat(
                linkLog.getVehicleOccupantsData())
                .isEqualTo(expectedOccupancyTable);
    }

    @Test
    public void testPersonCanEnterVehicleThatIsAlreadyMoving() {
        RowSortedTable<Long, String, Object> expectedTable = new LinkLogTableBuilder()
                .withCompleteEntry(0L, "someLink", "someVehicle", "unknown", 12.0, 24.0, 1)
                .build();

        LinkLog linkLog = new LinkLog();
        linkLog.newLinkLogEntry("someVehicle", "someLink", 12.0);
        linkLog.personBoardsVehicle("someVehicle", "somePerson");
        linkLog.completeLinkLogEntry("someVehicle", 24.0);

        assertThat(
                linkLog.getLinkLogData())
                .isEqualTo(expectedTable);
    }

    @Test
    public void testRecordsMultiplePeopleBoardingAtOnce() {
        RowSortedTable<Long, String, Object> expectedLinkLogTable = new LinkLogTableBuilder()
                .withCompleteEntry(0L, "someLink", "PartyBus", "unknown", 12.0, 24.0, 3)
                .build();
        RowSortedTable<Long, String, Object> expectedOccupancyTable = new LinkLogVehicleOccupancyTableBuilder()
                .withEntry(0L, "partyPerson1")
                .withEntry(0L, "partyPerson2")
                .withEntry(0L, "partyPerson3")
                .build();

        LinkLog linkLog = new LinkLog();
        // bus enters a link
        linkLog.newLinkLogEntry("PartyBus", "someLink", 12.0);
        // time to get this party started
        linkLog.personBoardsVehicle("PartyBus", "partyPerson1");
        linkLog.personBoardsVehicle("PartyBus", "partyPerson2");
        linkLog.personBoardsVehicle("PartyBus", "partyPerson3");
        // bus completes its travel on link
        linkLog.completeLinkLogEntry("PartyBus", 24.0);

        assertThat(
                linkLog.getLinkLogData())
                .isEqualTo(expectedLinkLogTable);
        assertThat(
                linkLog.getVehicleOccupantsData())
                .isEqualTo(expectedOccupancyTable);
    }

    @Test
    public void testTracksBoardingAndAlighting() {
        RowSortedTable<Long, String, Object> expectedLinkLogTable = new LinkLogTableBuilder()
                .withCompleteEntry(0L, "startLink", "PartyBus", "unknown", 0.0, 5.0, 1)
                .withCompleteEntry(1L, "gerryLinkBoard", "PartyBus", "unknown", 5.0, 10.0, 2)
                .withCompleteEntry(2L, "gerryLinkAlight", "PartyBus", "unknown", 10.0, 15.0, 1)
                .withCompleteEntry(3L, "endLink", "PartyBus", "unknown", 15.0, 20.0, 1)
                .build();
        RowSortedTable<Long, String, Object> expectedOccupancyTable = new LinkLogVehicleOccupancyTableBuilder()
                .withEntry(0L, "driver")
                .withEntry(1L, "driver")
                .withEntry(1L, "gerry")
                .withEntry(2L, "driver")
                .withEntry(3L, "driver")
                .build();

        LinkLog linkLog = new LinkLog();
        // driver hops on and bus starts
        linkLog.personBoardsVehicle("PartyBus", "driver");
        linkLog.newLinkLogEntry("PartyBus", "startLink", 0.0);
        linkLog.completeLinkLogEntry("PartyBus", 5.0);
        // gerry gets on
        linkLog.newLinkLogEntry("PartyBus", "gerryLinkBoard", 5.0);
        linkLog.personBoardsVehicle("PartyBus", "gerry");
        linkLog.completeLinkLogEntry("PartyBus", 10.0);
        // gerry leaves
        linkLog.newLinkLogEntry("PartyBus", "gerryLinkAlight", 10.0);
        linkLog.personAlightsVehicle("PartyBus", "gerry");
        linkLog.completeLinkLogEntry("PartyBus", 15.0);
        // bus completes its travel
        linkLog.newLinkLogEntry("PartyBus", "endLink", 15.0);
        linkLog.completeLinkLogEntry("PartyBus", 20.0);

        assertThat(
                linkLog.getLinkLogData())
                .isEqualTo(expectedLinkLogTable);
        assertThat(
                linkLog.getVehicleOccupantsData())
                .isEqualTo(expectedOccupancyTable);
    }

    @Test(expected = LinkLogPassengerConsistencyException.class)
    public void testThrowsExceptionWhenTryingToCompleteLogOfUnrecordedVehicle() {
        LinkLog linkLog = new LinkLog();
        linkLog.newLinkLogEntry("someVehicle", "someLink", 12.0);
        linkLog.completeLinkLogEntry("someVehicle", 24.0);
    }

    @Test(expected = LinkLogPassengerConsistencyException.class)
    public void testThrowsExceptionWhenPassengerWantsToLeaveUnrecordedVehicle() {
        LinkLog linkLog = new LinkLog();
        linkLog.personAlightsVehicle("someVehicle", "someDude");
    }

    @Test(expected = LinkLogPassengerConsistencyException.class)
    public void testThrowsExceptionWhenPersonWantsToLeaveVehicleTheyDidntBoard() {
        LinkLog linkLog = new LinkLog();
        linkLog.personBoardsVehicle("someVehicle", "someDude");
        linkLog.personAlightsVehicle("someVehicle", "someRandomDude");
    }

    private class LinkLogTableBuilder {

        private LinkLogTableBuilder() {
        }

        RowSortedTable<Long, String, Object> linkLog = TreeBasedTable.create();

        public LinkLogTableBuilder withInitialEntry(
                Long index, String linkID, String vehicleID, String mode, double startTime) {
            linkLog.put(index, "linkID", linkID);
            linkLog.put(index, "vehicleID", vehicleID);
            linkLog.put(index, "mode", mode);
            linkLog.put(index, "startTime", startTime);
            return this;
        }

        public LinkLogTableBuilder withCompleteEntry(
                Long index, String linkID, String vehicleID, String mode, double startTime, double endTime, int numberOfPeople) {
            linkLog.put(index, "linkID", linkID);
            linkLog.put(index, "vehicleID", vehicleID);
            linkLog.put(index, "mode", mode);
            linkLog.put(index, "startTime", startTime);
            linkLog.put(index, "endTime", endTime);
            linkLog.put(index, "numberOfPeople", numberOfPeople);
            return this;
        }

        public RowSortedTable<Long, String, Object> build() {
            return linkLog;
        }
    }

    private class LinkLogVehicleOccupancyTableBuilder {

        private LinkLogVehicleOccupancyTableBuilder() {
        }

        RowSortedTable<Long, String, Object> linkLogVehicleOccupants = TreeBasedTable.create();

        public LinkLogVehicleOccupancyTableBuilder withEntry(Long linkLogIndex, String agentId) {
            linkLogVehicleOccupants.put(linkLogIndex, "agentId", agentId);
            return this;
        }

        public RowSortedTable<Long, String, Object> build() {
            return linkLogVehicleOccupants;
        }
    }
}
