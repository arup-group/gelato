package com.arup.cml.abm.kpi.tablesaw;

import com.arup.cml.abm.kpi.builders.KpiCalculatorBuilder;
import com.arup.cml.abm.kpi.builders.LinkLogBuilder;
import com.arup.cml.abm.kpi.builders.NetworkBuilder;
import com.arup.cml.abm.kpi.builders.VehiclesBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.nio.file.Path;

public class TestTablesawCongestionKpi {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Test
    public void linkWithInfiniteSpeedDoesNotBreakCongestionKpi() {
        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withNetwork(new NetworkBuilder()
                        .withNetworkNode("A", 1, 1)
                        .withNetworkNode("B", 2, 2)
                        .withNetworkLinkWithSpeed("infLink", "A", "B", Double.POSITIVE_INFINITY)
                        .withNetworkLink("otherLink", "B", "A")
                        .build())
                .withVehicles(new VehiclesBuilder()
                        .withVehicle("someCar", "car")
                        .build())
                .withLinkLog(new LinkLogBuilder()
                        .withEntry("someCar", "infLink", (9 * 60 * 60), (9 * 60 * 60) + 25)
                        .withEntry("someCar", "otherLink", (9 * 60 * 60) + 25, (9 * 60 * 60) + 30)
                        .build())
                .build();
        Table outputKpi = kpiCalculator.writeCongestionKpi(Path.of(tmpDir.getRoot().getAbsolutePath()));

        assertThat(outputKpi.rowCount()).isEqualTo(1).as("Congestion KPI table should include only one row/mode");
        Row metrics = outputKpi.row(0);
        assertThat(metrics.getString("mode")).isEqualTo("car").as("Mode should be car");
        assertThat(metrics.getDouble("Mean [delayRatio]")).isEqualTo(5).as("Mean delay should be 5");
    }
}

