package com.arup.cml.abm.kpi.matsim.handlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;
import tech.tablesaw.api.*;

import java.util.*;
import java.util.stream.LongStream;

public class LinkLogHandler implements
        VehicleEntersTrafficEventHandler,
        VehicleLeavesTrafficEventHandler,
        PersonEntersVehicleEventHandler,
        PersonLeavesVehicleEventHandler,
        LinkEnterEventHandler,
        LinkLeaveEventHandler {

    // arrays to collect Link Log data, each will form a column of the Link Log
    private final ArrayList<String> vehicleIDColumn = new ArrayList<>();
    private final ArrayList<String> linkIDColumn = new ArrayList<>();
    private final LinkedHashMap<Long, String> modeColumn = new LinkedHashMap<>();
    private final ArrayList<Double> startTimeColumn = new ArrayList<>();
    private final LinkedHashMap<Long, Double> endTimeColumn = new LinkedHashMap<>();
    private final LinkedHashMap<Long, Integer> numberOfPeopleColumn = new LinkedHashMap<>();

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

    private void newLinkLogEntry(Id<Vehicle> vehicleID, Id<Link> linkID, String mode, double startTime) {
        vehicleIDColumn.add(vehicleID.toString());
        linkIDColumn.add(linkID.toString());
        modeColumn.put(index, mode);
        startTimeColumn.add(startTime);
        // end time is not known yet, a placeholder in the ordered list is saved
        endTimeColumn.put(index, -1.0);
        // placeholder for people in the vehicle as well - someone might enter the vehicle before it leaves the link
        numberOfPeopleColumn.put(index, -1);
        // todo label aborted vehicles that retain -1 placeholder values
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
        // update end time
        endTimeColumn.put(latestStateIndex, endTime);
        // update vehicle occupants
        ArrayList<Id<Person>> currentOccupants = vehicleLatestOccupants.get(vehicleID);
        numberOfPeopleColumn.put(latestStateIndex, currentOccupants.size());
        newVehicleOccupantsEntry(vehicleID, latestStateIndex);
    }

    public Table getLinkLog() {
        return Table.create("Link Log")
                .addColumns(
                        LongColumn.create("index", LongStream.range(0, index).toArray()),
                        StringColumn.create("linkID", linkIDColumn),
                        StringColumn.create("vehicleID", vehicleIDColumn),
                        StringColumn.create("mode", modeColumn.values()),
                        DoubleColumn.create("startTime", startTimeColumn),
                        DoubleColumn.create("endTime", endTimeColumn.values()),
                        DoubleColumn.create("numberOfPeople", numberOfPeopleColumn.values())
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

//    public void write(String outputDir) {
//        getLinkLog().write().csv(String.format("%s/linkLog.csv", outputDir));
//        getVehicleOccupancy().write().csv(String.format("%s/vehicleOccupancy.csv", outputDir));
//    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        newLinkLogEntry(event.getVehicleId(), event.getLinkId(), event.getNetworkMode(), event.getTime());
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        updateLinkLogEntry(event.getVehicleId(), event.getTime());
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        Id<Vehicle> vehicle = event.getVehicleId();
        if (vehicleLatestOccupants.containsKey(vehicle)) {
            ArrayList<Id<Person>> latestOccupants = vehicleLatestOccupants.get(vehicle);
            latestOccupants.add(event.getPersonId());
        } else {
            ArrayList<Id<Person>> latestOccupants = new ArrayList<>();
            latestOccupants.add(event.getPersonId());
            vehicleLatestOccupants.put(vehicle, latestOccupants);
        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        Id<Vehicle> vehicle = event.getVehicleId();
        ArrayList<Id<Person>> latestOccupants = vehicleLatestOccupants.get(vehicle);
        latestOccupants.remove(event.getPersonId());
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        Id<Vehicle> vehicleID = event.getVehicleId();
        // this event does not hold mode information, we take it from previous record of this vehicle
        long latestStateIndex = vehicleLatestLogIndex.get(vehicleID);
        String mode = modeColumn.get(latestStateIndex);
        newLinkLogEntry(vehicleID,  event.getLinkId(),  mode, event.getTime());
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        updateLinkLogEntry(event.getVehicleId(), event.getTime());
    }
}