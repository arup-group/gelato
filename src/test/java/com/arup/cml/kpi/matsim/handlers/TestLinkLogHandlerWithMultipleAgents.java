package com.arup.cml.kpi.matsim.handlers;

import com.arup.cml.kpi.TableHelpers;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.vehicles.Vehicle;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.LongColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.Collections;
import java.util.stream.LongStream;


public class TestLinkLogHandlerWithMultipleAgents {
    private final LinkLogHandler linkLogHandler = new LinkLogHandler();

    @Before
    public void setup() {
        Id<Person> driver = Id.createPersonId("driver");
        Id<Person> gerry = Id.createPersonId("gerry");
        Id<Person> fitz = Id.createPersonId("fitz");
        Id<Vehicle> partyBusVehicle = Id.createVehicleId("party_bus");

        EventsManager eventsManager = EventsUtils.createEventsManager();
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
    public void testExpectedDataInLinkLogTable() {
        TableHelpers.assertTableDataEqual(
                linkLogHandler.getLinkLog(),
                Table.create("Link Log")
                        .addColumns(
                                LongColumn.create("index", LongStream.range(0, 6).toArray()),
                                StringColumn.create("linkID",
                                        "start_link", "gerry_link_board", "fitz_link_board", "gerry_link_alight", "fitz_link_alight",  "end_link"),
                                StringColumn.create("vehicleID", Collections.nCopies(6, "party_bus")),
                                DoubleColumn.create("startTime", new Double[]{0.0, 5.0, 10.0, 15.0, 20.0, 25.0}),
                                DoubleColumn.create("endTime", new Double[]{5.0, 10.0, 15.0, 20.0, 25.0, 30.0}),
                                DoubleColumn.create("numberOfPeople", new Double[]{1.0, 2.0, 3.0, 2.0, 1.0, 1.0})
                        )
        );
    }

    @Test
    public void testExpectedDataInVehicleOccupancyTable() {
        TableHelpers.assertTableDataEqual(
                linkLogHandler.getVehicleOccupancy(),
                Table.create("Vehicle Occupancy")
                        .addColumns(
                                DoubleColumn.create("linkLogIndex", new Long[]{0L, 1L, 1L, 2L, 2L, 2L, 3L, 3L, 4L, 5L}),
                                StringColumn.create("agentId",
                                        "driver", "driver", "gerry", "driver", "gerry", "fitz", "driver", "fitz", "driver", "driver")
                        )
        );
    }
}
