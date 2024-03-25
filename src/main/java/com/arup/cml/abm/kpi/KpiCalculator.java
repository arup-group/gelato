package com.arup.cml.abm.kpi;

import tech.tablesaw.api.Table;

import java.nio.file.Path;
import java.util.Map;

public interface KpiCalculator {
    double writeAffordabilityKpi(Path outputDirectory, Normaliser normaliser);

    double writePtWaitTimeKpi(Path outputDirectory, Normaliser normaliser);

    void writeModalSplitKpi(Path outputDirectory);

    double writeOccupancyRateKpi(Path outputDirectory, Normaliser normaliser);

    double writeVehicleKMKpi(Path outputDirectory);

    void writePassengerKMKpi(Path outputDirectory);

    void writeSpeedKpi(Path outputDirectory);

    double writeGHGKpi(Path outputDirectory, Normaliser normaliser);

    double writeTravelTimeKpi(Path outputDirectory, Normaliser normaliser);

    Map<String, Double> writeAccessToMobilityServicesKpi(Path outputDirectory, Normaliser normaliser);

    Table writeCongestionKpi(Path directory, Normaliser normaliser);

    double writeMobilitySpaceUsageKpi(Path outputDirectory);
}
