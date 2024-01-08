package com.arup.cml.kpi.matsim.handlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;
import tech.tablesaw.api.*;

//import tech.tablesaw.DoubleColoumn;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

public class LinkLogHandler implements VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler,
        PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
        LinkEnterEventHandler, LinkLeaveEventHandler {
    private final Map<Integer, Map<Object, Object>> linkLog = new HashMap<>();
    private final ArrayList<String> vehicleIDColumn = new ArrayList<String>();
    private final ArrayList<String> linkIDColumn = new ArrayList<String>();
    private final ArrayList<Double> startTimeColumn = new ArrayList<Double>();
    private final ArrayList<Double> endTimeColumn = new ArrayList<Double>();
    private final ArrayList<Integer> numberOfPeopleColumn = new ArrayList<Integer>();

    private final Map<Id<Vehicle>, Integer> vehicleLatestLog = new HashMap<>();
    private final Map<Integer, ArrayList<Id<Person>>> vehicleOccupants = new HashMap<Integer, ArrayList<Id<Person>>>();
    private final Map<Id<Vehicle>, ArrayList<Id<Person>>> vehicleLatestOccupants = new HashMap<>();
    private int index = 0;

    private void newLinkLogEntry(Id<Vehicle> vehicleID, Id<Link> linkID, double startTime) {
        vehicleIDColumn.add(vehicleID.toString());
        linkIDColumn.add(linkID.toString());
        startTimeColumn.add(startTime);
        // end time is not known yet, a placeholder in the ordered list is saved
        endTimeColumn.add((double) -1);
        ArrayList<Id<Person>> currentOccupants = vehicleLatestOccupants.get(vehicleID);
        numberOfPeopleColumn.add(currentOccupants.size());
    }

    private void updateEndTimeInLinkLog(Id<Vehicle> vehicleID, double endTime) {
        int latestStateIndex = this.vehicleLatestLog.get(vehicleID);
        endTimeColumn.set(latestStateIndex, endTime);
    }

    public Table getLinkLog() {
        return Table.create("Link Log")
                .addColumns(
                        IntColumn.create("index", IntStream.range(0, index).toArray()),
                        StringColumn.create("linkID", linkIDColumn),
                        StringColumn.create("vehicleID", vehicleIDColumn),
                        DoubleColumn.create("startTime", startTimeColumn),
                        DoubleColumn.create("endTime", endTimeColumn),
                        DoubleColumn.create("numberOfPeople", numberOfPeopleColumn)
                );
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        newLinkLogEntry(event.getVehicleId(), event.getLinkId(), event.getTime());

        ArrayList<Id<Person>> currentOccupants = vehicleLatestOccupants.get(event.getVehicleId());
        vehicleOccupants.put(
                index,
                (ArrayList<Id<Person>>) currentOccupants.clone()
        );
        this.vehicleLatestLog.put(event.getVehicleId(), index);
        this.index++;
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        updateEndTimeInLinkLog(event.getVehicleId(), event.getTime());
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        Id<Vehicle> vehicle = event.getVehicleId();
        if (this.vehicleLatestOccupants.containsKey(vehicle)) {
            ArrayList<Id<Person>> latestOccupants = this.vehicleLatestOccupants.get(vehicle);
            latestOccupants.add(event.getPersonId());
        } else {
            ArrayList<Id<Person>> latestOccupants = new ArrayList<>();
            latestOccupants.add(event.getPersonId());
            this.vehicleLatestOccupants.put(vehicle, latestOccupants);
        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        Id<Vehicle> vehicle = event.getVehicleId();
        ArrayList<Id<Person>> latestOccupants = this.vehicleLatestOccupants.get(vehicle);
        latestOccupants.remove(event.getPersonId());
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        newLinkLogEntry(event.getVehicleId(), event.getLinkId(), event.getTime());

        ArrayList<Id<Person>> currentOccupants = vehicleLatestOccupants.get(event.getVehicleId());
        this.vehicleOccupants.put(
                index,
                (ArrayList<Id<Person>>) currentOccupants.clone()
        );
        this.vehicleLatestLog.put(event.getVehicleId(), index);
        this.index++;
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        updateEndTimeInLinkLog(event.getVehicleId(), event.getTime());
    }
}