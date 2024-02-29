package com.arup.cml.abm.kpi.matsim.handlers;

import com.arup.cml.abm.kpi.data.MoneyLog;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;
import org.mockito.Mockito;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;


public class TestMatsimMoneyLogHandler {
    private final Id<Person> gerry = Id.createPersonId("gerry");
    private final double eventTime = 0;
    private final double costAmount = 10;
    private final String purpose = "theySeeMeRollin";
    private final String transactionPartner = "Tesco";
    private final String reference = "SelfCheckout";

    @Test
    public void recordsMoneyEvent() {
        MoneyLog mockMoneyLog = Mockito.mock(MoneyLog.class);
        MatsimPersonMoneyHandler moneyLogHandler = new MatsimPersonMoneyHandler(mockMoneyLog);

        moneyLogHandler.handleEvent(new PersonMoneyEvent(eventTime, gerry, costAmount, purpose, transactionPartner, reference));

        verify(mockMoneyLog).createMoneyLogEntry(gerry.toString(), eventTime, costAmount);
    }

    @Test
    public void returnsImmutableCopyOfEventCountsMap() {
        MatsimPersonMoneyHandler moneyLogHandler = new MatsimPersonMoneyHandler(Mockito.mock(MoneyLog.class));
        Map<String, AtomicInteger>  counts = moneyLogHandler.getEventCounts();
        assertThat(counts).isInstanceOf(ImmutableMap.class);
    }

    @Test
    public void maintainsACountOfEventTypesSeen() {
        MatsimPersonMoneyHandler moneyLogHandler = new MatsimPersonMoneyHandler(Mockito.mock(MoneyLog.class));
        assertThat(moneyLogHandler.getEventCounts().isEmpty()).as("Event counts should initially be empty");

        for (int i = 1; i <= 3; i++) {
            PersonMoneyEvent personMoneyEvent = new PersonMoneyEvent(eventTime, gerry, costAmount, purpose, transactionPartner, reference);
            moneyLogHandler.handleEvent(personMoneyEvent);

            int eventCount = moneyLogHandler.getEventCounts().get(personMoneyEvent.getEventType()).get();
            assertThat(eventCount).isEqualTo(i).as(String.format("Event count should be {}", i));
        }
    }
}
