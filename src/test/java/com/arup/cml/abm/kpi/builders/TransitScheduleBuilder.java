package com.arup.cml.abm.kpi.builders;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.*;

import java.util.List;

public class TransitScheduleBuilder {
    TransitScheduleFactoryImpl transitScheduleFactory;
    TransitSchedule schedule;
    String defaultMode = "bus";
    String defaultTransitStopId = "StopA";
    double defaultStopCoordX = 0.0;
    double defaultStopCoordY = 0.0;


    public TransitScheduleBuilder() {
        transitScheduleFactory = new TransitScheduleFactoryImpl();
        schedule = transitScheduleFactory.createTransitSchedule();
    }

    public TransitScheduleBuilder withTransitLine(String transitLineId, String mode) {
        return this.withTransitLineWithStop(transitLineId, defaultTransitStopId, defaultStopCoordX, defaultStopCoordY, mode);
    };

    public TransitScheduleBuilder withTransitLineWithStop(
            String transitLineId, String transitStopId, double transitStopCoordX, double transitStopCoordY, String mode) {
        TransitStopFacility transitStop = transitScheduleFactory.createTransitStopFacility(
                Id.create(transitStopId, TransitStopFacility.class),
                new Coord(transitStopCoordX, transitStopCoordY),
                false
        );
        schedule.addStopFacility(transitStop);
        TransitRouteStop routeStop = transitScheduleFactory.createTransitRouteStop(transitStop, 0,0);
        transitStop.setLinkId(Id.create("accessLink", Link.class));

        TransitLine tL = transitScheduleFactory.createTransitLine(Id.create(transitLineId, TransitLine.class));
        tL.addRoute(transitScheduleFactory.createTransitRoute(
                Id.create(String.format("%s_1", transitLineId), TransitRoute.class),
                RouteUtils.createNetworkRoute(List.of(Id.create("accessLink", Link.class))),
                List.of(routeStop),
                mode
        ));
        schedule.addTransitLine(tL);
        return this;
    }

    public TransitScheduleBuilder withTransitStopWithMode(
            String transitStopID, double coordX, double coordY, String mode) {
        String transitLineId = String.format("transitLine_%d", schedule.getTransitLines().size());
        return this.withTransitLineWithStop(transitLineId, transitStopID, coordX, coordY, mode);
    }

    public TransitScheduleBuilder withTransitStop(
            String transitStopID, double coordX, double coordY) {
        return this.withTransitStopWithMode(transitStopID, coordX, coordY, defaultMode);
    }

    public TransitSchedule build() {
        return this.schedule;
    }
}
