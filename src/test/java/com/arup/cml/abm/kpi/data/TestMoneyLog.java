package com.arup.cml.abm.kpi.data;

import org.junit.Test;

import java.util.HashMap;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class TestMoneyLog {

    @Test
    public void recordsMoneyEvent() {
        MoneyLog moneyLog = new MoneyLog();
        assertThat(moneyLog.getMoneyLogData().isEmpty()).isTrue().as("Money log should be empty to begin with");

        String somePersonID = "somePerson";
        Double someTime = 5.0;
        Double someCost = 100.0;
        moneyLog.createMoneyLogEntry(somePersonID, someTime, someCost);

        HashMap<String, HashMap<Double, Double>> moneyData = moneyLog.getMoneyLogData();
        assertThat(moneyData.size())
                .isEqualTo(1)
                .as("Money Log should contain entries for a single person");
        assertThat(moneyData.containsKey(somePersonID))
                .isTrue()
                .as(String.format("Money Log should contain entries for %s", somePersonID));
        assertThat(moneyData.get(somePersonID).size())
                .isEqualTo(1)
                .as(String.format("Money Log for %s should contain a single entry", somePersonID));
        assertThat(moneyData.get(somePersonID).containsKey(someTime))
                .isTrue()
                .as(String.format("Money Log should have logged an event for time %f", someTime));
        assertThat(moneyData.get(somePersonID).get(someTime))
                .isEqualTo(someCost)
                .as(String.format("Money Log should have logged cost of %f for time %f", someCost, someTime));
    }

    @Test
    public void recordsTwoSeparateMoneyEvents() {
        MoneyLog moneyLog = new MoneyLog();
        assertThat(moneyLog.getMoneyLogData().isEmpty()).isTrue().as("Money log should be empty to begin with");

        String somePersonID = "somePerson";
        Double firstTime = 5.0;
        Double firstCost = 100.0;
        Double secondTime = 10.0;
        Double secondCost = 200.0;
        moneyLog.createMoneyLogEntry(somePersonID, firstTime, firstCost);
        moneyLog.createMoneyLogEntry(somePersonID, secondTime, secondCost);

        HashMap<String, HashMap<Double, Double>> moneyData = moneyLog.getMoneyLogData();
        assertThat(moneyData.size())
                .isEqualTo(1)
                .as("Money Log should contain entries for a single person");
        assertThat(moneyData.containsKey(somePersonID))
                .isTrue()
                .as(String.format("Money Log should contain entries for %s", somePersonID));
        assertThat(moneyData.get(somePersonID).size())
                .isEqualTo(2)
                .as(String.format("Money Log for %s should contain two entries", somePersonID));
        assertThat(moneyData.get(somePersonID).containsKey(firstTime))
                .isTrue()
                .as(String.format("Money Log should have logged an event for time %f", firstTime));
        assertThat(moneyData.get(somePersonID).containsKey(secondTime))
                .isTrue()
                .as(String.format("Money Log should have logged an event for time %f", secondTime));
    }

    @Test
    public void combinesTwoMoneyEventsHappeningAtTheSameTime() {
        MoneyLog moneyLog = new MoneyLog();
        assertThat(moneyLog.getMoneyLogData().isEmpty()).isTrue().as("Money log should be empty to begin with");

        String somePersonID = "somePerson";
        Double someTime = 5.0;
        Double firstCost = 100.0;
        Double secondCost = 200.0;
        moneyLog.createMoneyLogEntry(somePersonID, someTime, firstCost);
        moneyLog.createMoneyLogEntry(somePersonID, someTime, secondCost);

        HashMap<String, HashMap<Double, Double>> moneyData = moneyLog.getMoneyLogData();
        assertThat(moneyData.size())
                .isEqualTo(1)
                .as("Money Log should contain entries for a single person");
        assertThat(moneyData.containsKey(somePersonID))
                .isTrue()
                .as(String.format("Money Log should contain entries for %s", somePersonID));
        assertThat(moneyData.get(somePersonID).size())
                .isEqualTo(1)
                .as(String.format("Money Log for %s should contain a single entry", somePersonID));
        assertThat(moneyData.get(somePersonID).containsKey(someTime))
                .isTrue()
                .as(String.format("Money Log should have logged an event for time %f", someTime));
        assertThat(moneyData.get(somePersonID).get(someTime))
                .isEqualTo(firstCost + secondCost)
                .as(String.format("Money Log should have logged cost of %f for time %f", firstCost + secondCost, someTime));
    }
}
