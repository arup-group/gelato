package com.arup.cml.abm.kpi.builders;

import org.junit.rules.TemporaryFolder;
import org.matsim.core.utils.io.IOUtils;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.index.StringIndex;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.io.InputStream;
import java.nio.file.Path;

public class LegsBuilder {
    TemporaryFolder tmpDir;
    Table legs = Table.create("legs").addColumns(
            StringColumn.create("person"),
            StringColumn.create("trip_id"),
            StringColumn.create("dep_time"),
            StringColumn.create("trav_time"),
            StringColumn.create("wait_time"),
            StringColumn.create("distance"),
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

    public String build() {
        String legsPath = String.valueOf(Path.of(tmpDir.getRoot().getAbsolutePath(), "output_legs.csv"));
        CsvWriteOptions options = CsvWriteOptions.builder(legsPath).separator(';').build();
        legs.write().usingOptions(options);
        return legsPath;
    }
}
