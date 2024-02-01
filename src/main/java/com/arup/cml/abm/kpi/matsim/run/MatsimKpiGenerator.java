package com.arup.cml.abm.kpi.matsim.run;

import com.arup.cml.abm.kpi.KpiCalculator;
import com.arup.cml.abm.kpi.data.LinkLog;
import com.arup.cml.abm.kpi.matsim.MatsimUtils;
import com.arup.cml.abm.kpi.matsim.handlers.MatsimLinkLogHandler;
import com.arup.cml.abm.kpi.tablesaw.TablesawKpiCalculator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Command(name = "MatsimKpiGenerator", version = "1.0-SNAPSHOT", mixinStandardHelpOptions = true)
public class MatsimKpiGenerator implements Runnable {
    @Option(names = "-mc", description = "Sets the MATSim config file to use.", required = true)
    private Path matsimConfigFile;

    @Option(names = "-mo", description = "Sets the MATSim output directory use.", required = true)
    private Path matsimOutputDirectory;

    @Option(names = "-o", description = "Sets the output directory. Defaults to stdout", required = true)
    private Path outputDir;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new MatsimKpiGenerator()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        System.out.printf(
                "Writing KPI metrics to %s, generated from MATSim outputs at %s, using MATSim config %s%n",
                outputDir,
                matsimOutputDirectory,
                matsimConfigFile
        );

        // We're not using a dependency injection framework, but we *are* programming
        // in a dependency injection style (explicit dependencies passed into
        // constructors) and then creating and wiring together the objects in the
        // object graph "manually" here. Switching to a DI framework in future should
        // be pretty straightforward if we need to.
        MatsimUtils matsimUtils = new MatsimUtils(matsimOutputDirectory, matsimConfigFile);
        LinkLog linkLog = new LinkLog();
        MatsimLinkLogHandler matsimLinkLogHandler = new MatsimLinkLogHandler(linkLog);
        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(matsimLinkLogHandler);

        String eventsFile = String.format("%s/output_events.xml.gz", matsimOutputDirectory);
        System.out.printf("Streaming MATSim events from %s%n", eventsFile);
        new MatsimEventsReader(eventsManager).readFile(eventsFile);
        summariseEventsHandled(eventsFile, matsimLinkLogHandler.getEventCounts());

        KpiCalculator kpiCalculator = new TablesawKpiCalculator(
                matsimUtils.getMatsimNetwork(), matsimUtils.getTransitSchedule(), matsimUtils.getMatsimVehicles(),
                linkLog, matsimUtils.getMatsimLegsCSVInputStream(), matsimUtils.getMatsimTripsCSVInputStream(),
                outputDir
                );

        kpiCalculator.writeAffordabilityKpi(outputDir);
        kpiCalculator.writePtWaitTimeKpi(outputDir);
        kpiCalculator.writeModalSplitKpi(outputDir);
        kpiCalculator.writeOccupancyRateKpi(outputDir);
        kpiCalculator.writeVehicleKMKpi(outputDir);
        kpiCalculator.writeSpeedKpi(outputDir);
        kpiCalculator.writeGHGKpi(outputDir);
        kpiCalculator.writeCongestionKpi(outputDir);
    }

    private static void summariseEventsHandled(String eventsFilePath, Map<String, AtomicInteger> eventCounts) {
        Integer eventCount = eventCounts.values()
                .stream()
                .mapToInt(AtomicInteger::intValue)
                .sum();
        System.out.printf("Recorded %,d relevant MATSim events from %s%n", eventCount, eventsFilePath);
        try {
            System.out.println(new ObjectMapper().
                    writerWithDefaultPrettyPrinter().
                    writeValueAsString(eventCounts));
        } catch (JsonProcessingException e) {
            // swallow, we're only trying to display event type counts
            e.printStackTrace();
        }
    }
}
