package com.arup.cml.abm.kpi.tablesaw;

import com.arup.cml.abm.kpi.KpiCalculator;
import com.arup.cml.abm.kpi.domain.NetworkLinkLog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.arup.cml.abm.kpi.data.LinkLog;
import com.arup.cml.abm.kpi.matsim.run.MatsimKpiGenerator;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.ControllerConfigGroup.CompressionType;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;
import tech.tablesaw.api.*;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static tech.tablesaw.aggregate.AggregateFunctions.mean;
import static tech.tablesaw.aggregate.AggregateFunctions.sum;

public class TablesawKpiCalculator implements KpiCalculator {
    private static final Logger LOGGER = LogManager.getLogger(TablesawKpiCalculator.class);
    private final Table legs;
    private final Table trips;
    private Table networkLinks;
    private Table networkLinkModes;
    private Table scheduleStops;
    private Table scheduleRoutes;
    private Table vehicles;

    private Table linkLogTable;
    private Table vehicleOccupancyTable;
    private CompressionType compressionType;

    public TablesawKpiCalculator(Network network,
                                 TransitSchedule schedule,
                                 Vehicles vehicles,
                                 NetworkLinkLog linkLog,
                                 InputStream legsInputStream,
                                 InputStream tripsInputStream,
                                 Path outputDirectory,
                                 CompressionType compressionType) {
        this.compressionType = compressionType;
        Map<String, ColumnType> columnMapping = new HashMap<>();
        columnMapping.put("dep_time", ColumnType.STRING);
        columnMapping.put("trav_time", ColumnType.STRING);
        columnMapping.put("wait_time", ColumnType.STRING);

        legs = readCSVInputStream(legsInputStream, columnMapping).setName("Legs");
        trips = readCSVInputStream(tripsInputStream, columnMapping).setName("Trips");
        createNetworkLinkTables(network);
        createTransitTables(schedule);
        createVehicleTable(vehicles);
        createLinkLogTables(linkLog);
        writeIntermediateData(outputDirectory);
    }

    @Override
    public void writeAffordabilityKpi(Path outputDirectory) {
        LOGGER.info("Writing Affordability KPI to {}", outputDirectory);
    }

    @Override
    public void writePtWaitTimeKpi(Path outputDirectory) {
        LOGGER.info("Writing PT Wait Time KPI to {}", outputDirectory);

        // pull out legs with PT stops information
        Table table = legs.where(
                legs.column("access_stop_id").isNotMissing()
                        .or(legs.stringColumn("mode").isEqualTo("drt"))
        );

        // convert H:M:S format to seconds
        IntColumn wait_time_seconds = IntColumn.create("wait_time_seconds");
        table.stringColumn("wait_time")
                .forEach(time -> wait_time_seconds.append(
                        (int) Time.parseTime(time)));
        table.addColumns(wait_time_seconds);

        // put in hour bins
        IntColumn hour = IntColumn.create("hour");
        table.column("dep_time")
                .forEach(time -> hour.append(
                        // MATSim departure times look like "09:03:04" - grab the hour value
                        Integer.parseInt(time.toString().split(":")[0])
                ));
        table.addColumns(hour);

        // ***** proposed intermediate output - average by mode, stop id and hour
        Table intermediate =
                table
                        .summarize("wait_time_seconds", mean)
                        .by("mode", "access_stop_id", "hour")
                        .setName("Average wait time at stops by mode");
        this.writeTableCompressed(intermediate, String.format("%s/intermediate-pt-wait-time.csv", outputDirectory), this.compressionType);

        // kpi output
        double kpi =
                table
                        .where(table.intColumn("hour").isGreaterThanOrEqualTo(8)
                                .and(table.intColumn("hour").isLessThan(10)))
                        .intColumn("wait_time_seconds")
                        .mean();
        kpi = round(kpi, 2);
        LOGGER.info("PT Wait Time KPI {}", kpi);
        writeContentToFile(String.format("%s/kpi-pt-wait-time.csv", outputDirectory), String.valueOf(kpi), this.compressionType);
    }

    @Override
    public void writeModalSplitKpi(Path outputDirectory) {
        LOGGER.info("Writing Modal Split KPI to {}", outputDirectory);

        // percentages of trips by dominant (by distance) modes
        Table kpi = trips.xTabPercents("longest_distance_mode");
        kpi.replaceColumn(
                round(
                        kpi.doubleColumn("Percents").multiply(100).setName("Percents"), 2));
        kpi.setName("Modal Split");
        this.writeTableCompressed(kpi, String.format("%s/kpi-modal-split.csv", outputDirectory), compressionType);
    }

    @Override
    public void writeOccupancyRateKpi(Path outputDirectory) {
        LOGGER.info("Writing Occupancy Rate KPI to {}", outputDirectory);

        // add capacity of the vehicle
        Table table = linkLogTable
                .joinOn("vehicleID")
                .inner(vehicles.selectColumns("vehicleID", "capacity"));

        // TODO include empty vehicles?
        long numberOfVehicles = table.selectColumns("vehicleID").dropDuplicateRows().stream().count();

        // average by vehicle
        Table averageOccupancyPerVehicle =
                table
                        .summarize("numberOfPeople", "capacity", mean)
                        .by("vehicleID")
                        .setName("Occupancy Rate");
        averageOccupancyPerVehicle.addColumns(
                averageOccupancyPerVehicle
                        .doubleColumn("Mean [numberOfPeople]")
                        .divide(averageOccupancyPerVehicle.doubleColumn("Mean [capacity]"))
        );
        Table intermediate = Table.create(
                averageOccupancyPerVehicle.stringColumn("vehicleID"),
                averageOccupancyPerVehicle.doubleColumn("Mean [numberOfPeople]"),
                averageOccupancyPerVehicle.doubleColumn("Mean [capacity]").setName("capacity"),
                round(averageOccupancyPerVehicle.doubleColumn("Mean [numberOfPeople] / Mean [capacity]"), 2)
                        .setName("Average occupancy rate")
        ).setName("Occupancy Rate");
        this.writeTableCompressed(intermediate, String.format("%s/intermediate-occupancy-rate.csv", outputDirectory), this.compressionType);

        double kpi = averageOccupancyPerVehicle.doubleColumn("Mean [numberOfPeople] / Mean [capacity]").sum();
        kpi = kpi / numberOfVehicles;
        kpi = round(kpi, 2);

        LOGGER.info("Occupancy Rate KPI {}", kpi);
        writeContentToFile(String.format("%s/kpi-occupancy-rate.csv", outputDirectory), String.valueOf(kpi), this.compressionType);
    }

    @Override
    public double writeVehicleKMKpi(Path outputDirectory) {
        LOGGER.info("Writing Vehicle KM KPI to {}", outputDirectory);

        // add link length to the link log table
        Table table = linkLogTable
                .joinOn("linkID")
                .inner(networkLinks.selectColumns("linkID", "length"));

        // get total km travelled for each vehicle
        table = table
                .summarize("length", sum)
                .by("vehicleID")
                .setName("Vehicle KM");
        table.addColumns(
                table
                        .doubleColumn("Sum [length]")
                        .divide(1000)
                        .setName("distance_km")
        );

        // TODO add kpi for passenger KM
        // to get to a passenger-km metric the easiest way is to go through legs
//        Table legs = dataModel.getLegs();
//        vehicles.stringColumn("vehicleID").setName("vehicle_id");
//        legs = legs
//                .joinOn("vehicle_id")
//                .inner(vehicles.selectColumns("vehicle_id", "PTLineID", "PTRouteID"));
//        Table kpi = legs.selectColumns(
//                "person", "trip_id", "dep_time", "trav_time",
//                "distance", "mode", "vehicle_id", "PTLineID", "PTRouteID");

        // suggestion as intermediate output, might be too aggregated though
        Table intermediate = table
                .joinOn("vehicleID")
                .inner(vehicles.selectColumns("vehicleID", "mode"));
        intermediate.setName("Vehicle KM per vehicle");
        this.writeTableCompressed(intermediate, String.format("%s/intermediate-vehicle-km.csv", outputDirectory), this.compressionType);

        double kpi = round(table.doubleColumn("distance_km").sum(), 2);
        LOGGER.info("Vehicle KM KPI {}", kpi);
        writeContentToFile(String.format("%s/kpi-vehicle-km.csv", outputDirectory), String.valueOf(kpi), this.compressionType);
        return kpi;
    }

    @Override
    public void writeSpeedKpi(Path outputDirectory) {
        LOGGER.info("Writing Speed KPI to {}", outputDirectory);
        networkLinks = sanitiseInfiniteColumnValuesInTable(networkLinks, networkLinks.doubleColumn("length"));

        // add length of links to log
        Table table =
                linkLogTable
                        .joinOn("linkID")
                        .inner(networkLinks.selectColumns("linkID", "length"));

        // compute time travelled
        table.addColumns(
                table.doubleColumn("endTime")
                        .subtract(table.doubleColumn("startTime"))
                        .setName("travelTime")
        );

        // compute speed
        table.addColumns(
                table.doubleColumn("length")
                        .divide(1000)
                        .divide(
                                table.doubleColumn("travelTime")
                                        .divide(60 * 60)
                        )
                        .setName("travelSpeedKMPH")
        );

        // put in hour bins
        IntColumn hour = IntColumn.create("hour");
        table.doubleColumn("endTime")
                .forEach(time -> hour.append(
                        (int) Math.floor(time / (60 * 60))
                ));
        table.addColumns(hour);

        // average travelSpeedKMPH by link (rows) and hour (columns)
        // TODO is it possible to order columns? atm sorted with integers as strings, not a timeline
        // TODO geojson output
        // TODO missing data results in empty result
        Table kpi = table
                .pivot("linkID", "hour", "travelSpeedKMPH", mean)
                .setName("Speed");
        for (NumericColumn<?> column : kpi.numericColumns()) {
            kpi.replaceColumn(round(column.asDoubleColumn(), 2));
        }
        this.writeTableCompressed(kpi, String.format("%s/kpi-speed.csv", outputDirectory), this.compressionType);
    }

    @Override
    public void writeGHGKpi(Path outputDirectory) {
        LOGGER.info("Writing GHG KPIs to {}", outputDirectory);
    }

    @Override
    public Table writeCongestionKpi(Path outputDirectory) {
        LOGGER.info("Writing Congestion KPIs to {}", outputDirectory);

        // compute travel time on links
        Table table =
                linkLogTable.addColumns(
                        linkLogTable.doubleColumn("endTime")
                                .subtract(linkLogTable.doubleColumn("startTime"))
                                .setName("travelTime")
                );

        // compute free flow time on links (length / freespeed)
        Table sanitisedNetworkLinks = sanitiseInfiniteColumnValuesInTable(
                networkLinks, networkLinks.doubleColumn("freespeed"));
        sanitisedNetworkLinks.addColumns(
                sanitisedNetworkLinks.doubleColumn("length")
                        .divide(sanitisedNetworkLinks.doubleColumn("freespeed"))
                        .setName("freeFlowTime")
        );

        // add freeflow time to link log
        table =
                table
                        .joinOn("linkID")
                        .inner(sanitisedNetworkLinks.selectColumns("linkID", "freeFlowTime"));

        // compute delay ratio
        table.addColumns(
                table.doubleColumn("travelTime")
                        .divide(table.doubleColumn("freeFlowTime"))
                        .setName("delayRatio")
        );

        // put in hour bins
        IntColumn hour = IntColumn.create("hour");
        table.doubleColumn("endTime")
                .forEach(time -> hour.append(
                        (int) Math.floor(time / (60 * 60))
                ));
        table.addColumns(hour);

        // intermediate output data
        Table intermediate =
                table
                        .summarize("delayRatio", mean)
                        .by("linkID", "mode", "hour");
        this.writeTableCompressed(intermediate, String.format("%s/intermediate-congestion.csv", outputDirectory), this.compressionType);

        // kpi output
        Table kpi =
                table
                        .where(table.intColumn("hour").isGreaterThanOrEqualTo(8)
                                .and(table.intColumn("hour").isLessThan(10)))
                        .summarize("delayRatio", mean)
                        .by("mode")
                        .setName("Congestion KPI");
        kpi.replaceColumn(round(kpi.doubleColumn("Mean [delayRatio]"), 2));
        this.writeTableCompressed(kpi, String.format("%s/kpi-congestion.csv", outputDirectory), compressionType);
        return kpi;
    }

    private DoubleColumn round(DoubleColumn column, int decimalPoints) {
        DoubleColumn roundedColumn = DoubleColumn.create(column.name());
        column.forEach(new Consumer<Double>() {
            @Override
            public void accept(Double aDouble) {
                if (aDouble.isNaN()) {
                    roundedColumn.appendMissing();
                } else {
                    roundedColumn.append(Math.round(aDouble * Math.pow(10.0, decimalPoints)) / Math.pow(10.0, decimalPoints));
                }
            }
        });
        return roundedColumn;
    }

    private double round(double number, int decimalPoints) {
        return Math.round(number * Math.pow(10.0, decimalPoints)) / Math.pow(10.0, decimalPoints);
    }

    private Table sanitiseInfiniteColumnValuesInTable(Table table, DoubleColumn column) {
        Table infiniteValuesTable = table.where(column.eval(Double::isInfinite));
        if (!infiniteValuesTable.isEmpty()) {
            LOGGER.warn(("Table: `%s` has %d row(s) affected by infinite values in column: `%s`. " +
                    "These rows will be dropped for this calculation.")
                    .formatted(table.name(), infiniteValuesTable.rowCount(), column.name()));
            return table.dropWhere(column.eval(Double::isInfinite));
        } else {
            return table;
        }
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

    private void createLinkLogTables(NetworkLinkLog networkLinkLog) {
        LOGGER.info("Creating Link Log Table");

        if (networkLinkLog instanceof TablesawNetworkLinkLog) {
            LOGGER.info("Link Log Tablesaw tables already exist - will only perform basic data cleaning");
            TablesawNetworkLinkLog tsLinkLog = (TablesawNetworkLinkLog)networkLinkLog;
            linkLogTable = tsLinkLog.getLinkLogTable();
            vehicleOccupancyTable = tsLinkLog.getVehicleOccupancyTable();
            int rowsBeforeCleaning = linkLogTable.rowCount();
            linkLogTable = linkLogTable.dropWhere(linkLogTable.doubleColumn("endTime").isMissing());
            int rowsAfterCleaning = linkLogTable.rowCount();
            if (rowsAfterCleaning != rowsBeforeCleaning) {
                LOGGER.warn("{} missing 'endTime' data points were encountered - some vehicles " +
                                "were stuck and did not complete their journey. These Link Log entries will " +
                                "be deleted.",
                        rowsBeforeCleaning - rowsAfterCleaning);
            }
        } else if (networkLinkLog instanceof LinkLog) {
            LinkLog gauvaLinkLog = (LinkLog)networkLinkLog;
            LongColumn indexColumn = LongColumn.create("index");
            StringColumn linkIDColumn = StringColumn.create("linkID");
            StringColumn vehicleIDColumn = StringColumn.create("vehicleID");
            StringColumn modeColumn = StringColumn.create("initialMode");
            DoubleColumn startTimeColumn = DoubleColumn.create("startTime");
            DoubleColumn endTimeColumn = DoubleColumn.create("endTime");
            IntColumn numberOfPeopleColumn = IntColumn.create("numberOfPeople");

            int openLinkLogEntryCount = 0;
            for (Map.Entry<Long, Map<String, Object>> entry : gauvaLinkLog.getLinkLogData().rowMap().entrySet()) {
                indexColumn.append(entry.getKey());
                linkIDColumn.append(entry.getValue().get("linkID").toString());
                vehicleIDColumn.append(entry.getValue().get("vehicleID").toString());
                modeColumn.append(entry.getValue().get("mode").toString());
                startTimeColumn.append((Double) entry.getValue().get("startTime"));
                if (entry.getValue().containsKey("endTime")) {
                    endTimeColumn.append((Double) entry.getValue().get("endTime"));
                    numberOfPeopleColumn.append((Integer) entry.getValue().get("numberOfPeople"));
                } else {
                    endTimeColumn.append(-1);
                    numberOfPeopleColumn.append(0);
                    if (openLinkLogEntryCount == 0) {
                        LOGGER.warn("A missing `endTime` was encountered. This message is shown only once.");
                    }
                    openLinkLogEntryCount++;
                }
            }

            linkLogTable = Table.create("Link Log")
                    .addColumns(
                            indexColumn,
                            linkIDColumn,
                            vehicleIDColumn,
                            modeColumn,
                            startTimeColumn,
                            endTimeColumn,
                            numberOfPeopleColumn
                    );
            if (openLinkLogEntryCount > 0) {
                LOGGER.warn("{} missing `endTime` data points were encountered - some vehicles " +
                                "were stuck and did not complete their journey. These Link Log entries will be deleted.",
                        openLinkLogEntryCount);
                linkLogTable = linkLogTable.where(linkLogTable.doubleColumn("endTime").isNotEqualTo(-1));
            }

            LOGGER.info("Creating Link Log Vehicle Occupancy Table");
            LongColumn linkLogIndexColumn = LongColumn.create("linkLogIndex");
            StringColumn agentIDColumn = StringColumn.create("agentId");

            for (Map.Entry<Long, Map<String, Object>> entry : gauvaLinkLog.getVehicleOccupantsData().rowMap()
                    .entrySet()) {
                linkLogIndexColumn.append((long) entry.getValue().get("linkLogIndex"));
                agentIDColumn.append(entry.getValue().get("agentId").toString());
            }
            vehicleOccupancyTable = Table.create("Vehicle Occupancy")
                    .addColumns(
                            linkLogIndexColumn,
                            agentIDColumn
                    );
        }

        fixVehicleModesInLinkLog();
    }

    private void fixVehicleModesInLinkLog() {
        linkLogTable = linkLogTable
                .joinOn("vehicleID")
                .leftOuter(vehicles.selectColumns("vehicleID", "mode"));
        int mismatchedModes = linkLogTable.where(
                linkLogTable.stringColumn("initialMode")
                        .isNotEqualTo(linkLogTable.stringColumn("mode")
                        )
        ).stringColumn("vehicleID").countUnique();
        if (mismatchedModes > 0) {
            LOGGER.warn(String.format(
                    "There are %d vehicles that have different modes to the ones found in the Link Log. " +
                            "The modes in the Link Log will be updated with the modes from the Vehicle Table.",
                    mismatchedModes));
        }
        linkLogTable.removeColumns("initialMode");
    }


    public Table readCSVInputStream(InputStream inputStream, Map<String, ColumnType> columnMapping) {
        // TODO Make separator accessible from outside
        CsvReadOptions.Builder builder = CsvReadOptions.builder(inputStream).separator(';').header(true)
                .columnTypesPartial(column -> {
                    if (columnMapping.keySet().contains(column)) {
                        return Optional.of(columnMapping.get(column));
                    }
                    return Optional.empty();
                });
        return Table.read().usingOptions(builder.build());
    }

    private void writeContentToFile(String path, String content, CompressionType compressionType) {
        try (Writer wr = IOUtils.getBufferedWriter(path.concat(compressionType.fileEnding))) {
            wr.write(content);
        } catch (IOException e) {
            LOGGER.warn("Failed to save content `{}` to file: `{}`", content, path);
        }
    }

    private void writeIntermediateData(Path outputDir) {
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }

        this.writeTableCompressed(legs, String.format("%s/supporting-data-legs.csv", outputDir), this.compressionType);
        this.writeTableCompressed(trips, String.format("%s/supporting-data-trips.csv", outputDir), this.compressionType);
        this.writeTableCompressed(linkLogTable, String.format("%s/supporting-data-linkLog.csv", outputDir), this.compressionType);
        this.writeTableCompressed(vehicleOccupancyTable, String.format("%s/supporting-data-vehicleOccupancy.csv", outputDir), this.compressionType);
        this.writeTableCompressed(networkLinks, String.format("%s/supporting-data-networkLinks.csv", outputDir), this.compressionType);
        this.writeTableCompressed(networkLinkModes, String.format("%s/supporting-data-networkLinkModes.csv", outputDir), this.compressionType);
        this.writeTableCompressed(scheduleStops, String.format("%s/supporting-data-scheduleStops.csv", outputDir), this.compressionType);
        this.writeTableCompressed(scheduleRoutes, String.format("%s/supporting-data-scheduleRoutes.csv", outputDir), this.compressionType);
        this.writeTableCompressed(vehicles, String.format("%s/supporting-data-vehicles.csv", outputDir), this.compressionType);
    }

    private OutputStream getCompressedOutputStream(String filepath, CompressionType compressionType) {
        return IOUtils.getOutputStream(IOUtils.getFileUrl(filepath.concat(compressionType.fileEnding)),
                false);
    }

    private void writeTableCompressed(Table table, String filePath, CompressionType compressionType) {
        try (OutputStream stream = getCompressedOutputStream(filePath, compressionType)) {
            CsvWriteOptions options = CsvWriteOptions.builder(stream).lineEnd(MatsimKpiGenerator.EOL).build();
            table.write().csv(options);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
