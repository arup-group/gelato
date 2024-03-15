package com.arup.cml.abm.kpi;

import tech.tablesaw.api.Table;

import java.nio.file.Path;

public interface KpiCalculator {
    double writeAffordabilityKpi(Path outputDirectory, ScalingFactor scalingFactor);

    double writePtWaitTimeKpi(Path outputDirectory, ScalingFactor scalingFactor);

    void writeModalSplitKpi(Path outputDirectory);

    void writeOccupancyRateKpi(Path outputDirectory);

    double writeVehicleKMKpi(Path outputDirectory);

    void writePassengerKMKpi(Path outputDirectory);

    void writeSpeedKpi(Path outputDirectory);

    double writeGHGKpi(Path outputDirectory);

    double writeTravelTimeKpi(Path outputDirectory);

    Table writeAccessToMobilityServicesKpi(Path outputDirectory);

    Table writeCongestionKpi(Path directory);

    double writeMobilitySpaceUsageKpi(Path outputDirectory);
}
