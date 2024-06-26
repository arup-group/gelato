package com.arup.cml.abm.kpi.data;

import java.util.*;

public class MoneyLog {

    Map<String, Map<Double, Double>> moneyLogData = new HashMap<>();

    public Map<String, Map<Double, Double>> getMoneyLogData() {
        return moneyLogData;
    }

    public Map<Double, Double> getMoneyLogData(String personID) {
        return getPersonLog(personID);
    }

    public void createMoneyLogEntry(String personID, double time, double amount) {
        Map<Double, Double> personLog = getPersonLog(personID);
        personLog.compute(time, (k, v) -> (v == null) ? amount : amount + v);
    }

    private Map<Double, Double> getPersonLog(String personID) {
        return moneyLogData.computeIfAbsent(personID, k -> new HashMap<>());
    }

}
