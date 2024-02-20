package com.arup.cml.abm.kpi.builders;

import org.junit.rules.TemporaryFolder;
import org.matsim.core.utils.io.IOUtils;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.io.InputStream;
import java.nio.file.Path;

public class TripsBuilder {
    TemporaryFolder tmpDir;
    Table trips = Table.create("trips").addColumns(
            StringColumn.create("person"),
            StringColumn.create("trip_number"),
            StringColumn.create("trip_id"),
            StringColumn.create("dep_time"),
            StringColumn.create("trav_time"),
            StringColumn.create("wait_time"),
            StringColumn.create("traveled_distance"),
            StringColumn.create("euclidean_distance"),
            StringColumn.create("main_mode"),
            StringColumn.create("longest_distance_mode"),
            StringColumn.create("modes"),
            StringColumn.create("start_activity_type"),
            StringColumn.create("start_facility_id"),
            StringColumn.create("end_activity_type"),
            StringColumn.create("end_facility_id"),
            StringColumn.create("start_link"),
            StringColumn.create("start_x"),
            StringColumn.create("start_y"),
            StringColumn.create("end_link"),
            StringColumn.create("end_x"),
            StringColumn.create("end_y"),
            StringColumn.create("first_pt_boarding_stop"),
            StringColumn.create("last_pt_egress_stop")
    );

    public TripsBuilder(TemporaryFolder tmpDir) {
        this.tmpDir = tmpDir;
    }

    public String build() {
        String tripsPath = String.valueOf(Path.of(tmpDir.getRoot().getAbsolutePath(), "output_trips.csv"));
        CsvWriteOptions options = CsvWriteOptions.builder(tripsPath).separator(';').build();
        trips.write().usingOptions(options);
        return tripsPath;
    }
}
