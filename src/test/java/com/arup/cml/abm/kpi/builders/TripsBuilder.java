package com.arup.cml.abm.kpi.builders;

import org.junit.rules.TemporaryFolder;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.nio.file.Path;
import java.util.Set;

public class TripsBuilder {
    TemporaryFolder tmpDir;
    LegsBuilder legsBuilder;
    String defaultPerson = "Bobby";
    String defaultTripNumber = "0";
    String defaultTripId = "Bobby_0";
    String defaultDepTime = "07:38:40";
    String defaultTravTime = "00:09:12";
    String defaultWaitTime = "00:00:00";
    Integer defaultTravelledDistance = 5500;
    Integer defaultEuclideanDistance = 4000;
    String defaultMainMode = "car";
    String defaultLongestDistanceMode = "car";
    String defaultModes = "car";
    String defaultStartActivityType = "home";
    String defaultStartFacilityId = "home_Bobby";
    String defaultEndActivityType = "work";
    String defaultEndFacilityId = "work_Bobby";
    String defaultStartLink = "1-3";
    double defaultStartX = -500.0;
    double defaultStartY = -200.0;
    String defaultEndLink = "4-5";
    double defaultEndX = 4600.0;
    double defaultEndY = 800.0;
    String defaultFirstPtBoardingStop = "";
    String defaultLastPtEgressStop = "";
    Table trips = Table.create("trips").addColumns(
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

    public TripsBuilder(TemporaryFolder tmpDir) {
        this.tmpDir = tmpDir;
        this.legsBuilder = new LegsBuilder(tmpDir);
    }

    public TripsBuilder withTrip(
            String person, String trip_number, String trip_id, String dep_time, String trav_time, String wait_time,
            Integer traveled_distance, Integer euclidean_distance, String main_mode, String longest_distance_mode,
            String modes, String start_activity_type, String start_facility_id, String end_activity_type, String end_facility_id,
            String start_link, double start_x, double start_y, String end_link, double end_x, double end_y,
            String first_pt_boarding_stop, String last_pt_egress_stop
    ) {
        trips.stringColumn("person").append(person);
        trips.stringColumn("trip_number").append(trip_number);
        trips.stringColumn("trip_id").append(trip_id);
        trips.stringColumn("dep_time").append(dep_time);
        trips.stringColumn("trav_time").append(trav_time);
        trips.stringColumn("wait_time").append(wait_time);
        trips.intColumn("traveled_distance").append(traveled_distance);
        trips.intColumn("euclidean_distance").append(euclidean_distance);
        trips.stringColumn("main_mode").append(main_mode);
        trips.stringColumn("longest_distance_mode").append(longest_distance_mode);
        trips.stringColumn("modes").append(modes);
        trips.stringColumn("start_activity_type").append(start_activity_type);
        trips.stringColumn("start_facility_id").append(start_facility_id);
        trips.stringColumn("end_activity_type").append(end_activity_type);
        trips.stringColumn("end_facility_id").append(end_facility_id);
        trips.stringColumn("start_link").append(start_link);
        trips.doubleColumn("start_x").append(start_x);
        trips.doubleColumn("start_y").append(start_y);
        trips.stringColumn("end_link").append(end_link);
        trips.doubleColumn("end_x").append(end_x);
        trips.doubleColumn("end_y").append(end_y);
        trips.stringColumn("first_pt_boarding_stop").append(first_pt_boarding_stop);
        trips.stringColumn("last_pt_egress_stop").append(last_pt_egress_stop);

        this.legsBuilder.withLeg(
                person, trip_id, dep_time, trav_time, wait_time, traveled_distance, main_mode,
                start_link, start_x, start_y, end_link, end_x, end_y, first_pt_boarding_stop,
                last_pt_egress_stop, this.legsBuilder.defaultTransitLine, this.legsBuilder.defaultTransitRoute,
                this.legsBuilder.defaultVehicleId
        );
        return this;
    }

    public TripsBuilder withTripWithTravelTime(String person, String trip_number, String trav_time) {
        return this.withTrip(
                person, trip_number, String.format("{}_{}", person, trip_number), defaultDepTime, trav_time, defaultWaitTime,
                defaultTravelledDistance, defaultEuclideanDistance, defaultMainMode, defaultLongestDistanceMode, defaultModes,
                defaultStartActivityType, defaultStartFacilityId, defaultEndActivityType, defaultEndFacilityId,
                defaultStartLink, defaultStartX, defaultStartY, defaultEndLink, defaultEndX, defaultEndY,
                defaultFirstPtBoardingStop, defaultLastPtEgressStop
        );
    }

    public TripsBuilder withDefaultTrip() {
        return this.withTrip(
                defaultPerson, defaultTripNumber, defaultTripId, defaultDepTime, defaultTravTime, defaultWaitTime,
                defaultTravelledDistance, defaultEuclideanDistance, defaultMainMode, defaultLongestDistanceMode, defaultModes,
                defaultStartActivityType, defaultStartFacilityId, defaultEndActivityType, defaultEndFacilityId,
                defaultStartLink, defaultStartX, defaultStartY, defaultEndLink, defaultEndX, defaultEndY,
                defaultFirstPtBoardingStop, defaultLastPtEgressStop
        );
    }

    public LegsBuilder getLegsBuilder() {
        return legsBuilder;
    }

    public String build() {
        if (trips.isEmpty()) {
            // empty table gets into trouble reading all the columns, if the table is empty, it is assumed it's not
            // being used, so filling it with dud vales, just for the shape is ok
            this.withDefaultTrip();
        }
        String tripsPath = String.valueOf(Path.of(tmpDir.getRoot().getAbsolutePath(), "output_trips.csv"));
        CsvWriteOptions options = CsvWriteOptions.builder(tripsPath).separator(';').build();
        trips.write().usingOptions(options);
        return tripsPath;
    }
}
