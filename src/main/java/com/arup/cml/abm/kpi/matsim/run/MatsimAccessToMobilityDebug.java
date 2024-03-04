package com.arup.cml.abm.kpi.matsim.run;

import com.arup.cml.abm.kpi.tablesaw.TablesawKpiCalculator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.MemoryObserver;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Command(name = "MatsimKpiGenerator", version = "0.0.3-alpha", mixinStandardHelpOptions = true)
public class MatsimAccessToMobilityDebug implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(MatsimAccessToMobilityDebug.class);
    public static final String EOL = "\n";

    @Option(names = "-st", description = "Full path to `supporting-data-trips.csv.gz` file from a previous run",
            required = true)
    private Path gelatoTrips;

    @Option(names = "-ss", description = "Full path to `supporting-data-scheduleStops.csv.gz` file from a previous run",
            required = true)
    private Path gelatoStops;

    @Option(names = "-o", description = "Full path to the directory you want outputs to be written to", required = true)
    private Path outputDir;

    public static void main(String[] args) {
        System.setProperty("line.separator", EOL); // Required to allow platform independent checksum similarity
        int exitCode = new CommandLine(new MatsimAccessToMobilityDebug()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        LOGGER.info("Writing KPI metrics to {}, generated from gelato trips at {} and stops at {}",
                new Object[]{outputDir, gelatoTrips, gelatoStops}
        );
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }

        MemoryObserver.start(60);

        TablesawKpiCalculator kpiCalculator = new TablesawKpiCalculator();
        Table trips = readSupportingDataCSV(gelatoTrips, getTripsColumnMap());
        Table stops = readSupportingDataCSV(gelatoStops, getStopsColumnMap());

        LOGGER.info("Filtering trips table with {} rows to find trips that started from 'home'",
                trips.rowCount());
        Table table = trips
                .where(trips.stringColumn("start_activity_type").isEqualTo("home"))
                .selectColumns("person", "start_activity_type", "start_x", "start_y", "first_pt_boarding_stop");
        LOGGER.info("Filtered down to {} trips initially", table.rowCount());
        table.column("start_activity_type").setName("location_type");
        table.column("start_x").setName("x");
        table.column("start_y").setName("y");

        LOGGER.info("Checking access to bus stops");
        table = kpiCalculator.addPTAccessColumnWithinDistance(
                table,
                stops.where(stops.stringColumn("mode").isEqualTo("bus")),
                400.0,
                "bus_access_400m"
        );
        table.write().csv(String.format("%s/accessToMobilityDebug.csv", outputDir));
        LOGGER.info("Checking access to rail and subway stops");
        table = kpiCalculator.addPTAccessColumnWithinDistance(
                table,
                stops.where(
                        stops.stringColumn("mode").isEqualTo("rail").or(
                                stops.stringColumn("mode").isEqualTo("subway")
                        )
                ),
                800.0,
                "rail_access_800m"
        );

        table.write().csv(String.format("%s/accessToMobilityDebug.csv", outputDir));
        MemoryObserver.stop();
    }

    private static Table readSupportingDataCSV(Path filePath, Map<String, ColumnType> columnMapping) {
        LOGGER.info("Reading CSV at {} into a table", filePath);
        InputStream stream = IOUtils.getInputStream(
                IOUtils.resolveFileOrResource(filePath.toString()));
        CsvReadOptions.Builder builder = CsvReadOptions.builder(stream).separator(',').header(true)
                .columnTypesPartial(column -> {
                    if (columnMapping.keySet().contains(column)) {
                        return Optional.of(columnMapping.get(column));
                    }
                    return Optional.empty();
                });
        return Table.read().usingOptions(builder.build());
    }

    private Map<String, ColumnType> getTripsColumnMap() {
        Map<String, ColumnType> columnMapping = new HashMap<>();
        columnMapping.put("person", ColumnType.STRING);
        columnMapping.put("dep_time", ColumnType.STRING);
        columnMapping.put("trav_time", ColumnType.STRING);
        columnMapping.put("wait_time", ColumnType.STRING);
        columnMapping.put("trip_id", ColumnType.STRING);
        columnMapping.put("first_pt_boarding_stop", ColumnType.STRING);
        columnMapping.put("start_x", ColumnType.DOUBLE);
        columnMapping.put("start_y", ColumnType.DOUBLE);
        columnMapping.put("end_x", ColumnType.DOUBLE);
        columnMapping.put("end_y", ColumnType.DOUBLE);
        columnMapping.put("start_activity_type", ColumnType.STRING);
        columnMapping.put("start_facility_id", ColumnType.STRING);
        columnMapping.put("end_activity_type", ColumnType.STRING);
        columnMapping.put("end_facility_id", ColumnType.STRING);
        columnMapping.put("longest_distance_mode", ColumnType.STRING);
        return columnMapping;
    }

    private Map<String, ColumnType> getStopsColumnMap() {
        Map<String, ColumnType> columnMapping = new HashMap<>();
        return columnMapping;
    }
}
