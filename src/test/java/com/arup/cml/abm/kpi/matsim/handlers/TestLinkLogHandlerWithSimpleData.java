package com.arup.cml.abm.kpi.matsim.handlers;

import com.arup.cml.abm.kpi.TableHelpers;
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


public class TestLinkLogHandlerWithSimpleData {
    private final LinkLogHandler linkLogHandler = new LinkLogHandler();

    @Before
    public void setup() {
        Id<Person> gerry = Id.createPersonId("gerry");
        Id<Vehicle> gerryVehicle = Id.createVehicleId("gerry_wheels");

        EventsManager eventsManager = EventsUtils.createEventsManager();
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
        TableHelpers.assertTableDataEqual(
                linkLogHandler.getLinkLog(),
                Table.create("Link Log")
                        .addColumns(
                                LongColumn.create("index", LongStream.range(0, 2).toArray()),
                                StringColumn.create("linkID", "link_A_B", "link_B_A"),
                                StringColumn.create("vehicleID", "gerry_wheels", "gerry_wheels"),
                                StringColumn.create("mode", Collections.nCopies(2, "car")),
                                DoubleColumn.create("startTime", new Double[]{0.0, 6.0}),
                                DoubleColumn.create("endTime", new Double[]{5.0, 10.0}),
                                DoubleColumn.create("numberOfPeople", new Double[]{1.0, 1.0})
                        )
        );
    }

    @Test
    public void vehicleOccupancyTableHasExpectedData() {
        TableHelpers.assertTableDataEqual(
                linkLogHandler.getVehicleOccupancy(),
                Table.create("Vehicle Occupancy")
                        .addColumns(
                                DoubleColumn.create("linkLogIndex", new Long[]{0L, 1L}),
                                StringColumn.create("agentId", "gerry", "gerry")
                        )
        );
    }
}
