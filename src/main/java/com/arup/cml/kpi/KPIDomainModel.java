package com.arup.cml.kpi;

import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import static tech.tablesaw.aggregate.AggregateFunctions.mean;

public class KPIDomainModel {
    public DataModel dataModel;
    public String outputDir;

    public KPIDomainModel(DataModel dataModel, String outputDir) {
        this.dataModel = dataModel;
        this.outputDir = outputDir;
    }

    public Table ptWaitTime() {
        System.out.println("Computing - PT Wait Time KPI");
        Table legs = dataModel.getLegs();
        Table scheduleStops = dataModel.getScheduleStops();

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
                        .setName("PT Wait Time KPI");
        // ***** current req
//        double kpi =
//                legs
//                        .where(legs.stringColumn("hour").asDoubleColumn().isGreaterThanOrEqualTo(7)
//                                .and(legs.stringColumn("hour").asDoubleColumn().isLessThanOrEqualTo(9)))
//                        .intColumn("wait_time_seconds")
//                        .mean();
        return kpi;
    }

    public Table congestion() {
        System.out.println("Computing - Congestion KPI");
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
                        .by("mode")
                        .setName("Congestion KPI");
        kpi.write().csv(String.format("%s/kpi_congestion.csv", outputDir));

        return kpi;
    }
}
