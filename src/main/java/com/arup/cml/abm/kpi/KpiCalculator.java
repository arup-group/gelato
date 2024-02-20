package com.arup.cml.abm.kpi;

import tech.tablesaw.api.Table;

import java.nio.file.Path;

public interface KpiCalculator {
    void writeAffordabilityKpi(Path outputDirectory);

    void writePtWaitTimeKpi(Path outputDirectory);

    void writeModalSplitKpi(Path outputDirectory);

    void writeOccupancyRateKpi(Path outputDirectory);

    void writeVehicleKMKpi(Path outputDirectory);

    void writeSpeedKpi(Path outputDirectory);

    void writeGHGKpi(Path outputDirectory);

    double writeTravelTime(Path outputDirectory);

    Table writeAccessToMobilityServices(Path outputDirectory);

    Table writeCongestionKpi(Path directory);

    Table writeMobilitySpaceUsage(Path outputDirectory);
}
