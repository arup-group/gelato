package com.arup.cml.abm.kpi.builders;

import org.junit.rules.TemporaryFolder;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.nio.file.Path;
import java.util.Arrays;

public class LegsBuilder {
    TemporaryFolder tmpDir;
    Table legs = Table.create("legs").addColumns(
            StringColumn.create("person"),
            StringColumn.create("trip_id"),
            StringColumn.create("dep_time"),
            StringColumn.create("trav_time"),
            StringColumn.create("wait_time"),
            IntColumn.create("distance"),
            StringColumn.create("mode"),
            StringColumn.create("start_link"),
            StringColumn.create("start_x"),
            StringColumn.create("start_y"),
            StringColumn.create("end_link"),
            StringColumn.create("end_x"),
            StringColumn.create("end_y"),
            StringColumn.create("access_stop_id"),
            StringColumn.create("egress_stop_id"),
            StringColumn.create("transit_line"),
            StringColumn.create("transit_route"),
            StringColumn.create("vehicle_id")
    );

    public LegsBuilder(TemporaryFolder tmpDir) {
        this.tmpDir = tmpDir;
    }

    public void fillWithDudValues() {
        String[] intCols = new String[]{"distance"};
        for (Column col : legs.columns()) {
            if (Arrays.asList(intCols).contains(col.name())) {
                col.append(0);
            } else {
                col.append("dud");
            }
        }
    }

    public String build() {
        if (legs.isEmpty()) {
            // empty table gets into trouble reading all the columns, if the table is empty, it is assumed it's not
            // being used, so filling it with dud vales, just for the shape is ok
            fillWithDudValues();
        }
        String legsPath = String.valueOf(Path.of(tmpDir.getRoot().getAbsolutePath(), "output_legs.csv"));
        CsvWriteOptions options = CsvWriteOptions.builder(legsPath).separator(';').build();
        legs.write().usingOptions(options);
        return legsPath;
    }
}
