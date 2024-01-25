package com.arup.cml.abm.kpi.matsim.run;

import com.arup.cml.abm.kpi.KpiCalculator;
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

        MatsimUtils matsimUtils = new MatsimUtils(matsimOutputDirectory, matsimConfigFile);
        KpiCalculator kpiCalculator = new TablesawKpiCalculator(matsimUtils.getMatsimNetwork());
        MatsimLinkLogHandler matsimLinkLogHandler = new MatsimLinkLogHandler(kpiCalculator);
        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(matsimLinkLogHandler);

        String eventsFile = String.format("%s/output_events.xml.gz", matsimOutputDirectory);
        System.out.printf("Streaming MATSim events from %s%n", eventsFile);
        new MatsimEventsReader(eventsManager).readFile(eventsFile);

        Map<String, AtomicInteger> eventsSeen = matsimLinkLogHandler.getEventCounts();
        Integer eventCount = eventsSeen.values()
                .stream()
                .mapToInt(AtomicInteger::intValue)
                .sum();
        System.out.printf("Recorded %,d relevant MATSim events from %s%n", eventCount, eventsFile);
        try {
            System.out.println(new ObjectMapper().
                    writerWithDefaultPrettyPrinter().
                    writeValueAsString(eventsSeen));
        } catch (JsonProcessingException e) {
            // swallow, we're only trying to display event type counts
            e.printStackTrace();
        }

        kpiCalculator.writeCongestionKpi(outputDir);
    }
}