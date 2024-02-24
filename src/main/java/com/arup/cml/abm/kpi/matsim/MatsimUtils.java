package com.arup.cml.abm.kpi.matsim;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.groups.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

public class MatsimUtils {
    private static final Logger LOGGER = LogManager.getLogger(MatsimUtils.class);
    private Path matsimOutputDir;
    private Config matsimConfig;
    private Scenario matsimScenario;

    private Network matsimNetwork;
    private TransitSchedule matsimTransitSchedule;
    private Vehicles matsimVehicles;
    private String runId;
    private String compressionFileEnd;

    private final Set<String> necessaryConfigGroups = new HashSet<>(Arrays.asList(
            GlobalConfigGroup.GROUP_NAME,
            PlansConfigGroup.GROUP_NAME,
                    FacilitiesConfigGroup.GROUP_NAME,
            HouseholdsConfigGroup.GROUP_NAME,
                    TransitConfigGroup.GROUP_NAME,
            VehiclesConfigGroup.GROUP_NAME,
                    NetworkConfigGroup.GROUP_NAME,
            ScoringConfigGroup.GROUP_NAME,
                    ScenarioConfigGroup.GROUP_NAME,
            ControllerConfigGroup.GROUP_NAME));

    public MatsimUtils(Path matsimOutputDir, Path matsimConfigFile) {
        this(matsimOutputDir, matsimConfigFile, true);
    }

    public MatsimUtils(Path matsimOutputDir, Path matsimConfigFile, boolean skipPlansFile) {
        this.matsimOutputDir = matsimOutputDir;
        this.matsimConfig = getConfig(matsimConfigFile.toString(), skipPlansFile);
        this.matsimScenario = ScenarioUtils.loadScenario(matsimConfig);
        this.matsimNetwork = matsimScenario.getNetwork();
        this.matsimTransitSchedule = matsimScenario.getTransitSchedule();
        this.matsimVehicles = collectVehicles(matsimScenario);
    }

    /** Disable certain memory heavy inputs e.g. plansFile */
    private void skipPlansFile(Config matsimConfig) {
        matsimConfig.plans().setInputFile(null);
    }

    private Config getConfig(String matsimInputConfig, boolean skipPlansFile) {
        Config config = ConfigUtils.createConfig();
        TreeMap<String, ConfigGroup> configuredModules = config.getModules();
        for (ConfigGroup module : configuredModules.values().stream().toList()) {
            if (necessaryConfigGroups.contains(module.getName())) {
                LOGGER.info("Config group {} is read as is", module);
            } else {
                ReflectiveConfigGroup relaxedModule =
                        new ReflectiveConfigGroup(module.getName(), true) {
                        };
                config.removeModule(module.getName());
                config.addModule(relaxedModule);
            }
        }
        ConfigUtils.loadConfig(config, String.format(matsimInputConfig));
        this.runId = getRunId(config.controller().getRunId());
        this.compressionFileEnd = config.controller().getCompressionType().fileEnding;
        setOutputFilePaths(config);

        if (skipPlansFile) {
            skipPlansFile(config);
        }

        return config;
    }

    private static String getRunId(String runid) {
        return runid == null ? "" : runid + ".";
    }

    private void setOutputFilePaths(Config config) {
        TreeMap<String, ConfigGroup> modules = config.getModules();
        modules.get("network")
                .addParam("inputNetworkFile",
                String.format("%s/%soutput_network.xml%s", this.matsimOutputDir, this.runId, this.compressionFileEnd));
        modules.get("transit")
                .addParam("transitScheduleFile",
                String.format("%s/%soutput_transitSchedule.xml%s", this.matsimOutputDir, this.runId, this.compressionFileEnd));
        modules.get("transit")
                .addParam("vehiclesFile",
                String.format("%s/%soutput_transitVehicles.xml%s", this.matsimOutputDir, this.runId, this.compressionFileEnd));
        modules.get("plans")
                .addParam("inputPlansFile",
                        String.format("%s/%soutput_plans.xml%s", this.matsimOutputDir, this.runId, this.compressionFileEnd));
        modules.get("households")
                .addParam("inputFile",
                String.format("%s/%soutput_households.xml%s", this.matsimOutputDir, this.runId, this.compressionFileEnd));
        modules.get("facilities")
                .addParam("inputFacilitiesFile",
                String.format("%s/%soutput_facilities.xml%s", this.matsimOutputDir, this.runId, this.compressionFileEnd));
        modules.get("vehicles")
                .addParam("vehiclesFile",
                        String.format("%s/%soutput_vehicles.xml%s", this.matsimOutputDir, this.runId, this.compressionFileEnd));
    }

    public Network getMatsimNetwork() {
        return matsimNetwork;
    }

    public TransitSchedule getTransitSchedule() {
        return matsimTransitSchedule;
    }

    public Vehicles getMatsimVehicles() {
        return matsimVehicles;
    }

    public InputStream getMatsimLegsCSVInputStream() {
        return IOUtils.getInputStream(
                IOUtils.resolveFileOrResource(
                        String.format("%s/%soutput_legs.csv%s", this.matsimOutputDir, runId, compressionFileEnd)));
    }

    public InputStream getMatsimTripsCSVInputStream() {
        return IOUtils.getInputStream(
                IOUtils.resolveFileOrResource(
                        String.format("%s/%soutput_trips.csv%s", this.matsimOutputDir, runId, compressionFileEnd)));
    }

    public String getRunId() {
        return this.runId;
    }

    public String getCompressionFileEnd() {
        return this.compressionFileEnd;
    }

    private Vehicles collectVehicles(Scenario scenario) {
        // get civilian vehicles
        Vehicles vehicles = scenario.getVehicles();

        // transit vehicles can report network mode as car which is useless, we want the vehicles to have the
        // transit route modes
        Vehicles transitVehicles = scenario.getTransitVehicles();
        TransitSchedule schedule = scenario.getTransitSchedule();
        schedule.getTransitLines().forEach((lineId, transitLine) -> {
            transitLine.getRoutes().forEach((routeId, route) -> {
                route.getDepartures().forEach((departureId, departure) -> {
                    if (vehicles.getVehicles().containsKey(departure.getVehicleId())) {
                        // update existing
                        Vehicle existingVehicle = vehicles.getVehicles().get(departure.getVehicleId());
                        existingVehicle.getType().setNetworkMode(route.getTransportMode());
                        existingVehicle.getAttributes().putAttribute("PTLineID", lineId);
                        existingVehicle.getAttributes().putAttribute("PTRouteID", routeId);
                    } else {
                        Vehicle transitVehicle = transitVehicles.getVehicles().get(departure.getVehicleId());
                        transitVehicle.getType().setNetworkMode(route.getTransportMode());
                        transitVehicle.getAttributes().putAttribute("PTLineID", lineId);
                        transitVehicle.getAttributes().putAttribute("PTRouteID", routeId);
                        if (!vehicles.getVehicleTypes().containsKey(transitVehicle.getType().getId())) {
                            vehicles.addVehicleType(transitVehicle.getType());
                        }
                        vehicles.addVehicle(transitVehicle);
                    }
                });
            });
        });

        // reading DRT vehicles if relevant matsim output found
        String drtVehiclesPath = String.format("%s/drt_vehicles.xml.gz", this.matsimOutputDir);
        File drtFile = new File(drtVehiclesPath);
        if (drtFile.exists()) {
            LOGGER.info("DRT Vehicles File was found and will be used to label DRT vehicles");
            FleetSpecification fleetSpecification = new FleetSpecificationImpl();
            new FleetReader(fleetSpecification).readFile(drtVehiclesPath);

            for (Map.Entry<Id<DvrpVehicle>,
                    DvrpVehicleSpecification> entry : fleetSpecification.getVehicleSpecifications().entrySet()) {
                Id<DvrpVehicle> drtVehicleId = entry.getKey();
                DvrpVehicleSpecification vehicleSpec = entry.getValue();
                int capacity = vehicleSpec.getCapacity();
                Id<VehicleType> vehicleTypeID = Id.create(String.format("drt-%d", capacity), VehicleType.class);
                if (vehicles.getVehicleTypes().containsKey(vehicleTypeID)) {
                    // add DRT vehicle for existing vehicle type
                    VehicleType drtVehicleType = vehicles.getVehicleTypes().get(vehicleTypeID);
                    Vehicle drtVehicle = VehicleUtils.createVehicle(Id.createVehicleId(drtVehicleId), drtVehicleType);
                    vehicles.addVehicle(drtVehicle);
                } else {
                    // create the DRT vehicle for that capacity
                    VehicleType drtVehicleType = VehicleUtils.createVehicleType(vehicleTypeID);
                    drtVehicleType.setNetworkMode("drt");
                    drtVehicleType.getCapacity().setSeats(capacity);
                    vehicles.addVehicleType(drtVehicleType);
                    // add DRT vehicle
                    Vehicle drtVehicle = VehicleUtils.createVehicle(Id.createVehicleId(drtVehicleId), drtVehicleType);
                    vehicles.addVehicle(drtVehicle);
                }
            }
        }

        return vehicles;
    }
}
