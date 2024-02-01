package com.arup.cml.abm.kpi.matsim.handlers;

import com.google.common.collect.RowSortedTable;
import com.google.common.collect.TreeBasedTable;

public class LinkLogVehicleOccupancyTable {
    RowSortedTable<Long, String, Object> linkLogVehicleOccupants = TreeBasedTable.create();

    public void withEntry(Long linkLogIndex, String agentId) {
        linkLogVehicleOccupants.put(linkLogIndex, "agentId", agentId);
    }

    public RowSortedTable<Long, String, Object> getTable() {
        return linkLogVehicleOccupants;
    }
}
