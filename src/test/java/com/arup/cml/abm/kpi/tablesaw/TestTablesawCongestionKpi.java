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
    public void reportsAverageDelayRatio() {
        double firstDelayRatio = 5.0;
        double secondDelayRatio = 10.0;
        double averageDelayRatio = (firstDelayRatio + secondDelayRatio) / 2;

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withNetwork(new NetworkBuilder()
                        .withNetworkNode("A", 1, 1)
                        .withNetworkNode("B", 2, 2)
                        .withNetworkLink("otherLink", "B", "A")
                        .build())
                .withVehicles(new VehiclesBuilder()
                        .withVehicle("someCar", "car")
                        .build())
                .withLinkLog(new LinkLogBuilder()
                        .withEntry("someCar", "otherLink", (9 * 60 * 60) + 25, (9 * 60 * 60) + 25 + firstDelayRatio)
                        .withEntry("someCar", "otherLink", (9 * 60 * 60) + 30, (9 * 60 * 60) + 30 + secondDelayRatio)
                        .build())
                .build();
        Table outputKpi = kpiCalculator.writeCongestionKpi(Path.of(tmpDir.getRoot().getAbsolutePath()));

        assertThat(outputKpi.rowCount()).isEqualTo(1).as("Congestion KPI table should include only one row/mode");
        Row metrics = outputKpi.row(0);
        assertThat(metrics.getString("mode")).isEqualTo("car").as("Mode should be car");
        assertThat(metrics.getDouble("Mean [delayRatio]")).isEqualTo(averageDelayRatio)
                .as("Mean delay should be the average of 5 and 10");
    }

    @Test
    public void reportsDelayRatiosPerMode() {
        double carDelayRatio = 15.0;
        double rocketDelayRatio = 1.0;
        double horseDelayRatio = 30.0;

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withNetwork(new NetworkBuilder()
                        .withNetworkNode("A", 1, 1)
                        .withNetworkNode("B", 2, 2)
                        .withNetworkLink("otherLink", "B", "A")
                        .build())
                .withVehicles(new VehiclesBuilder()
                        .withVehicle("someCar", "car")
                        .withVehicleOfMode("someRocket", "rocket", "rocket")
                        .withVehicleOfMode("someHorse", "horse", "horse")
                        .build())
                .withLinkLog(new LinkLogBuilder()
                        .withEntry("someCar", "otherLink", (9 * 60 * 60) + 25, (9 * 60 * 60) + 25 + carDelayRatio)
                        .withEntry("someRocket", "otherLink", (9 * 60 * 60) + 25, (9 * 60 * 60) + 25 + rocketDelayRatio)
                        .withEntry("someHorse", "otherLink", (9 * 60 * 60) + 25, (9 * 60 * 60) + 25 + horseDelayRatio)
                        .build())
                .build();
        Table outputKpi = kpiCalculator.writeCongestionKpi(Path.of(tmpDir.getRoot().getAbsolutePath()));

        assertThat(outputKpi.rowCount()).isEqualTo(3).as("Congestion KPI table should include three rows/modes");
        Row carMetric = outputKpi.row(0);
        assertThat(carMetric.getString("mode")).isEqualTo("car").as("Mode should be car");
        assertThat(carMetric.getDouble("Mean [delayRatio]")).isEqualTo(carDelayRatio).as("Mean delay should be 15");
        Row rocketMetric = outputKpi.row(1);
        assertThat(rocketMetric.getString("mode")).isEqualTo("rocket").as("Mode should be rocket");
        assertThat(rocketMetric.getDouble("Mean [delayRatio]")).isEqualTo(rocketDelayRatio).as("Mean delay should be 1");
        Row horseMetric = outputKpi.row(2);
        assertThat(horseMetric.getString("mode")).isEqualTo("horse").as("Mode should be horse");
        assertThat(horseMetric.getDouble("Mean [delayRatio]")).isEqualTo(horseDelayRatio).as("Mean delay should be 30");
    }

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

