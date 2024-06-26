package com.arup.cml.abm.kpi.builders;

import com.arup.cml.abm.kpi.data.LinkLog;
import com.arup.cml.abm.kpi.data.MoneyLog;
import com.arup.cml.abm.kpi.tablesaw.TablesawKpiCalculator;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;
import java.io.InputStream;
import java.nio.file.Path;

public class KpiCalculatorBuilder {
    TemporaryFolder tmpDir;
    Network network = new NetworkBuilder().build();
    TransitSchedule schedule = new TransitScheduleBuilder().build();
    String legs;
    String trips;
    Vehicles vehicles = new VehiclesBuilder().build();
    LinkLog linkLog = new LinkLog();
    MoneyLog moneyLog = new MoneyLog();
    String persons;
    ActivityFacilities facilities = new FacilitiesBuilder().build();
    ScoringConfigGroup scoring = new ScoringConfigBuilder().build();

    public KpiCalculatorBuilder(TemporaryFolder tmpDir) {
        this.tmpDir = tmpDir;
        this.legs = new LegsTableBuilder(tmpDir).build();
        this.trips = new TripsTableBuilder(tmpDir).build();
        this.persons = new PersonsBuilder(tmpDir).build();
    }

    public KpiCalculatorBuilder withLinkLog(LinkLog linkLog) {
        this.linkLog = linkLog;
        return this;
    }

    public KpiCalculatorBuilder withNetwork(Network network) {
        this.network = network;
        return this;
    }

    public KpiCalculatorBuilder withTransitSchedule(TransitSchedule schedule) {
        this.schedule = schedule;
        return this;
    }

    public KpiCalculatorBuilder withLegs(String legs) {
        this.legs = legs;
        return this;
    }

    public KpiCalculatorBuilder withTrips(String trips) {
        this.trips = trips;
        return this;
    }

    public KpiCalculatorBuilder withVehicles(Vehicles vehicles) {
        this.vehicles = vehicles;
        return this;
    }

    public KpiCalculatorBuilder withPersons(String persons) {
        this.persons = persons;
        return this;
    }

    public KpiCalculatorBuilder withFacilities(ActivityFacilities facilities) {
        this.facilities = facilities;
        return this;
    }

    public KpiCalculatorBuilder withScoring(ScoringConfigGroup scoring) {
        this.scoring = scoring;
        return this;
    }

    public TablesawKpiCalculator build() {
        return new TablesawKpiCalculator(network, schedule, vehicles, linkLog, getInputStream(persons), moneyLog, scoring,
                facilities, getInputStream(legs), getInputStream(trips),
                Path.of(tmpDir.getRoot().getAbsolutePath()), ControllerConfigGroup.CompressionType.gzip
        );
    }

    private InputStream getInputStream(String path) {
        return IOUtils.getInputStream(IOUtils.resolveFileOrResource(path));
    }
}
