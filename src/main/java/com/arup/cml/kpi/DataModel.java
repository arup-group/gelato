package com.arup.cml.kpi;

import tech.tablesaw.api.Table;

public interface DataModel {

    Table getLinkLog();

    Table getVehicleOccupancy();

    Table getLegs();

    Table getNetworkLinks();

    Table getNetworkLinkModes();

    Table getScheduleStops();

    void write(String outputDir);
}
