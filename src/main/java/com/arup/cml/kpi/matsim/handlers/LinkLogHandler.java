package com.arup.cml.kpi.matsim.handlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

//import tech.tablesaw.DoubleColoumn;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class LinkLogHandler implements VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler,
        PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
        LinkEnterEventHandler, LinkLeaveEventHandler {
    private final Map<Long, Map<Object, Object>> linkLog = new HashMap<>();
    private final Map<Id<Vehicle>, Long> vehicleLatestLog = new HashMap<>();
    private final Map<Long, ArrayList<Id<Person>>> vehicleOccupants = new HashMap<Long, ArrayList<Id<Person>>>();
    private final Map<Id<Vehicle>, ArrayList<Id<Person>>> vehicleLatestOccupants = new HashMap<>();
    private long index = 0;

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        Map<Object, Object> log = new HashMap<>();
        log.put("vehicleID", event.getVehicleId());
        log.put("linkID", event.getLinkId().toString());
        log.put("startTime", event.getTime());
        ArrayList<Id<Person>> currentOccupants = this.vehicleLatestOccupants.get(event.getVehicleId());
        log.put("numberOfPeople", currentOccupants.size());
        log.put("people", currentOccupants.clone());
        this.linkLog.put(
                index,
                log
        );
        this.vehicleOccupants.put(
                index,
                (ArrayList<Id<Person>>) currentOccupants.clone()
        );
        this.vehicleLatestLog.put(event.getVehicleId(), index);
        this.index++;
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        long latestStateIndex = this.vehicleLatestLog.get(event.getVehicleId());
        Map<Object, Object> log = this.linkLog.get(latestStateIndex);
        log.put("endTime", event.getTime());
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
        Map<Object, Object> log = new HashMap<>();
        log.put("vehicleID", event.getVehicleId());
        log.put("linkID", event.getLinkId().toString());
        log.put("startTime", event.getTime());
        ArrayList<Id<Person>> currentOccupants = this.vehicleLatestOccupants.get(event.getVehicleId());
        log.put("numberOfPeople", currentOccupants.size());
        log.put("people", currentOccupants.clone());
        this.linkLog.put(
                index,
                log
        );
        this.vehicleOccupants.put(
                index,
                (ArrayList<Id<Person>>) currentOccupants.clone()
        );
        this.vehicleLatestLog.put(event.getVehicleId(), index);
        this.index++;
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        long latestStateIndex = this.vehicleLatestLog.get(event.getVehicleId());
        Map<Object, Object> log = this.linkLog.get(latestStateIndex);
        log.put("endTime", event.getTime());
    }

    public void toCsv() {
        FileWriter fileWriter = null;

        final String COMMA_DELIMITER = ",";
        final String NEW_LINE_SEPARATOR = "\n";
        final String FILE_HEADER = "id,vehicleID,linkID,startTime,endTime,numberOfPeople,people";

        try {
            fileWriter = new FileWriter("./linkLog.csv");

            // Write the CSV file header
            fileWriter.append(FILE_HEADER.toString());
            // Add a new line separator after the header
            fileWriter.append(NEW_LINE_SEPARATOR);

            // Write user data to the CSV file
            for (var linkLogEntry : this.linkLog.entrySet()) {
                index = linkLogEntry.getKey();
                fileWriter.append(String.valueOf(index));
                fileWriter.append(COMMA_DELIMITER);
                Map<Object, Object> linkLog = linkLogEntry.getValue();
                fileWriter.append(String.valueOf(linkLog.get("vehicleID")));
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(String.valueOf(linkLog.get("linkID")));
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(String.valueOf(linkLog.get("startTime")));
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(String.valueOf(linkLog.get("endTime")));
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(String.valueOf(linkLog.get("numberOfPeople")));
                fileWriter.append(COMMA_DELIMITER);
                if (this.vehicleOccupants.containsKey(index)) {
                    ArrayList<Id<Person>> ppl = (ArrayList<Id<Person>>) linkLog.get("people");
                    StringBuilder pplString = new StringBuilder();
                    for (Iterator<Id<Person>> it = ppl.iterator(); it.hasNext(); ) {
                        Id<Person> id = it.next();
                        pplString.append(id.toString()).append(":");
                    }
                    fileWriter.append(pplString.toString());
                } else {
                    fileWriter.append("None");
                }
                fileWriter.append(NEW_LINE_SEPARATOR);
            }
        } catch (IOException e) {
            System.out.println("Error in CsvFileWriter !!!");
            e.printStackTrace();
        } finally {

            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }

        }
    }
}