package com.arup.cml.abm.kpi.domain;

public interface NetworkLinkLog {

    void createLinkLogEntry(String vehicleID, String linkID, double startTime);
    void completeLinkLogEntry(String vehicleID, double endTime);
    void recordVehicleMode(String vehicleId, String mode);
    void personBoardsVehicle(String vehicleID, String personID);
    void personAlightsVehicle(String vehicleID, String personID) throws LinkLogConsistencyException;

    /* TODO method signatures for iterators/other accessors for linklog and occupancy table data */
}
