package com.arup.cml.abm.kpi.tablesaw;

import com.arup.cml.abm.kpi.LinearNormaliser;
import com.arup.cml.abm.kpi.Normaliser;
import com.arup.cml.abm.kpi.builders.*;
import org.assertj.core.data.Offset;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestTablesawAffordabilityKpiWithLinearNormaliser {
    // the scale interval is not the one proposed for this KPI. The Value bounds where chosen so that we have a multiplicative
    // `equivalentScalingFactor` to multiply the expected Affordability KPI Ratio by
    Normaliser linearNormaliser = new LinearNormaliser(0, 1, 0, 10);
    double equivalentScalingFactor = 1.0 / 10.0;

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Test
    public void singleAgentGivesRatioOfOne() {
        String bobby = "Bobby";
        String bobbySubpop = "default";
        Integer bobbyTripLength = 10;

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withLegs(new LegsBuilder(tmpDir)
                        .withLegWithDistanceAndMode(bobby, "bobby_1", bobbyTripLength, "car")
                        .build())
                .withPersons(new PersonsBuilder(tmpDir)
                        .withPerson(bobby, 10000, bobbySubpop)
                        .build())
                .withScoring(new ScoringConfigBuilder()
                        .withMonetaryCostsForSubpopulationAndMode(bobbySubpop, "car", 1.0, 1.0)
                        .build())
                .build();
        Map<String, Double> outputKpi = kpiCalculator.writeAffordabilityKpi(
                Path.of(tmpDir.getRoot().getAbsolutePath()),
                linearNormaliser
        );

        double expectedRatio = 1;
        assertThat(outputKpi.get("actual")).isEqualTo(expectedRatio);
        assertThat(outputKpi.get("normalised")).isEqualTo(expectedRatio * equivalentScalingFactor)
                .as("There is only one agent so the ratio of all travel to the low income group " +
                        "is expected to be 1, and 0.1 after scaling");
    }

    @Test
    public void lowerIncomeAgentSpendsTwiceAsMuch() {
        String subpop = "default";
        String mode = "car";
        Integer tripLength = 10;
        double dailyConstant = 1;
        double distanceCost = 1;

        String poorBobby = "PoorBobby";
        double poorBobbyIncome = 10000;

        String richBobby = "MintedBobby";
        double richBobbyIncome = 1000000;  // it's all about those zeros :)

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withLegs(new LegsBuilder(tmpDir)
                        // poor Bobby has to make two trips and spends twice as much on travel
                        .withLegWithDistanceAndMode(poorBobby, "poor_bobby_1", tripLength, mode)
                        .withLegWithDistanceAndMode(poorBobby, "poor_bobby_2", tripLength, mode)
                        .withLegWithDistanceAndMode(richBobby, "rich_bobby_1", tripLength, mode)
                        .build())
                .withPersons(new PersonsBuilder(tmpDir)
                        .withPerson(poorBobby, poorBobbyIncome, subpop)
                        .withPerson(richBobby, richBobbyIncome, subpop)
                        .build())
                .withScoring(new ScoringConfigBuilder()
                        .withMonetaryCostsForSubpopulationAndMode(subpop, mode, dailyConstant, distanceCost)
                        .build())
                .build();
        Map<String, Double> outputKpi = kpiCalculator.writeAffordabilityKpi(
                Path.of(tmpDir.getRoot().getAbsolutePath()),
                linearNormaliser
        );

        // why 1.33.. ?
        // 2x / (3x/2) = 4x/3x = 1 + 1/3
        // 2x <- poor Bobby cost
        // (3x/2) <- overall average
        double expectedRatio = 1 + 1.0/3;

        assertThat(outputKpi.get("actual")).isCloseTo(expectedRatio, Offset.offset(0.009));
        assertThat(outputKpi.get("normalised")).isCloseTo(expectedRatio * equivalentScalingFactor, Offset.offset(0.009))
                .as("There are two agents, the poorer agent spends twice as much on travel. The ratio of " +
                        "all travel to the low income group is expected to be 1.33, and 0.133 after scaling.");
    }

    @Test
    public void lowerIncomeAgentSpendsTwiceAsMuchRelyingOnSubpopulations() {
        String mode = "car";
        Integer tripLength = 10;
        double dailyConstant = 1;
        double distanceCost = 1;

        String poorBobby = "PoorBobby";
        String poorBobbySubpop = "low income";

        String richBobby = "MintedBobby";
        String richBobbySubpop = "high income";

        TablesawKpiCalculator kpiCalculator = new KpiCalculatorBuilder(tmpDir)
                .withLegs(new LegsBuilder(tmpDir)
                        // poor Bobby has to make two trips and spends twice as much on travel
                        .withLegWithDistanceAndMode(poorBobby, "poor_bobby_1", tripLength, mode)
                        .withLegWithDistanceAndMode(poorBobby, "poor_bobby_2", tripLength, mode)
                        .withLegWithDistanceAndMode(richBobby, "rich_bobby_1", tripLength, mode)
                        .build())
                .withPersons(new PersonsBuilder(tmpDir)
                        .withPersonWithMissingIncome(poorBobby, poorBobbySubpop)
                        .withPersonWithMissingIncome(richBobby, richBobbySubpop)
                        .build())
                .withScoring(new ScoringConfigBuilder()
                        .withMonetaryCostsForSubpopulationAndMode(poorBobbySubpop, mode, dailyConstant, distanceCost)
                        .withMonetaryCostsForSubpopulationAndMode(richBobbySubpop, mode, dailyConstant, distanceCost)
                        .build())
                .build();
        Map<String, Double> outputKpi = kpiCalculator.writeAffordabilityKpi(
                Path.of(tmpDir.getRoot().getAbsolutePath()),
                linearNormaliser
        );

        // why 1.33.. ?
        // 2x / (3x/2) = 4x/3x = 1 + 1/3
        // 2x <- poor Bobby cost
        // (3x/2) <- overall average
        double expectedRatio = 1 + 1.0/3;
        assertThat(outputKpi.get("actual")).isCloseTo(expectedRatio, Offset.offset(0.009));
        assertThat(outputKpi.get("normalised")).isCloseTo(expectedRatio * equivalentScalingFactor, Offset.offset(0.009))
                .as("There are two agents, the poorer agent spends twice as much on travel. The ratio of " +
                        "all travel to the low income group is expected to be 1.33, and 0.133 after scaling.");
    }


}

