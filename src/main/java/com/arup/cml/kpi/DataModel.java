package com.arup.cml.kpi;

import tech.tablesaw.api.Table;

public interface DataModel {

    Table getLinkLog();

    Table getVehicleOccupancy();

    Table getLegs();

    Table getTrips();

    Table getVehicles();

    Table getNetworkLinks();

    Table getNetworkLinkModes();

    Table getScheduleStops();

    Table getScheduleRoutes();

    void write(String outputDir);
}
