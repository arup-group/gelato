package com.arup.cml.kpi;

import tech.tablesaw.api.Table;

public interface DataModel {

    Table getLinkLog();

    Table getVehicleOccupancy();

    Table getNetworkLinks();

    Table getNetworkLinkModes();
    void write(String outputDir);
}
