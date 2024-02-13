package com.arup.cml.abm.kpi.data;

import com.arup.cml.abm.kpi.data.exceptions.LinkLogPassengerConsistencyException;
import com.google.common.collect.RowSortedTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import java.util.*;

public class LinkLog {
    RowSortedTable<Long, String, Object> linkLogData = TreeBasedTable.create();
    RowSortedTable<Long, String, Object> vehicleOccupantsData = TreeBasedTable.create();

    // records mode of a vehicle
    private final Map<String, String> vehicleModes = new HashMap<>();

    // points to the index of the most recent reference of that vehicle ID in the Link Log
    private final Map<String, Long> vehicleLatestLogIndex = new HashMap<>();

    // tracks the most recent occupants of a vehicle
    private final Map<String, List<String>> vehicleLatestOccupants = new HashMap<>();

    // Link Log entry index
    private long index = 0;
    // Vehicle Occupants index
    private long vehicleOccupancyIndex = 0;

    public Table<Long, String, Object> getLinkLogData() {
        return linkLogData;
    }

    public Table<Long, String, Object> getVehicleOccupantsData() {
        return vehicleOccupantsData;
    }

    public void newLinkLogEntry(String vehicleID, String linkID, double startTime) {
        linkLogData.put(index, "linkID", linkID);
        linkLogData.put(index, "vehicleID", vehicleID);
        linkLogData.put(index, "mode", vehicleModes.getOrDefault(vehicleID, "unknown"));
        linkLogData.put(index, "startTime", startTime);
        vehicleLatestLogIndex.put(vehicleID, index);
        index++;
    }

    public void completeLinkLogEntry(String vehicleID, double endTime) {
        long latestStateIndex = this.vehicleLatestLogIndex.get(vehicleID);
        linkLogData.put(latestStateIndex, "endTime", endTime);
        linkLogData.put(latestStateIndex, "numberOfPeople", vehicleLatestOccupants.getOrDefault(vehicleID, new ArrayList<>()).size());
        newVehicleOccupantsEntry(vehicleID, latestStateIndex);
    }

    public void newVehicleOccupantsEntry(String vehicleID, long idx) {
        List<String> currentOccupants = getLatestVehicleOccupants(vehicleID);
        for (String personID : currentOccupants) {
            vehicleOccupantsData.put(vehicleOccupancyIndex, "linkLogIndex", idx);
            vehicleOccupantsData.put(vehicleOccupancyIndex, "agentId", personID);
            vehicleOccupancyIndex += 1;
        }
    }

    public void recordVehicleMode(String vehicleId, String mode) {
        vehicleModes.put(vehicleId, mode);
    }

    public void personBoardsVehicle(String vehicleID, String personID) {
        if (vehicleLatestOccupants.containsKey(vehicleID)) {
            getLatestVehicleOccupants(vehicleID).add(personID);
        } else {
            vehicleLatestOccupants.put(vehicleID, new ArrayList<>(Arrays.asList(personID)));
        }
    }

    public void personAlightsVehicle(String vehicleID, String personID) throws LinkLogPassengerConsistencyException {
        List<String> latestOccupants = getLatestVehicleOccupants(vehicleID);
        if (latestOccupants.contains(personID)) {
            latestOccupants.remove(personID);
        } else {
            throw new LinkLogPassengerConsistencyException(String.format(
                    "The requested person: `%s` cannot leave vehicle `%s` because they didn't board it",
                    personID,
                    vehicleID));
        }
    }

    private List<String> getLatestVehicleOccupants(String vehicleID) throws LinkLogPassengerConsistencyException {
        if (vehicleLatestOccupants.containsKey(vehicleID)) {
            return vehicleLatestOccupants.get(vehicleID);
        } else
            throw new LinkLogPassengerConsistencyException(String.format(
                    "The requested vehicle: `%s` has not been logged as having any passengers", vehicleID));
    }
}
