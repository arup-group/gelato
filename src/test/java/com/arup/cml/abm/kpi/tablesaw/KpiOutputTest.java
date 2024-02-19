package com.arup.cml.abm.kpi.tablesaw;

import com.arup.cml.abm.kpi.TableHelpers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.nio.file.Path;
public class KpiOutputTest {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Test
    public void linkWithInfiniteSpeedDoesNotBreakCongestionKpiOutput() {
        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withNetworkNode("A", 1, 1)
                .withNetworkNode("B", 2, 2)
                .withNetworkLinkWithSpeed("infLink", "A", "B", Double.POSITIVE_INFINITY)
                .withNetworkLink("otherLink", "B", "A")
                .withVehicle("someCar", "car")
                .withLinkLogEntry("someCar", "infLink", (9 * 60 * 60), (9 * 60 * 60) + 5)
                .withLinkLogEntry("someCar", "otherLink", (9 * 60 * 60) + 5, (9 * 60 * 60) + 10)
                .build();
        Table outputKpi = kpiCalculator.writeCongestionKpi(Path.of(tmpDir.getRoot().getAbsolutePath()));

        Table expectedTable =
                Table.create("Congestion KPI")
                        .addColumns(
                                StringColumn.create("mode", new String[]{"car"}),
                                DoubleColumn.create("Mean [delayRatio]", new double[]{5}));
        TableHelpers.assertTableDataEqual(outputKpi, expectedTable);
    }
}

