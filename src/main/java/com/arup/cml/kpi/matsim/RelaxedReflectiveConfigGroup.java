package com.arup.cml.kpi.matsim;

import org.matsim.core.config.ReflectiveConfigGroup;

public class RelaxedReflectiveConfigGroup extends ReflectiveConfigGroup {

    public RelaxedReflectiveConfigGroup(String name) {
        super(name, true);
    }

//    @Override
//    public void handleAddUnknownParam(final String paramName, final String value) {
//        System.out.println("hey");
//    }

//    @Override
//    public String handleGetUnknownValue(final String paramName) {
//        return "hello";
//    }
}
