package com.arup.cml.abm.kpi;

import java.nio.file.Path;

public interface KpiCalculator {
    void linkEntered(String vehicleId, String linkId, double timestamp);
    void linkExited(String vehicleId, String linkId, double timestamp);
    void vehicleEntered(String vehicleId, String personId);
    void vehicleExited(String vehicleId, String personId);
    void recordVehicleMode(String vehicleId, String mode);
    void writeCongestionKpi(Path directory);
}
