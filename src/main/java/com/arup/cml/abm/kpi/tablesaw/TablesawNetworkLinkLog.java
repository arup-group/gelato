package com.arup.cml.abm.kpi.tablesaw;

import com.arup.cml.abm.kpi.domain.LinkLogConsistencyException;
import com.arup.cml.abm.kpi.domain.NetworkLinkLog;
import tech.tablesaw.api.*;

import java.util.*;

public class TablesawNetworkLinkLog implements NetworkLinkLog {

    private final Map<String, String> vehicleModes = new HashMap<>();
    // points to the index of the most recent reference of that vehicle ID in the Link Log
    private final Map<String, Integer> vehicleLatestLogIndex = new HashMap<>();
    private final Map<String, List<String>> vehicleLatestOccupants = new HashMap<>();
    private Table linkLogTable;
    private Table vehicleOccupancyTable;
    private int linkLogRowIndex = 0;

    public TablesawNetworkLinkLog() {
        linkLogTable = createLinkLogTable();
        vehicleOccupancyTable = createVehicleOccupancyTable();
    }

    private Table createLinkLogTable() {
        return Table.create("Link Log").addColumns(
                LongColumn.create("index"),
                StringColumn.create("linkID"),
                StringColumn.create("vehicleID"),
                StringColumn.create("initialMode"),
                DoubleColumn.create("startTime"),
                DoubleColumn.create("endTime"),
                IntColumn.create("numberOfPeople")
        );
    }

    private Table createVehicleOccupancyTable() {
        return Table.create("Vehicle Occupancy").addColumns(
                LongColumn.create("linkLogIndex"),
                StringColumn.create("agentId")
        );
    }

    @Override
    public void createLinkLogEntry(String vehicleID, String linkID, double startTime) {
        Row row = linkLogTable.appendRow();
        row.setLong("index", linkLogRowIndex);
        row.setString("linkID", linkID);
        row.setString("vehicleID", vehicleID);
        row.setString("initialMode", vehicleModes.getOrDefault(vehicleID, "unknown"));
        row.setDouble("startTime", startTime);
        vehicleLatestLogIndex.put(vehicleID, linkLogRowIndex);
        linkLogRowIndex++;
    }

    @Override
    public void completeLinkLogEntry(String vehicleID, double endTime) {
        int latestStateIndex = vehicleLatestLogIndex.get(vehicleID);
        Row row = linkLogTable.row(latestStateIndex);
        row.setDouble("endTime", endTime);
        List<String> currentVehicleOccupants = vehicleLatestOccupants.getOrDefault(vehicleID, new ArrayList<>());
        row.setInt("numberOfPeople", currentVehicleOccupants.size());
        updateVehicleOccupancyTable(latestStateIndex, currentVehicleOccupants);
    }

    @Override
    public void recordVehicleMode(String vehicleId, String mode) {
        vehicleModes.put(vehicleId, mode);
    }

    @Override
    public void personBoardsVehicle(String vehicleID, String personID) {
        if (vehicleLatestOccupants.containsKey(vehicleID)) {
            vehicleLatestOccupants.get(vehicleID).add(personID);
        } else {
            vehicleLatestOccupants.put(vehicleID, new ArrayList<>(Arrays.asList(personID)));
        }
    }

    @Override
    public void personAlightsVehicle(String vehicleID, String personID) throws LinkLogConsistencyException {
        if (vehicleLatestOccupants.containsKey(vehicleID)
                && vehicleLatestOccupants.get(vehicleID).contains(personID)) {
            vehicleLatestOccupants.get(vehicleID).remove(personID);
        } else {
            throw new LinkLogConsistencyException(String.format(
                    "Person '%s' cannot leave vehicle '%s' because they didn't board it",
                    personID,
                    vehicleID));
        }
    }

    private void updateVehicleOccupancyTable(long linkLogIndex, List<String> vehicleOccupants) {
        for (String personID : vehicleOccupants) {
            Row row = vehicleOccupancyTable.appendRow();
            row.setLong("linkLogIndex", linkLogIndex);
            row.setString("agentId", personID);
        }
    }

    public Table getLinkLogTable() {
        return linkLogTable;
    }

    public Table getVehicleOccupancyTable() {
        return vehicleOccupancyTable;
    }
}
