package com.arup.cml.abm.kpi.tablesaw;

import com.arup.cml.abm.kpi.KpiCalculator;
import com.arup.cml.abm.kpi.data.MoneyLog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.arup.cml.abm.kpi.data.LinkLog;
import com.arup.cml.abm.kpi.matsim.run.MatsimKpiGenerator;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.ControllerConfigGroup.CompressionType;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.Vehicles;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.io.csv.CsvWriteOptions;
import tech.tablesaw.selection.Selection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

import static tech.tablesaw.aggregate.AggregateFunctions.*;

public class TablesawKpiCalculator implements KpiCalculator {
    private static final Logger LOGGER = LogManager.getLogger(TablesawKpiCalculator.class);
    private Table legs;
    private Table trips;
    private final Table activities;
    private Table activityFacilities;
    private Table personModeScores;
    private Table networkLinks;
    private Table networkLinkModes;
    private Table scheduleStops;
    private Table scheduleRoutes;
    private Table vehicles;

    private Table linkLog;
    private Table linkLogVehicleOccupancy;
    private CompressionType compressionType;

    public TablesawKpiCalculator(Network network, TransitSchedule schedule, Vehicles vehicles, LinkLog linkLog,
                                 Population population, MoneyLog moneyLog, ScoringConfigGroup scoring,
                                 ActivityFacilities facilities, InputStream legsInputStream, InputStream tripsInputStream,
                                 Path outputDirectory, CompressionType compressionType) {
        this.compressionType = compressionType;
        // TODO: 24/01/2024 replace this ASAP with a representation of the network
        // that isn't from the MATSim API (a map, or dedicated domain object, or
        // whatever)
        createPeopleTables(population, scoring);
        createFacilitiesTable(facilities);
        this.legs = readLegs(legsInputStream, personModeScores, moneyLog);
        this.trips = readTrips(tripsInputStream, legs);
        this.activities = createActivitiesTable(trips);
        createNetworkLinkTables(network);
        createTransitTables(schedule);
        createVehicleTable(vehicles);
        createLinkLogTables(linkLog);
        writeIntermediateData(outputDirectory);
    }

    private Map<String, ColumnType> getLegsColumnMap () {
        Map<String, ColumnType> columnMapping = new HashMap<>();
        columnMapping.put("person", ColumnType.STRING);
        columnMapping.put("dep_time", ColumnType.STRING);
        columnMapping.put("trav_time", ColumnType.STRING);
        columnMapping.put("wait_time", ColumnType.STRING);
        return columnMapping;
    }

    private Map<String, ColumnType> getTripsColumnMap () {
        Map<String, ColumnType> columnMapping = getLegsColumnMap();
        columnMapping.put("first_pt_boarding_stop", ColumnType.STRING);
        columnMapping.put("start_facility_id", ColumnType.STRING);
        return columnMapping;
    }

    private Table readLegs(InputStream legsInputStream, Table personModeScores, MoneyLog moneyLog) {
        legs = readCSVInputStream(legsInputStream, getLegsColumnMap()).setName("Legs");
        legs = addCostToLegs(legs, personModeScores, moneyLog);
        return legs;
    }

    private Table readTrips(InputStream tripsInputStream, Table legs) {
        trips = readCSVInputStream(tripsInputStream, getTripsColumnMap()).setName("Trips");
        trips = fixFacilitiesInTripsTable(activityFacilities, trips);
        trips = addCostToTrips(legs, trips);
        return trips;
    }

    @Override
    public double writeAffordabilityKpi(Path outputDirectory) {
        LOGGER.info("Writing Affordability KPI to {}", outputDirectory);

        // join personal income / subpop info
        Table table = legs
                .joinOn("person", "mode")
                .inner(personModeScores.selectColumns("person", "mode", "income", "income_numeric", "subpopulation"));

        table = table
                .selectColumns("person", "income", "income_numeric", "subpopulation", "monetaryCostOfTravel")
                .setName("Monetary Travel Costs");

        // we decide which income column we should use, and what name is given to as the low income bracket
        String incomeColumnName = null;
        String lowIncomeName = null;
        if (table.column("income_numeric").countMissing() != table.column("income_numeric").size()) {
            // numeric income values present, so we assign income percentiles and use this new column
            double perc_25 = table.doubleColumn("income_numeric").percentile(25.0);
            double perc_50 = table.doubleColumn("income_numeric").percentile(50.0);
            double perc_75 = table.doubleColumn("income_numeric").percentile(75.0);
            incomeColumnName = "income_bracket";
            lowIncomeName = "25th percentile";
            StringColumn incomeBracket = StringColumn.create(incomeColumnName);
            String finalLowIncomeName = lowIncomeName;
            table.doubleColumn("income_numeric").forEach(new Consumer<Double>() {
                @Override
                public void accept(Double income) {
                    if (income.isNaN()) {
                        incomeBracket.appendMissing();
                    } else if (income <= perc_25) {
                        incomeBracket.append(finalLowIncomeName);
                    } else if (income <= perc_50) {
                        incomeBracket.append("26-50th percentile");
                    } else if (income <= perc_75) {
                        incomeBracket.append("51-75th percentile");
                    } else {
                        incomeBracket.append("75th+ percentile");
                    }
                }
            });
            table.addColumns(incomeBracket);
        } else {
            lowIncomeName = findStringWithSubstring(table.stringColumn("income"), "low");
            if (lowIncomeName != null) {
                incomeColumnName = "income";
            } else {
                lowIncomeName = findStringWithSubstring(table.stringColumn("subpopulation"), "low income");
                if (lowIncomeName != null) {
                    incomeColumnName = "subpopulation";
                }
            }
            if (lowIncomeName == null) {
                incomeColumnName = "subpopulation";
                LOGGER.warn("Low Income category was not found anywhere. You will only receive intermediate outputs " +
                        "for the Affordability Kpi and they will be grouped by subpopulation. Let's hope you " +
                        "configured something for this sim!");
            }
        }

        // total daily cost for each person
        table = table
                .summarize("monetaryCostOfTravel", sum)
                .by("person", incomeColumnName)
                .setName("Daily Monetary Travel Cost");

        // average across income bracket or subpopulation
        Table intermediate = table
                .summarize("Sum [monetaryCostOfTravel]", sum, mean)
                .by(incomeColumnName)
                .setName("Average Monetary Travel Cost by ...");
        intermediate.column("Sum [Sum [monetaryCostOfTravel]]").setName("total_daily_monetary_cost");
        intermediate.column("Mean [Sum [monetaryCostOfTravel]]").setName("mean_daily_monetary_cost");
        // append an overall cost result
        Table overallRow = intermediate.emptyCopy();
        overallRow.stringColumn(incomeColumnName).append("overall");
        overallRow.doubleColumn("total_daily_monetary_cost").append(
                intermediate.doubleColumn("total_daily_monetary_cost").sum());
        double overallAverageCost = intermediate.doubleColumn("total_daily_monetary_cost").sum() /
                table.column("person").size();
        overallRow.doubleColumn("mean_daily_monetary_cost").append(overallAverageCost);
        intermediate.append(overallRow);
        this.writeTableCompressed(intermediate, String.format("%s/intermediate-affordability.csv", outputDirectory), this.compressionType);

        if (lowIncomeName != null) {
            // average daily cost for agents in the low income bracket
            double lowIncomeAverageCost = intermediate
                    .where(intermediate.stringColumn(incomeColumnName).isEqualTo(lowIncomeName))
                    .doubleColumn("mean_daily_monetary_cost")
                    .get(0);
            double kpi = round(lowIncomeAverageCost / overallAverageCost, 2);
            writeContentToFile(String.format("%s/kpi-affordability.csv", outputDirectory), String.valueOf(kpi), this.compressionType);
            return kpi;
        }
        LOGGER.warn("We could not give you a KPI, check logs and intermediate output.");
        return -1.0;
    }

    private static String findStringWithSubstring(StringColumn col, String substring) {
        StringColumn values = col.unique();
        for (String value : values) {
            if (value.toUpperCase().contains(substring.toUpperCase())) {
                return value;
            }
        }
        return null;
    }

    @Override
    public void writePtWaitTimeKpi(Path outputDirectory) {
        LOGGER.info("Writing PT Wait Time KPI to {}", outputDirectory);

        // pull out legs with PT stops information
        Table table = legs.where(
                legs.column("access_stop_id").isNotMissing()
        );

        // convert H:M:S format to seconds
        IntColumn wait_time_seconds = IntColumn.create("wait_time_seconds");
        table.stringColumn("wait_time")
                .forEach(time -> wait_time_seconds.append(
                        (int) Time.parseTime(time)));
        table.addColumns(wait_time_seconds);

        // average wait by mode
        // ***** current req - average wait time by mode
//        Table intermediate =
//                table
//                        .summarize("wait_time_seconds", mean)
//                        .by("mode")
//                        .setName("Average wait time at stops by mode");
//        intermediate.write().csv(String.format("%s/pt_wait_time.csv", outputDir));

        // put in hour bins
        IntColumn hour = IntColumn.create("hour");
        table.column("dep_time")
                .forEach(time -> hour.append(
                        // MATSim departure times look like "09:03:04" - grab the hour value
                        Integer.parseInt(time.toString().split(":")[0])
                ));
        table.addColumns(hour);

        // ***** proposed intermediate output - average by mode, stop id and hour
        Table intermediate =
                table
                        .summarize("wait_time_seconds", mean)
                        .by("mode", "access_stop_id", "hour")
                        .setName("Average wait time at stops by mode");
        this.writeTableCompressed(intermediate, String.format("%s/intermediate-pt-wait-time.csv", outputDirectory), this.compressionType);

        // kpi output
        double kpi =
                table
                        .where(table.intColumn("hour").isGreaterThanOrEqualTo(8)
                                .and(table.intColumn("hour").isLessThan(10)))
                        .intColumn("wait_time_seconds")
                        .mean();
        kpi = round(kpi, 2);
        LOGGER.info("PT Wait Time KPI {}", kpi);
        writeContentToFile(String.format("%s/kpi-pt-wait-time.csv", outputDirectory), String.valueOf(kpi), this.compressionType);
    }

    @Override
    public void writeModalSplitKpi(Path outputDirectory) {
        LOGGER.info("Writing Modal Split KPI to {}", outputDirectory);

        // percentages of trips by dominant (by distance) modes
        Table kpi = trips.xTabPercents("longest_distance_mode");
        kpi.replaceColumn(
                round(
                        kpi.doubleColumn("Percents").multiply(100).setName("Percents"), 2));
        kpi.setName("Modal Split");
        this.writeTableCompressed(kpi, String.format("%s/kpi-modal-split.csv", outputDirectory), compressionType);
    }

    @Override
    public void writeOccupancyRateKpi(Path outputDirectory) {
        LOGGER.info("Writing Occupancy Rate KPI to {}", outputDirectory);

        // add capacity of the vehicle
        Table table = linkLog
                .joinOn("vehicleID")
                .inner(vehicles.selectColumns("vehicleID", "capacity"));

        // TODO include empty vehicles?
        long numberOfVehicles = table.selectColumns("vehicleID").dropDuplicateRows().stream().count();

        // average by vehicle
        Table averageOccupancyPerVehicle =
                table
                        .summarize("numberOfPeople", "capacity", mean)
                        .by("vehicleID")
                        .setName("Occupancy Rate");
        averageOccupancyPerVehicle.addColumns(
                averageOccupancyPerVehicle
                        .doubleColumn("Mean [numberOfPeople]")
                        .divide(averageOccupancyPerVehicle.doubleColumn("Mean [capacity]"))
        );
        Table intermediate = Table.create(
                averageOccupancyPerVehicle.stringColumn("vehicleID"),
                averageOccupancyPerVehicle.doubleColumn("Mean [numberOfPeople]"),
                averageOccupancyPerVehicle.doubleColumn("Mean [capacity]").setName("capacity"),
                round(averageOccupancyPerVehicle.doubleColumn("Mean [numberOfPeople] / Mean [capacity]"), 2)
                        .setName("Average occupancy rate")
        ).setName("Occupancy Rate");
        this.writeTableCompressed(intermediate, String.format("%s/intermediate-occupancy-rate.csv", outputDirectory), this.compressionType);

        double kpi = averageOccupancyPerVehicle.doubleColumn("Mean [numberOfPeople] / Mean [capacity]").sum();
        kpi = kpi / numberOfVehicles;
        kpi = round(kpi, 2);

        LOGGER.info("Occupancy Rate KPI {}", kpi);
        writeContentToFile(String.format("%s/kpi-occupancy-rate.csv", outputDirectory), String.valueOf(kpi), this.compressionType);
    }

    @Override
    public void writeVehicleKMKpi(Path outputDirectory) {
        LOGGER.info("Writing Vehicle KM KPI to {}", outputDirectory);

        // add link length to the link log table
        Table table = linkLog
                .joinOn("linkID")
                .inner(networkLinks.selectColumns("linkID", "length"));

        // get total km travelled for each vehicle
        table = table
                .summarize("length", sum)
                .by("vehicleID")
                .setName("Vehicle KM");
        table.addColumns(
                table
                        .doubleColumn("Sum [length]")
                        .divide(100)
                        .setName("distance_km")
        );

        Table intermediate = table
                .joinOn("vehicleID")
                .inner(vehicles.selectColumns("vehicleID", "mode"));
        intermediate.setName("Vehicle KM per vehicle");
        this.writeTableCompressed(intermediate, String.format("%s/intermediate-vehicle-km.csv", outputDirectory), this.compressionType);

        double kpi = round(table.doubleColumn("distance_km").sum(), 2);
        LOGGER.info("Vehicle KM KPI {}", kpi);
        writeContentToFile(String.format("%s/kpi-vehicle-km.csv", outputDirectory), String.valueOf(kpi), this.compressionType);
    }

    @Override
    public void writePassengerKMKpi(Path outputDirectory) {
        LOGGER.info("Writing Passenger KM KPI to {}", outputDirectory);
        // to get to a passenger-km metric the easiest way is to go through legs
        Table intermediate = trips
                .summarize("traveled_distance", sum)
                .by("person")
                .setName("Passenger KM per person");
        intermediate.column("Sum [traveled_distance]").setName("traveled_distance");
        intermediate.addColumns(
                intermediate.doubleColumn("traveled_distance")
                        .divide(1000)
                        .setName("traveled_distance_km")
        );
        this.writeTableCompressed(intermediate, String.format("%s/intermediate-passenger-km.csv", outputDirectory), this.compressionType);

        double kpi = round(trips.numberColumn("traveled_distance").divide(1000).sum(), 2);
        LOGGER.info("Passenger KM KPI: {} km", kpi);
        writeContentToFile(String.format("%s/kpi-passenger-km.csv", outputDirectory), String.valueOf(kpi), this.compressionType);
    }

    @Override
    public void writeSpeedKpi(Path outputDirectory) {
        LOGGER.info("Writing Speed KPI to {}", outputDirectory);
        networkLinks = sanitiseInfiniteColumnValuesInTable(networkLinks, networkLinks.doubleColumn("length"));

        // add length of links to log
        Table table =
                linkLog
                        .joinOn("linkID")
                        .inner(networkLinks.selectColumns("linkID", "length"));

        // compute time travelled
        table.addColumns(
                table.doubleColumn("endTime")
                        .subtract(table.doubleColumn("startTime"))
                        .setName("travelTime")
        );

        // compute speed
        table.addColumns(
                table.doubleColumn("length")
                        .divide(1000)
                        .divide(
                                table.doubleColumn("travelTime")
                                        .divide(60 * 60)
                        )
                        .setName("travelSpeedKMPH")
        );

        // put in hour bins
        IntColumn hour = IntColumn.create("hour");
        table.doubleColumn("endTime")
                .forEach(time -> hour.append(
                        (int) Math.floor(time / (60 * 60))
                ));
        table.addColumns(hour);

        // average travelSpeedKMPH by link (rows) and hour (columns)
        // TODO is it possible to order columns? atm sorted with integers as strings, not a timeline
        // TODO geojson output
        // TODO missing data results in empty result
        Table kpi = table
                .pivot("linkID", "hour", "travelSpeedKMPH", mean)
                .setName("Speed");
        for (NumericColumn<?> column : kpi.numericColumns()) {
            kpi.replaceColumn(round(column.asDoubleColumn(), 2));
        }
        this.writeTableCompressed(kpi, String.format("%s/kpi-speed.csv", outputDirectory), this.compressionType);
    }

    @Override
    public double writeGHGKpi(Path outputDirectory) {
        LOGGER.info("Writing GHG KPIs to {}", outputDirectory);

        // take car and bus link log events
        Table table = linkLog
                .where(linkLog.stringColumn("mode").isEqualTo("car")
                        .or(linkLog.stringColumn("mode").isEqualTo("bus")));

        // add link length to the link log table
        table = table
                .joinOn("linkID")
                .inner(networkLinks.selectColumns("linkID", "length"));
        table.addColumns(table.numberColumn("length").divide(1000).setName("distance_km"));

        // total distance by vehicle
        table = table.summarize("distance_km", sum).by("vehicleID");

        table = table
                .joinOn("vehicleID")
                .inner(vehicles.selectColumns("vehicleID", "emissionsFactor"));

        table.addColumns(table.numberColumn("Sum [distance_km]")
                .multiply(table.numberColumn("emissionsFactor"))
                .setName("emissions"));

        double emissionsTotal = round(table.numberColumn("emissions").sum(), 2);
        double emissionsPerCapita = round(emissionsTotal / personModeScores.column("person").size(), 2);
        writeContentToFile(
                String.format("%s/intermediate-ghg-emissions.csv", outputDirectory),
                String.format("emissions_total,emissions_per_capita\n%f,%f", emissionsTotal, emissionsPerCapita),
                this.compressionType);

        // TODO Add Scaling
        double kpi = emissionsPerCapita;
        writeContentToFile(String.format("%s/kpi-ghg-emissions.csv", outputDirectory), String.valueOf(kpi), this.compressionType);
        return kpi;
    }

    @Override
    public double writeTravelTime(Path outputDirectory) {
        // TODO update method name KPI missing
        LOGGER.info("Writing Travel Time KPI to {}", outputDirectory);

        // convert H:M:S format to seconds
        IntColumn trav_time_minutes = IntColumn.create("trav_time_minutes");
        trips.stringColumn("trav_time")
                .forEach(time -> trav_time_minutes.append(
                        (int) Math.round(Time.parseTime(time) / 60)));
        trips.addColumns(trav_time_minutes);

        Table intermediate =
                trips
                        .summarize("trav_time_minutes", mean)
                        .by("end_activity_type")
                        .setName("Travel Time by trip purpose");
        this.writeTableCompressed(intermediate, String.format("%s/intermediate-travel-time.csv", outputDirectory), this.compressionType);

        double kpi = trips.intColumn("trav_time_minutes").mean();
        writeContentToFile(String.format("%s/kpi-travel-time.csv", outputDirectory), String.valueOf(kpi), this.compressionType);
        return kpi;
    }

    @Override
    public Table writeAccessToMobilityServices(Path outputDirectory) {
        // TODO update method name KPI missing
        LOGGER.info("Writing Access To Mobility Services KPI to {}", outputDirectory);

        // get home locations for persons
        // todo persons table
        Table table = trips
                .where(trips.stringColumn("start_activity_type").isEqualTo("home"))
                .selectColumns("person", "start_activity_type", "start_x", "start_y", "first_pt_boarding_stop");
        table.column("start_activity_type").setName("location_type");
        table.column("start_x").setName("x");
        table.column("start_y").setName("y");
        BooleanColumn usedPtColumn = BooleanColumn.create("used_pt");
        table.stringColumn("first_pt_boarding_stop").forEach(new Consumer<String>() {
            @Override
            public void accept(String aString) {
                if (aString.isEmpty()) {
                    usedPtColumn.append(false);
                } else {
                    usedPtColumn.append(true);
                }
            }
        });
        table.addColumns(usedPtColumn);
        table.removeColumns(table.column("first_pt_boarding_stop"));
        table = table.dropDuplicateRows();

        // find out if they have access to pt
        table = addPTAccessColumnWithinDistance(
                table,
                scheduleStops.where(scheduleStops.stringColumn("mode").isEqualTo("bus")),
                400.0,
                "bus_access_400m"
        );
        // todo add subway
        table = addPTAccessColumnWithinDistance(
                table,
                scheduleStops.where(scheduleStops.stringColumn("mode").isEqualTo("rail")),
                800.0,
                "rail_access_800m"
        );
        this.writeTableCompressed(
                table,
                String.format("%s/intermediate-access-to-mobility-services.csv", outputDirectory),
                this.compressionType);

        // compute KPIs
        double bus_kpi = ((double) table.booleanColumn("bus_access_400m").countTrue() /
                table.booleanColumn("bus_access_400m").size())
                * 100;
        bus_kpi = round(bus_kpi, 2);
        writeContentToFile(String.format("%s/kpi-access-to-mobility-services-access-to-bus.csv", outputDirectory),
                String.valueOf(bus_kpi), this.compressionType);

        double rail_kpi = ((double) table.booleanColumn("rail_access_800m").countTrue() /
                table.booleanColumn("rail_access_800m").size())
                * 100;
        rail_kpi = round(rail_kpi, 2);
        writeContentToFile(String.format("%s/kpi-access-to-mobility-services-access-to-rail.csv", outputDirectory),
                String.valueOf(rail_kpi), this.compressionType);

        Selection ptAccess = table.booleanColumn("bus_access_400m").isTrue()
                .or(table.booleanColumn("rail_access_800m").isTrue());
        double used_pt_kpi = ((double) table.where(ptAccess.and(table.booleanColumn("used_pt").isTrue())
        ).rowCount() / table.rowCount())
                * 100;
        used_pt_kpi = round(used_pt_kpi, 2);
        writeContentToFile(String.format("%s/kpi-access-to-mobility-services-access-to-pt-and-pt-used.csv", outputDirectory),
                String.valueOf(used_pt_kpi), this.compressionType);

        return table;
    }

    private Table addPTAccessColumnWithinDistance(Table table, Table stops, double distance, String columnName) {
        table.addColumns(
                BooleanColumn.create(columnName,
                        Collections.nCopies(table.column("person").size(), false)));
        // to collect people with access, we remove them from table to not process them again
        Table trueTable = table.emptyCopy();
        for (Row stopRow : stops) {
            double x = stopRow.getNumber("x");
            double y = stopRow.getNumber("y");
            for (Row personRow : table) {
                double circleCalc = Math.pow(personRow.getNumber("x") - x, 2) + Math.pow(personRow.getNumber("y") - y, 2);
                if (circleCalc <= Math.pow(distance, 2)) {
                    // is within radius of 'distance'
                    personRow.setBoolean(columnName, true);
                    trueTable.append(personRow);
                    table = table.dropWhere(table.stringColumn("person").isEqualTo(personRow.getString("person")));
                }
            }
        }
        return table.append(trueTable);
    }

    @Override
    public Table writeCongestionKpi(Path outputDirectory) {
        LOGGER.info("Writing Congestion KPIs to {}", outputDirectory);

        // compute travel time on links
        Table table =
                linkLog.addColumns(
                        linkLog.doubleColumn("endTime")
                                .subtract(linkLog.doubleColumn("startTime"))
                                .setName("travelTime")
                );

        // compute free flow time on links (length / freespeed)
        Table sanitisedNetworkLinks = sanitiseInfiniteColumnValuesInTable(
                networkLinks, networkLinks.doubleColumn("freespeed"));
        sanitisedNetworkLinks.addColumns(
                sanitisedNetworkLinks.doubleColumn("length")
                        .divide(sanitisedNetworkLinks.doubleColumn("freespeed"))
                        .setName("freeFlowTime")
        );

        // add freeflow time to link log
        table =
                table
                        .joinOn("linkID")
                        .inner(sanitisedNetworkLinks.selectColumns("linkID", "freeFlowTime"));

        // compute delay ratio
        table.addColumns(
                table.doubleColumn("travelTime")
                        .divide(table.doubleColumn("freeFlowTime"))
                        .setName("delayRatio")
        );

        // put in hour bins
        IntColumn hour = IntColumn.create("hour");
        table.doubleColumn("endTime")
                .forEach(time -> hour.append(
                        (int) Math.floor(time / (60 * 60))
                ));
        table.addColumns(hour);

        // intermediate output data
        Table intermediate =
                table
                        .summarize("delayRatio", mean)
                        .by("linkID", "mode", "hour");
        this.writeTableCompressed(intermediate, String.format("%s/intermediate-congestion.csv", outputDirectory), this.compressionType);

        // kpi output
        Table kpi =
                table
                        .where(table.intColumn("hour").isGreaterThanOrEqualTo(8)
                                .and(table.intColumn("hour").isLessThan(10)))
                        .summarize("delayRatio", mean)
                        .by("mode")
                        .setName("Congestion KPI");
        kpi.replaceColumn(round(kpi.doubleColumn("Mean [delayRatio]"), 2));
        this.writeTableCompressed(kpi, String.format("%s/kpi-congestion.csv", outputDirectory), compressionType);
        return kpi;
    }

    @Override
    public double writeMobilitySpaceUsage(Path outputDirectory) {
        // TODO: implement KPI
        LOGGER.info("Writing Mobility Space Usage KPI to {}", outputDirectory);

        Table carActivities = activities
                .where(activities.stringColumn("access_mode").isEqualTo("car")
                        .or(activities.stringColumn("egress_mode").isEqualTo("car")));

        Table peopleInFacilities = carActivities
                .summarize("person", countUnique)
                .by("facility_id", "activity_type");
        Table tripsToFacilities = carActivities
                .summarize("access_trip_id", countNonMissing)
                .by("facility_id", "activity_type");

        Table intermediate = peopleInFacilities
                .joinOn("facility_id", "activity_type")
                .inner(tripsToFacilities)
                .setName("Mobility Space Usage");
        intermediate.column("Count Unique [person]").setName("max_occupancy");
        intermediate.column("Count [access_trip_id]").setName("total_trips");

        // calculate parking space demand with parking factor: 11.5
        // https://www.interparking-france.com/en/what-are-the-dimensions-of-a-parking-space/
        intermediate.addColumns(
                intermediate.numberColumn("max_occupancy")
                        .multiply(11.5)
                        .setName("parking_space_demand")
        );
        this.writeTableCompressed(intermediate, String.format("%s/intermediate-mobility-space-usage.csv", outputDirectory), this.compressionType);

        // compute kpi number one, demand by activity type
        Table kpi = intermediate
                .summarize("parking_space_demand", "total_trips", sum)
                .by("activity_type")
                .setName("Mobility Space Usage");
        kpi.column("Sum [parking_space_demand]").setName("parking_space_demand");
        kpi.column("Sum [total_trips]").setName("total_trips");

        kpi.addColumns(
                kpi.numberColumn("parking_space_demand")
                        .multiply(kpi.numberColumn("total_trips")
                                .divide(kpi.numberColumn("total_trips").sum()))
                        .setName("weighted_demand")
        );
        this.writeTableCompressed(intermediate, String.format("%s/kpi-mobility-space-usage-per-activity-type.csv", outputDirectory), this.compressionType);

        double finalKpi = kpi.numberColumn("parking_space_demand").sum()
                / personModeScores.column("person").size();
        // TODO Add Scaling
        finalKpi = round(finalKpi, 2);
        writeContentToFile(String.format("%s/kpi-mobility-space-usage.csv", outputDirectory), String.valueOf(finalKpi), this.compressionType);
        return finalKpi;
    }

    private DoubleColumn round(DoubleColumn column, int decimalPoints) {
        DoubleColumn roundedColumn = DoubleColumn.create(column.name());
        column.forEach(new Consumer<Double>() {
            @Override
            public void accept(Double aDouble) {
                if (aDouble.isNaN()) {
                    roundedColumn.appendMissing();
                } else {
                    roundedColumn.append(Math.round(aDouble * Math.pow(10.0, decimalPoints)) / Math.pow(10.0, decimalPoints));
                }
            }
        });
        return roundedColumn;
    }

    private double round(double number, int decimalPoints) {
        return Math.round(number * Math.pow(10.0, decimalPoints)) / Math.pow(10.0, decimalPoints);
    }

    private Table sanitiseInfiniteColumnValuesInTable(Table table, DoubleColumn column) {
        Table infiniteValuesTable = table.where(column.eval(Double::isInfinite));
        if (!infiniteValuesTable.isEmpty()) {
            LOGGER.warn(("Table: `%s` has %d row(s) affected by infinite values in column: `%s`. " +
                    "These rows will be dropped for this calculation.")
                    .formatted(table.name(), infiniteValuesTable.rowCount(), column.name()));
            return table.dropWhere(column.eval(Double::isInfinite));
        } else {
            return table;
        }
    }

    private void createFacilitiesTable(ActivityFacilities facilities) {
        LOGGER.info("Creating Facilities Table");
        StringColumn facilityIDColumn = StringColumn.create("facilityID");
        StringColumn linkIDColumn = StringColumn.create("linkID");
        DoubleColumn xColumn = DoubleColumn.create("x");
        DoubleColumn yColumn = DoubleColumn.create("y");
        StringColumn activityTypeColumn = StringColumn.create("activityType");

        facilities.getFacilities().forEach((facilityId, activityFacility) -> {
            activityFacility.getActivityOptions().forEach((activityName, activityOption) -> {
                facilityIDColumn.append(facilityId.toString());
                linkIDColumn.append(activityFacility.getLinkId().toString());
                xColumn.append(activityFacility.getCoord().getX());
                yColumn.append(activityFacility.getCoord().getY());
                activityTypeColumn.append(activityOption.getType());
            });
        });

        this.activityFacilities = Table.create("Activity Facilities")
                .addColumns(
                        facilityIDColumn,
                        linkIDColumn,
                        xColumn,
                        yColumn,
                        activityTypeColumn
                );
    }

    private Table fixFacilitiesInTripsTable(Table activityFacilities, Table trips) {
        if (trips.column("start_facility_id").countMissing() != 0) {
            LOGGER.warn("`start_facility_id` column in `trips` table has missing values. We will try to fix that now.");
            trips.removeColumns("start_facility_id");
            Table startActivityTable = activityFacilities.selectColumns("linkID", "activityType", "facilityID");
            startActivityTable.column("linkID").setName("start_link");
            startActivityTable.column("activityType").setName("start_activity_type");
            startActivityTable.column("facilityID").setName("start_facility_id");
            trips = trips
                    .joinOn("start_link", "start_activity_type")
                    .inner(startActivityTable);
        }
        if (trips.column("end_facility_id").countMissing() != 0) {
            LOGGER.warn("`end_facility_id` column in `trips` table has missing values. We will try to fix that now.");
            trips.removeColumns("end_facility_id");
            Table endActivityTable = activityFacilities.selectColumns("linkID", "activityType", "facilityID");
            endActivityTable.column("linkID").setName("end_link");
            endActivityTable.column("activityType").setName("end_activity_type");
            endActivityTable.column("facilityID").setName("end_facility_id");
            trips = trips
                    .joinOn("end_link", "end_activity_type")
                    .inner(endActivityTable);
        }
        return trips;
    }

    private Table addCostToLegs(Table legs, Table personModeScores, MoneyLog moneyLog) {
        // Add Costs to Legs
        // join personal monetary costs, constant and per distance unit
        legs = legs
                .joinOn("person", "mode")
                .inner(personModeScores
                        .selectColumns("person", "mode", "monetaryDistanceRate", "dailyMonetaryConstant"));

        // compute monetary cost for each leg from scoring params
        legs.addColumns(
                legs.intColumn("distance")
                        .multiply(legs.doubleColumn("monetaryDistanceRate"))
                        .add(legs.doubleColumn("dailyMonetaryConstant"))
                        .abs()
                        .setName("monetaryCostOfTravel"));
        legs.removeColumns("monetaryDistanceRate", "dailyMonetaryConstant");

        // add contribution from person money events
        // first create time columns in seconds
        DoubleColumn dep_time_seconds = DoubleColumn.create("dep_time_seconds");
        DoubleColumn trav_time_seconds = DoubleColumn.create("trav_time_seconds");
        legs.stringColumn("dep_time")
                .forEach(time -> dep_time_seconds.append(
                        (int) Time.parseTime(time)));
        legs.stringColumn("trav_time")
                .forEach(time -> trav_time_seconds.append(
                        (int) Time.parseTime(time)));
        DoubleColumn arr_time_seconds = dep_time_seconds.add(trav_time_seconds).setName("arr_time_seconds");
        legs.addColumns(dep_time_seconds, arr_time_seconds);
        for (String person : moneyLog.getMoneyLogData().keySet()) {
            for (Map.Entry<Double, Double> costEntry : moneyLog.getMoneyLogData(person).entrySet()) {
                Double time = costEntry.getKey();
                Double cost = costEntry.getValue();
                legs.doubleColumn("monetaryCostOfTravel").set(
                        legs.stringColumn("person").isEqualTo(person)
                                .and(legs.doubleColumn("dep_time_seconds").isLessThan(time)
                                        .and(legs.doubleColumn("arr_time_seconds").isGreaterThanOrEqualTo(time))),
                        legs.doubleColumn("monetaryCostOfTravel").add(cost)
                );
            }
        }
        legs.removeColumns(dep_time_seconds, arr_time_seconds);
        return legs;
    }

    private Table addCostToTrips(Table legs, Table trips) {
        if (!legs.columnNames().contains("monetaryCostOfTravel")) {
            throw new RuntimeException("Add costs to legs before attempting to add them to trips");
        }

        Table combinedTripCost = legs
                .summarize("monetaryCostOfTravel", sum)
                .by("trip_id");
        combinedTripCost.column("Sum [monetaryCostOfTravel]").setName("monetaryCostOfTravel");

        // Add Costs to Trips
        trips = trips
                .joinOn("trip_id")
                .inner(combinedTripCost);

        return trips;
    }

    private Table createActivitiesTable(Table trips) {
        LOGGER.info("Creating Activities Table");
        Table activities = Table.create("Activities")
                .addColumns(
                        StringColumn.create("person"),
                        StringColumn.create("activity_type"),
                        StringColumn.create("facility_id"),
                        StringColumn.create("access_mode"),
                        StringColumn.create("egress_mode"),
                        StringColumn.create("start_time"),
                        StringColumn.create("end_time"),
                        StringColumn.create("access_trip_id"),
                        StringColumn.create("egress_trip_id")
                );

        for (String person : trips.stringColumn("person").unique()) {
            Table personTrips = trips
                    .where(trips.stringColumn("person").isEqualTo(person))
                    .sortAscendingOn("trip_number");
            Table personActivities = activities.emptyCopy();
            for (int i = 0; i < personTrips.rowCount(); i++) {
                Row thisTrip = personTrips.row(i);
                personActivities.stringColumn("person").append(person);
                personActivities.stringColumn("activity_type").append(thisTrip.getString("start_activity_type"));
                personActivities.stringColumn("facility_id").append(thisTrip.getString("start_facility_id"));
                personActivities.stringColumn("egress_mode").append(thisTrip.getString("longest_distance_mode"));
                personActivities.stringColumn("end_time").append(thisTrip.getString("dep_time"));
                personActivities.stringColumn("egress_trip_id").append(thisTrip.getString("trip_id"));
                // access mode and start time comes from the previous trip
                if (i == 0) {
                    personActivities.stringColumn("access_mode").appendMissing();
                    personActivities.stringColumn("start_time").appendMissing();
                    personActivities.stringColumn("access_trip_id").appendMissing();
                } else {
                    Row previousTrip = personTrips.row(i - 1);
                    personActivities.stringColumn("access_mode").append(previousTrip.getString("longest_distance_mode"));
                    personActivities.stringColumn("access_trip_id").append(previousTrip.getString("trip_id"));
                    int arrivalTime = (int) (Time.parseTime(previousTrip.getString("dep_time"))
                            + Time.parseTime(previousTrip.getString("trav_time")));
                    personActivities.stringColumn("start_time").append(integerToStringDate(arrivalTime));
                }
            }
            // last row of activities table
            Row lastTrip = personTrips.row(personTrips.rowCount() - 1);
            personActivities.stringColumn("person").append(person);
            personActivities.stringColumn("activity_type").append(lastTrip.getString("end_activity_type"));
            personActivities.stringColumn("facility_id").append(lastTrip.getString("end_facility_id"));
            personActivities.stringColumn("access_mode").append(lastTrip.getString("longest_distance_mode"));
            personActivities.stringColumn("access_trip_id").append(lastTrip.getString("trip_id"));
            int arrivalTime = (int) (Time.parseTime(lastTrip.getString("dep_time"))
                    + Time.parseTime(lastTrip.getString("trav_time")));
            personActivities.stringColumn("start_time").append(integerToStringDate(arrivalTime));
            personActivities.stringColumn("egress_mode").appendMissing();
            personActivities.stringColumn("end_time").appendMissing();
            personActivities.stringColumn("egress_trip_id").appendMissing();

            // finally append the activities of this person
            activities.append(personActivities);
        }

        return activities;
    }

    private String integerToStringDate(int time) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.of("UTC"));
        Instant instant = Instant.ofEpochMilli((long) (time * 1000));
        return formatter.format(instant);
    }

    private void createPeopleTables(Population population, ScoringConfigGroup scoring) {
        LOGGER.info("Creating Population Mode Scoring Table");

        StringColumn personIDColumn = StringColumn.create("person");
        StringColumn incomeColumn = StringColumn.create("income");
        StringColumn subpopulationColumn = StringColumn.create("subpopulation");
        StringColumn modeColumn = StringColumn.create("mode");
        DoubleColumn monetaryDistanceRateColumn = DoubleColumn.create("monetaryDistanceRate");
        DoubleColumn dailyMonetaryConstantColumn = DoubleColumn.create("dailyMonetaryConstant");

        for (Person person : population.getPersons().values()) {
            ScoringConfigGroup.ScoringParameterSet scoringParams = scoring.getScoringParameters(PopulationUtils.getSubpopulation(person));
            for (ScoringConfigGroup.ModeParams modeParams : scoringParams.getModes().values()) {
                personIDColumn.append(person.getId().toString());
                incomeColumn.append(
                        person.getAttributes().getAsMap().getOrDefault("income", "Unknown").toString());
                subpopulationColumn.append(PopulationUtils.getSubpopulation(person));
                modeColumn.append(modeParams.getMode());
                monetaryDistanceRateColumn.append(modeParams.getMonetaryDistanceRate());
                dailyMonetaryConstantColumn.append(modeParams.getDailyMonetaryConstant());
            }
        }

        personModeScores = Table.create("Person Mode Scoring Parameters")
                .addColumns(
                        personIDColumn,
                        incomeColumn,
                        subpopulationColumn,
                        modeColumn,
                        monetaryDistanceRateColumn,
                        dailyMonetaryConstantColumn
                );

        // attempt to parse income to a numeric column
        DoubleColumn incomeNumeric = DoubleColumn.create("income_numeric");
        personModeScores.stringColumn("income").forEach(new Consumer<String>() {
            @Override
            public void accept(String aString) {
                if (aString.matches("([0-9]*[.])?[0-9]+")) {
                    incomeNumeric.append(Double.parseDouble(aString));
                } else {
                    incomeNumeric.appendMissing();
                }
            }
        });
        personModeScores.addColumns(incomeNumeric);
    }

    private void createNetworkLinkTables(Network network) {
        LOGGER.info("Creating Network Link Tables");

        // Network Links Table Columns
        StringColumn linkIDColumn = StringColumn.create("linkID");
        StringColumn fromNodeColumn = StringColumn.create("fromNode");
        StringColumn toNodeColumn = StringColumn.create("toNode");
        DoubleColumn freespeedColumn = DoubleColumn.create("freespeed");
        DoubleColumn capacityColumn = DoubleColumn.create("capacity");
        DoubleColumn lengthColumn = DoubleColumn.create("length");
        DoubleColumn lanesColumn = DoubleColumn.create("lanes");

        // Columns for the Modes Table
        // This Table supports the main Network Links Table
        ArrayList<String> modesLinkIDColumn = new ArrayList<>();
        ArrayList<String> modesColumn = new ArrayList<>();

        network.getLinks().forEach((id, link) -> {
            String linkId = id.toString();

            // Network Links Table data
            linkIDColumn.append(linkId);
            fromNodeColumn.append(link.getFromNode().getId().toString());
            toNodeColumn.append(link.getToNode().getId().toString());
            freespeedColumn.append(link.getFreespeed());
            capacityColumn.append(link.getCapacity());
            lengthColumn.append(link.getLength());
            lanesColumn.append(link.getNumberOfLanes());

            // Modes Table data
            for (String mode : link.getAllowedModes()) {
                modesLinkIDColumn.add(linkId);
                modesColumn.add(mode);
            }
        });

        networkLinks = Table.create("Network Links")
                .addColumns(
                        linkIDColumn,
                        fromNodeColumn,
                        toNodeColumn,
                        freespeedColumn,
                        capacityColumn,
                        lengthColumn,
                        lanesColumn
                );

        networkLinkModes = Table.create("Network Link Modes")
                .addColumns(
                        StringColumn.create("linkID", modesLinkIDColumn),
                        StringColumn.create("mode", modesColumn)
                );
    }

    private void createTransitTables(TransitSchedule schedule) {
        LOGGER.info("Creating Transit Tables");
        LOGGER.info("Creating Schedule Stop Table");
        // Schedule Stop Table Columns
        StringColumn stopIDColumn = StringColumn.create("stopID");
        DoubleColumn xColumn = DoubleColumn.create("x");
        DoubleColumn yColumn = DoubleColumn.create("y");
        StringColumn nameColumn = StringColumn.create("name");
        StringColumn linkIdColumn = StringColumn.create("linkID");
        BooleanColumn isBlockingColumn = BooleanColumn.create("isBlocking");

        schedule.getFacilities().forEach((id, stop) -> {
            String stopId = id.toString();

            // Schedule Stop Table data
            stopIDColumn.append(stopId);
            xColumn.append(stop.getCoord().getX());
            yColumn.append(stop.getCoord().getY());
            nameColumn.append(stop.getName());
            linkIdColumn.append(stop.getLinkId().toString());
            isBlockingColumn.append(stop.getIsBlockingLane());
        });

        scheduleStops = Table.create("Schedule Stops")
                .addColumns(
                        stopIDColumn,
                        xColumn,
                        yColumn,
                        nameColumn,
                        linkIdColumn,
                        isBlockingColumn
                );

        StringColumn lineIDColumn = StringColumn.create("transitLineID");
        StringColumn routeIDColumn = StringColumn.create("routeID");
        StringColumn modeColumn = StringColumn.create("mode");

        // for adding modes to stops
        StringColumn stopIDModeColumn = StringColumn.create("stopID");
        StringColumn modeModeColumn = StringColumn.create("mode");

        LOGGER.info("Creating Schedule Transit Tables");
        schedule.getTransitLines().forEach((lineId, transitLine) -> {
            transitLine.getRoutes().forEach((routeId, route) -> {
                lineIDColumn.append(lineId.toString());
                routeIDColumn.append(routeId.toString());
                modeColumn.append(route.getTransportMode());
                route.getStops().forEach((stop) -> {
                    stopIDModeColumn.append(stop.getStopFacility().getId().toString());
                    modeModeColumn.append(route.getTransportMode());
                });
            });
        });
        Table tmpStopModeTable = Table.create("Stop Modes").addColumns(
                stopIDModeColumn,
                modeModeColumn
        ).dropDuplicateRows();
        scheduleStops = scheduleStops.joinOn("stopID").inner(tmpStopModeTable);

        scheduleRoutes = Table.create("Schedule Routes")
                .addColumns(
                        lineIDColumn,
                        routeIDColumn,
                        modeColumn
                );
    }

    private void createVehicleTable(Vehicles inputVehicles) {
        LOGGER.info("Creating Vehicle Table");
        StringColumn vehicleIDColumn = StringColumn.create("vehicleID");
        StringColumn modeColumn = StringColumn.create("mode");
        IntColumn capacityColumn = IntColumn.create("capacity");
        StringColumn fuelTypeColumn = StringColumn.create("fuelType");
        DoubleColumn emissionsFactorColumn = DoubleColumn.create("emissionsFactor");
        StringColumn ptLineIDColumn = StringColumn.create("PTLineID");
        StringColumn ptRouteIDColumn = StringColumn.create("PTRouteID");

        inputVehicles.getVehicles().forEach((id, vehicle) -> {
            vehicleIDColumn.append(id.toString());
            modeColumn.append(vehicle.getType().getNetworkMode());
            capacityColumn.append(
                    vehicle.getType().getCapacity().getSeats() + vehicle.getType().getCapacity().getStandingRoom()
            );
            appendAttributeValueOrMissing(vehicle.getType().getEngineInformation().getAttributes(), "fuelType", fuelTypeColumn);
            appendAttributeValueOrMissing(vehicle.getType().getEngineInformation().getAttributes(), "emissionsFactor", emissionsFactorColumn);
            appendAttributeValueOrMissing(vehicle.getAttributes(), "PTLineID", ptLineIDColumn);
            appendAttributeValueOrMissing(vehicle.getAttributes(), "PTRouteID", ptRouteIDColumn);
        });

        vehicles = Table.create("Vehicles")
                .addColumns(
                        vehicleIDColumn,
                        modeColumn,
                        capacityColumn,
                        fuelTypeColumn,
                        emissionsFactorColumn,
                        ptLineIDColumn,
                        ptRouteIDColumn
                );
    }

    private static void appendAttributeValueOrMissing(Attributes attributes, String attributeName, Column column) {
        Object attributeValue = attributes.getAttribute(attributeName);
        if (attributeValue != null) {
            column.append(attributeValue);
        } else {
            column.appendMissing();
        }
    }

    private void createLinkLogTables(LinkLog _linkLog) {
        LOGGER.info("Creating Link Log Table");
        LongColumn indexColumn = LongColumn.create("index");
        StringColumn linkIDColumn = StringColumn.create("linkID");
        StringColumn vehicleIDColumn = StringColumn.create("vehicleID");
        StringColumn modeColumn = StringColumn.create("initialMode");
        DoubleColumn startTimeColumn = DoubleColumn.create("startTime");
        DoubleColumn endTimeColumn = DoubleColumn.create("endTime");
        IntColumn numberOfPeopleColumn = IntColumn.create("numberOfPeople");

        int missingValues = 0;
        for (Map.Entry<Long, Map<String, Object>> entry : _linkLog.getLinkLogData().rowMap().entrySet()) {
            indexColumn.append(entry.getKey());
            linkIDColumn.append(entry.getValue().get("linkID").toString());
            vehicleIDColumn.append(entry.getValue().get("vehicleID").toString());
            modeColumn.append(entry.getValue().get("mode").toString());
            startTimeColumn.append((Double) entry.getValue().get("startTime"));
            if (entry.getValue().containsKey("endTime")) {
                endTimeColumn.append((Double) entry.getValue().get("endTime"));
                numberOfPeopleColumn.append((Integer) entry.getValue().get("numberOfPeople"));
            } else {
                endTimeColumn.append(-1);
                numberOfPeopleColumn.append(0);
                if (missingValues == 0) {
                    LOGGER.warn("A missing `endTime` was encountered. This message is shown only once.");
                }
                missingValues++;
            }
        }

        linkLog = Table.create("Link Log")
                .addColumns(
                        indexColumn,
                        linkIDColumn,
                        vehicleIDColumn,
                        modeColumn,
                        startTimeColumn,
                        endTimeColumn,
                        numberOfPeopleColumn
                );
        if (missingValues > 0) {
            LOGGER.warn("{} missing `endTime` data points were encountered - some vehicles " +
                            "were stuck and did not complete their journey. These Link Log entries will be deleted.",
                    missingValues);
            linkLog = linkLog.where(linkLog.doubleColumn("endTime").isNotEqualTo(-1));
        }

        // fix vehicle modes with vehicle table
        linkLog = linkLog
                .joinOn("vehicleID")
                .leftOuter(vehicles.selectColumns("vehicleID", "mode"));
        int mismatchedModes = linkLog.where(
                linkLog.stringColumn("initialMode")
                        .isNotEqualTo(linkLog.stringColumn("mode")
                        )
        ).stringColumn("vehicleID").countUnique();
        if (mismatchedModes > 0) {
            LOGGER.warn(String.format(
                    "There are %d vehicles that have different modes to the ones found in the Link Log. " +
                            "The modes in the Link Log will be updated with the modes from the Vehicle Table.",
                    mismatchedModes));
        }
        linkLog.removeColumns("initialMode");

        LOGGER.info("Creating Link Log Vehicle Occupancy Table");
        LongColumn linkLogIndexColumn = LongColumn.create("linkLogIndex");
        StringColumn agentIDColumn = StringColumn.create("agentId");

        for (Map.Entry<Long, Map<String, Object>> entry : _linkLog.getVehicleOccupantsData().rowMap()
                .entrySet()) {
            linkLogIndexColumn.append((long) entry.getValue().get("linkLogIndex"));
            agentIDColumn.append(entry.getValue().get("agentId").toString());
        }
        linkLogVehicleOccupancy = Table.create("Vehicle Occupancy")
                .addColumns(
                        linkLogIndexColumn,
                        agentIDColumn
                );
    }

    public Table readCSVInputStream(InputStream inputStream) {
        return readCSVInputStream(inputStream, Collections.emptyMap());
    }

    public Table readCSVInputStream(InputStream inputStream, Map<String, ColumnType> columnMapping) {
        // TODO Make separator accessible from outside
        CsvReadOptions.Builder builder = CsvReadOptions.builder(inputStream).separator(';').header(true)
                .columnTypesPartial(column -> {
                    if (columnMapping.keySet().contains(column)) {
                        return Optional.of(columnMapping.get(column));
                    }
                    return Optional.empty();
                });
        return Table.read().usingOptions(builder.build());
    }

    private void writeContentToFile(String path, String content, CompressionType compressionType) {
        try (Writer wr = IOUtils.getBufferedWriter(path.concat(compressionType.fileEnding))) {
            wr.write(content);
        } catch (IOException e) {
            LOGGER.warn("Failed to save content `{}` to file: `{}`", content, path);
        }
    }

    private void writeIntermediateData(Path outputDir) {
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }

        this.writeTableCompressed(legs, String.format("%s/supporting-data-legs.csv", outputDir), this.compressionType);
        this.writeTableCompressed(trips, String.format("%s/supporting-data-trips.csv", outputDir), this.compressionType);
        this.writeTableCompressed(activityFacilities, String.format("%s/supporting-data-activity-facilities.csv", outputDir), this.compressionType);
        this.writeTableCompressed(activityFacilities, String.format("%s/supporting-data-activities.csv", outputDir), this.compressionType);
        this.writeTableCompressed(personModeScores, String.format("%s/supporting-data-person-mode-score-parameters.csv", outputDir), this.compressionType);
        this.writeTableCompressed(linkLog, String.format("%s/supporting-data-linkLog.csv", outputDir), this.compressionType);
        this.writeTableCompressed(linkLogVehicleOccupancy, String.format("%s/supporting-data-vehicleOccupancy.csv", outputDir), this.compressionType);
        this.writeTableCompressed(networkLinks, String.format("%s/supporting-data-networkLinks.csv", outputDir), this.compressionType);
        this.writeTableCompressed(networkLinkModes, String.format("%s/supporting-data-networkLinkModes.csv", outputDir), this.compressionType);
        this.writeTableCompressed(scheduleStops, String.format("%s/supporting-data-scheduleStops.csv", outputDir), this.compressionType);
        this.writeTableCompressed(scheduleRoutes, String.format("%s/supporting-data-scheduleRoutes.csv", outputDir), this.compressionType);
        this.writeTableCompressed(vehicles, String.format("%s/supporting-data-vehicles.csv", outputDir), this.compressionType);
    }

    private OutputStream getCompressedOutputStream(String filepath, CompressionType compressionType) {
        return IOUtils.getOutputStream(IOUtils.getFileUrl(filepath.concat(compressionType.fileEnding)),
                false);
    }

    private void writeTableCompressed(Table table, String filePath, CompressionType compressionType) {
        try (OutputStream stream = getCompressedOutputStream(filePath, compressionType)) {
            CsvWriteOptions options = CsvWriteOptions.builder(stream).lineEnd(MatsimKpiGenerator.EOL).build();
            table.write().csv(options);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
