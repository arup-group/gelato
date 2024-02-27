package com.arup.cml.abm.kpi.builders;

import org.junit.rules.TemporaryFolder;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.nio.file.Path;
import java.util.Arrays;

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

    public void fillWithDudValues() {
        String[] timeCols = new String[]{"dep_time", "trav_time", "wait_time"};
        for (Column col : trips.columns()) {
            if (Arrays.asList(timeCols).contains(col.name())) {
                col.append("00:00:00");
            } else {
                col.append("dud");
            }
        }
    }

    public String build() {
        if (trips.isEmpty()) {
            // empty table gets into trouble reading all the columns, if the table is empty, it is assumed it's not
            // being used, so filling it with dud vales, just for the shape is ok
            fillWithDudValues();
        }
        String tripsPath = String.valueOf(Path.of(tmpDir.getRoot().getAbsolutePath(), "output_trips.csv"));
        CsvWriteOptions options = CsvWriteOptions.builder(tripsPath).separator(';').build();
        trips.write().usingOptions(options);
        return tripsPath;
    }
}
