package com.arup.cml.abm.kpi.builders;

import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;

public class PopulationBuilder {
    Population population;

    public PopulationBuilder() {
        Config config = ConfigUtils.createConfig();
        population = PopulationUtils.createPopulation(config);
    }

    public Population build() {
        return this.population;
    }
}
