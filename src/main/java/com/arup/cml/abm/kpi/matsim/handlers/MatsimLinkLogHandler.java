package com.arup.cml.abm.kpi.matsim.handlers;

import com.arup.cml.abm.kpi.data.LinkLog;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MatsimLinkLogHandler implements
        VehicleEntersTrafficEventHandler,
        VehicleLeavesTrafficEventHandler,
        PersonEntersVehicleEventHandler,
        PersonLeavesVehicleEventHandler,
        LinkEnterEventHandler,
        LinkLeaveEventHandler {

    private LinkLog linkLog;
    private final Map<String, AtomicInteger> eventCounts = new HashMap<>();

    public MatsimLinkLogHandler(LinkLog linkLog) {
        this.linkLog = linkLog;
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        incrementEventCount(event);
        linkLog.newLinkLogEntry(
                event.getVehicleId().toString(),
                event.getLinkId().toString(),
                event.getTime()
        );
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        incrementEventCount(event);
        linkLog.completeLinkLogEntry(
                event.getVehicleId().toString(),
                event.getTime()
        );
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        incrementEventCount(event);
        linkLog.personBoardsVehicle(
                event.getVehicleId().toString(),
                event.getPersonId().toString()
        );
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        incrementEventCount(event);
        linkLog.personAlightsVehicle(
                event.getVehicleId().toString(),
                event.getPersonId().toString()
        );
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        incrementEventCount(event);
        linkLog.recordVehicleMode(event.getVehicleId().toString(), event.getNetworkMode());
        linkLog.newLinkLogEntry(
                event.getVehicleId().toString(),
                event.getLinkId().toString(),
                event.getTime()
        );
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        incrementEventCount(event);
        linkLog.completeLinkLogEntry(
                event.getVehicleId().toString(),
                event.getTime()
        );
    }

    public Map<String, AtomicInteger> getEventCounts() {
        return this.eventCounts;
    }

    private void incrementEventCount(Event e) {
        eventCounts.putIfAbsent(e.getEventType(), new AtomicInteger(0));
        eventCounts.get(e.getEventType()).incrementAndGet();
    }
}
