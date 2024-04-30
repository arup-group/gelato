package com.arup.cml.abm.kpi.builders;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class LegBuilder {
    Leg leg = PopulationUtils.createLeg("car");

    public LegBuilder() {
        leg.setRoute(RouteUtils.createGenericRouteImpl(Id.create("1-1", Link.class), Id.create("2-2", Link.class)));
        leg.getAttributes().putAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME, "specialVehicle");
        leg.setDepartureTime(0.0);
        leg.setTravelTime(0.0);
    }

    public LegBuilder withDistance(Integer distance) {
        return this;
    }

    public LegBuilder withMode(String mode) {
        return this;
    }

    public LegBuilder withDepTime(String depTime) {
        leg.setDepartureTime(getTimeInSeconds(depTime));
        return this;
    }

    private double getTimeInSeconds(String time) {
        String[] timeComponents = time.split(":");
        double hours = Double.parseDouble(timeComponents[0]);
        double minutes = Double.parseDouble(timeComponents[1]);
        double seconds = Double.parseDouble(timeComponents[2]);
        return (hours * 60.0 * 60.0) + (minutes * 60.0) + seconds;
    }

    public LegBuilder withTravTime(String travTime) {
        leg.setTravelTime(getTimeInSeconds(travTime));
        return this;
    }

    public LegBuilder withWaitTime(String waitTime) {
        // this is not the official way to ser wait time in a matsim leg but the official way is too complicated for
        // our purpose
        leg.getAttributes().putAttribute("waitTime", getTimeInSeconds(waitTime));
        return this;
    }

    public LegBuilder ofSomePtType() {
        TransitPassengerRoute route = new DefaultTransitPassengerRoute(
                Id.create("accessLinkId", Link.class),
                Id.create("egressLinkId", Link.class),
                Id.create("accessTransitStopFacilityId", TransitStopFacility.class),
                Id.create("egressTransitStopFacilityId", TransitStopFacility.class),
                Id.create("transitLineId", TransitLine.class),
                Id.create("transitRouteId", TransitRoute.class)
        );
        leg.setRoute(route);
        return this.withWaitTime("00:00:00");
    }

    public Leg build() {
        return leg;
    }
}
