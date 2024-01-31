package com.arup.cml.abm.kpi;

import java.nio.file.Path;

public interface KpiCalculator {
    void writeCongestionKpi(Path directory);
}
