package com.arup.cml.abm.kpi.tablesaw;

import com.arup.cml.abm.kpi.builders.KpiCalculatorBuilder;
import com.arup.cml.abm.kpi.builders.LinkLogBuilder;
import com.arup.cml.abm.kpi.builders.NetworkBuilder;
import com.arup.cml.abm.kpi.builders.VehiclesBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestTablesawVehicleKmKpi {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Test
    public void vehicleKmKpiOutputIsOfTheRightUnit() {
        double metreLength = 30;
        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withNetwork(new NetworkBuilder()
                        .withNetworkNode("A", 1, 1)
                        .withNetworkNode("B", 2, 2)
                        .withNetworkLinkWithLength("someLink", "A", "B", metreLength)
                        .build())
                .withVehicles(new VehiclesBuilder()
                        .withVehicle("someCar", "car", "car")
                        .build())
                .withLinkLog(new LinkLogBuilder()
                        .withEntry("someCar", "someLink", (9 * 60 * 60), (9 * 60 * 60) + 25)
                        .build())
                .build();
        double outputKpi = kpiCalculator.writeVehicleKMKpi(Path.of(tmpDir.getRoot().getAbsolutePath()));

        double kmLength = metreLength / 1000;
        assertThat(outputKpi).isEqualTo(kmLength);
    }
}