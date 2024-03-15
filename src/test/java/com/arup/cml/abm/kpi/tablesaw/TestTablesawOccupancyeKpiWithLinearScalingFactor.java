package com.arup.cml.abm.kpi.tablesaw;

import com.arup.cml.abm.kpi.LinearScale;
import com.arup.cml.abm.kpi.ScalingFactor;
import com.arup.cml.abm.kpi.builders.KpiCalculatorBuilder;
import com.arup.cml.abm.kpi.builders.LinkLogBuilder;
import com.arup.cml.abm.kpi.builders.VehiclesBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestTablesawOccupancyeKpiWithLinearScalingFactor {
    // this scale is not the proposed KPI scale. The Value bounds where chosen so that we have a multiplicative
    // `equivalentScalingFactor` to multiply the expected KPI output by
    ScalingFactor linearScalingFactor = new LinearScale(0, 10, 0, 1);
    double equivalentScalingFactor = 10.0;
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Test
    public void twoPeopleInAFourSeaterGiveHalfOccupancy() {
        String someCar = "someCar";

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withLinkLog(new LinkLogBuilder()
                        .withOccupant(someCar, "Bobby")
                        .withOccupant(someCar, "Bobbina")
                        .withEntry(someCar, "someLink", (9 * 60 * 60), (9 * 60 * 60) + 25)
                        .build())
                .withVehicles(new VehiclesBuilder()
                        .withVehicleTypeOfCapacity("car", "car", 4)
                        .withVehicle(someCar, "car", "car")
                        .build())
                .build();
        double outputKpi = kpiCalculator.writeOccupancyRateKpi(
                Path.of(tmpDir.getRoot().getAbsolutePath()),
                linearScalingFactor
        );

        assertThat(outputKpi).isEqualTo(0.5 * equivalentScalingFactor)
                .as("Occupancy Rate should be at half with two people in a car that fits four," +
                        "and 5 after scaling.");
    }

}

