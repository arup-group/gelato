package com.arup.cml.abm.kpi.matsim.handlers;

import com.arup.cml.abm.kpi.KpiCalculator;
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

    private KpiCalculator kpiCalculator;
    private final Map<String, AtomicInteger> eventCounts = new HashMap<>();

    public MatsimLinkLogHandler(KpiCalculator kpiCalculator) {
        this.kpiCalculator = kpiCalculator;
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        incrementEventCount(event);
        kpiCalculator.linkEntered(
                event.getVehicleId().toString(),
                event.getLinkId().toString(),
                event.getTime()
        );
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        incrementEventCount(event);
        kpiCalculator.linkExited(
                event.getVehicleId().toString(),
                event.getLinkId().toString(),
                event.getTime()
        );
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        incrementEventCount(event);
        kpiCalculator.vehicleEntered(
                event.getVehicleId().toString(),
                event.getPersonId().toString()
        );
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        incrementEventCount(event);
        kpiCalculator.vehicleExited(
                event.getVehicleId().toString(),
                event.getPersonId().toString()
        );
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        incrementEventCount(event);
        // this is the earliest event we will see for this vehicle, and includes the
        // mode, which is missing from subsequent link events, so we must grab it now
        kpiCalculator.recordVehicleMode(event.getVehicleId().toString(), event.getNetworkMode());
        kpiCalculator.linkEntered(
                event.getVehicleId().toString(),
                event.getLinkId().toString(),
                event.getTime()
        );
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        incrementEventCount(event);
        kpiCalculator.linkExited(
                event.getVehicleId().toString(),
                event.getLinkId().toString(),
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
