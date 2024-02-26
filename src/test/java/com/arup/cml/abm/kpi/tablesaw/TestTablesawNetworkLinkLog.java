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
        assertThat(linkLogTable.rowCount())
                    .isEqualTo(1)
                .as("Link log table should now contain a single row");

        Row linkLogEntry = linkLogTable.row(0);
        assertThat(linkLogEntry.getString("initialMode"))
                .isEqualTo("unknown")
                .as("Vehicle mode should default to 'unknown'");
    }
}
