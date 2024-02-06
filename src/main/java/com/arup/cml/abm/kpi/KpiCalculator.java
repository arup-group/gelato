package com.arup.cml.abm.kpi;

import java.nio.file.Path;

public interface KpiCalculator {
    void writeAffordabilityKpi(Path outputDirectory);

    void writePtWaitTimeKpi(Path outputDirectory);

    void writeModalSplitKpi(Path outputDirectory);

    void writeOccupancyRateKpi(Path outputDirectory);

    void writeVehicleKMKpi(Path outputDirectory);

    void writeSpeedKpi(Path outputDirectory);

    void writeGHGKpi(Path outputDirectory);

    void writeCongestionKpi(Path directory);
}
