package com.arup.cml.abm.kpi.tablesaw;

import com.arup.cml.abm.kpi.LinearScale;
import com.arup.cml.abm.kpi.ScalingFactor;
import com.arup.cml.abm.kpi.builders.KpiCalculatorBuilder;
import com.arup.cml.abm.kpi.builders.TransitScheduleBuilder;
import com.arup.cml.abm.kpi.builders.TripsBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestTablesawAccessToMobility {
    // In this case, this scale is the proposed KPI scale. We have a natural multiplicative
    // `equivalentScalingFactor` to multiply the expected KPI output by
    ScalingFactor linearScalingFactor = new LinearScale(0, 10, 0, 100);
    double equivalentScalingFactor = 1 / 10.0;
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Test
    public void singleAgentHasAccessToBus() {
        TripsBuilder tripsBuilder = new TripsBuilder(tmpDir);

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withTrips(tripsBuilder
                        .withTripWithStartLocationAndType("Bobby", "1", "home", 0.0, 0.0)
                        .build())
                .withLegs(tripsBuilder.getLegsBuilder().build())
                .withTransitSchedule(new TransitScheduleBuilder()
                        .withTransitStopWithMode("BusStop", 400.0, 0.0, "bus")
                        .build())
                .build();
        Map<String, Double> outputKpi = kpiCalculator.writeAccessToMobilityServicesKpi(
                Path.of(tmpDir.getRoot().getAbsolutePath()),
                linearScalingFactor
        );

        assertThat(outputKpi.get("bus_kpi")).isEqualTo(100.0 * equivalentScalingFactor)
                .as("Bus KPI output is expected to be 100%, " +
                        "because there is only one agent and they have access to a bus stop.");
        assertThat(outputKpi.get("rail_kpi")).isEqualTo(0.0 * equivalentScalingFactor)
                .as("Rail KPI output is expected to be 0%, " +
                        "because there is only one agent and they don't have access to rail.");
        assertThat(outputKpi.get("used_pt_kpi")).isEqualTo(0.0 * equivalentScalingFactor)
                .as("Used PT KPI output is expected to be 0%, " +
                        "because there is only one agent and they didn't use PT");
    }

    @Test
    public void nonHomeTripsAreFilteredOutAndDontContributeToKpiOutput() {
        TripsBuilder tripsBuilder = new TripsBuilder(tmpDir);

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withTrips(tripsBuilder
                        .withTripWithStartLocationAndType("Bobby", "1", "home", 0.0, 0.0)
                        .withTripWithStartLocationAndType("Bobby", "2", "work", 0.0, 0.0)
                        .build())
                .withLegs(tripsBuilder.getLegsBuilder().build())
                .withTransitSchedule(new TransitScheduleBuilder()
                        .withTransitStopWithMode("BusStop", 400.0, 0.0, "bus")
                        .build())
                .build();
        Map<String, Double> outputKpi = kpiCalculator.writeAccessToMobilityServicesKpi(
                Path.of(tmpDir.getRoot().getAbsolutePath()),
                linearScalingFactor
        );

        assertThat(outputKpi.get("bus_kpi")).isEqualTo(100.0 * equivalentScalingFactor)
                .as("Bus KPI output is expected to be 100%, " +
                        "because there is only one agent and they have access to a bus stop.");
    }

    @Test
    public void singleAgentHasAccessToRail() {
        TripsBuilder tripsBuilder = new TripsBuilder(tmpDir);

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withTrips(tripsBuilder
                        .withTripWithStartLocationAndType("Bobby", "1", "home", 0.0, 0.0)
                        .build())
                .withLegs(tripsBuilder.getLegsBuilder().build())
                .withTransitSchedule(new TransitScheduleBuilder()
                        .withTransitStopWithMode("RailStop", 800.0, 0.0, "rail")
                        .build())
                .build();
        Map<String, Double> outputKpi = kpiCalculator.writeAccessToMobilityServicesKpi(
                Path.of(tmpDir.getRoot().getAbsolutePath()),
                linearScalingFactor
        );

        assertThat(outputKpi.get("bus_kpi")).isEqualTo(0.0 * equivalentScalingFactor)
                .as("Bus KPI output is expected to be 0%, " +
                        "because there is only one agent and they don't have access to a bus stop.");
        assertThat(outputKpi.get("rail_kpi")).isEqualTo(100.0 * equivalentScalingFactor)
                .as("Rail KPI output is expected to be 100%, " +
                        "because there is only one agent and they have access to rail.");
        assertThat(outputKpi.get("used_pt_kpi")).isEqualTo(0.0 * equivalentScalingFactor)
                .as("Used PT KPI output is expected to be 0%, " +
                        "because there is only one agent and they didn't use PT");
    }

    @Test
    public void singleAgentHasAccessToRailAndUsesPT() {
        TripsBuilder tripsBuilder = new TripsBuilder(tmpDir);

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withTrips(tripsBuilder
                        .withPtTripWithStartLocationAndType("Bobby", "1", "home",
                                0.0, 0.0, "rail", "A", "B")
                        .build())
                .withLegs(tripsBuilder.getLegsBuilder().build())
                .withTransitSchedule(new TransitScheduleBuilder()
                        .withTransitStopWithMode("RailStop", 800.0, 0.0, "rail")
                        .build())
                .build();
        Map<String, Double> outputKpi = kpiCalculator.writeAccessToMobilityServicesKpi(
                Path.of(tmpDir.getRoot().getAbsolutePath()),
                linearScalingFactor
        );

        assertThat(outputKpi.get("rail_kpi")).isEqualTo(100.0 * equivalentScalingFactor)
                .as("Rail KPI output is expected to be 100%, " +
                        "because there is only one agent and they have access to rail.");
        assertThat(outputKpi.get("used_pt_kpi")).isEqualTo(100.0 * equivalentScalingFactor)
                .as("Used PT KPI output is expected to be 100%, " +
                        "because there is only one agent and they used PT");
    }

    @Test
    public void twoAgentsWithBusAccess() {
        TripsBuilder tripsBuilder = new TripsBuilder(tmpDir);

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withTrips(tripsBuilder
                        .withTripWithStartLocationAndType("Bobby", "1", "home",
                                0.0, 0.0)
                        .withTripWithStartLocationAndType("Bobbina", "1", "home",
                                399.0, 0.0)
                        .build())
                .withLegs(tripsBuilder.getLegsBuilder().build())
                .withTransitSchedule(new TransitScheduleBuilder()
                        .withTransitStopWithMode("BusStop", 0.0, 0.0, "bus")
                        .build())
                .build();
        Map<String, Double> outputKpi = kpiCalculator.writeAccessToMobilityServicesKpi(
                Path.of(tmpDir.getRoot().getAbsolutePath()),
                linearScalingFactor
        );

        assertThat(outputKpi.get("bus_kpi")).isEqualTo(100.0 * equivalentScalingFactor)
                .as("Bus KPI output is expected to be 100%, " +
                        "because both agents have access to bus.");
    }

    @Test
    public void twoAgentsWithDifferentBusAccess() {
        TripsBuilder tripsBuilder = new TripsBuilder(tmpDir);

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withTrips(tripsBuilder
                        .withTripWithStartLocationAndType("Bobby", "1", "home",
                                0.0, 0.0)
                        .withTripWithStartLocationAndType("Bobbina", "1", "home",
                                401.0, 0.0)
                        .build())
                .withLegs(tripsBuilder.getLegsBuilder().build())
                .withTransitSchedule(new TransitScheduleBuilder()
                        .withTransitStopWithMode("BusStop", 0.0, 0.0, "bus")
                        .build())
                .build();
        Map<String, Double> outputKpi = kpiCalculator.writeAccessToMobilityServicesKpi(
                Path.of(tmpDir.getRoot().getAbsolutePath()),
                linearScalingFactor
        );

        assertThat(outputKpi.get("bus_kpi")).isEqualTo(50.0 * equivalentScalingFactor)
                .as("Bus KPI output is expected to be 50%, " +
                        "because one agent has access to bus and the other doesn't.");
    }

    @Test
    public void twoAgentsWithRailAccess() {
        TripsBuilder tripsBuilder = new TripsBuilder(tmpDir);

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withTrips(tripsBuilder
                        .withTripWithStartLocationAndType("Bobby", "1", "home",
                                0.0, 0.0)
                        .withTripWithStartLocationAndType("Bobbina", "1", "home",
                                799.0, 0.0)
                        .build())
                .withLegs(tripsBuilder.getLegsBuilder().build())
                .withTransitSchedule(new TransitScheduleBuilder()
                        .withTransitStopWithMode("RailStop", 0.0, 0.0, "rail")
                        .build())
                .build();
        Map<String, Double> outputKpi = kpiCalculator.writeAccessToMobilityServicesKpi(
                Path.of(tmpDir.getRoot().getAbsolutePath()),
                linearScalingFactor
        );

        assertThat(outputKpi.get("rail_kpi")).isEqualTo(100.0 * equivalentScalingFactor)
                .as("Bus KPI output is expected to be 100%, " +
                        "because both agents have access to rail.");
    }

    @Test
    public void twoAgentsWithDifferentRailAccess() {
        TripsBuilder tripsBuilder = new TripsBuilder(tmpDir);

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withTrips(tripsBuilder
                        .withTripWithStartLocationAndType("Bobby", "1", "home",
                                0.0, 0.0)
                        .withTripWithStartLocationAndType("Bobbina", "1", "home",
                                801.0, 0.0)
                        .build())
                .withLegs(tripsBuilder.getLegsBuilder().build())
                .withTransitSchedule(new TransitScheduleBuilder()
                        .withTransitStopWithMode("RailStop", 0.0, 0.0, "rail")
                        .build())
                .build();
        Map<String, Double> outputKpi = kpiCalculator.writeAccessToMobilityServicesKpi(
                Path.of(tmpDir.getRoot().getAbsolutePath()),
                linearScalingFactor
        );

        assertThat(outputKpi.get("rail_kpi")).isEqualTo(50.0 * equivalentScalingFactor)
                .as("Rail KPI output is expected to be 50%, " +
                        "because one agent has access to rail and the other doesn't.");
    }

    @Test
    public void twoAgentsWithDifferentPTAccessButBothUsePT() {
        TripsBuilder tripsBuilder = new TripsBuilder(tmpDir);

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withTrips(tripsBuilder
                        .withPtTripWithStartLocationAndType("Bobby", "1", "home",
                                0.0, 0.0, "rail", "A", "B")
                        .withPtTripWithStartLocationAndType("Bobbina", "1", "home",
                                801.0, 0.0, "bus", "A", "B")
                        .build())
                .withLegs(tripsBuilder.getLegsBuilder().build())
                .withTransitSchedule(new TransitScheduleBuilder()
                        .withTransitStopWithMode("RailStop", 0.0, 0.0, "rail")
                        .withTransitStopWithMode("BusStop", 800.0, 0.0, "bus")
                        .build())
                .build();
        Map<String, Double> outputKpi = kpiCalculator.writeAccessToMobilityServicesKpi(
                Path.of(tmpDir.getRoot().getAbsolutePath()),
                linearScalingFactor
        );

        assertThat(outputKpi.get("bus_kpi")).isEqualTo(50.0 * equivalentScalingFactor)
                .as("Bus KPI output is expected to be 50%, " +
                        "because one agent has access to bus and the other doesn't.");
        assertThat(outputKpi.get("rail_kpi")).isEqualTo(50.0 * equivalentScalingFactor)
                .as("Rail KPI output is expected to be 50%, " +
                        "because one agent has access to rail and the other doesn't.");
        assertThat(outputKpi.get("used_pt_kpi")).isEqualTo(100.0 * equivalentScalingFactor)
                .as("Used KPI output is expected to be 100%, " +
                        "because both agents use PT.");
    }

}

