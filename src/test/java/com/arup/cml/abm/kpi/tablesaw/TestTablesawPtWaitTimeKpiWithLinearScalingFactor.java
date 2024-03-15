package com.arup.cml.abm.kpi.tablesaw;

import com.arup.cml.abm.kpi.LinearScale;
import com.arup.cml.abm.kpi.ScalingFactor;
import com.arup.cml.abm.kpi.builders.KpiCalculatorBuilder;
import com.arup.cml.abm.kpi.builders.LegsBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestTablesawPtWaitTimeKpiWithLinearScalingFactor {
    // this scale is not the proposed KPI scale. The Value bounds where chosen so that we have a multiplicative
    // `equivalentScalingFactor` to multiply the expected KPI output by
    ScalingFactor linearScalingFactor = new LinearScale(0, 1, 0, 15 * 60);
    double equivalentScalingFactor = 1.0 / (15.0 * 60);
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Test
    public void ptWaitTimeReturnsTheSinglePtWaitTimeWithSingleAgent() {
        String bobby = "Bobby";
        String bobbyPtWaitTime = "00:06:00";
        Double bobbyPtWaitTimeSeconds = 6.0 * 60.0;

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withLegs(new LegsBuilder(tmpDir)
                        .withDefaultPtLegWithTiming(bobby, "bobby_1", "09:00:00", "00:30:00", bobbyPtWaitTime)
                        .build())
                .build();
        double outputKpi = kpiCalculator.writePtWaitTimeKpi(
                Path.of(tmpDir.getRoot().getAbsolutePath()),
                linearScalingFactor
        );

        assertThat(outputKpi).isEqualTo(bobbyPtWaitTimeSeconds * equivalentScalingFactor)
                .as("PT Wait Time KPI should return the only PT wait time recorded with one agent");
    }

    @Test
    public void ptWaitTimeReturnsZeroWhenPtLegIsOutsidePeakTime() {
        String bobby = "Bobby";
        String bobbyPtWaitTime = "00:06:00";

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withLegs(new LegsBuilder(tmpDir)
                        .withDefaultPtLegWithTiming(bobby, "bobby_1", "23:00:00", "00:30:00", bobbyPtWaitTime)
                        .build())
                .build();
        double outputKpi = kpiCalculator.writePtWaitTimeKpi(
                Path.of(tmpDir.getRoot().getAbsolutePath()),
                linearScalingFactor
        );

        assertThat(outputKpi).isEqualTo(0.0)
                .as("PT Wait Time KPI should return 0, as PT Leg is outside peak time");
    }

    @Test
    public void givesAverageOfTwoPeakPtLegs() {
        String bobby = "Bobby";
        String bobbyPtWaitTime = "00:06:00";
        Double bobbyPtWaitTimeSeconds = 6.0 * 60.0;

        String bobbina = "Bobbina";
        String bobbinaPtWaitTime = "00:12:00";
        Double bobbinaPtWaitTimeSeconds = 12.0 * 60.0;

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withLegs(new LegsBuilder(tmpDir)
                        .withDefaultPtLegWithTiming(bobby, "bobby_1", "09:00:00", "00:30:00", bobbyPtWaitTime)
                        .withDefaultPtLegWithTiming(bobbina, "bobbina_1", "09:00:00", "00:30:00", bobbinaPtWaitTime)
                        .build())
                .build();
        double outputKpi = kpiCalculator.writePtWaitTimeKpi(
                Path.of(tmpDir.getRoot().getAbsolutePath()),
                linearScalingFactor
        );

        assertThat(outputKpi).isEqualTo((bobbyPtWaitTimeSeconds + bobbinaPtWaitTimeSeconds) / 2 * equivalentScalingFactor)
                .as("PT Wait Time KPI should return average of 6 and 12 minutes for Bobby " +
                        "and his friend Bobbina");
    }

}

