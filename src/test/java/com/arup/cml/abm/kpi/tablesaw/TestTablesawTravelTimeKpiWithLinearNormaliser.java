package com.arup.cml.abm.kpi.tablesaw;

import com.arup.cml.abm.kpi.LinearNormaliser;
import com.arup.cml.abm.kpi.Normaliser;
import com.arup.cml.abm.kpi.builders.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestTablesawTravelTimeKpiWithLinearNormaliser {
    // the scale interval is not the one proposed for this KPI. The Value bounds where chosen so that we have a multiplicative
    // `equivalentScalingFactor` to multiply the expected KPI output by
    Normaliser linearNormaliser = new LinearNormaliser(0, 10, 0, 50);
    double equivalentScalingFactor = 1 / 5.0;
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Test
    public void singleTripGivesTrivialAverage() {
        String trav_time = "00:20:00";
        Double trav_time_minutes = 20.0;
        TripsTableBuilder tripsTableBuilder = new TripsTableBuilder(tmpDir);

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withTrips(tripsTableBuilder.reset()
                                .withTrip(
                                        "Bobby",
                                        "1",
                                        new TripBuilder()
                                                .withLegs(Arrays.asList(new LegBuilder().withTravTime(trav_time).build()))
                                                .build())
                        .build())
                .withLegs(tripsTableBuilder.getLegsBuilder().build())
                .build();
        Map<String, Double> outputKpi = kpiCalculator.writeTravelTimeKpi(
                Path.of(tmpDir.getRoot().getAbsolutePath()),
                linearNormaliser
        );

        assertThat(outputKpi.get("actual")).isEqualTo(trav_time_minutes);
        assertThat(outputKpi.get("normalised")).isEqualTo(trav_time_minutes * equivalentScalingFactor)
                .as("KPI output is the average of travel times. " +
                        "Average of one travel time is that travel time");
    }

    @Test
    public void twoTripsProduceAverageOfTravelTimes() {
        String bobby_trav_time = "00:20:00";
        Double bobby_trav_time_minutes = 20.0;
        String bobbina_trav_time = "01:04:00";
        Double bobbina_trav_time_minutes = 64.0;

        TripsTableBuilder tripsTableBuilder = new TripsTableBuilder(tmpDir);

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withTrips(tripsTableBuilder.reset()
                        .withTrip(
                                "Bobby",
                                "1",
                                new TripBuilder()
                                        .withLegs(Arrays.asList(new LegBuilder().withTravTime(bobby_trav_time).build()))
                                        .build())
                        .withTrip(
                                "Bobbina",
                                "1",
                                new TripBuilder()
                                        .withLegs(Arrays.asList(new LegBuilder().withTravTime(bobbina_trav_time).build()))
                                        .build())
                        .build())
                .withLegs(tripsTableBuilder.getLegsBuilder().build())
                .build();
        Map<String, Double> outputKpi = kpiCalculator.writeTravelTimeKpi(
                Path.of(tmpDir.getRoot().getAbsolutePath()),
                linearNormaliser
        );

        assertThat(outputKpi.get("actual")).isEqualTo(
                (bobby_trav_time_minutes + bobbina_trav_time_minutes) / 2);
        assertThat(outputKpi.get("normalised")).isEqualTo(
                ((bobby_trav_time_minutes + bobbina_trav_time_minutes) / 2) * equivalentScalingFactor)
                .as("Should be the average of two travel times");
    }

}

