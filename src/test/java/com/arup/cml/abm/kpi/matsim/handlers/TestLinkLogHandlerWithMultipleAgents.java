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


public class TestLinkLogHandlerWithMultipleAgents {
    private LinkLog linkLog;

    @Before
    public void setup() {
        Id<Person> driver = Id.createPersonId("driver");
        Id<Person> gerry = Id.createPersonId("gerry");
        Id<Person> fitz = Id.createPersonId("fitz");
        Id<Vehicle> partyBusVehicle = Id.createVehicleId("party_bus");

        EventsManager eventsManager = EventsUtils.createEventsManager();
        linkLog = new LinkLog();
        MatsimLinkLogHandler linkLogHandler = new MatsimLinkLogHandler(linkLog);
        eventsManager.addHandler(linkLogHandler);

        eventsManager.processEvent(
                new PersonEntersVehicleEvent(0, driver, partyBusVehicle)
        );
        eventsManager.processEvent(
                new VehicleEntersTrafficEvent(0, driver, Id.createLinkId("start_link"), partyBusVehicle, "bus", 1.0)
        );
        eventsManager.processEvent(
                new LinkLeaveEvent(5, partyBusVehicle, Id.createLinkId("start_link"))
        );

        // Gerry gets on
        eventsManager.processEvent(
                new LinkEnterEvent(5, partyBusVehicle, Id.createLinkId("gerry_link_board"))
        );
        eventsManager.processEvent(
                new PersonEntersVehicleEvent(5, gerry, partyBusVehicle)
        );
        eventsManager.processEvent(
                new LinkLeaveEvent(10, partyBusVehicle, Id.createLinkId("gerry_link_board"))
        );
        // Fitz gets on
        eventsManager.processEvent(
                new LinkEnterEvent(10, partyBusVehicle, Id.createLinkId("fitz_link_board"))
        );
        eventsManager.processEvent(
                new PersonEntersVehicleEvent(10, fitz, partyBusVehicle)
        );
        eventsManager.processEvent(
                new LinkLeaveEvent(15, partyBusVehicle, Id.createLinkId("fitz_link_board"))
        );
        // Gerry leaves
        eventsManager.processEvent(
                new LinkEnterEvent(15, partyBusVehicle, Id.createLinkId("gerry_link_alight"))
        );
        eventsManager.processEvent(
                new PersonLeavesVehicleEvent(15, gerry, partyBusVehicle)
        );
        eventsManager.processEvent(
                new LinkLeaveEvent(20, partyBusVehicle, Id.createLinkId("gerry_link_alight"))
        );
        // Fitz leaves
        eventsManager.processEvent(
                new LinkEnterEvent(20, partyBusVehicle, Id.createLinkId("fitz_link_alight"))
        );
        eventsManager.processEvent(
                new PersonLeavesVehicleEvent(20, fitz, partyBusVehicle)
        );
        eventsManager.processEvent(
                new LinkLeaveEvent(25, partyBusVehicle, Id.createLinkId("fitz_link_alight"))
        );

        eventsManager.processEvent(
                new LinkEnterEvent(25, partyBusVehicle, Id.createLinkId("end_link"))
        );
        eventsManager.processEvent(
                new VehicleLeavesTrafficEvent(30, driver, Id.createLinkId("end_link"), partyBusVehicle, "bus", 1.0)
        );
        eventsManager.processEvent(
                new PersonLeavesVehicleEvent(30, driver, partyBusVehicle)
        );
    }


    @Test
    public void linkLogTableHasExpectedData() {
        LinkLogTable expectedTable = new LinkLogTable();
        expectedTable.withEntry(0L, "start_link", "party_bus", "bus", 0.0, 5.0, 1);
        expectedTable.withEntry(1L, "gerry_link_board", "party_bus", "bus", 5.0, 10.0, 2);
        expectedTable.withEntry(2L, "fitz_link_board", "party_bus", "bus", 10.0, 15.0, 3);
        expectedTable.withEntry(3L, "gerry_link_alight", "party_bus", "bus", 15.0, 20.0, 2);
        expectedTable.withEntry(4L, "fitz_link_alight", "party_bus", "bus", 20.0, 25.0, 1);
        expectedTable.withEntry(5L, "end_link", "party_bus", "bus", 25.0, 30.0, 1);

        assertThat(
                linkLog.getLinkLogData())
                .isEqualTo(expectedTable.getTable());
    }

    @Test
    public void vehicleOccupancyTableHasExpectedData() {
        LinkLogVehicleOccupancyTable expectedTable = new LinkLogVehicleOccupancyTable();
        expectedTable.withEntry(0L, "driver");
        expectedTable.withEntry(1L, "driver");
        expectedTable.withEntry(1L, "gerry");
        expectedTable.withEntry(2L, "driver");
        expectedTable.withEntry(2L, "gerry");
        expectedTable.withEntry(2L, "fitz");
        expectedTable.withEntry(3L, "driver");
        expectedTable.withEntry(3L, "fitz");
        expectedTable.withEntry(4L, "driver");
        expectedTable.withEntry(5L, "driver");

        assertThat(
                linkLog.getVehicleOccupantsData())
                .isEqualTo(expectedTable.getTable());
    }
}
