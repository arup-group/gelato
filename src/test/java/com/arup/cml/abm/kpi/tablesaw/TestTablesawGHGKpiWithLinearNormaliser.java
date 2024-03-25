package com.arup.cml.abm.kpi.tablesaw;

import com.arup.cml.abm.kpi.LinearNormaliser;
import com.arup.cml.abm.kpi.Normaliser;
import com.arup.cml.abm.kpi.builders.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestTablesawGHGKpiWithLinearNormaliser {
    // the scale interval is not the one proposed for this KPI. The Value bounds where chosen so that we have a multiplicative
    // `equivalentScalingFactor` to multiply the expected KPI output by
    Normaliser linearNormaliser = new LinearNormaliser(0, 10, 0, 20);
    double equivalentScalingFactor = 1.0 / 2.0;
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Test
    public void oneKilometerJourneyWithEmissionsFactorTwoAndSinglePersonGivesKpiOutputTwo() {
        String someCar = "someCar";
        String someLink = "someLink";

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withNetwork(new NetworkBuilder()
                        .withNetworkNode("A", 1, 1)
                        .withNetworkNode("B", 2, 2)
                        .withNetworkLinkWithLength(someLink, "A", "B", 1000.0)
                        .build())
                .withLinkLog(new LinkLogBuilder()
                        .withEntry(someCar, someLink, (9 * 60 * 60), (9 * 60 * 60) + 25)
                        .build())
                .withVehicles(new VehiclesBuilder()
                        .withVehicleWithEmissionsFactor(someCar, "car", 2.0)
                        .build())
                .withPersons(new PersonsBuilder(tmpDir)
                        .withPerson("Bobby")
                        .build())
                .build();
        double outputKpi = kpiCalculator.writeGHGKpi(
                Path.of(tmpDir.getRoot().getAbsolutePath()),
                linearNormaliser
        );

        assertThat(outputKpi).isEqualTo(2.0 * equivalentScalingFactor)
                .as("A vehicle with emissions factor 2 drives a kilometer, " +
                        "so the output of the GHG KPI is expected to be 2, and 1 after scaling");
    }

}

