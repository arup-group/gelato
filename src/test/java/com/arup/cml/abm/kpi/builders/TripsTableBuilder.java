package com.arup.cml.abm.kpi.builders;

import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.pt.routes.TransitPassengerRoute;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.nio.file.Path;

public class TripsTableBuilder {
    TemporaryFolder tmpDir;
    LegsTableBuilder legsTableBuilder;
    Table trips;

    public TripsTableBuilder(TemporaryFolder tmpDir) {
        this.tmpDir = tmpDir;
        this.legsTableBuilder = new LegsTableBuilder(tmpDir);
        init().withTrip("Bobby", "0", new TripBuilder().build());
    }

    private TripsTableBuilder init() {
        trips = Table.create("trips").addColumns(
                StringColumn.create("person"),
                StringColumn.create("trip_number"),
                StringColumn.create("trip_id"),
                StringColumn.create("dep_time"),
                StringColumn.create("trav_time"),
                StringColumn.create("wait_time"),
                IntColumn.create("traveled_distance"),
                IntColumn.create("euclidean_distance"),
                StringColumn.create("main_mode"),
                StringColumn.create("longest_distance_mode"),
                StringColumn.create("modes"),
                StringColumn.create("start_activity_type"),
                StringColumn.create("start_facility_id"),
                StringColumn.create("end_activity_type"),
                StringColumn.create("end_facility_id"),
                StringColumn.create("start_link"),
                DoubleColumn.create("start_x"),
                DoubleColumn.create("start_y"),
                StringColumn.create("end_link"),
                DoubleColumn.create("end_x"),
                DoubleColumn.create("end_y"),
                StringColumn.create("first_pt_boarding_stop"),
                StringColumn.create("last_pt_egress_stop")
        );
        return this;
    }

    public TripsTableBuilder reset() {
        legsTableBuilder.reset();
        return this.init();
    }

    public TripsTableBuilder withTrip(String person, String trip_number, TripBuilder.Trip trip) {
        String trip_id = String.format("%s_%s", person, trip_number);
        String first_pt_boarding_stop = "";
        String last_pt_egress_stop = "";
        StringBuilder modes = new StringBuilder();
        String longest_distance_mode = "";
        int longest_distance = 0;
        boolean isPtTrip = false;
        double totalTravTime = 0.0;
        double totalWaitTime = 0.0;
        int totalDistance = 0;
        for (Leg leg : trip.legs) {
            // check for PT info
            if (leg.getRoute() instanceof TransitPassengerRoute) {
                isPtTrip = true;
                TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();
                if (first_pt_boarding_stop.isBlank()) {
                    first_pt_boarding_stop = route.getAccessStopId().toString();
                }
                last_pt_egress_stop = route.getEgressStopId().toString();
            }
            // get times
            totalTravTime += leg.getTravelTime().seconds();
            totalWaitTime += (double) leg.getAttributes().getAsMap().getOrDefault("waitTime", 0.0);
            // distance and modes
            int dist = (int) Math.round(leg.getRoute().getDistance());
            totalDistance += dist;
            if (longest_distance < dist){
                longest_distance_mode = leg.getMode();
                longest_distance = dist;
            }
            modes.append(leg.getMode()).append(",");
            // update legs builder
            this.legsTableBuilder.withLeg(person, trip_id, leg);
        }
        modes.deleteCharAt(modes.length() - 1);

        trips.stringColumn("person").append(person);
        trips.stringColumn("trip_number").append(trip_number);
        trips.stringColumn("trip_id").append(trip_id);
        trips.stringColumn("dep_time").append(secondsToString(trip.legs.get(0).getDepartureTime().seconds()));
        trips.stringColumn("trav_time").append(secondsToString(totalTravTime));
        trips.stringColumn("wait_time").append(secondsToString(totalWaitTime));
        trips.intColumn("traveled_distance").append(totalDistance);
        trips.intColumn("euclidean_distance").append(totalDistance);
        trips.stringColumn("main_mode").append(longest_distance_mode);
        trips.stringColumn("longest_distance_mode").append(longest_distance_mode);
        trips.stringColumn("modes").append(modes.toString());
        trips.stringColumn("start_activity_type").append(trip.startActivity.getType());
        trips.stringColumn("start_facility_id").append(trip.startActivity.getFacilityId().toString());
        trips.stringColumn("end_activity_type").append(trip.endActivity.getType());
        trips.stringColumn("end_facility_id").append(trip.endActivity.getFacilityId().toString());
        trips.stringColumn("start_link").append(trip.startActivity.getLinkId().toString());
        trips.doubleColumn("start_x").append(trip.startActivity.getCoord().getX());
        trips.doubleColumn("start_y").append(trip.startActivity.getCoord().getY());
        trips.stringColumn("end_link").append(trip.endActivity.getLinkId().toString());
        trips.doubleColumn("end_x").append(trip.endActivity.getCoord().getX());
        trips.doubleColumn("end_y").append(trip.endActivity.getCoord().getY());
        if (isPtTrip) {
            trips.stringColumn("first_pt_boarding_stop").append(first_pt_boarding_stop);
            trips.stringColumn("last_pt_egress_stop").append(last_pt_egress_stop);
        } else {
            trips.stringColumn("first_pt_boarding_stop").appendMissing();
            trips.stringColumn("last_pt_egress_stop").appendMissing();
        }
        return this;
    }

    private String secondsToString(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secondsRemainder = (int) seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secondsRemainder);
    }

    public LegsTableBuilder getLegsBuilder() {
        return legsTableBuilder;
    }

    public String build() {
        String tripsPath = String.valueOf(Path.of(tmpDir.getRoot().getAbsolutePath(), "output_trips.csv"));
        CsvWriteOptions options = CsvWriteOptions.builder(tripsPath).separator(';').build();
        trips.write().usingOptions(options);
        return tripsPath;
    }
}
