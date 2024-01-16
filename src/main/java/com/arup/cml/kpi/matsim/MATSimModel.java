package com.arup.cml.kpi.matsim;

import com.arup.cml.kpi.DataModel;
import com.arup.cml.kpi.matsim.handlers.LinkLogHandler;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.ArrayList;


public class MATSimModel implements DataModel {
    private static final Logger log = LogManager.getLogger(MATSimModel.class);

    private final String matsimOutputDir;
    private final Scenario scenario;
    private final EventsManager eventsManager;

    private final Table linkLog;
    private final Table vehicleOccupancy;
    private Table networkLinks;
    private Table networkLinkModes;

    public MATSimModel(String matsimInputConfig, String matsimOutputDir) {

        // there will be other stuff read for a matsim model if the KPIs require
        this.matsimOutputDir = matsimOutputDir;
        Config config = ConfigUtils.loadConfig(
                String.format(matsimInputConfig)
        );
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
