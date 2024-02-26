package com.arup.cml.abm.kpi.tablesaw;

import com.arup.cml.abm.kpi.domain.LinkLogConsistencyException;
import com.arup.cml.abm.kpi.domain.NetworkLinkLog;
import tech.tablesaw.api.Table;

public class TablesawNetworkLinkLog implements NetworkLinkLog {

    @Override
    public void createLinkLogEntry(String vehicleID, String linkID, double startTime) {

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
        return null;
    }

    public Table getVehicleOccupancyTable() {
        return null;
    }
}
