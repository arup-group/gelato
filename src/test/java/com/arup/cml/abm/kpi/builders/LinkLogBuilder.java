package com.arup.cml.abm.kpi.builders;

import com.arup.cml.abm.kpi.data.LinkLog;
public class LinkLogBuilder {
    LinkLog linkLog = new LinkLog();

    public LinkLogBuilder withEntry(String vehicleID, String linkID, double startTime, double endTime) {
        linkLog.recordVehicleMode(vehicleID, "car");
        linkLog.createLinkLogEntry(vehicleID, linkID, startTime);
        linkLog.completeLinkLogEntry(vehicleID, endTime);
        return this;
    }

    public LinkLogBuilder withOccupant(String vehicleID, String personID) {
        linkLog.personBoardsVehicle(vehicleID, personID);
        return this;
    }

    public LinkLog build() {
        return this.linkLog;
    }
}
