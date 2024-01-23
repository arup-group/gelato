package com.arup.cml.abm.kpi;

import tech.tablesaw.api.Table;

public interface DataModel {

    Table getLinkLog();

    Table getVehicleOccupancy();

    Table getNetworkLinks();

    Table getNetworkLinkModes();
    void write(String outputDir);
}
