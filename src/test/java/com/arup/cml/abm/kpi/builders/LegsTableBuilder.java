package com.arup.cml.abm.kpi.builders;

import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.pt.routes.TransitPassengerRoute;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.nio.file.Path;

public class LegsTableBuilder {
    TemporaryFolder tmpDir;
    Table legs;

    public LegsTableBuilder(TemporaryFolder tmpDir) {
        this.tmpDir = tmpDir;
        init().withLeg("Bobby", "1", new LegBuilder().build());
    }

    private LegsTableBuilder init() {
        legs = Table.create("legs").addColumns(
                StringColumn.create("person"),
                StringColumn.create("trip_id"),
                StringColumn.create("dep_time"),
                StringColumn.create("trav_time"),
                StringColumn.create("wait_time"),
                IntColumn.create("distance"),
                StringColumn.create("mode"),
                StringColumn.create("start_link"),
                DoubleColumn.create("start_x"),
                DoubleColumn.create("start_y"),
                StringColumn.create("end_link"),
                DoubleColumn.create("end_x"),
                DoubleColumn.create("end_y"),
                StringColumn.create("access_stop_id"),
                StringColumn.create("egress_stop_id"),
                StringColumn.create("transit_line"),
                StringColumn.create("transit_route"),
                StringColumn.create("vehicle_id")
        );
        return this;
    }

    public LegsTableBuilder reset() {
        return this.init();
    }

    public LegsTableBuilder withLeg(String person, String trip_id, Leg leg) {
        legs.stringColumn("person").append(person);
        legs.stringColumn("trip_id").append(trip_id);
        legs.stringColumn("dep_time").append(secondsToString(leg.getDepartureTime().seconds()));
        legs.stringColumn("trav_time").append(secondsToString(leg.getTravelTime().seconds()));
        legs.stringColumn("wait_time").append(secondsToString((double) leg.getAttributes().getAsMap().getOrDefault("waitTime", 0.0)));
        legs.intColumn("distance").append((int) Math.round(leg.getRoute().getDistance()));
        legs.stringColumn("mode").append(leg.getMode());
        legs.stringColumn("start_link").append(leg.getRoute().getStartLinkId().toString());
        legs.doubleColumn("start_x").append(0.0);
        legs.doubleColumn("start_y").append(0.0);
        legs.stringColumn("end_link").append(leg.getRoute().getEndLinkId().toString());
        legs.doubleColumn("end_x").append(0.0);
        legs.doubleColumn("end_y").append(0.0);
        if (leg.getRoute() instanceof TransitPassengerRoute) {
            TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();
            legs.stringColumn("access_stop_id").append(route.getAccessStopId().toString());
            legs.stringColumn("egress_stop_id").append(route.getEgressStopId().toString());
            legs.stringColumn("transit_line").append(route.getLineId().toString());
            legs.stringColumn("transit_route").append(route.getRouteId().toString());
        } else {
            legs.stringColumn("access_stop_id").appendMissing();
            legs.stringColumn("egress_stop_id").appendMissing();
            legs.stringColumn("transit_line").appendMissing();
            legs.stringColumn("transit_route").appendMissing();
        }
        legs.stringColumn("vehicle_id").append(
                leg.getAttributes().getAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME).toString());
        return this;
    }

    private String secondsToString(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secondsRemainder = (int) seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secondsRemainder);
    }

    public String build() {
        String legsPath = String.valueOf(Path.of(tmpDir.getRoot().getAbsolutePath(), "output_legs.csv"));
        CsvWriteOptions options = CsvWriteOptions.builder(legsPath).separator(';').build();
        legs.write().usingOptions(options);
        return legsPath;
    }
}
