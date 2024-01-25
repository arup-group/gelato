package com.arup.cml.abm.kpi.tablesaw;

import com.arup.cml.abm.kpi.KpiCalculator;
import com.arup.cml.abm.kpi.matsim.MATSimModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import tech.tablesaw.api.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.LongStream;

import static tech.tablesaw.aggregate.AggregateFunctions.mean;

public class TablesawKpiCalculator implements KpiCalculator {

    private static final Logger LOGGER = LogManager.getLogger(TablesawKpiCalculator.class);

    private Map<String, String> vehicleModes = new HashMap<>();

    // arrays to collect Link Log data, each will form a column of the Link Log
    private final ArrayList<String> vehicleIDColumn = new ArrayList<>();
    private final ArrayList<String> linkIDColumn = new ArrayList<>();
    private final LinkedHashMap<Long, String> modeColumn = new LinkedHashMap<>();
    private final ArrayList<Double> startTimeColumn = new ArrayList<>();
    private final LinkedHashMap<Long, Double> endTimeColumn = new LinkedHashMap<>();
    private final LinkedHashMap<Long, Integer> numberOfPeopleColumn = new LinkedHashMap<>();

    // points to the index of the most recent reference of that vehicle ID in the Link Log
    private final Map<String, Long> vehicleLatestLogIndex = new HashMap<>();

    // arrays to collect agent IDs that are inside the vehicle in reference to the Link Log entries
    // these will form columns of the Vehicle Occupancy table, it shows who is in the vehicle at a point in the Link Log
    private final ArrayList<Long> linkLogIndexColumn = new ArrayList<>();
    private final ArrayList<String> agentIDColumn = new ArrayList<>();

    // tracks the most recent occupants of a vehicle 
    // TODO: 24/01/2024 should this be a set?
    private final Map<String, ArrayList<String>> vehicleLatestOccupants = new HashMap<>();

    // Link Log entry index
    private long index = 0;

    // TODO: 24/01/2024 replace this ASAP with a representation of the network
    // that isn't from the MATSim API (a map, or dedicated domain object, or whatever)
    private Network network;

    private Table networkLinks;
    private Table networkLinkModes;

    public TablesawKpiCalculator(Network network) {
        this.network = network;
        createNetworkLinkTables(network);
    }

    @Override
    public void linkEntered(String vehicleId, String linkId, double timestamp) {
        newLinkLogEntry(
                vehicleId,
                linkId,
                vehicleModes.getOrDefault(vehicleId, "unknown"),
                timestamp
        );
    }

    @Override
    public void linkExited(String vehicleId, String linkId, double timestamp) {
        updateLinkLogEntry(vehicleId, timestamp);
    }

    @Override
    public void vehicleEntered(String vehicleId, String personId) {
        if (vehicleLatestOccupants.containsKey(vehicleId)) {
            vehicleLatestOccupants.get(vehicleId).add(personId);
        } else {
            ArrayList<String> latestOccupants = new ArrayList<>();
            latestOccupants.add(personId);
            vehicleLatestOccupants.put(vehicleId, latestOccupants);
        }
    }

    @Override
    public void vehicleExited(String vehicleId, String personId) {
        vehicleLatestOccupants.get(vehicleId).remove(personId);
    }

    @Override
    public void recordVehicleMode(String vehicleId, String mode) {
        this.vehicleModes.put(vehicleId, mode);
    }

    @Override
    public void writeCongestionKpi(Path outputDirectory) {
        System.out.printf("Writing Congestion KPIs to %s%n", outputDirectory);
        Table linkLog = getLinkLog();

        // compute travel time on links
        linkLog.addColumns(
                linkLog.doubleColumn("endTime")
                        .subtract(linkLog.doubleColumn("startTime"))
                        .setName("travelTime")
        );

        // compute free flow time on links (length / freespeed)
        networkLinks.addColumns(
                networkLinks.doubleColumn("length")
                        .divide(networkLinks.doubleColumn("freespeed"))
                        .setName("freeFlowTime")
        );

        // add freeflow time to link log
        linkLog =
                linkLog
                        .joinOn("linkID")
                        .inner(networkLinks.selectColumns("linkID", "freeFlowTime"));

        // compute delay ratio
        linkLog.addColumns(
                linkLog.doubleColumn("travelTime")
                        .divide(linkLog.doubleColumn("freeFlowTime"))
                        .setName("delayRatio")
        );

        // put in hour bins
        IntColumn hour = IntColumn.create("hour");
        linkLog.doubleColumn("endTime")
                .forEach(time -> hour.append(
                        (int) Math.floor(time / (60 * 60))
                ));
        linkLog.addColumns(hour);

        // intermediate output data
        Table intermediate =
                linkLog
                        .summarize("delayRatio", mean)
                        .by("linkID", "mode", "hour");
        intermediate.write().csv(String.format("%s/congestion.csv", outputDirectory));

        // kpi output
        Table kpi =
                linkLog
                        .where(linkLog.intColumn("hour").isGreaterThanOrEqualTo(7)
                                .and(linkLog.intColumn("hour").isLessThanOrEqualTo(9)))
                        .summarize("delayRatio", mean)
                        .by("mode")
                        .setName("Congestion KPI");
        kpi.write().csv(String.format("%s/kpi.csv", outputDirectory));
    }

    private void newLinkLogEntry(String vehicleID, String linkID, String mode, double startTime) {
        vehicleIDColumn.add(vehicleID);
        linkIDColumn.add(linkID.toString());
        modeColumn.put(index, mode);
        startTimeColumn.add(startTime);
        // end time is not known yet, a placeholder in the ordered list is saved
        endTimeColumn.put(index, -1.0);
        // placeholder for people in the vehicle as well - someone might enter the vehicle before it leaves the link
        numberOfPeopleColumn.put(index, -1);
        vehicleLatestLogIndex.put(vehicleID, index);
        index++;
    }

    private void newVehicleOccupantsEntry(String vehicleID, long idx) {
        ArrayList<String> currentOccupants = vehicleLatestOccupants.get(vehicleID);
        for (String personID : currentOccupants) {
            linkLogIndexColumn.add(idx);
            agentIDColumn.add(personID.toString());
        }
    }

    private void updateLinkLogEntry(String vehicleID, double endTime) {
        long latestStateIndex = this.vehicleLatestLogIndex.get(vehicleID);
        // update end time
        endTimeColumn.put(latestStateIndex, endTime);
        // update vehicle occupants
        ArrayList<String> currentOccupants = vehicleLatestOccupants.get(vehicleID);
        numberOfPeopleColumn.put(latestStateIndex, currentOccupants.size());
        newVehicleOccupantsEntry(vehicleID, latestStateIndex);
    }

    private Table getLinkLog() {
        return Table.create("Link Log")
                .addColumns(
                        LongColumn.create("index", LongStream.range(0, index).toArray()),
                        StringColumn.create("linkID", linkIDColumn),
                        StringColumn.create("vehicleID", vehicleIDColumn),
                        StringColumn.create("mode", modeColumn.values()),
                        DoubleColumn.create("startTime", startTimeColumn),
                        DoubleColumn.create("endTime", endTimeColumn.values()),
                        DoubleColumn.create("numberOfPeople", numberOfPeopleColumn.values())
                );
    }

    private Table getVehicleOccupancy() {
        return Table.create("Vehicle Occupancy")
                .addColumns(
                        // TODO: This should be LongColumn (reference to index LongColumn of "Link Log" table)
                        // but can't get it to work with LongColumn constructor as ArrayList<Long>
                        DoubleColumn.create("linkLogIndex", linkLogIndexColumn),
                        StringColumn.create("agentId", agentIDColumn)
                );
    }

    private void createNetworkLinkTables(Network network) {
        LOGGER.info("Creating Network Link Tables");

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

    private void writeIntermediateData(String outputDir) {
        getLinkLog().write().csv(String.format("%s/linkLog.csv", outputDir));
        getVehicleOccupancy().write().csv(String.format("%s/vehicleOccupancy.csv", outputDir));
        networkLinks.write().csv(String.format("%s/networkLinks.csv", outputDir));
        networkLinkModes.write().csv(String.format("%s/networkLinkModes.csv", outputDir));
    }
}
