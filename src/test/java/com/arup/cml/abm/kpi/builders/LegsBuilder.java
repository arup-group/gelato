package com.arup.cml.abm.kpi.builders;

import org.junit.rules.TemporaryFolder;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.nio.file.Path;

public class LegsBuilder {
    TemporaryFolder tmpDir;

    String defaultPerson = "Bobby";
    String defaultTripId = "Bobby_0";
    String defaultDepTime = "07:38:40";
    String defaultTravTime = "00:09:12";
    String defaultWaitTime = "00:00:00";
    Integer defaultDistance = 5500;
    String defaultMode = "car";
    String defaultStartLink = "1-3";
    String defaultStartX = "-500";
    String defaultStartY = "-200";
    String defaultEndLink = "4-5";
    String defaultEndX = "4600";
    String defaultEndY = "800";
    String defaultAccessStopId = "";
    String defaultEgressStopId = "";
    String defaultTransitLine = "";
    String defaultTransitRoute = "";
    String defaultVehicleId = "Bobby";

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

    public LegsBuilder withLeg(
            String person, String trip_id, String dep_time, String trav_time, String wait_time, Integer distance,
            String mode, String start_link, String start_x, String start_y, String end_link, String end_x, String end_y,
            String access_stop_id, String egress_stop_id, String transit_line, String transit_route, String vehicle_id
    ) {
        legs.stringColumn("person").append(person);
        legs.stringColumn("trip_id").append(trip_id);
        legs.stringColumn("dep_time").append(dep_time);
        legs.stringColumn("trav_time").append(trav_time);
        legs.stringColumn("wait_time").append(wait_time);
        legs.intColumn("distance").append(distance);
        legs.stringColumn("mode").append(mode);
        legs.stringColumn("start_link").append(start_link);
        legs.stringColumn("start_x").append(start_x);
        legs.stringColumn("start_y").append(start_y);
        legs.stringColumn("end_link").append(end_link);
        legs.stringColumn("end_x").append(end_x);
        legs.stringColumn("end_y").append(end_y);
        legs.stringColumn("access_stop_id").append(access_stop_id);
        legs.stringColumn("egress_stop_id").append(egress_stop_id);
        legs.stringColumn("transit_line").append(transit_line);
        legs.stringColumn("transit_route").append(transit_route);
        legs.stringColumn("vehicle_id").append(vehicle_id);
        return this;
    }

    public LegsBuilder withDefaultLeg() {
        return this.withLeg(
                defaultPerson, defaultTripId, defaultDepTime, defaultTravTime, defaultWaitTime, defaultDistance, defaultMode,
                defaultStartLink, defaultStartX, defaultStartY, defaultEndLink, defaultEndX, defaultEndY, defaultAccessStopId,
                defaultEgressStopId, defaultTransitLine, defaultTransitRoute, defaultVehicleId
        );
    }

    public LegsBuilder withLegWithDistanceAndMode(String person, String tripId, Integer distance, String mode) {
        return this.withLeg(
                person, tripId, defaultDepTime, defaultTravTime, defaultWaitTime, distance, mode,
                defaultStartLink, defaultStartX, defaultStartY, defaultEndLink, defaultEndX, defaultEndY, defaultAccessStopId,
                defaultEgressStopId, defaultTransitLine, defaultTransitRoute, defaultVehicleId
        );
    }

    public String build() {
        if (legs.isEmpty()) {
            // empty table gets into trouble reading all the columns, if the table is empty, it is assumed it's not
            // being used, so filling it with dud vales, just for the shape is ok
            this.withDefaultLeg();
        }
        String legsPath = String.valueOf(Path.of(tmpDir.getRoot().getAbsolutePath(), "output_legs.csv"));
        CsvWriteOptions options = CsvWriteOptions.builder(legsPath).separator(';').build();
        legs.write().usingOptions(options);
        return legsPath;
    }
}
