package com.arup.cml.kpi.matsim.handlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;
import tech.tablesaw.api.*;

import java.util.*;
import java.util.stream.LongStream;

public class LinkLogHandler implements VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler,
        PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
        LinkEnterEventHandler, LinkLeaveEventHandler {

    // arrays to collect Link Log data, each will form a column of the Link Log
    private final ArrayList<String> vehicleIDColumn = new ArrayList<>();
    private final ArrayList<String> linkIDColumn = new ArrayList<>();
    private final ArrayList<Double> startTimeColumn = new ArrayList<>();
    private final ArrayList<Double> endTimeColumn = new ArrayList<>();
    private final ArrayList<Integer> numberOfPeopleColumn = new ArrayList<>();

    // points to the index of the most recent reference of that vehicle ID in the Link Log
    private final Map<Id<Vehicle>, Long> vehicleLatestLogIndex = new HashMap<>();

    // arrays to collect agent IDs that are inside the vehicle in reference to the Link Log entries
    // these will form columns of the Vehicle Occupancy table, it shows who is in the vehicle at a point in the Link Log
    private final ArrayList<Long> linkLogIndexColumn = new ArrayList<>();
    private final ArrayList<String> agentIDColumn = new ArrayList<>();

    // tracks the most recent occupants of a vehicle
    private final Map<Id<Vehicle>, ArrayList<Id<Person>>> vehicleLatestOccupants = new HashMap<>();

    // Link Log entry index
    private long index = 0;

    private void newLinkLogEntry(Id<Vehicle> vehicleID, Id<Link> linkID, double startTime) {
        vehicleIDColumn.add(vehicleID.toString());
        linkIDColumn.add(linkID.toString());
        startTimeColumn.add(startTime);
        // end time is not known yet, a placeholder in the ordered list is saved
        endTimeColumn.add(-1.0);
        // placeholder for people in the vehicle as well - someone might enter the vehicle before it leaves the link
        numberOfPeopleColumn.add(-1);
        vehicleLatestLogIndex.put(vehicleID, index);
        index++;
    }

    private void newVehicleOccupantsEntry(Id<Vehicle> vehicleID, long idx) {
        ArrayList<Id<Person>> currentOccupants = vehicleLatestOccupants.get(vehicleID);
        for (Id<Person> personID : currentOccupants) {
            linkLogIndexColumn.add(idx);
            agentIDColumn.add(personID.toString());
        }
    }

    private void updateLinkLogEntry(Id<Vehicle> vehicleID, double endTime) {
        long latestStateIndex = this.vehicleLatestLogIndex.get(vehicleID);
        // TODO: this cast to int is undesirable but seems impossible to set at non int index of array
        // update end time
        endTimeColumn.set((int) latestStateIndex, endTime);
        // update vehicle occupants
        ArrayList<Id<Person>> currentOccupants = vehicleLatestOccupants.get(vehicleID);
        numberOfPeopleColumn.set((int) latestStateIndex, currentOccupants.size());
        newVehicleOccupantsEntry(vehicleID, latestStateIndex);
    }

    public Table getLinkLog() {
        return Table.create("Link Log")
                .addColumns(
                        LongColumn.create("index", LongStream.range(0, index).toArray()),
                        StringColumn.create("linkID", linkIDColumn),
                        StringColumn.create("vehicleID", vehicleIDColumn),
                        DoubleColumn.create("startTime", startTimeColumn),
                        DoubleColumn.create("endTime", endTimeColumn),
                        DoubleColumn.create("numberOfPeople", numberOfPeopleColumn)
                );
    }

    public Table getVehicleOccupancy() {
        return Table.create("Vehicle Occupancy")
                .addColumns(
                        // TODO: This should be LongColumn (reference to index LongColumn of "Link Log" table)
                        // but can't get it to work with LongColumn constructor as ArrayList<Long>
                        DoubleColumn.create("linkLogIndex", linkLogIndexColumn),
                        StringColumn.create("agentId", agentIDColumn)
                );
    }

    public void write(String outputDir) {
        getLinkLog().write().csv(String.format("%s/linkLog.csv", outputDir));
        getVehicleOccupancy().write().csv(String.format("%s/vehicleOccupancy.csv", outputDir));
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        newLinkLogEntry(event.getVehicleId(), event.getLinkId(), event.getTime());
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        updateLinkLogEntry(event.getVehicleId(), event.getTime());
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
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        updateLinkLogEntry(event.getVehicleId(), event.getTime());
    }
}