package com.arup.cml.kpi.matsim;

import com.arup.cml.kpi.DataModel;
import com.arup.cml.kpi.matsim.handlers.LinkLogHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;
import tech.tablesaw.api.*;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.util.ArrayList;
import java.util.Arrays;


public class MATSimModel implements DataModel {
    private static final Logger log = LogManager.getLogger(MATSimModel.class);

    private final String matsimOutputDir;
    private final Scenario scenario;
    private final EventsManager eventsManager;

    private final String[] necessaryConfigGroups = new String[]{
            GlobalConfigGroup.GROUP_NAME,
            PlansConfigGroup.GROUP_NAME,
            FacilitiesConfigGroup.GROUP_NAME,
            HouseholdsConfigGroup.GROUP_NAME,
            TransitConfigGroup.GROUP_NAME,
            VehiclesConfigGroup.GROUP_NAME,
            NetworkConfigGroup.GROUP_NAME
    };

    private final Table linkLog;
    private final Table vehicleOccupancy;
    private Table vehicles;
    private Table legs;
    private Table trips;
    private Table networkLinks;
    private Table networkLinkModes;
    private Table scheduleStops;
    private Table scheduleRoutes;

    public MATSimModel(String matsimInputConfig, String matsimOutputDir) {
        // there will be other stuff read for a matsim model if the KPIs require
        this.matsimOutputDir = matsimOutputDir;
        Config config = getConfig(matsimInputConfig);
        this.scenario = ScenarioUtils.loadScenario(config);

        readLegs();
        readTrips();
        createVehicleTables();
        createNetworkLinkTables();
        createTransitTables();

        this.eventsManager = EventsUtils.createEventsManager();
        LinkLogHandler linkLogHandler = new LinkLogHandler();
        this.eventsManager.addHandler(linkLogHandler);

        processEvents();

        this.linkLog = linkLogHandler.getLinkLog();
        this.vehicleOccupancy = linkLogHandler.getVehicleOccupancy();

        log.info("Finished processing MATSim outputs");
    }

    private Config getConfig(String matsimInputConfig) {
        Config config = ConfigUtils.createConfig();
        ArrayList<String> configGroups = new ArrayList<>(config.getModules().keySet());
        for (String module : configGroups) {
            if (Arrays.asList(necessaryConfigGroups).contains(module)) {
                System.out.println("Config group " + module + " is read as is");
            } else {
                config.removeModule(module);
                config.addModule(new RelaxedReflectiveConfigGroup(module));
            }
        }
        ConfigUtils.loadConfig(
                config, String.format(matsimInputConfig)
        );
        setOutputFilePaths(config);
        return config;
    }

    private void setOutputFilePaths(Config config) {
        config.getModules().get("network").addParam("inputNetworkFile", String.format("%s/output_network.xml.gz", this.matsimOutputDir));
        config.getModules().get("transit").addParam("transitScheduleFile", String.format("%s/output_transitSchedule.xml.gz", this.matsimOutputDir));
        config.getModules().get("transit").addParam("vehiclesFile", String.format("%s/output_transitVehicles.xml.gz", this.matsimOutputDir));
        config.getModules().get("plans").addParam("inputPlansFile", String.format("%s/output_plans.xml.gz", this.matsimOutputDir));
        config.getModules().get("households").addParam("inputFile", String.format("%s/output_households.xml.gz", this.matsimOutputDir));
        config.getModules().get("facilities").addParam("inputFacilitiesFile", String.format("%s/output_facilities.xml.gz", this.matsimOutputDir));
        config.getModules().get("vehicles").addParam("vehiclesFile", String.format("%s/output_vehicles.xml.gz", this.matsimOutputDir));
    }

    public void processEvents() {
        // hardcoded name for events output file for now
        String outputEventsFile = String.format("%s/output_events.xml.gz", matsimOutputDir);
        new MatsimEventsReader(this.eventsManager).readFile(outputEventsFile);
    }

    private void readLegs() {
        log.info("Reading Legs Table");
        legs = Table.read().usingOptions(
                getMatsimCsvReadOptions(
                        String.format("%s/output_legs.csv.gz", matsimOutputDir)
                )
        );
        legs.setName("Legs");
    }

    private void readTrips() {
        log.info("Reading Trips Table");
        trips = Table.read().usingOptions(
                getMatsimCsvReadOptions(
                        String.format("%s/output_trips.csv.gz", matsimOutputDir)
                )
        );
        trips.setName("Trips");
    }

    private CsvReadOptions getMatsimCsvReadOptions(String path) {
        CsvReadOptions.Builder builder =
                CsvReadOptions.builder(
                                IOUtils.getInputStream(IOUtils.resolveFileOrResource(path)))
                        .separator(';');
        return builder.build();
    }

    private void createVehicleTables() {
        log.info("Creating Vehicle Table");
        StringColumn vehicleIDColumn = StringColumn.create("vehicleID");
        StringColumn modeColumn = StringColumn.create("mode");
        IntColumn capacityColumn = IntColumn.create("capacity");


        Vehicles civilianVehicles = scenario.getVehicles();
        civilianVehicles.getVehicles().forEach((id, vehicle) -> {
            vehicleIDColumn.append(id.toString());
            modeColumn.append(vehicle.getType().getNetworkMode());
            capacityColumn.append(
                    vehicle.getType().getCapacity().getSeats() + vehicle.getType().getCapacity().getStandingRoom()
            );
        });

        // transit vehicles can report network mode as car which is useless, we want the vehicles to have the
        // transit route modes
//        Vehicles transitVehicles = scenario.getTransitVehicles();
//        transitVehicles.getVehicles().forEach((id, vehicle) -> {
//            vehicleIDColumn.append(id.toString());
//            modeColumn.append(vehicle.getType().getNetworkMode());
//        });

        vehicles = Table.create("Vehicles")
            .addColumns(
                    vehicleIDColumn,
                    modeColumn,
                    capacityColumn
            );
    }

    private void createNetworkLinkTables() {
        log.info("Creating Network Link Tables");
        Network network = scenario.getNetwork();

        // Network Links Table Columns
        StringColumn linkIDColumn = StringColumn.create("linkID");
        StringColumn fromNodeColumn = StringColumn.create("fromNode");
        StringColumn toNodeColumn = StringColumn.create("toNode");
        DoubleColumn freespeedColumn = DoubleColumn.create("freespeed");
        DoubleColumn capacityColumn = DoubleColumn.create("capacity");
        DoubleColumn lengthColumn = DoubleColumn.create("length");
        DoubleColumn lanesColumn = DoubleColumn.create("lanes");

        // Columns for the Modes Table
        // This Table supports the main Network Links Table
        ArrayList<String> modesLinkIDColumn = new ArrayList<>();
        ArrayList<String> modesColumn = new ArrayList<>();

        network.getLinks().forEach((id, link) -> {
            String linkId = id.toString();

            // Network Links Table data
            linkIDColumn.append(linkId);
            fromNodeColumn.append(link.getFromNode().getId().toString());
            toNodeColumn.append(link.getToNode().getId().toString());
            freespeedColumn.append(link.getFreespeed());
            capacityColumn.append(link.getCapacity());
            lengthColumn.append(link.getLength());
            lanesColumn.append(link.getNumberOfLanes());

            // Modes Table data
            for (String mode : link.getAllowedModes()) {
                modesLinkIDColumn.add(linkId);
                modesColumn.add(mode);
            }
        });

        networkLinks = Table.create("Network Links")
                .addColumns(
                        linkIDColumn,
                        fromNodeColumn,
                        toNodeColumn,
                        freespeedColumn,
                        capacityColumn,
                        lengthColumn,
                        lanesColumn
                );

        networkLinkModes = Table.create("Network Link Modes")
                .addColumns(
                        StringColumn.create("linkID", modesLinkIDColumn),
                        StringColumn.create("mode", modesColumn)
                );
    }

    private void createTransitTables() {
        log.info("Creating Transit Tables");
        TransitSchedule schedule = scenario.getTransitSchedule();

        log.info("Creating Schedule Stop Table");
        // Schedule Stop Table Columns
        StringColumn stopIDColumn = StringColumn.create("stopID");
        DoubleColumn xColumn = DoubleColumn.create("x");
        DoubleColumn yColumn = DoubleColumn.create("y");
        StringColumn nameColumn = StringColumn.create("name");
        StringColumn linkIdColumn = StringColumn.create("linkID");
        BooleanColumn isBlockingColumn = BooleanColumn.create("isBlocking");

        schedule.getFacilities().forEach((id, stop) -> {
            String stopId = id.toString();

            // Schedule Stop Table data
            stopIDColumn.append(stopId);
            xColumn.append(stop.getCoord().getX());
            yColumn.append(stop.getCoord().getY());
            nameColumn.append(stop.getName());
            linkIdColumn.append(stop.getLinkId().toString());
            isBlockingColumn.append(stop.getIsBlockingLane());
        });

        scheduleStops = Table.create("Schedule Stops")
                .addColumns(
                        stopIDColumn,
                        xColumn,
                        yColumn,
                        nameColumn,
                        linkIdColumn,
                        isBlockingColumn
                );

        StringColumn lineIDColumn = StringColumn.create("transitLineID");
        StringColumn routeIDColumn = StringColumn.create("routeID");
        StringColumn modeColumn = StringColumn.create("mode");

        StringColumn vehicleIDColumn = vehicles.stringColumn("vehicleID");
        StringColumn vehicleModeColumn = vehicles.stringColumn("mode");
        IntColumn capacityColumn = vehicles.intColumn("capacity");
        Vehicles transitVehicles = scenario.getTransitVehicles();
        log.info("Creating Schedule Transit Tables");
        schedule.getTransitLines().forEach((lineId, transitLine) -> {
            transitLine.getRoutes().forEach((routeId, route) -> {
                lineIDColumn.append(lineId.toString());
                routeIDColumn.append(routeId.toString());
                modeColumn.append(route.getTransportMode());

                route.getDepartures().forEach((departureId, departure) -> {
                    vehicleModeColumn.append(route.getTransportMode());
                    Vehicle vehicle = transitVehicles.getVehicles().get(departure.getVehicleId());
                    vehicleIDColumn.append(vehicle.getId().toString());
                    capacityColumn.append(
                            vehicle.getType().getCapacity().getSeats() + vehicle.getType().getCapacity().getStandingRoom()
                    );
                });
            });
        });

        scheduleRoutes = Table.create("Schedule Routes")
                .addColumns(
                        lineIDColumn,
                        routeIDColumn,
                        modeColumn
                );
    }

    @Override
    public Table getLinkLog() {
        return linkLog;
    }

    @Override
    public Table getVehicleOccupancy() {
        return vehicleOccupancy;
    }

    @Override
    public Table getLegs() {
        return legs;
    }

    @Override
    public Table getTrips() {
        return trips;
    }

    @Override
    public Table getVehicles() {
        return vehicles;
    }

    @Override
    public Table getNetworkLinks() {
        return networkLinks;
    }

    @Override
    public Table getNetworkLinkModes() {
        return networkLinkModes;
    }

    @Override
    public Table getScheduleStops() {
        return scheduleStops;
    }

    @Override
    public Table getScheduleRoutes() {
        return scheduleRoutes;
    }

    @Override
    public void write(String outputDir) {
        linkLog.write().csv(String.format("%s/linkLog.csv", outputDir));
        vehicleOccupancy.write().csv(String.format("%s/vehicleOccupancy.csv", outputDir));
        networkLinks.write().csv(String.format("%s/networkLinks.csv", outputDir));
        networkLinkModes.write().csv(String.format("%s/networkLinkModes.csv", outputDir));
        scheduleStops.write().csv(String.format("%s/scheduleStops.csv", outputDir));
    }
}