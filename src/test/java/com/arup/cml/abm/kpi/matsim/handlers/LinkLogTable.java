package com.arup.cml.abm.kpi.matsim.handlers;

import com.google.common.collect.RowSortedTable;
import com.google.common.collect.TreeBasedTable;

public class LinkLogTable {
    RowSortedTable<Long, String, Object> linkLog = TreeBasedTable.create();

    public void withEntry(Long index, String linkID, String vehicleID, String mode, double startTime, double endTime, int numberOfPeople) {
        linkLog.put(index, "linkID", linkID);
        linkLog.put(index, "vehicleID", vehicleID);
        linkLog.put(index, "mode", mode);
        linkLog.put(index, "startTime", startTime);
        linkLog.put(index, "endTime", endTime);
        linkLog.put(index, "numberOfPeople", numberOfPeople);
    }

    public RowSortedTable<Long, String, Object> getTable() {
        return linkLog;
    }
}
