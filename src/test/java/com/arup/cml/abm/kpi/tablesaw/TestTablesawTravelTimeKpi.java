package com.arup.cml.abm.kpi.tablesaw;

import com.arup.cml.abm.kpi.LinearScale;
import com.arup.cml.abm.kpi.ScalingFactor;
import com.arup.cml.abm.kpi.builders.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestTablesawTravelTimeKpi {
    // this scale is not the proposed KPI scale. The Value bounds where chosen so that we have a multiplicative
    // `equivalentScalingFactor` to multiply the expected KPI output by
//    ScalingFactor linearScalingFactor = new LinearScale(0, 10, 0, 20);
    double equivalentScalingFactor = 1.0;
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Test
    public void singleTripGivesTrivialAverage() {
        String trav_time = "00:20:00";
        Double trav_time_minutes = 20.0;
        TripsBuilder tripsBuilder = new TripsBuilder(tmpDir);

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withTrips(tripsBuilder
                        .withTripWithTravelTime("Bobby", "1", trav_time)
                        .build())
                .withLegs(tripsBuilder.getLegsBuilder().build())
                .build();
        double outputKpi = kpiCalculator.writeTravelTimeKpi(
                Path.of(tmpDir.getRoot().getAbsolutePath())
//                linearScalingFactor
        );

        assertThat(outputKpi).isEqualTo(trav_time_minutes * equivalentScalingFactor)
                .as("KPI output is the average of travel times. " +
                        "Average of one travel time is that travel time");
    }

    @Test
    public void twoTripsProduceAverageOfTravelTimes() {
        String bobby_trav_time = "00:20:00";
        Double bobby_trav_time_minutes = 20.0;
        String bobbina_trav_time = "01:04:00";
        Double bobbina_trav_time_minutes = 64.0;

        TripsBuilder tripsBuilder = new TripsBuilder(tmpDir);

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withTrips(tripsBuilder
                        .withTripWithTravelTime("Bobby", "1", bobby_trav_time)
                        .withTripWithTravelTime("Bobbina", "1", bobbina_trav_time)
                        .build())
                .withLegs(tripsBuilder.getLegsBuilder().build())
                .build();
        double outputKpi = kpiCalculator.writeTravelTimeKpi(
                Path.of(tmpDir.getRoot().getAbsolutePath())
//                linearScalingFactor
        );

        assertThat(outputKpi).isEqualTo(
                ((bobby_trav_time_minutes + bobbina_trav_time_minutes) / 2) * equivalentScalingFactor)
                .as("Should be the average of two travel times");
    }

}

