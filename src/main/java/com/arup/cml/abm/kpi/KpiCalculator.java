package com.arup.cml.abm.kpi;

import java.nio.file.Path;

public interface KpiCalculator {

    public void linkEntered(String vehicleId, String linkId, double timestamp);
    public void linkExited(String vehicleId, String linkId, double timestamp);
    public void vehicleEntered(String vehicleId, String personId);
    public void vehicleExited(String vehicleId, String personId);
    public void recordVehicleMode(String vehicleId, String mode);
    public void writeCongestionKpi(Path directory);
}
