package com.arup.cml.abm.kpi.matsim;

import com.arup.cml.abm.kpi.matsim.handlers.LinkLogHandler;
import com.arup.cml.abm.kpi.DataModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.groups.*;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.config.TransitConfigGroup;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.*;


public class MATSimModel implements DataModel {
    private static final Logger log = LogManager.getLogger(MATSimModel.class);

    private final String matsimOutputDir;
    private final Scenario scenario;
    private final EventsManager eventsManager;
    private final Set<String> necessaryConfigGroups = new HashSet<>(Arrays.asList(
        GlobalConfigGroup.GROUP_NAME,
        PlansConfigGroup.GROUP_NAME,
        FacilitiesConfigGroup.GROUP_NAME,
        HouseholdsConfigGroup.GROUP_NAME,
        TransitConfigGroup.GROUP_NAME,
        VehiclesConfigGroup.GROUP_NAME,
        NetworkConfigGroup.GROUP_NAME
    ));

    private final Table linkLog;
    private final Table vehicleOccupancy;
    private Table networkLinks;
    private Table networkLinkModes;

    public MATSimModel(String matsimInputConfig, String matsimOutputDir) {
        // there will be other stuff read for a matsim model if the KPIs require
        this.matsimOutputDir = matsimOutputDir;
        Config config = getConfig(matsimInputConfig);
        this.scenario = ScenarioUtils.loadScenario(config);
        createNetworkLinkTables();

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
//        TreeMap<String, ConfigGroup> configuredModules = config.getModules();
//        for (ConfigGroup module : configuredModules.values().stream().toList()){
//            for (Map.Entry<String, String> entry : module.getParams().entrySet()) {
//                System.out.println((entry.getKey() + "," + entry.getValue()));
//            }
//        }

        TreeMap<String, ConfigGroup> configuredModules = config.getModules();
        for (ConfigGroup module : configuredModules.values().stream().toList()) {
            if (necessaryConfigGroups.contains(module.getName())) {
                System.out.println("Config group " + module + " is read as is");
            } else {
                ReflectiveConfigGroup relaxedModule =
                        new ReflectiveConfigGroup(module.getName(), true) {};
                config.removeModule(module.getName());
                config.addModule(relaxedModule);
            }
        }
        ConfigUtils.loadConfig(config, String.format(matsimInputConfig));
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

    @Override
    public Table getLinkLog() {
        return linkLog;
    }

    @Override
    public Table getVehicleOccupancy() {
        return vehicleOccupancy;
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
    public void write(String outputDir) {
        linkLog.write().csv(String.format("%s/linkLog.csv", outputDir));
        vehicleOccupancy.write().csv(String.format("%s/vehicleOccupancy.csv", outputDir));
        networkLinks.write().csv(String.format("%s/networkLinks.csv", outputDir));
        networkLinkModes.write().csv(String.format("%s/networkLinkModes.csv", outputDir));
    }
}
