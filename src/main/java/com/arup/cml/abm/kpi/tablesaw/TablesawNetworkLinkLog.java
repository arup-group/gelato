package com.arup.cml.abm.kpi.tablesaw;

import com.arup.cml.abm.kpi.data.exceptions.LinkLogPassengerConsistencyException;
import com.arup.cml.abm.kpi.domain.LinkLogConsistencyException;
import com.arup.cml.abm.kpi.domain.NetworkLinkLog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.tablesaw.api.*;

import java.util.*;

public class TablesawNetworkLinkLog implements NetworkLinkLog {

    private static final Logger LOGGER = LogManager.getLogger(TablesawNetworkLinkLog.class);

    private final Map<String, String> vehicleModes = new HashMap<>();

    // points to the index of the most recent reference of that vehicle ID in the Link Log
    private final Map<String, Integer> vehicleLatestLogIndex = new HashMap<>();

    private final Map<String, List<String>> vehicleLatestOccupants = new HashMap<>();

    private Table linkLogTable;
    private Table vehicleOccupancyTable;
    private int rowIndex = 0;
//    private int vehicleOccupancyIndex = 0;

    public TablesawNetworkLinkLog() {
        linkLogTable = createLinkLogTable();
        vehicleOccupancyTable = createVehicleOccupancyTable();
    }

    private Table createLinkLogTable() {
        LOGGER.info("Creating Tablesaw Link Log tables");
        LongColumn indexColumn = LongColumn.create("index");
        StringColumn linkIDColumn = StringColumn.create("linkID");
        StringColumn vehicleIDColumn = StringColumn.create("vehicleID");
        StringColumn modeColumn = StringColumn.create("initialMode");
        DoubleColumn startTimeColumn = DoubleColumn.create("startTime");
        DoubleColumn endTimeColumn = DoubleColumn.create("endTime");
        IntColumn numberOfPeopleColumn = IntColumn.create("numberOfPeople");

        return Table.create("Link Log").addColumns(
                indexColumn,
                linkIDColumn,
                vehicleIDColumn,
                modeColumn,
                startTimeColumn,
                endTimeColumn,
                numberOfPeopleColumn
        );
    }

    private Table createVehicleOccupancyTable() {
        LOGGER.info("Creating Tablesaw Vehicle Occupancy table");
        LongColumn linkLogIndexColumn = LongColumn.create("linkLogIndex");
        StringColumn agentIDColumn = StringColumn.create("agentId");
        return Table.create("Vehicle Occupancy").addColumns(linkLogIndexColumn, agentIDColumn);
    }

    @Override
    public void createLinkLogEntry(String vehicleID, String linkID, double startTime) {
        Row row = linkLogTable.appendRow();
        row.setLong("index", rowIndex);
        row.setString("linkID", linkID);
        row.setString("vehicleID", vehicleID);
        row.setString("initialMode", vehicleModes.getOrDefault(vehicleID, "unknown"));
        row.setDouble("startTime", startTime);
        vehicleLatestLogIndex.put(vehicleID, rowIndex);
        rowIndex++;
    }

    @Override
    public void completeLinkLogEntry(String vehicleID, double endTime) {
        int latestStateIndex = this.vehicleLatestLogIndex.get(vehicleID);
        Row row = linkLogTable.row(latestStateIndex);
        row.setDouble("endTime", endTime);
        row.setInt("numberOfPeople",
                vehicleLatestOccupants.getOrDefault(vehicleID, new ArrayList<>()).size());
        newVehicleOccupantsEntry(vehicleID, latestStateIndex);
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
            throw new LinkLogPassengerConsistencyException(String.format(
                    "The requested person: `%s` cannot leave vehicle `%s` because they didn't board it",
                    personID,
                    vehicleID));
        }
    }

    public Table getLinkLogTable() {
        return linkLogTable;
    }

    public Table getVehicleOccupancyTable() {
        return vehicleOccupancyTable;
    }

    private void newVehicleOccupantsEntry(String vehicleID, long idx) {
        System.out.println("" + vehicleID + "," + idx);
//        List<String> currentOccupants = getLatestVehicleOccupants(vehicleID);
//        for (String personID : currentOccupants) {
//            vehicleOccupantsData.put(vehicleOccupancyIndex, "linkLogIndex", idx);
//            vehicleOccupantsData.put(vehicleOccupancyIndex, "agentId", personID);
//            vehicleOccupancyIndex++;
//        }
    }

}
