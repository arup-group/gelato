package com.arup.cml.abm.kpi.builders;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class VehiclesBuilder {
    Vehicles vehicles = VehicleUtils.createVehiclesContainer();

    public VehiclesBuilder withVehicle(String id, String vehicleType) {
        return withVehicle(id, vehicleType, "car");
    }

    public VehiclesBuilder withVehicle(String id, String vehicleType, String mode) {
        withVehicleType(vehicleType, mode);
        VehicleType matsimVehicleType = vehicles.getVehicleTypes().get(Id.create(vehicleType, VehicleType.class));
        Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId(id), matsimVehicleType);
        vehicles.addVehicle(vehicle);
        return this;
    }

    public VehiclesBuilder withVehicleType(String vehicleType) {
        return withVehicleType(vehicleType, "car");
    }

    public VehiclesBuilder withVehicleType(String vehicleType, String mode) {
        Id<VehicleType> vehicleTypeId = Id.create(vehicleType, VehicleType.class);
        if (!vehicles.getVehicleTypes().containsKey(vehicleTypeId)) {
            VehicleType matsimVehicleType = VehicleUtils.createVehicleType(vehicleTypeId);
            matsimVehicleType.setNetworkMode(mode);
            vehicles.addVehicleType(matsimVehicleType);
        }
        return this;
    }

    public Vehicles build() {
        return this.vehicles;
    }
}
