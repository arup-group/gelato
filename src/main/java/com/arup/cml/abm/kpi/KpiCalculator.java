package com.arup.cml.abm.kpi;

import tech.tablesaw.api.Table;

import java.nio.file.Path;
import java.util.Map;

public interface KpiCalculator {
    Map<String, Double> writeAffordabilityKpi(Path outputDirectory, Normaliser normaliser);

    Map<String, Double> writePtWaitTimeKpi(Path outputDirectory, Normaliser normaliser);

    void writeModalSplitKpi(Path outputDirectory);

    Map<String, Double> writeOccupancyRateKpi(Path outputDirectory, Normaliser normaliser);

    double writeVehicleKMKpi(Path outputDirectory);

    void writePassengerKMKpi(Path outputDirectory);

    void writeSpeedKpi(Path outputDirectory);

    Map<String, Double> writeGHGKpi(Path outputDirectory, Normaliser normaliser);

    Map<String, Double> writeTravelTimeKpi(Path outputDirectory, Normaliser normaliser);

    Map<String, Map<String, Double>> writeAccessToMobilityServicesKpi(Path outputDirectory, Normaliser normaliser);

    Table writeCongestionKpi(Path directory, Normaliser normaliser);

    double writeMobilitySpaceUsageKpi(Path outputDirectory);
}
