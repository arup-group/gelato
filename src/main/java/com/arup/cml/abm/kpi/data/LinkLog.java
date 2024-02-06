package com.arup.cml.abm.kpi.data;

import com.google.common.collect.RowSortedTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LinkLog {
    RowSortedTable<Long, String, Object> linkLogData = TreeBasedTable.create();
    RowSortedTable<Long, String, Object> vehicleOccupantsData = TreeBasedTable.create();

    // records mode of a vehicle
    private final Map<String, String> vehicleModes = new HashMap<>();

    // points to the index of the most recent reference of that vehicle ID in the Link Log
    private final Map<String, Long> vehicleLatestLogIndex = new HashMap<>();

    // tracks the most recent occupants of a vehicle
    private final Map<String, ArrayList<String>> vehicleLatestOccupants = new HashMap<>();

    // Link Log entry index
    private long index = 0;

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

    public void updateLinkLogEntry(String vehicleID, double endTime) {
        long latestStateIndex = this.vehicleLatestLogIndex.get(vehicleID);
        linkLogData.put(latestStateIndex, "endTime", endTime);
        linkLogData.put(latestStateIndex, "numberOfPeople", vehicleLatestOccupants.get(vehicleID).size());
        newVehicleOccupantsEntry(vehicleID, latestStateIndex);
    }

    public void newVehicleOccupantsEntry(String vehicleID, long idx) {
        ArrayList<String> currentOccupants = vehicleLatestOccupants.get(vehicleID);
        for (String personID : currentOccupants) {
            vehicleOccupantsData.put(idx, "agentId", personID);
        }
    }

    public void recordVehicleMode(String vehicleId, String mode) {
        vehicleModes.put(vehicleId, mode);
    }

    public void personBoardsVehicle(String vehicleID, String personID) {
        if (vehicleLatestOccupants.containsKey(vehicleID)) {
            ArrayList<String> latestOccupants = vehicleLatestOccupants.get(vehicleID);
            latestOccupants.add(personID);
        } else {
            ArrayList<String> latestOccupants = new ArrayList<>();
            latestOccupants.add(personID);
            vehicleLatestOccupants.put(vehicleID, latestOccupants);
        }
    }

    public void personAlightsVehicle(String vehicleID, String personID) {
        ArrayList<String> latestOccupants = vehicleLatestOccupants.get(vehicleID);
        latestOccupants.remove(personID);
    }
}
