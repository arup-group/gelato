package com.arup.cml.abm.kpi.tablesaw;

import com.arup.cml.abm.kpi.domain.LinkLogConsistencyException;
import com.arup.cml.abm.kpi.domain.NetworkLinkLog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.tablesaw.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TablesawNetworkLinkLog implements NetworkLinkLog {

    private static final Logger LOGGER = LogManager.getLogger(TablesawNetworkLinkLog.class);

    private final Map<String, String> vehicleModes = new HashMap<>();

    // points to the index of the most recent reference of that vehicle ID in the Link Log
    private final Map<String, Long> vehicleLatestLogIndex = new HashMap<>();

    // tracks the most recent occupants of a vehicle
    private final Map<String, List<String>> vehicleLatestOccupants = new HashMap<>();

    private Table linkLogTable;
    private Table vehicleOccupancyTable;
    private long rowIndex = 0;
    private long vehicleOccupancyIndex = 0;

    public TablesawNetworkLinkLog() {
        linkLogTable = createLinkLogTable();
        vehicleOccupancyTable = createVehicleOccupancyTable(linkLogTable);
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
//        if (missingValues > 0) {
//            LOGGER.warn("{} missing `endTime` data points were encountered - some vehicles " +
//                            "were stuck and did not complete their journey. These Link Log entries will be deleted.",
//                    missingValues);
//            linkLog = linkLog.where(linkLog.doubleColumn("endTime").isNotEqualTo(-1));
//        }

        // fix vehicle modes with vehicle table
//        linkLogTable = linkLogTable
//                .joinOn("vehicleID")
//                .leftOuter(vehicles.selectColumns("vehicleID", "mode"));
//        int mismatchedModes = linkLogTable.where(
//                linkLogTable.stringColumn("initialMode")
//                        .isNotEqualTo(linkLogTable.stringColumn("mode")
//                        )
//        ).stringColumn("vehicleID").countUnique();
//        if (mismatchedModes > 0) {
//            LOGGER.warn(String.format(
//                    "There are %d vehicles that have different modes to the ones found in the Link Log. " +
//                            "The modes in the Link Log will be updated with the modes from the Vehicle Table.",
//                    mismatchedModes));
//        }
//        linkLogTable.removeColumns("initialMode");

    }

    private Table createVehicleOccupancyTable(Table linkLogTable) {
        LOGGER.info("Creating Tablesaw Vehicle Occupancy table");
        LongColumn linkLogIndexColumn = LongColumn.create("linkLogIndex");
        StringColumn agentIDColumn = StringColumn.create("agentId");

//        for (Map.Entry<Long, Map<String, Object>> entry : _linkLog.getVehicleOccupantsData().rowMap()
//                .entrySet()) {
//            linkLogIndexColumn.append((long) entry.getValue().get("linkLogIndex"));
//            agentIDColumn.append(entry.getValue().get("agentId").toString());
//        }
        return Table.create("Vehicle Occupancy").addColumns(linkLogIndexColumn, agentIDColumn);
    }

    @Override
    public void createLinkLogEntry(String vehicleID, String linkID, double startTime) {
//                linkLogData.put(index, "linkID", linkID);
//        linkLogData.put(index, "vehicleID", vehicleID);
//        linkLogData.put(index, "mode", vehicleModes.getOrDefault(vehicleID, "unknown"));
//        linkLogData.put(index, "startTime", startTime);
//        vehicleLatestLogIndex.put(vehicleID, index);
//        index++;
        Row row = linkLogTable.appendRow();
        row.setLong("index", rowIndex);
        row.setString("linkID", linkID);
        row.setString("vehicleID", vehicleID);
        row.setString("initialMode", "unknown");
        row.setDouble("startTime", startTime);
        rowIndex++;

//        LongColumn indexColumn = LongColumn.create("index");
//        StringColumn linkIDColumn = StringColumn.create("linkID");
//        StringColumn vehicleIDColumn = StringColumn.create("vehicleID");
//        StringColumn modeColumn = StringColumn.create("initialMode");
//        DoubleColumn startTimeColumn = DoubleColumn.create("startTime");
//        DoubleColumn endTimeColumn = DoubleColumn.create("endTime");
//        IntColumn numberOfPeopleColumn = IntColumn.create("numberOfPeople");

    }

    @Override
    public void completeLinkLogEntry(String vehicleID, double endTime) {

    }

    @Override
    public void recordVehicleMode(String vehicleId, String mode) {

    }

    @Override
    public void personBoardsVehicle(String vehicleID, String personID) {

    }

    @Override
    public void personAlightsVehicle(String vehicleID, String personID) throws LinkLogConsistencyException {

    }

    public Table getLinkLogTable() {
        return linkLogTable;
    }

    public Table getVehicleOccupancyTable() {
        return vehicleOccupancyTable;
    }
}
