package com.arup.cml.abm.kpi.builders;

import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;

public class FacilitiesBuilder {
    ActivityFacilities facilities;

    public FacilitiesBuilder() {
        facilities = FacilitiesUtils.createActivityFacilities();
    }

    public ActivityFacilities build() {
        return this.facilities;
    }
}
