package com.arup.cml.abm.kpi.tablesaw;

import com.arup.cml.abm.kpi.domain.LinkLogConsistencyException;
import org.junit.Test;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.selection.Selection;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestTablesawNetworkLinkLog {

    @Test
    public void defaultsToUnknownModeWhenCreatingLinkLogEntryWithUnrecordedMode() {
        TablesawNetworkLinkLog linkLog = new TablesawNetworkLinkLog();
        assertThat(linkLog.getLinkLogTable().isEmpty()).isTrue().as("Link log table should be empty initially");

        linkLog.createLinkLogEntry("someVehicle", "someLink", 12.0);

        Table linkLogTable = linkLog.getLinkLogTable();
        assertTableHasSingleRow(linkLogTable);
        Row linkLogEntry = linkLogTable.row(0);
        assertThat(linkLogEntry.getString("initialMode"))
                .as("Vehicle mode should default to 'unknown'")
                .isEqualTo("unknown");
    }

    @Test
    public void usesRecordedModeWhenAvailableWhenCreatingLinkLogEntry() {
        TablesawNetworkLinkLog linkLog = new TablesawNetworkLinkLog();
        assertThat(linkLog.getLinkLogTable().isEmpty()).isTrue().as("Link log table should be empty initially");

        linkLog.recordVehicleMode("someVehicle", "someMode");
        linkLog.createLinkLogEntry("someVehicle", "someLink", 12.0);

        Table linkLogTable = linkLog.getLinkLogTable();
        assertTableHasSingleRow(linkLogTable);
        Row linkLogEntry = linkLogTable.row(0);
        assertThat(linkLogEntry.getString("initialMode"))
                .as("Vehicle mode should be as previously recorded ('someMode')")
                .isEqualTo("someMode");
    }

    @Test
    public void setsEndTimeWhenClosingLinkLogEntry() {
        TablesawNetworkLinkLog linkLog = new TablesawNetworkLinkLog();
        assertThat(linkLog.getLinkLogTable().isEmpty()).isTrue().as("Link log table should be empty initially");

        linkLog.personBoardsVehicle("someVehicle", "somePerson");
        linkLog.createLinkLogEntry("someVehicle", "someLink", 12.0);
        linkLog.completeLinkLogEntry("someVehicle", 24.0);

        Table linkLogTable = linkLog.getLinkLogTable();
        assertTableHasSingleRow(linkLogTable);
        Row linkLogEntry = linkLogTable.row(0);
        assertThat(linkLogEntry.getDouble("endTime"))
                .as("Finish time on the link should have been recorded as `24.0`")
                .isEqualTo(24.0);
    }

    @Test
    public void setsPassengerCountWhenPersonBoardsVehicleBeforeLinkLogEntryExists() {
        TablesawNetworkLinkLog linkLog = new TablesawNetworkLinkLog();
        assertThat(linkLog.getLinkLogTable().isEmpty()).isTrue().as("Link log table should be empty initially");

        linkLog.personBoardsVehicle("someVehicle", "somePerson");
        linkLog.createLinkLogEntry("someVehicle", "someLink", 12.0);
        linkLog.completeLinkLogEntry("someVehicle", 24.0);

        Table linkLogTable = linkLog.getLinkLogTable();
        assertTableHasSingleRow(linkLogTable);
        Row linkLogEntry = linkLogTable.row(0);
        assertThat(linkLogEntry.getInt("numberOfPeople"))
                .as("Number of people in vehicle should have been recorded as `1`")
                .isEqualTo(1);
    }

    @Test
    public void setsPassengerCountWhenPersonBoardsVehicleAfterLinkLogEntryExists() {
        TablesawNetworkLinkLog linkLog = new TablesawNetworkLinkLog();
        assertThat(linkLog.getLinkLogTable().isEmpty()).isTrue().as("Link log table should be empty initially");

        linkLog.createLinkLogEntry("someVehicle", "someLink", 12.0);
        linkLog.personBoardsVehicle("someVehicle", "somePerson");
        linkLog.completeLinkLogEntry("someVehicle", 24.0);

        Table linkLogTable = linkLog.getLinkLogTable();
        assertTableHasSingleRow(linkLogTable);
        Row linkLogEntry = linkLogTable.row(0);
        assertThat(linkLogEntry.getInt("numberOfPeople"))
                .as("Number of people in vehicle should have been recorded as '1'")
                .isEqualTo(1);
    }

    @Test
    public void updatesVehicleOccupancyTableOnClosingLinkLogEntry() {
        TablesawNetworkLinkLog linkLog = new TablesawNetworkLinkLog();

        linkLog.personBoardsVehicle("someVehicle", "somePerson");
        linkLog.createLinkLogEntry("someVehicle", "someLink", 12.0);
        linkLog.completeLinkLogEntry("someVehicle", 24.0);

        Table vehicleOccupantsTable = linkLog.getVehicleOccupancyTable();
        assertTableHasSingleRow(vehicleOccupantsTable);
        Row vehicleOccupantsEntry = vehicleOccupantsTable.row(0);
        assertThat(vehicleOccupantsEntry.getString("agentId"))
                .as("Agent in the vehicle should have been recorded as 'somePerson'")
                .isEqualTo("somePerson");
    }

    @Test
    public void defaultsToZeroVehicleOccupancyOnClosingLinkLogEntryOfUnrecordedVehicle() {
        TablesawNetworkLinkLog linkLog = new TablesawNetworkLinkLog();

        linkLog.createLinkLogEntry("someVehicle", "someLink", 12.0);
        linkLog.completeLinkLogEntry("someVehicle", 24.0);

        Table linkLogTable = linkLog.getLinkLogTable();
        assertTableHasSingleRow(linkLogTable);
        Row linkLogEntry = linkLogTable.row(0);
        assertThat(linkLogEntry.getInt("numberOfPeople"))
                .as("Number of people in unrecorded vehicle")
                .withFailMessage(String.format("Number of people should default to 0, but was %s",
                        linkLogEntry.getInt("numberOfPeople")))
                .isEqualTo(0);
    }

    @Test
    public void linkLogRecordsMultiplePeopleBoardingAtOnce() {
        TablesawNetworkLinkLog linkLog = new TablesawNetworkLinkLog();

        linkLog.createLinkLogEntry("PartyBus", "someLink", 12.0);
        // time to get this party started
        String[] partyPeople = {"partyPerson1", "partyPerson2", "partyPerson3"};
        for (int i = 0; i < partyPeople.length; i++) {
            linkLog.personBoardsVehicle("PartyBus", partyPeople[i]);
        }
        // bus completes its travel on link
        linkLog.completeLinkLogEntry("PartyBus", 24.0);

        Table linkLogTable = linkLog.getLinkLogTable();
        assertTableHasSingleRow(linkLogTable);
        Row linkLogEntry = linkLogTable.row(0);
        assertThat(linkLogEntry.getInt("numberOfPeople"))
                .as("Number of people in vehicle should have been recorded as 3")
                .isEqualTo(3);
    }

    @Test
    public void vehicleOccupancyTableRecordsMultiplePeopleBoardingAtOnce() {
        TablesawNetworkLinkLog linkLog = new TablesawNetworkLinkLog();
        String[] partyPeople = {"partyPerson1", "partyPerson2", "partyPerson3"};

        linkLog.createLinkLogEntry("PartyBus", "someLink", 12.0);
        for (int i = 0; i < partyPeople.length; i++) {
            linkLog.personBoardsVehicle("PartyBus", partyPeople[i]);
        }
        linkLog.completeLinkLogEntry("PartyBus", 24.0);

        Table vehicleOccupantsTable = linkLog.getVehicleOccupancyTable();
        // we expect:
        // linkLogIndex  |    agentId     |
        //---------------------------------
        //            0  |  partyPerson1  |
        //            0  |  partyPerson2  |
        //            0  |  partyPerson3  |
        assertTableRowCount(vehicleOccupantsTable, 3);
        for (int i = 0; i < partyPeople.length; i++) {
            Selection filter =
                    vehicleOccupantsTable.stringColumn("agentId").isEqualTo(partyPeople[i])
                            .and(vehicleOccupantsTable.longColumn("linkLogIndex").isEqualTo(0)
            );
            assertTableContainsRow(vehicleOccupantsTable, filter);
        }
    }

    @Test(expected = LinkLogConsistencyException.class)
    public void throwsExceptionWhenPersonAlightsUnrecordedVehicle() {
        new TablesawNetworkLinkLog().personAlightsVehicle("badVehicle", "someDude");
    }

    @Test(expected = LinkLogConsistencyException.class)
    public void throwsExceptionWhenPersonAlightsVehicleTheyDidNotBoard() {
        TablesawNetworkLinkLog linkLog = new TablesawNetworkLinkLog();

        linkLog.personBoardsVehicle("someVehicle", "someDude");
        linkLog.personAlightsVehicle("someVehicle", "nonBoardingDude");
    }

    @Test
    public void linkLogTracksPassengerBoardingAndAlighting() {
        TablesawNetworkLinkLog linkLog = new TablesawNetworkLinkLog();

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

        Table linkLogTable = linkLog.getLinkLogTable();
        // we expect:
        // index  |      linkID       |  vehicleID  |  initialMode  |  startTime  |  endTime  |  numberOfPeople  |
        //--------------------------------------------------------------------------------------------------------
        //     0  |        startLink  |   PartyBus  |      unknown  |          0  |        5  |               1  |
        //     1  |   gerryLinkBoard  |   PartyBus  |      unknown  |          5  |       10  |               2  |
        //     2  |  gerryLinkAlight  |   PartyBus  |      unknown  |         10  |       15  |               1  |
        //     3  |          endLink  |   PartyBus  |      unknown  |         15  |       20  |               1  |
        assertTableRowCount(linkLogTable, 4);
        Selection filter = linkLogTable.stringColumn("linkID").isEqualTo("startLink")
                        .and(linkLogTable.intColumn("numberOfPeople").isEqualTo(1))
                        .and(linkLogTable.stringColumn("vehicleID").isEqualTo("PartyBus"));
        assertTableContainsRow(linkLogTable, filter, "linkID='startLink', numberOfPeople=1, vehicleID='PartyBus'");

        filter = linkLogTable.stringColumn("linkID").isEqualTo("gerryLinkBoard")
                .and(linkLogTable.intColumn("numberOfPeople").isEqualTo(2))
                .and(linkLogTable.stringColumn("vehicleID").isEqualTo("PartyBus"));
        assertTableContainsRow(linkLogTable,
                filter,
                "linkID='gerryLinkBoard', numberOfPeople=2, vehicleID='PartyBus'");

        filter = linkLogTable.stringColumn("linkID").isEqualTo("gerryLinkAlight")
                .and(linkLogTable.intColumn("numberOfPeople").isEqualTo(1))
                .and(linkLogTable.stringColumn("vehicleID").isEqualTo("PartyBus"));
        assertTableContainsRow(linkLogTable,
                filter,
                "linkID='gerryLinkAlight', numberOfPeople=1, vehicleID='PartyBus'");

        filter = linkLogTable.stringColumn("linkID").isEqualTo("endLink")
                .and(linkLogTable.intColumn("numberOfPeople").isEqualTo(1))
                .and(linkLogTable.stringColumn("vehicleID").isEqualTo("PartyBus"));
        assertTableContainsRow(linkLogTable, filter, "linkID='endLink', numberOfPeople=1, vehicleID='PartyBus'");
    }

    @Test
    public void vehicleOccupantsTableTracksBoardingAndAlighting() {
        TablesawNetworkLinkLog linkLog = new TablesawNetworkLinkLog();

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

        Table linkLogVehicleOccupantsTable = linkLog.getVehicleOccupancyTable();
        // we expect:
        // linkLogIndex  |  agentId  |
        //----------------------------
        //            0  |   driver  |
        //            1  |   driver  |
        //            1  |    gerry  |
        //            2  |   driver  |
        //            3  |   driver  |
        assertTableRowCount(linkLogVehicleOccupantsTable, 5);
        Selection filter = linkLogVehicleOccupantsTable.longColumn("linkLogIndex").isEqualTo(0)
                .and(linkLogVehicleOccupantsTable.stringColumn("agentId").isEqualTo("driver"));
        assertTableContainsRow(linkLogVehicleOccupantsTable, filter, "linkLogIndex=0, agentId='driver'");

        filter = linkLogVehicleOccupantsTable.longColumn("linkLogIndex").isEqualTo(1)
                .and(linkLogVehicleOccupantsTable.stringColumn("agentId").isEqualTo("driver"));
        assertTableContainsRow(linkLogVehicleOccupantsTable, filter, "linkLogIndex=1, agentId='driver'");

        filter = linkLogVehicleOccupantsTable.longColumn("linkLogIndex").isEqualTo(1)
                .and(linkLogVehicleOccupantsTable.stringColumn("agentId").isEqualTo("gerry"));
        assertTableContainsRow(linkLogVehicleOccupantsTable, filter, "linkLogIndex=1, agentId='gerry'");

        filter = linkLogVehicleOccupantsTable.longColumn("linkLogIndex").isEqualTo(2)
                .and(linkLogVehicleOccupantsTable.stringColumn("agentId").isEqualTo("driver"));
        assertTableContainsRow(linkLogVehicleOccupantsTable, filter, "linkLogIndex=2, agentId='driver'");

        filter = linkLogVehicleOccupantsTable.longColumn("linkLogIndex").isEqualTo(3)
                .and(linkLogVehicleOccupantsTable.stringColumn("agentId").isEqualTo("driver"));
        assertTableContainsRow(linkLogVehicleOccupantsTable, filter, "linkLogIndex=3, agentId='driver'");
    }

    ///////////////////////////////////////////////////////
    // helper methods
    ///////////////////////////////////////////////////////
    private static void assertTableHasSingleRow(Table table) {
        assertTableRowCount(table, 1);
    }

    private static void assertTableRowCount(Table table, int expectedRowCount) {
        assertThat(table.rowCount())
                .as(String.format("Table '%s' should contain %s rows",
                        table.name(),
                        expectedRowCount))
                .withFailMessage("Actually contained %s rows".formatted(table.rowCount()))
                .isEqualTo(expectedRowCount);
    }

    private static void assertTableContainsRow(Table table, Selection filter, String filterDescription) {
        try {
            assertTableHasSingleRow(table.where(filter));
        } catch (AssertionError e) {
            String description = filterDescription == null ? filter.toString() : filterDescription;
            String detailedFailureMessage =
                    String.format("Unmatched filter was (%s) and the table being queried was %n%s", description, table);
            throw new AssertionError(detailedFailureMessage, e);
        }
    }

    private static void assertTableContainsRow(Table table, Selection filter) {
        assertTableContainsRow(table, filter, null);
    }
}
