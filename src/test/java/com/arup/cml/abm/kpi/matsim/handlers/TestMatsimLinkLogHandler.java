package com.arup.cml.abm.kpi.matsim.handlers;

import com.arup.cml.abm.kpi.data.LinkLog;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;


public class TestMatsimLinkLogHandler {
    private final Id<Person> gerry = Id.createPersonId("gerry");
    private final Id<Vehicle> gerryVehicle = Id.createVehicleId("gerry_wheels");
    private final Id<Link> link = Id.createLinkId("link_A_B");
    private final String mode = "car";

    @Test
    public void testPersonBoardingVehicle() {
        LinkLog mockLinkLog = Mockito.mock(LinkLog.class);
        MatsimLinkLogHandler linkLogHandler = new MatsimLinkLogHandler(mockLinkLog);

        linkLogHandler.handleEvent(
                new PersonEntersVehicleEvent(0, gerry, gerryVehicle)
        );

        verify(mockLinkLog).personBoardsVehicle(gerryVehicle.toString(), gerry.toString());
    }

    @Test
    public void testPersonLeavingVehicle() {
        LinkLog mockLinkLog = Mockito.mock(LinkLog.class);
        MatsimLinkLogHandler linkLogHandler = new MatsimLinkLogHandler(mockLinkLog);

        linkLogHandler.handleEvent(
                new PersonLeavesVehicleEvent(11, gerry, gerryVehicle)
        );

        verify(mockLinkLog).personAlightsVehicle(gerryVehicle.toString(), gerry.toString());
    }

    @Test
    public void testModeOfVehicleIsRecordedWhenJoiningTraffic() {
        LinkLog mockLinkLog = Mockito.mock(LinkLog.class);
        MatsimLinkLogHandler linkLogHandler = new MatsimLinkLogHandler(mockLinkLog);

        linkLogHandler.handleEvent(
                new VehicleEntersTrafficEvent(0, gerry, link, gerryVehicle, mode, 1.0)
        );

        verify(mockLinkLog).recordVehicleMode(gerryVehicle.toString(), mode);
    }

    @Test
    public void testNewLinkLogEntryWhenVehicleJoinsTraffic() {
        LinkLog mockLinkLog = Mockito.mock(LinkLog.class);
        MatsimLinkLogHandler linkLogHandler = new MatsimLinkLogHandler(mockLinkLog);

        linkLogHandler.handleEvent(
                new VehicleEntersTrafficEvent(0, gerry, link, gerryVehicle, mode, 1.0)
        );

        verify(mockLinkLog).newLinkLogEntry(gerryVehicle.toString(), link.toString(), 0);
    }

    @Test
    public void testLinkLogEntryIsUpdatedWhenVehicleLeavesTraffic() {
        LinkLog mockLinkLog = Mockito.mock(LinkLog.class);
        MatsimLinkLogHandler linkLogHandler = new MatsimLinkLogHandler(mockLinkLog);

        linkLogHandler.handleEvent(
                new VehicleLeavesTrafficEvent(10, gerry, link, gerryVehicle, mode, 1.0)
        );

        verify(mockLinkLog).completeLinkLogEntry(gerryVehicle.toString(), 10);
    }

    @Test
    public void testNewLinkLogEntryWhenVehicleEntersNewLink() {
        LinkLog mockLinkLog = Mockito.mock(LinkLog.class);
        MatsimLinkLogHandler linkLogHandler = new MatsimLinkLogHandler(mockLinkLog);

        linkLogHandler.handleEvent(
                new LinkEnterEvent(5, gerryVehicle, link)
        );

        verify(mockLinkLog).newLinkLogEntry(gerryVehicle.toString(), link.toString(), 5);
    }

    @Test
    public void testLogEntryIsUpdatedWhenVehicleLeavesLink() {
        LinkLog mockLinkLog = Mockito.mock(LinkLog.class);
        MatsimLinkLogHandler linkLogHandler = new MatsimLinkLogHandler(mockLinkLog);

        linkLogHandler.handleEvent(
                new LinkLeaveEvent(5, gerryVehicle, link)
        );

        verify(mockLinkLog).completeLinkLogEntry(gerryVehicle.toString(), 5);
    }
}
