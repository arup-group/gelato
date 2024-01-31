package com.arup.cml.abm.kpi.tablesaw;

import com.arup.cml.abm.kpi.KpiCalculator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.arup.cml.abm.kpi.data.LinkLog;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;
import tech.tablesaw.api.*;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import static tech.tablesaw.aggregate.AggregateFunctions.mean;

public class TablesawKpiCalculator implements KpiCalculator {
    private static final Logger LOGGER = LogManager.getLogger(TablesawKpiCalculator.class);
    private Table networkLinks;
    private Table networkLinkModes;
    private Table scheduleStops;
    private Table scheduleRoutes;
    private Table vehicles;

    private Table linkLog;
    private Table linkLogVehicleOccupancy;

    public TablesawKpiCalculator(Network network, TransitSchedule schedule, Vehicles vehicles, LinkLog linkLog) {
        // TODO: 24/01/2024 replace this ASAP with a representation of the network
        // that isn't from the MATSim API (a map, or dedicated domain object, or whatever)
        createNetworkLinkTables(network);
        createTransitTables(schedule);
        createVehicleTable(vehicles);
        createLinkLogTables(linkLog);
    }

    @Override
    public void writeCongestionKpi(Path outputDirectory) {
        System.out.printf("Writing Congestion KPIs to %s%n", outputDirectory);

        // compute travel time on links
        Table kpi =
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
        kpi =
                kpi
                        .joinOn("linkID")
                        .inner(networkLinks.selectColumns("linkID", "freeFlowTime"));

        // compute delay ratio
        kpi.addColumns(
                kpi.doubleColumn("travelTime")
                        .divide(kpi.doubleColumn("freeFlowTime"))
                        .setName("delayRatio")
        );

        // put in hour bins
        IntColumn hour = IntColumn.create("hour");
        kpi.doubleColumn("endTime")
                .forEach(time -> hour.append(
                        (int) Math.floor(time / (60 * 60))
                ));
        kpi.addColumns(hour);

        // intermediate output data
        Table intermediate =
                kpi
                        .summarize("delayRatio", mean)
                        .by("linkID", "mode", "hour");
        intermediate.write().csv(String.format("%s/congestion.csv", outputDirectory));

        // kpi output
        kpi =
                kpi
                        .where(kpi.intColumn("hour").isGreaterThanOrEqualTo(7)
                                .and(kpi.intColumn("hour").isLessThanOrEqualTo(9)))
                        .summarize("delayRatio", mean)
                        .by("mode")
                        .setName("Congestion KPI");
        kpi.write().csv(String.format("%s/kpi.csv", outputDirectory));
        writeIntermediateData(outputDirectory);
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

    private void createTransitTables(TransitSchedule schedule) {
        LOGGER.info("Creating Transit Tables");
        LOGGER.info("Creating Schedule Stop Table");
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

        LOGGER.info("Creating Schedule Transit Tables");
        schedule.getTransitLines().forEach((lineId, transitLine) -> {
            transitLine.getRoutes().forEach((routeId, route) -> {
                lineIDColumn.append(lineId.toString());
                routeIDColumn.append(routeId.toString());
                modeColumn.append(route.getTransportMode());
            });
        });

        scheduleRoutes = Table.create("Schedule Routes")
                .addColumns(
                        lineIDColumn,
                        routeIDColumn,
                        modeColumn
                );
    }

    private void createVehicleTable(Vehicles inputVehicles) {
        LOGGER.info("Creating Vehicle Table");
        StringColumn vehicleIDColumn = StringColumn.create("vehicleID");
        StringColumn modeColumn = StringColumn.create("mode");
        IntColumn capacityColumn = IntColumn.create("capacity");
        StringColumn ptLineIDColumn = StringColumn.create("PTLineID");
        StringColumn ptRouteIDColumn = StringColumn.create("PTRouteID");

        inputVehicles.getVehicles().forEach((id, vehicle) -> {
            vehicleIDColumn.append(id.toString());
            modeColumn.append(vehicle.getType().getNetworkMode());
            capacityColumn.append(
                    vehicle.getType().getCapacity().getSeats() + vehicle.getType().getCapacity().getStandingRoom()
            );

            ptLineIDColumn.append(
                    Objects.requireNonNullElse(
                            vehicle.getAttributes().getAttribute("PTLineID"),
                            "Null"
                    ).toString());
            ptRouteIDColumn.append(
                    Objects.requireNonNullElse(
                            vehicle.getAttributes().getAttribute("PTRouteID"),
                            "Null"
                    ).toString());
//            ptRouteIDColumn.append(vehicle.getAttributes().getAttribute("PTRouteID").toString());
        });

        vehicles = Table.create("Vehicles")
                .addColumns(
                        vehicleIDColumn,
                        modeColumn,
                        capacityColumn,
                        StringColumn.create("PTLineID"),
                        StringColumn.create("PTRouteID")
                );
    }

    private void createLinkLogTables(LinkLog _linkLog) {
        LOGGER.info("Creating Link Log Table");
        LongColumn indexColumn = LongColumn.create("index");
        StringColumn linkIDColumn = StringColumn.create("linkID");
        StringColumn vehicleIDColumn = StringColumn.create("vehicleID");
        StringColumn modeColumn = StringColumn.create("initialMode");
        DoubleColumn startTimeColumn = DoubleColumn.create("startTime");
        DoubleColumn endTimeColumn = DoubleColumn.create("endTime");
        IntColumn numberOfPeopleColumn = IntColumn.create("numberOfPeople");

        int missingValues = 0;
        for (Map.Entry<Long, Map<String, Object>> entry : _linkLog.getLinkLogData().rowMap().entrySet()) {
            indexColumn.append(entry.getKey());
            linkIDColumn.append(entry.getValue().get("linkID").toString());
            vehicleIDColumn.append(entry.getValue().get("vehicleID").toString());
            modeColumn.append(entry.getValue().get("mode").toString());
            startTimeColumn.append((Double) entry.getValue().get("startTime"));
            if (entry.getValue().containsKey("endTime")) {
                endTimeColumn.append((Double) entry.getValue().get("endTime"));
                numberOfPeopleColumn.append((Integer) entry.getValue().get("numberOfPeople"));
            } else {
                if (missingValues == 0) {
                    LOGGER.warn("A missing `endTime` was encountered. This message is showed only once.");
                }
                missingValues++;
            }
            if (missingValues > 0) {
                LOGGER.warn(String.format(
                        "%d missing `endTime` data points were encountered - some vehicles were stuck and did not complete their journey",
                        missingValues
                ));
            }
        }
        linkLog = Table.create("Link Log")
                .addColumns(
                        indexColumn,
                        linkIDColumn,
                        vehicleIDColumn,
                        modeColumn,
                        startTimeColumn,
                        endTimeColumn,
                        numberOfPeopleColumn
                );
        // fix vehicle modes with vehicle table
        linkLog = linkLog
                .joinOn("vehicleID")
                .leftOuter(vehicles.selectColumns("vehicleID", "mode"));
        int mismatchedModes = linkLog.where(
                linkLog.stringColumn("initialMode")
                        .isNotEqualTo(linkLog.stringColumn("mode")
                )
        ).stringColumn("vehicleID").countUnique();
        if (mismatchedModes > 0) {
            LOGGER.warn(String.format(
                    "There are %d vehicles that have different modes to the ones found in the Link Log. " +
                            "The modes in the Link Log will be updated with the modes from the Vehicle Table.",
                    mismatchedModes));
        }
        linkLog.removeColumns("initialMode");

        LOGGER.info("Creating Link Log Vehicle Occupancy Table");
        LongColumn linkLogIndexColumn = LongColumn.create("linkLogIndex");
        StringColumn agentIDColumn = StringColumn.create("agentId");

        for (Map.Entry<Long, Map<String, Object>> entry : _linkLog.getVehicleOccupantsData().rowMap().entrySet()) {
            linkLogIndexColumn.append(entry.getKey());
            agentIDColumn.append(entry.getValue().get("agentId").toString());
        }
        linkLogVehicleOccupancy = Table.create("Vehicle Occupancy")
                .addColumns(
                        linkLogIndexColumn,
                        agentIDColumn
                );
    }

    public Table readCSVInputStream(InputStream inputStream) {
        CsvReadOptions.Builder builder = CsvReadOptions.builder(inputStream).separator(';');
        return Table.read().usingOptions(builder.build());
    }

    private void writeIntermediateData(Path outputDir) {
        linkLog.write().csv(String.format("%s/linkLog.csv", outputDir));
        linkLogVehicleOccupancy.write().csv(String.format("%s/vehicleOccupancy.csv", outputDir));
        networkLinks.write().csv(String.format("%s/networkLinks.csv", outputDir));
        networkLinkModes.write().csv(String.format("%s/networkLinkModes.csv", outputDir));
        scheduleStops.write().csv(String.format("%s/scheduleStops.csv", outputDir));
        scheduleRoutes.write().csv(String.format("%s/scheduleRoutes.csv", outputDir));
        vehicles.write().csv(String.format("%s/vehicles.csv", outputDir));
    }
}
