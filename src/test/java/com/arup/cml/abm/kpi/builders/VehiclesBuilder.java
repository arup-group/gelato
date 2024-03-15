package com.arup.cml.abm.kpi.builders;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class VehiclesBuilder {
    Vehicles vehicles = VehicleUtils.createVehiclesContainer();

    public VehiclesBuilder withVehicle(String vehicleId, String vehicleType) {
        return withVehicleOfMode(vehicleId, vehicleType, "car");
    }

    public VehiclesBuilder withVehicleOfMode(String vehicleId, String vehicleType, String mode) {
        return this.withVehicleOfCapacity(vehicleId, vehicleType, mode, 2);
    }

    public VehiclesBuilder withVehicleOfCapacity(String vehicleId, String vehicleTypeID, String mode, Integer seats) {
        Id<VehicleType> vehTypeId = Id.create(vehicleTypeID, VehicleType.class);
        VehicleType matsimVehicleType = vehicles.getVehicleTypes().getOrDefault(
                vehTypeId, VehicleUtils.createVehicleType(vehTypeId));
        matsimVehicleType.setNetworkMode(mode);
        matsimVehicleType.getCapacity().setSeats(seats);
        vehicles.addVehicleType(matsimVehicleType);
        vehicles.addVehicle(VehicleUtils.createVehicle(Id.createVehicleId(vehicleId), matsimVehicleType));
        return this;
    }

    public VehiclesBuilder withVehicleWithEmissionsFactor(String vehicleId, String vehicleTypeId, double emissionsFactor) {
        Id<VehicleType> vehTypeId = Id.create(vehicleTypeId, VehicleType.class);
        VehicleType matsimVehicleType = vehicles.getVehicleTypes().getOrDefault(
                vehTypeId, VehicleUtils.createVehicleType(vehTypeId));
        matsimVehicleType.getEngineInformation().getAttributes().putAttribute("emissionsFactor", emissionsFactor);
        vehicles.addVehicleType(matsimVehicleType);
        vehicles.addVehicle(VehicleUtils.createVehicle(Id.createVehicleId(vehicleId), matsimVehicleType));
        return this;
    }

    public Vehicles build() {
        return this.vehicles;
    }
}
