package com.arup.cml.abm.kpi.builders;

import org.matsim.core.config.groups.ScoringConfigGroup;

public class ScoringConfigBuilder {
    ScoringConfigGroup scoring;

    public ScoringConfigBuilder() {
        scoring = new ScoringConfigGroup();
        this.withMode("car");
        this.withMode("drt");
        this.withMode("bus");
        this.withMode("rail");
    }

    public ScoringConfigBuilder withMode(String mode) {
        ScoringConfigGroup.ScoringParameterSet paramSet = scoring.getOrCreateScoringParameters("default");
        ScoringConfigGroup.ModeParams modeParams = new ScoringConfigGroup.ModeParams(mode);
        modeParams.setDailyMonetaryConstant(0.0);
        modeParams.setMonetaryDistanceRate(0.0);
        modeParams.setConstant(0.0);
        modeParams.setMarginalUtilityOfDistance(0.0);
        modeParams.setDailyUtilityConstant(0.0);
        modeParams.setMarginalUtilityOfTraveling(0.0);
        paramSet.addModeParams(modeParams);
        return this;
    }

    public ScoringConfigBuilder withMonetaryCosts(
            String subpopulation, String mode, double dailyMonetaryConstant, double monetaryDistanceRate) {
        ScoringConfigGroup.ScoringParameterSet paramSet = scoring.getOrCreateScoringParameters(subpopulation);
        ScoringConfigGroup.ModeParams modeParams = new ScoringConfigGroup.ModeParams(mode);
        modeParams.setDailyMonetaryConstant(dailyMonetaryConstant);
        modeParams.setMonetaryDistanceRate(monetaryDistanceRate);
        paramSet.addModeParams(modeParams);
        return this;
    }

    public ScoringConfigGroup build() {
        return this.scoring;
    }
}
