package com.arup.cml.abm.kpi.tablesaw;

import com.arup.cml.abm.kpi.data.LinkLog;
import org.junit.rules.TemporaryFolder;
import org.matsim.analysis.TripsAndLegsWriter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.router.DefaultAnalysisMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class KpiCalculatorBuilder {
    TemporaryFolder tmpDir;
    Scenario scenario;
    LinkLog linkLog = new LinkLog();
    double defaultLinkLength = 10;
    double defaultLinkSpeed = 10;
    double defaultLinkCapacity = 300;
    int defaultLinkLanes = 1;

    public KpiCalculatorBuilder(TemporaryFolder tmpDir) {
        this.tmpDir = tmpDir;
        // default, 'empty', matsim scenario
        Config config = ConfigUtils.createConfig();
        config.transit().setUseTransit(true);

        ScenarioUtils.ScenarioBuilder scBuilder = new ScenarioUtils.ScenarioBuilder(config);
        this.scenario = (MutableScenario) scBuilder.build();
    }

    public KpiCalculatorBuilder withLinkLogEntry(String vehicleID, String linkID, double startTime, double endTime) {
        linkLog.recordVehicleMode(vehicleID, "car");
        linkLog.newLinkLogEntry(vehicleID, linkID, startTime);
        linkLog.completeLinkLogEntry(vehicleID, endTime);

        if (!scenario.getNetwork().getLinks().containsKey(Id.create(linkID, Link.class))) {
            withNetworkNode("A", 1, 1);
            withNetworkNode("B", 2, 2);
            withNetworkLink(linkID, "A", "B");
        }
        return this;
    }

    public KpiCalculatorBuilder withNetworkLink(String id, String fromNode, String toNode, double length, double speed, double capacity, int lanes) {
        Link link = scenario.getNetwork().getFactory().createLink(
                Id.create(id, Link.class),
                scenario.getNetwork().getNodes().get(Id.create(fromNode, Node.class)),
                scenario.getNetwork().getNodes().get(Id.create(toNode, Node.class))
        );
        link.setLength(length);
        link.setFreespeed(speed);
        link.setCapacity(capacity);
        link.setNumberOfLanes(lanes);
        scenario.getNetwork().addLink(link);
        return this;
    }

    public KpiCalculatorBuilder withNetworkLink(String id, String fromNode, String toNode) {
        return withNetworkLink(id, fromNode, toNode, defaultLinkLength, defaultLinkSpeed, defaultLinkCapacity, defaultLinkLanes);
    }

    public KpiCalculatorBuilder withNetworkLinkWithSpeed(String id, String fromNode, String toNode, double speed) {
        return withNetworkLink(id, fromNode, toNode, defaultLinkLength, speed, defaultLinkCapacity, defaultLinkLanes);
    }

    public KpiCalculatorBuilder withNetworkNode(String id, double x, double y) {
        Node node = scenario.getNetwork().getFactory().createNode(Id.create(id, Node.class), new Coord(x, y));
        scenario.getNetwork().addNode(node);
        return this;
    }

    public KpiCalculatorBuilder withVehicle(String id, String vehicleType) {
        Id<VehicleType> vehicleTypeId = Id.create(vehicleType, VehicleType.class);
        if (scenario.getVehicles().getVehicleTypes().containsKey(vehicleTypeId)) {
            VehicleType matsimVehicleType = scenario.getVehicles().getVehicleTypes().get(vehicleTypeId);
            Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId(id), matsimVehicleType);
            scenario.getVehicles().addVehicle(vehicle);
        } else {
            VehicleType matsimVehicleType = VehicleUtils.createVehicleType(vehicleTypeId);
            scenario.getVehicles().addVehicleType(matsimVehicleType);
            Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId(id), matsimVehicleType);
            scenario.getVehicles().addVehicle(vehicle);
        }
        return this;
    }

    public TablesawKpiCalculator build() {
        // this writes the legs and trips and then produces an input stream for the kpi calculator
        NoTripWriterExtension tripsWriterExtension = new NoTripWriterExtension();
        NoLegsWriterExtension legWriterExtension = new NoLegsWriterExtension();
        DefaultTimeWriter timeWriter = new DefaultTimeWriter();
        DefaultAnalysisMainModeIdentifier mainModeIdentifier = new DefaultAnalysisMainModeIdentifier();
        TripsAndLegsWriter writer = new TripsAndLegsWriter(scenario, tripsWriterExtension, legWriterExtension, mainModeIdentifier, timeWriter);
        IdMap<Person, Plan> plans = new IdMap<>(Person.class);
        for (Person pers : scenario.getPopulation().getPersons().values()) {
            for (Plan pl : pers.getPlans()) {
                plans.put(pers.getId(), pl);
            }
        }

        String legsPath = String.valueOf(Path.of(tmpDir.getRoot().getAbsolutePath(), "output_trips.csv"));
        String tripsPath = String.valueOf(Path.of(tmpDir.getRoot().getAbsolutePath(), "output_legs.csv"));
        writer.write(plans, tripsPath, legsPath);

        return new TablesawKpiCalculator(
                scenario.getNetwork(), scenario.getTransitSchedule(), scenario.getVehicles(),
                linkLog, getInputStream(legsPath), getInputStream(tripsPath),
                Path.of(tmpDir.getRoot().getAbsolutePath()), ControllerConfigGroup.CompressionType.gzip
        );

    }

    public InputStream getInputStream(String path) {
        return IOUtils.getInputStream(IOUtils.resolveFileOrResource(path));
    }

    //////
    // for some reason I could not import these classes from TripsAndLegsWriter so I copy-pasted them and I hate it.
    //////
    static class NoLegsWriterExtension implements TripsAndLegsWriter.CustomLegsWriterExtension {
        NoLegsWriterExtension() {
        }

        public String[] getAdditionalLegHeader() {
            return new String[0];
        }

        public List<String> getAdditionalLegColumns(TripStructureUtils.Trip experiencedTrip, Leg experiencedLeg) {
            return Collections.EMPTY_LIST;
        }
    }

    static class NoTripWriterExtension implements TripsAndLegsWriter.CustomTripsWriterExtension {
        NoTripWriterExtension() {
        }

        public String[] getAdditionalTripHeader() {
            return new String[0];
        }

        public List<String> getAdditionalTripColumns(TripStructureUtils.Trip trip) {
            return Collections.EMPTY_LIST;
        }
    }

    static class DefaultTimeWriter implements TripsAndLegsWriter.CustomTimeWriter {
        DefaultTimeWriter() {
        }

        public String writeTime(double time) {
            return Time.writeTime(time);
        }
    }
}

