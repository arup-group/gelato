package com.arup.cml.abm.kpi.tablesaw;

import org.junit.Test;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestTablesawNetworkLinkLog {

    @Test
    public void defaultsToUnknownModeForNewLinkLogEntryWithUnrecordedMode() {
        TablesawNetworkLinkLog linkLog = new TablesawNetworkLinkLog();
        assertThat(linkLog.getLinkLogTable().isEmpty()).isTrue().as("Link log table should be empty initially");

        linkLog.createLinkLogEntry("someVehicle", "someLink", 12.0);

        Table linkLogTable = linkLog.getLinkLogTable();
        assertTableHasSingleRow(linkLogTable);
        Row linkLogEntry = linkLogTable.row(0);
        assertThat(linkLogEntry.getString("initialMode"))
                .isEqualTo("unknown")
                .as("Vehicle mode should default to 'unknown'");
    }

    @Test
    public void usesRecordedModeWhenAvailableForNewLinkLogEntry() {
        TablesawNetworkLinkLog linkLog = new TablesawNetworkLinkLog();
        assertThat(linkLog.getLinkLogTable().isEmpty()).isTrue().as("Link log table should be empty initially");

        linkLog.recordVehicleMode("someVehicle", "someMode");
        linkLog.createLinkLogEntry("someVehicle", "someLink", 12.0);

        Table linkLogTable = linkLog.getLinkLogTable();
        assertTableHasSingleRow(linkLogTable);
        Row linkLogEntry = linkLogTable.row(0);
        assertThat(linkLogEntry.getString("initialMode"))
                .isEqualTo("someMode")
                .as("Vehicle mode should be the one previous recorded ('someMode')");
    }

    @Test
    public void completeLinkLogEntryHasEndTime() {
        TablesawNetworkLinkLog linkLog = new TablesawNetworkLinkLog();
        assertThat(linkLog.getLinkLogTable().isEmpty()).isTrue().as("Link log table should be empty initially");

        linkLog.personBoardsVehicle("someVehicle", "somePerson");
        linkLog.createLinkLogEntry("someVehicle", "someLink", 12.0);
        linkLog.completeLinkLogEntry("someVehicle", 24.0);

        Table linkLogTable = linkLog.getLinkLogTable();
        assertTableHasSingleRow(linkLogTable);
        Row linkLogEntry = linkLogTable.row(0);
        assertThat(linkLogEntry.getDouble("endTime"))
                .isEqualTo(24.0)
                .as("Finish time on the link should have been recorded as `24.0`");
    }

    @Test
    public void completeLinkLogEntryTracksPassengerNumber() {
        TablesawNetworkLinkLog linkLog = new TablesawNetworkLinkLog();
        assertThat(linkLog.getLinkLogTable().isEmpty()).isTrue().as("Link log table should be empty initially");

        linkLog.personBoardsVehicle("someVehicle", "somePerson");
        linkLog.createLinkLogEntry("someVehicle", "someLink", 12.0);
        linkLog.completeLinkLogEntry("someVehicle", 24.0);

        Table linkLogTable = linkLog.getLinkLogTable();
        assertTableHasSingleRow(linkLogTable);
        Row linkLogEntry = linkLogTable.row(0);

        assertThat(linkLogEntry.getInt("numberOfPeople"))
                .isEqualTo(1)
                .as("Number of people in vehicle should have been recorded as `1`");
    }

    private static void assertTableHasSingleRow(Table table) {
        assertThat(table.rowCount())
                .isEqualTo(1)
                .as(String.format("Table '%s' should contain a single row, but contained %s rows",
                        table.name(),
                        table.rowCount()));
    }
}
