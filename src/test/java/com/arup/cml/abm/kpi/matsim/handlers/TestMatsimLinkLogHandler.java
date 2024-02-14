package com.arup.cml.abm.kpi.matsim.handlers;

import com.arup.cml.abm.kpi.data.LinkLog;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;
import org.mockito.Mockito;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;


public class TestMatsimLinkLogHandler {
    private final Id<Person> gerry = Id.createPersonId("gerry");
    private final Id<Vehicle> gerryVehicle = Id.createVehicleId("gerry_wheels");
    private final double eventTime = 0;
    private final Id<Link> link = Id.createLinkId("link_A_B");
    private final String mode = "car";

    @Test
    public void recordsBoardingVehicle() {
        LinkLog mockLinkLog = Mockito.mock(LinkLog.class);
        MatsimLinkLogHandler linkLogHandler = new MatsimLinkLogHandler(mockLinkLog);

        linkLogHandler.handleEvent(new PersonEntersVehicleEvent(eventTime, gerry, gerryVehicle));

        verify(mockLinkLog).personBoardsVehicle(gerryVehicle.toString(), gerry.toString());
    }

    @Test
    public void recordsPersonLeavingVehicle() {
        LinkLog mockLinkLog = Mockito.mock(LinkLog.class);
        MatsimLinkLogHandler linkLogHandler = new MatsimLinkLogHandler(mockLinkLog);

        linkLogHandler.handleEvent(new PersonLeavesVehicleEvent(eventTime, gerry, gerryVehicle));

        verify(mockLinkLog).personAlightsVehicle(gerryVehicle.toString(), gerry.toString());
    }

    @Test
    public void recordsVehicleModeWhenWhenVehicleJoinsTraffic() {
        LinkLog mockLinkLog = Mockito.mock(LinkLog.class);
        MatsimLinkLogHandler linkLogHandler = new MatsimLinkLogHandler(mockLinkLog);

        linkLogHandler.handleEvent(
                new VehicleEntersTrafficEvent(eventTime, gerry, link, gerryVehicle, mode, 1.0)
        );

        verify(mockLinkLog).recordVehicleMode(gerryVehicle.toString(), mode);
    }

    @Test
    public void createsLinkLogEntryWhenVehicleJoinsTraffic() {
        LinkLog mockLinkLog = Mockito.mock(LinkLog.class);
        MatsimLinkLogHandler linkLogHandler = new MatsimLinkLogHandler(mockLinkLog);

        linkLogHandler.handleEvent(
                new VehicleEntersTrafficEvent(eventTime, gerry, link, gerryVehicle, mode, 1.0)
        );

        verify(mockLinkLog).newLinkLogEntry(gerryVehicle.toString(), link.toString(), eventTime);
    }

    @Test
    public void updatesLogEntryWhenVehicleLeavesTraffic() {
        LinkLog mockLinkLog = Mockito.mock(LinkLog.class);
        MatsimLinkLogHandler linkLogHandler = new MatsimLinkLogHandler(mockLinkLog);

        linkLogHandler.handleEvent(
                new VehicleLeavesTrafficEvent(eventTime, gerry, link, gerryVehicle, mode, 1.0)
        );

        verify(mockLinkLog).completeLinkLogEntry(gerryVehicle.toString(), eventTime);
    }

    @Test
    public void createsNewLinkLogWhenVehicleEntersNewLink() {
        LinkLog mockLinkLog = Mockito.mock(LinkLog.class);
        MatsimLinkLogHandler linkLogHandler = new MatsimLinkLogHandler(mockLinkLog);

        linkLogHandler.handleEvent(new LinkEnterEvent(eventTime, gerryVehicle, link));

        verify(mockLinkLog).newLinkLogEntry(gerryVehicle.toString(), link.toString(), eventTime);
    }

    @Test
    public void updatesLinkLogEntryWhenVehicleLeavesLink() {
        LinkLog mockLinkLog = Mockito.mock(LinkLog.class);
        MatsimLinkLogHandler linkLogHandler = new MatsimLinkLogHandler(mockLinkLog);

        linkLogHandler.handleEvent(new LinkLeaveEvent(eventTime, gerryVehicle, link));

        verify(mockLinkLog).completeLinkLogEntry(gerryVehicle.toString(), eventTime);
    }

    @Test
    public void returnsImmutableCopyOfEventCountsMap() {
        MatsimLinkLogHandler linkLogHandler = new MatsimLinkLogHandler(Mockito.mock(LinkLog.class));
        Map<String, AtomicInteger>  counts = linkLogHandler.getEventCounts();
        assertThat(counts).isInstanceOf(ImmutableMap.class);
    }

    @Test
    public void maintainsACountOfEventTypesSeen() {
        MatsimLinkLogHandler linkLogHandler = new MatsimLinkLogHandler(Mockito.mock(LinkLog.class));
        assertThat(linkLogHandler.getEventCounts().isEmpty()).as("Event counts should initially be empty");

        for (int i = 1; i <= 3; i++) {
            LinkLeaveEvent linkLeaveEvent = new LinkLeaveEvent(eventTime, gerryVehicle, link);
            linkLogHandler.handleEvent(linkLeaveEvent);

            int eventCount = linkLogHandler.getEventCounts().get(linkLeaveEvent.getEventType()).get();
            assertThat(eventCount).isEqualTo(i).as(String.format("Event count should be {}", i));
        }



    }
}
