package com.arup.cml.abm.kpi.data;

import java.util.*;

public class MoneyLog {
    HashMap<String, HashMap<Double, Double>> moneyLogData = new HashMap<>();

    public HashMap<String, HashMap<Double, Double>> getMoneyLogData() {
        return moneyLogData;
    }

    public HashMap<Double, Double> getMoneyLogData(String personID) {
        return getPersonLog(personID);
    }

    public void createMoneyLogEntry(String personID, double time, double amount) {
        HashMap<Double, Double> personLog = getPersonLog(personID);
        if (personLog.containsKey(time)) {
            personLog.put(time, amount + personLog.get(time));
        } else {
            personLog.put(time, amount);
        }
    }

    private HashMap<Double, Double> getPersonLog(String personID) {
        moneyLogData.putIfAbsent(personID, new HashMap<>());
        return moneyLogData.get(personID);
    }

}
