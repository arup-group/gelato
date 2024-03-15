package com.arup.cml.abm.kpi.tablesaw;

import com.arup.cml.abm.kpi.builders.*;
import org.assertj.core.data.Offset;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestTablesawAffordabilityKpi {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @Test
    public void testSingleAgentGivesRatioOfOne() {
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
        double outputKpi = kpiCalculator.writeAffordabilityKpi(Path.of(tmpDir.getRoot().getAbsolutePath()));

        assertThat(outputKpi).isEqualTo(1)
                .as("There is only one agent so the ratio of all travel to the low income group " +
                        "is expected to be 1");
    }

    @Test
    public void lowerIncomeAgentSpendsTwiceAsMuch() {
        String subpop = "default";
        String mode = "car";
        Integer tripLength = 10;
        double dailyConstant = 1;
        double distanceCost = 1;
        double singleTripCost = dailyConstant + tripLength * distanceCost;

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
        double outputKpi = kpiCalculator.writeAffordabilityKpi(Path.of(tmpDir.getRoot().getAbsolutePath()));

        double poorBobbyDayAverage = 2 * singleTripCost;
        double overallDayAverage = 3 * singleTripCost / 2;
        assertThat(outputKpi).isCloseTo(poorBobbyDayAverage / overallDayAverage, Offset.offset(0.009))
                .as("There is only one agent so the ratio of all travel to the low income group " +
                        "is expected to be 1");
    }

    @Test
    public void lowerIncomeAgentSpendsTwiceAsMuchRelyingOnSubpopulations() {
        String mode = "car";
        Integer tripLength = 10;
        double dailyConstant = 1;
        double distanceCost = 1;
        double singleTripCost = dailyConstant + tripLength * distanceCost;

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
        double outputKpi = kpiCalculator.writeAffordabilityKpi(Path.of(tmpDir.getRoot().getAbsolutePath()));

        double poorBobbyDayAverage = 2 * singleTripCost;
        double overallDayAverage = 3 * singleTripCost / 2;
        assertThat(outputKpi).isCloseTo(poorBobbyDayAverage / overallDayAverage, Offset.offset(0.009))
                .as("There is only one agent so the ratio of all travel to the low income group " +
                        "is expected to be 1");
    }


}

