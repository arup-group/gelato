package com.arup.cml.abm.kpi.matsim;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
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
import org.matsim.vehicles.Vehicles;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

public class MatsimUtils {
    private static final Logger LOGGER = LogManager.getLogger(MatsimUtils.class);
    private Path matsimOutputDir;
    private Config matsimConfig;
    private Scenario matsimScenario;

    private Network matsimNetwork;
    private TransitSchedule matsimTransitSchedule;
    private Vehicles matsimVehicles;

    private final Set<String> necessaryConfigGroups = new HashSet<>(Arrays.asList(
            GlobalConfigGroup.GROUP_NAME,
            PlansConfigGroup.GROUP_NAME,
            FacilitiesConfigGroup.GROUP_NAME,
            HouseholdsConfigGroup.GROUP_NAME,
            TransitConfigGroup.GROUP_NAME,
            VehiclesConfigGroup.GROUP_NAME,
            NetworkConfigGroup.GROUP_NAME,
            ScoringConfigGroup.GROUP_NAME,
            ScenarioConfigGroup.GROUP_NAME
    ));

    public MatsimUtils(Path matsimOutputDir, Path matsimConfigFile) {
        this.matsimOutputDir = matsimOutputDir;
        this.matsimConfig = getConfig(matsimConfigFile.toString());
        this.matsimScenario = ScenarioUtils.loadScenario(matsimConfig);
        this.matsimNetwork = matsimScenario.getNetwork();
        this.matsimTransitSchedule = matsimScenario.getTransitSchedule();
        this.matsimVehicles = collectVehicles(matsimScenario);
    }

    private Config getConfig(String matsimInputConfig) {
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
        setOutputFilePaths(config);
        return config;
    }

    private void setOutputFilePaths(Config config) {
        TreeMap<String, ConfigGroup> modules = config.getModules();
        modules.get("network")
                .addParam("inputNetworkFile",
                        String.format("%s/output_network.xml.gz", this.matsimOutputDir));
        modules.get("transit")
                .addParam("transitScheduleFile",
                        String.format("%s/output_transitSchedule.xml.gz", this.matsimOutputDir));
        modules.get("transit")
                .addParam("vehiclesFile",
                        String.format("%s/output_transitVehicles.xml.gz", this.matsimOutputDir));
        modules.get("plans")
                .addParam("inputPlansFile",
                        String.format("%s/output_plans.xml.gz", this.matsimOutputDir));
        modules.get("households")
                .addParam("inputFile",
                        String.format("%s/output_households.xml.gz", this.matsimOutputDir));
        modules.get("facilities")
                .addParam("inputFacilitiesFile",
                        String.format("%s/output_facilities.xml.gz", this.matsimOutputDir));
        modules.get("vehicles")
                .addParam("vehiclesFile",
                        String.format("%s/output_vehicles.xml.gz", this.matsimOutputDir));
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
                        String.format("%s/output_legs.csv.gz", matsimOutputDir)
                )
        );
    }

    public InputStream getMatsimTripsCSVInputStream() {
        return IOUtils.getInputStream(
                IOUtils.resolveFileOrResource(
                        String.format("%s/output_trips.csv.gz", matsimOutputDir)
                )
        );
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
                        vehicles.addVehicle(transitVehicle);
                    }
                });
            });
        });

        // TODO: add reading DRT vehicles if relevant matsim output found

        return vehicles;
    }
}
