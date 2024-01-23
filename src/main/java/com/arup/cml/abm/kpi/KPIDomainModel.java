package com.arup.cml.abm.kpi;

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
        IntColumn hour = IntColumn.create("hour");
        linkLog.doubleColumn("endTime")
                .forEach(time -> hour.append(
                        (int) Math.floor(time / (60 * 60))
                ));
        linkLog.addColumns(hour);

        // intermediate output data
        Table intermediate =
                linkLog
                    .summarize("delayRatio", mean)
                    .by("linkID", "mode", "hour");
        intermediate.write().csv(String.format("%s/congestion.csv", outputDir));

        // kpi output
        Table kpi =
                linkLog
                        .where(linkLog.intColumn("hour").isGreaterThanOrEqualTo(7)
                                .and(linkLog.intColumn("hour").isLessThanOrEqualTo(9)))
                        .summarize("delayRatio", mean)
                        .by("mode")
                        .setName("Congestion KPI");
        kpi.write().csv(String.format("%s/kpi.csv", outputDir));

        return kpi;
    }
}
