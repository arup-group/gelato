package com.arup.cml.abm.kpi.builders;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.PopulationUtils;
import org.matsim.facilities.ActivityFacility;

import java.util.Arrays;
import java.util.List;


public class TripBuilder {
    PopulationFactory popfactory = PopulationUtils.getFactory();
    Trip trip = new Trip();
    public class Trip {
        // so it seems making a matsim Trip is kind of convoluted, so here is a simple version
        public Activity startActivity;
        public Activity endActivity;
        public List<Leg> legs;
        public Trip(){
            this.setStartActivity("home", "0-0", new Coord(0, 0));
            this.setEndActivity("work", "1-1", new Coord(1, 1));
            this.legs = Arrays.asList(new LegBuilder().build());
        }

        public void setStartActivity(String type, String linkId, Coord coord) {
            this.startActivity = popfactory.createActivityFromCoord(type, coord);
            this.startActivity.setFacilityId(Id.create(String.format("%s_%s", type, linkId), ActivityFacility.class));
            this.startActivity.setLinkId(Id.create(linkId, Link.class));
        }

        public void setEndActivity(String type, String linkId, Coord coord) {
            this.endActivity = popfactory.createActivityFromCoord(type, coord);
            this.endActivity.setFacilityId(Id.create(String.format("%s_%s", type, linkId), ActivityFacility.class));
            this.endActivity.setLinkId(Id.create(linkId, Link.class));
        }
    }

    public TripBuilder() {}

    public TripBuilder withStartLocation(double startX, double startY) {
        trip.setStartActivity(trip.startActivity.getType(), trip.startActivity.getLinkId().toString(), new Coord(startX, startY));
        return this;
    }

    public TripBuilder withStartActivityType(String startActivityType) {
        trip.setStartActivity(startActivityType, trip.startActivity.getLinkId().toString(), trip.startActivity.getCoord());
        return this;
    }

    public TripBuilder withLegs(List<Leg> legs) {
        trip.legs = legs;
        return this;
    }

    private void setFacilityIds() {
        trip.startActivity.setFacilityId(Id.create(
                String.format("%s_%f_%f",
                        trip.startActivity.getType(),
                        trip.startActivity.getCoord().getX(),
                        trip.startActivity.getCoord().getY()),
                ActivityFacility.class));
        trip.endActivity.setFacilityId(Id.create(
                String.format("%s_%f_%f",
                        trip.endActivity.getType(),
                        trip.endActivity.getCoord().getX(),
                        trip.endActivity.getCoord().getY()),
                ActivityFacility.class));
    }

    public Trip build() {
        return trip;
    }
}
