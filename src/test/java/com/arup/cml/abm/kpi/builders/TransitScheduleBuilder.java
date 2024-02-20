package com.arup.cml.abm.kpi.builders;

import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;

public class TransitScheduleBuilder {
    TransitSchedule schedule;

    public TransitScheduleBuilder() {
        TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
        schedule = builder.createTransitSchedule();
    }

    public TransitSchedule build() {
        return this.schedule;
    }
}
