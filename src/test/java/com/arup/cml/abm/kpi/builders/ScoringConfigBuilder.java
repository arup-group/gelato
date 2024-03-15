package com.arup.cml.abm.kpi.builders;

import org.matsim.core.config.groups.ScoringConfigGroup;

public class ScoringConfigBuilder {
    ScoringConfigGroup scoring;
    String defaultSubpopulation = "default";
    double defaultDailyMonetaryConstant = 0.0;
    double defaultMonetaryDistanceRate = 0.0;
    double defaultConstant = 0.0;
    double defaultMarginalUtilityOfDistance = 0.0;
    double defaultDailyUtilityConstant = 0.0;
    double defaultMarginalUtilityOfTraveling = 0.0;

    public ScoringConfigBuilder() {
        scoring = new ScoringConfigGroup();
    }

    public ScoringConfigBuilder withScoringParams(
            String subpopulation, String mode, double dailyMonetaryConstant, double monetaryDistanceRate,
            double constant, double marginalUtilityOfDistance, double dailyUtilityConstant,
            double marginalUtilityOfTraveling) {
        ScoringConfigGroup.ScoringParameterSet paramSet = scoring.getOrCreateScoringParameters(subpopulation);
        ScoringConfigGroup.ModeParams modeParams = new ScoringConfigGroup.ModeParams(mode);
        modeParams.setDailyMonetaryConstant(dailyMonetaryConstant);
        modeParams.setMonetaryDistanceRate(monetaryDistanceRate);
        modeParams.setConstant(constant);
        modeParams.setMarginalUtilityOfDistance(marginalUtilityOfDistance);
        modeParams.setDailyUtilityConstant(dailyUtilityConstant);
        modeParams.setMarginalUtilityOfTraveling(marginalUtilityOfTraveling);
        paramSet.addModeParams(modeParams);
        return this;
    }

    public ScoringConfigBuilder withMonetaryCostsForSubpopulationAndMode(
            String subpopulation, String mode, double dailyMonetaryConstant, double monetaryDistanceRate) {
        return this.withScoringParams(subpopulation, mode, dailyMonetaryConstant, monetaryDistanceRate,
                defaultConstant, defaultMarginalUtilityOfDistance, defaultDailyUtilityConstant,
                defaultMarginalUtilityOfTraveling);
    }

    public ScoringConfigBuilder withDefaultScoringParams() {
        this.withScoringParams(defaultSubpopulation, "car", defaultDailyMonetaryConstant,
                defaultMonetaryDistanceRate, defaultConstant, defaultMarginalUtilityOfDistance,
                defaultDailyUtilityConstant, defaultMarginalUtilityOfTraveling);
        this.withScoringParams(defaultSubpopulation, "drt", defaultDailyMonetaryConstant,
                defaultMonetaryDistanceRate, defaultConstant, defaultMarginalUtilityOfDistance,
                defaultDailyUtilityConstant, defaultMarginalUtilityOfTraveling);
        this.withScoringParams(defaultSubpopulation, "bus", defaultDailyMonetaryConstant,
                defaultMonetaryDistanceRate, defaultConstant, defaultMarginalUtilityOfDistance,
                defaultDailyUtilityConstant, defaultMarginalUtilityOfTraveling);
        this.withScoringParams(defaultSubpopulation, "rail", defaultDailyMonetaryConstant,
                defaultMonetaryDistanceRate, defaultConstant, defaultMarginalUtilityOfDistance,
                defaultDailyUtilityConstant, defaultMarginalUtilityOfTraveling);
        return this;
    }

    public ScoringConfigGroup build() {
        return this.scoring;
    }
}
