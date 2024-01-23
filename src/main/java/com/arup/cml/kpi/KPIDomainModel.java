package com.arup.cml.kpi;

import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.numbers.NumberColumnFormatter;

import static tech.tablesaw.aggregate.AggregateFunctions.mean;
import static tech.tablesaw.aggregate.AggregateFunctions.sum;

public class KPIDomainModel {
    public DataModel dataModel;
    public String outputDir;

    public KPIDomainModel(DataModel dataModel, String outputDir) {
        this.dataModel = dataModel;
        this.outputDir = outputDir;
    }

    public Table ptWaitTime() {
        System.out.println("Computing KPI - PT Wait Time");
        Table legs = dataModel.getLegs();

        // pull out legs with stop waits
        legs = legs.where(
                legs.stringColumn("access_stop_id").isNotMissing()
        );

        // put in hour bins
        IntColumn wait_time_seconds = IntColumn.create("wait_time_seconds");
        legs.timeColumn("wait_time")
                .forEach(time -> wait_time_seconds.append(
                        time.toSecondOfDay()
                ));
        legs.addColumns(wait_time_seconds);

        // average wait by mode
        // ***** current req
//        Table intermediate =
//                legs
//                        .summarize("wait_time_seconds", mean)
//                        .by("mode")
//                        .setName("Average wait time at stops by mode");
//        intermediate.write().csv(String.format("%s/pt_wait_time.csv", outputDir));

        // put in hour bins
        StringColumn hour = StringColumn.create("hour");
        legs.timeColumn("dep_time")
                .forEach(time -> hour.append(
                        String.valueOf(time.getHour())
                ));
        legs.addColumns(hour);

        // ***** more balanced than req
        Table intermediate =
                legs
                        .summarize("wait_time_seconds", mean)
                        .by("mode", "access_stop_id", "hour")
                        .setName("Average wait time at stops by mode");
        intermediate.write().csv(String.format("%s/pt_wait_time.csv", outputDir));

        // kpi output
        // ***** more balanced than req
        Table kpi =
                legs
                        .where(legs.stringColumn("hour").asDoubleColumn().isGreaterThanOrEqualTo(7)
                                .and(legs.stringColumn("hour").asDoubleColumn().isLessThanOrEqualTo(9)))
                        .summarize("wait_time_seconds", mean)
                        .by("mode")
                        .setName("PT Wait Time");
        // TODO discuss this KPIs requirements / outputs
        // ***** current req
//        double kpi =
//                legs
//                        .where(legs.stringColumn("hour").asDoubleColumn().isGreaterThanOrEqualTo(7)
//                                .and(legs.stringColumn("hour").asDoubleColumn().isLessThanOrEqualTo(9)))
//                        .intColumn("wait_time_seconds")
//                        .mean();
        return kpi.setName("PT Wait Time");
    }

    public Table modalSplit() {
        System.out.println("Computing KPI - Modal Split");
        Table trips = dataModel.getTrips();

        // percentages of trips by dominant (by distance) modes
        Table kpi = trips.xTabPercents("longest_distance_mode");
        kpi.doubleColumn("Percents").setPrintFormatter(NumberColumnFormatter.percent(2));
        return kpi.setName("Modal Split");
    }

    public Table occupancyRate() {
        System.out.println("Computing KPI - Occupancy Rate");
        Table linkLog = dataModel.getLinkLog();
        Table vehicles = dataModel.getVehicles();

        // drop vehicles that aborted their journey
        linkLog = linkLog
                .dropWhere(
                        linkLog.doubleColumn("numberOfPeople").isEqualTo(-1)
                );

        // replace the network mode of the vehicle - PT vehicles default their network mode to car if not specified
        // also adds capacity of the vehicle
        linkLog.removeColumns("mode");
        linkLog = linkLog
                .joinOn("vehicleID")
                .inner(vehicles.selectColumns("vehicleID", "mode", "capacity"));
        // TODO add empty vehicles?

        long numberOfVehicles = linkLog.selectColumns("vehicleID").dropDuplicateRows().stream().count();

        // average by mode
        Table averageOccupancyPerMode =
                linkLog
                        .summarize("numberOfPeople", "capacity", mean)
                        .by("mode");
        averageOccupancyPerMode = averageOccupancyPerMode
                .joinOn("mode")
                .inner(linkLog
                        .selectColumns("vehicleID","mode")
                        .dropDuplicateRows()
                        .countBy("mode"));

        averageOccupancyPerMode.addColumns(
                averageOccupancyPerMode
                        .doubleColumn("Mean [numberOfPeople]")
                        .divide(averageOccupancyPerMode.doubleColumn("Mean [capacity]"))
                        .multiply(averageOccupancyPerMode.intColumn("Count"))
        );
        // current req - per mode computation
        double pm = averageOccupancyPerMode.doubleColumn("Mean [numberOfPeople] / Mean [capacity] * Count").sum();
        pm = pm / numberOfVehicles;

        // average by vehicle
        Table averageOccupancyPerVehicle =
                linkLog
                        .summarize("numberOfPeople", "capacity", mean)
                        .by("vehicleID");
        averageOccupancyPerVehicle.addColumns(
                averageOccupancyPerVehicle
                        .doubleColumn("Mean [numberOfPeople]")
                        .divide(averageOccupancyPerVehicle.doubleColumn("Mean [capacity]"))
        );
        double pv = averageOccupancyPerVehicle.doubleColumn("Mean [numberOfPeople] / Mean [capacity]").sum();
        pv = pv / numberOfVehicles;

        // TODO decide which approach this should be, get intermediate results too
        return averageOccupancyPerMode.setName("Occupancy Rate");
    }

    public double vehicleKM() {
        System.out.println("Computing KPI - Vehicle KM");
        Table linkLog = dataModel.getLinkLog();
        Table vehicles = dataModel.getVehicles();
        Table networkLinks = dataModel.getNetworkLinks();

        linkLog = linkLog
                .joinOn("linkID")
                .inner(networkLinks.selectColumns("linkID", "length"));

        Table kpi = linkLog
                .summarize("length", sum)
                .by("vehicleID")
                .setName("Vehicle KM");
        kpi.addColumns(
                kpi
                        .doubleColumn("Sum [length]")
                        .divide(100)
                        .setName("distance_km")
        );

        kpi = kpi
                .joinOn("vehicleID")
                .inner(vehicles.selectColumns("vehicleID", "mode", "PTLineID", "PTRouteID"));

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
//        Table intermediate = kpi
//                .summarize("distance_km", sum)
//                .by("mode");

        return kpi.doubleColumn("distance_km").sum();
    }

    public Table speed() {
        System.out.println("Computing KPI - Speed");
        return Table.create("Speed");
    }

    public Table GHG() {
        System.out.println("Computing KPI - GHG");
        return Table.create("GHG");
    }

    public Table congestion() {
        System.out.println("Computing KPI - Congestion");
        Table linkLog = dataModel.getLinkLog();
        Table networkLinks = dataModel.getNetworkLinks();

        // compute travel time on links
        linkLog.addColumns(
                linkLog.doubleColumn("endTime")
                        .subtract(linkLog.doubleColumn("startTime"))
                        .setName("travelTime")
        );

        // compute free flow time on links (length / freespeed)
        networkLinks.addColumns(
                networkLinks.doubleColumn("length")
                        .divide(networkLinks.doubleColumn("freespeed"))
                        .setName("freeFlowTime")
        );

        // add freeflow time to link log
        linkLog =
                linkLog
                        .joinOn("linkID")
                        .inner(networkLinks.selectColumns("linkID", "freeFlowTime"));

        // compute delay ratio
        linkLog.addColumns(
                linkLog.doubleColumn("travelTime")
                        .divide(linkLog.doubleColumn("freeFlowTime"))
                        .setName("delayRatio")
        );

        // put in hour bins
        StringColumn hour = StringColumn.create("hour");
        linkLog.doubleColumn("endTime")
                .forEach(time -> hour.append(
                        String.valueOf((int) Math.floor(time / (60 * 60)))
                ));
        linkLog.addColumns(hour);

        // intermediate output data
        Table intermediate =
                linkLog
                        .summarize("delayRatio", mean)
                        .by("linkID", "mode", "hour")
                        .setName("Average Delay Ratio on each link by mode and hour of the day");
        intermediate.write().csv(String.format("%s/congestion.csv", outputDir));

        // kpi output
        Table kpi =
                linkLog
                        .where(linkLog.stringColumn("hour").asDoubleColumn().isGreaterThanOrEqualTo(7)
                                .and(linkLog.stringColumn("hour").asDoubleColumn().isLessThanOrEqualTo(9)))
                        .summarize("delayRatio", mean)
                        .by("mode");
        kpi.write().csv(String.format("%s/kpi_congestion.csv", outputDir));

        return kpi.setName("Congestion");
    }
}
