package com.arup.cml.abm.kpi.matsim.handlers;

import com.arup.cml.abm.kpi.data.MoneyLog;
import com.google.common.collect.ImmutableMap;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MatsimPersonMoneyHandler implements PersonMoneyEventHandler {

    private MoneyLog moneyLog;
    private final Map<String, AtomicInteger> eventCounts = new HashMap<>();

    public MatsimPersonMoneyHandler(MoneyLog moneyLog) {
        this.moneyLog = moneyLog;
    }

    public Map<String, AtomicInteger> getEventCounts() {
        return ImmutableMap.copyOf(this.eventCounts);
    }

    private void incrementEventCount(Event e) {
        eventCounts.putIfAbsent(e.getEventType(), new AtomicInteger(0));
        eventCounts.get(e.getEventType()).incrementAndGet();
    }

    @Override
    public void handleEvent(PersonMoneyEvent event) {
        incrementEventCount(event);
        moneyLog.createMoneyLogEntry(event.getPersonId().toString(), event.getTime(), event.getAmount());
    }

}
