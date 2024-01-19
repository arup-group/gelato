package com.arup.cml.kpi.matsim.handlers;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import static org.assertj.core.api.Assertions.assertThat;


public class TestLinkLogHandler {
    Id<Person> gerry = Id.createPersonId("gerry");
    Id<Vehicle> gerryVehicle = Id.createVehicleId("gerry_wheels");

    @Test
    public void testNothing() {
        LinkLogHandler linkLogHandler = new LinkLogHandler();
        linkLogHandler.handleEvent(new PersonEntersVehicleEvent(0, gerry, gerryVehicle));
        // check correct data was recorded somewhere in linkLogHandler
        assertThat(true).isEqualTo(true);
    }

}
