package com.arup.cml.abm.kpi.matsim;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.fleet.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.groups.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesFromPopulation;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.*;

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
    private ActivityFacilities facilities;
    private ScoringConfigGroup scoring;
    private Population population;
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
        this.matsimOutputDir = matsimOutputDir;
        this.matsimConfig = buildConfig(matsimConfigFile.toString());
        this.matsimScenario = ScenarioUtils.loadScenario(matsimConfig);
        this.matsimNetwork = matsimScenario.getNetwork();
        this.matsimTransitSchedule = matsimScenario.getTransitSchedule();
        this.matsimVehicles = collectVehicles(matsimScenario);
        this.population = matsimScenario.getPopulation();
        this.facilities = getOrGenerateFacilities(matsimScenario, matsimConfig, population);
        this.scoring = matsimConfig.scoring();
    }

    private ActivityFacilities getOrGenerateFacilities(Scenario scenario, Config config, Population pop) {
        // TODO atm facilities cannot be generated because the population object is empty
        if (scenario.getActivityFacilities().getFacilities().isEmpty()) {
            config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.onePerActivityLinkInPlansFile);
            FacilitiesFromPopulation facilitiesFromPopulation = new FacilitiesFromPopulation(scenario);
            facilitiesFromPopulation.run(pop);
        }
        return scenario.getActivityFacilities();
    }

    private Config buildConfig(String matsimInputConfig) {
        Config config = ConfigUtils.createConfig();
        TreeMap<String, ConfigGroup> configuredModules = config.getModules();
        for (ConfigGroup module : configuredModules.values().stream().toList()) {
            if (necessaryConfigGroups.contains(module.getName())) {
                LOGGER.info("Config group {} is read as is", module);
            } else {
                ReflectiveConfigGroup relaxedModule =
                        new ReflectiveConfigGroup(module.getName(), true) {};
                config.removeModule(module.getName());
                config.addModule(relaxedModule);
            }
        }
        ConfigUtils.loadConfig(config, String.format(matsimInputConfig));
        this.runId = getRunId(config.controller().getRunId());
        this.compressionFileEnd = config.controller().getCompressionType().fileEnding;
        // don't load the (often large) plans - we don't use them
        config.plans().setInputFile(null);
        setOutputFilePaths(config);
        return config;
    }

    private static String getRunId(String runid) {
        return runid == null ? "" : runid + ".";
    }

    private void setOutputFilePaths(Config config) {
        TreeMap<String, ConfigGroup> modules = config.getModules();
        modules.get("network")
                .addParam("inputNetworkFile",
                        String.format("%s/%soutput_network.xml%s", matsimOutputDir, runId, compressionFileEnd));
        modules.get("transit")
                .addParam("transitScheduleFile",
                        String.format("%s/%soutput_transitSchedule.xml%s", matsimOutputDir, runId, compressionFileEnd));
        modules.get("transit")
                .addParam("vehiclesFile",
                        String.format("%s/%soutput_transitVehicles.xml%s", matsimOutputDir, runId, compressionFileEnd));
        modules.get("households")
                .addParam("inputFile",
                        String.format("%s/%soutput_households.xml%s", matsimOutputDir, runId, compressionFileEnd));
        modules.get("facilities")
                .addParam("inputFacilitiesFile",
                        String.format("%s/%soutput_facilities.xml%s", matsimOutputDir, runId, compressionFileEnd));
        modules.get("vehicles")
                .addParam("vehiclesFile",
                        String.format("%s/%soutput_vehicles.xml%s", matsimOutputDir, runId, compressionFileEnd));
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

    public ScoringConfigGroup getScoring() {
        return scoring;
    }

    public ActivityFacilities getFacilities() {
        return facilities;
    }

    public Population getPopulation() {
        return population;
    }

    public InputStream getMatsimPersonsCSVInputStream() {
        return IOUtils.getInputStream(
                IOUtils.resolveFileOrResource(
                        String.format("%s/%soutput_persons.csv%s", matsimOutputDir, runId, compressionFileEnd)));
    }

    public InputStream getMatsimLegsCSVInputStream() {
        return IOUtils.getInputStream(
                IOUtils.resolveFileOrResource(
                        String.format("%s/%soutput_legs.csv%s", matsimOutputDir, runId, compressionFileEnd)));
    }

    public InputStream getMatsimTripsCSVInputStream() {
        return IOUtils.getInputStream(
                IOUtils.resolveFileOrResource(
                        String.format("%s/%soutput_trips.csv%s", matsimOutputDir, runId, compressionFileEnd)));
    }

    public String getRunId() {
        return runId;
    }

    public String getCompressionFileEnd() {
        return compressionFileEnd;
    }

    public Config getMatsimConfig() {
        return matsimConfig;
    }

    public Scenario getMatsimScenario() {
        return matsimScenario;
    }

    private void setDefaultsForEngineInformationIfNotAvailable(
            VehicleType vehicleType, String defaultFuelType, Double defaultEmissionsFactor) {
        Object fuelType = vehicleType.getEngineInformation().getAttributes().getAttribute("fuelType");
        if (fuelType == null) {
            vehicleType.getEngineInformation().getAttributes().putAttribute("fuelType", defaultFuelType);
        }
        Object emissionsFactor = vehicleType.getEngineInformation().getAttributes().getAttribute("emissionsFactor");
        if (emissionsFactor == null) {
            vehicleType.getEngineInformation().getAttributes().putAttribute("emissionsFactor", defaultEmissionsFactor);
        }
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
                        existingVehicle.getAttributes().putAttribute("PTLineID", lineId.toString());
                        existingVehicle.getAttributes().putAttribute("PTRouteID", routeId.toString());
                    } else {
                        Vehicle transitVehicle = transitVehicles.getVehicles().get(departure.getVehicleId());
                        transitVehicle.getType().setNetworkMode(route.getTransportMode());
                        transitVehicle.getAttributes().putAttribute("PTLineID", lineId.toString());
                        transitVehicle.getAttributes().putAttribute("PTRouteID", routeId.toString());
                        if (!vehicles.getVehicleTypes().containsKey(transitVehicle.getType().getId())) {
                            vehicles.addVehicleType(transitVehicle.getType());
                        }
                        vehicles.addVehicle(transitVehicle);
                    }
                });
            });
        });

        // reading DRT vehicles if relevant matsim output found
        String drtVehiclesPath = String.format("%s/drt_vehicles.xml.gz", matsimOutputDir);
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

        // check fuel types and emissions, set defaults if missing
        vehicles.getVehicleTypes().forEach((vehicleTypeId, vehicleType) -> {
            switch (vehicleType.getNetworkMode()) {
                case "car" -> setDefaultsForEngineInformationIfNotAvailable(vehicleType, "petrol", 0.222);
                case "bus" -> setDefaultsForEngineInformationIfNotAvailable(vehicleType, "cng", 1.372);
            }
        });
        return vehicles;
    }
}
