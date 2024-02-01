package com.arup.cml.abm.kpi.matsim.handlers;

import com.arup.cml.abm.kpi.data.LinkLog;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.vehicles.Vehicle;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class TestLinkLogHandlerWithSimpleData {
    private LinkLog linkLog;

    @Before
    public void setup() {
        Id<Person> gerry = Id.createPersonId("gerry");
        Id<Vehicle> gerryVehicle = Id.createVehicleId("gerry_wheels");

        EventsManager eventsManager = EventsUtils.createEventsManager();
        linkLog = new LinkLog();
        MatsimLinkLogHandler linkLogHandler = new MatsimLinkLogHandler(linkLog);
        eventsManager.addHandler(linkLogHandler);

        eventsManager.processEvent(
                new PersonEntersVehicleEvent(0, gerry, gerryVehicle)
        );
        eventsManager.processEvent(
                new VehicleEntersTrafficEvent(0, gerry, Id.createLinkId("link_A_B"), gerryVehicle, "car", 1.0)
        );
        eventsManager.processEvent(
                new LinkLeaveEvent(5, gerryVehicle, Id.createLinkId("link_A_B"))
        );
        eventsManager.processEvent(
                new LinkEnterEvent(6, gerryVehicle, Id.createLinkId("link_B_A"))
        );
        eventsManager.processEvent(
                new VehicleLeavesTrafficEvent(10, gerry, Id.createLinkId("link_A_B"), gerryVehicle, "car", 1.0)
        );
        eventsManager.processEvent(
                new PersonLeavesVehicleEvent(11, gerry, gerryVehicle)
        );
    }

    @Test
    public void linkLogTableHasExpectedData() {
        LinkLogTable expectedTable = new LinkLogTable();
        expectedTable.withEntry(0L, "link_A_B", "gerry_wheels", "car", 0.0, 5.0, 1);
        expectedTable.withEntry(1L, "link_B_A", "gerry_wheels", "car", 6.0, 10.0, 1);

        assertThat(
                linkLog.getLinkLogData())
                .isEqualTo(expectedTable.getTable());
    }

    @Test
    public void vehicleOccupancyTableHasExpectedData() {
        LinkLogVehicleOccupancyTable expectedTable = new LinkLogVehicleOccupancyTable();
        expectedTable.withEntry(0L, "gerry");
        expectedTable.withEntry(1L, "gerry");

        assertThat(
                linkLog.getVehicleOccupantsData())
                .isEqualTo(expectedTable.getTable());
    }
}
