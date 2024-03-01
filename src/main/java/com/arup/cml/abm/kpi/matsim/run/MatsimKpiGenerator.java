package com.arup.cml.abm.kpi.matsim.run;

import com.arup.cml.abm.kpi.KpiCalculator;
import com.arup.cml.abm.kpi.data.MoneyLog;
import com.arup.cml.abm.kpi.domain.NetworkLinkLog;
import com.arup.cml.abm.kpi.matsim.MatsimUtils;
import com.arup.cml.abm.kpi.matsim.handlers.MatsimLinkLogHandler;
import com.arup.cml.abm.kpi.matsim.handlers.MatsimPersonMoneyHandler;
import com.arup.cml.abm.kpi.tablesaw.TablesawKpiCalculator;
import com.arup.cml.abm.kpi.tablesaw.TablesawNetworkLinkLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ControllerConfigGroup.CompressionType;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.utils.MemoryObserver;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Command(name = "MatsimKpiGenerator", version = "0.0.3-alpha", mixinStandardHelpOptions = true)
public class MatsimKpiGenerator implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(MatsimKpiGenerator.class);
    public static final String EOL = "\n";

    @Option(names = "-mc", description = "Full path to your model's MATSim config file", required = true)
    private Path matsimConfigFile;

    @Option(names = "-mo", description = "Full path to your model's MATSim output directory", required = true)
    private Path matsimOutputDirectory;

    @Option(names = "-o", description = "Full path to the directory you want KPIs to be written to", required = true)
    private Path outputDir;

    public static void main(String[] args) {
        System.setProperty("line.separator", EOL); // Required to allow platform independent checksum similarity
        int exitCode = new CommandLine(new MatsimKpiGenerator()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        LOGGER.info("Writing KPI metrics to {}, generated from MATSim outputs at {}, using MATSim config {}",
                new Object[]{outputDir, matsimOutputDirectory, matsimConfigFile}
        );

        MemoryObserver.start(60);

        // We're not using a dependency injection framework, but we *are* programming
        // in a dependency injection style (explicit dependencies passed into
        // constructors) and then creating and wiring together the objects in the
        // object graph "manually" here. Switching to a DI framework in future should
        // be pretty straightforward if we need to.
        MatsimUtils matsimUtils = new MatsimUtils(matsimOutputDirectory, matsimConfigFile);
        NetworkLinkLog linkLog = new TablesawNetworkLinkLog();
        MatsimLinkLogHandler matsimLinkLogHandler = new MatsimLinkLogHandler(linkLog);
        MoneyLog moneyLog = new MoneyLog();
        MatsimPersonMoneyHandler matsimPersonMoneyHandler = new MatsimPersonMoneyHandler(moneyLog);
        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(matsimLinkLogHandler);
        eventsManager.addHandler(matsimPersonMoneyHandler);

        String eventsFile = String.format("%s/%soutput_events.xml%s",
                matsimOutputDirectory,
                matsimUtils.getRunId(),
                matsimUtils.getCompressionFileEnd());

        LOGGER.info("Streaming MATSim events from {}", eventsFile);
        new MatsimEventsReader(eventsManager).readFile(eventsFile);
        summariseEventsHandled(eventsFile, matsimLinkLogHandler.getEventCounts());
        summariseEventsHandled(eventsFile, matsimPersonMoneyHandler.getEventCounts());

        KpiCalculator kpiCalculator = new TablesawKpiCalculator(
                matsimUtils.getMatsimNetwork(), matsimUtils.getTransitSchedule(), matsimUtils.getMatsimVehicles(),
                linkLog, matsimUtils.getMatsimPersonsCSVInputStream(), moneyLog, matsimUtils.getScoring(), matsimUtils.getFacilities(),
                matsimUtils.getMatsimLegsCSVInputStream(),  matsimUtils.getMatsimTripsCSVInputStream(),
                outputDir, CompressionType.gzip
                );

        kpiCalculator.writeAffordabilityKpi(outputDir);
        kpiCalculator.writePtWaitTimeKpi(outputDir);
        kpiCalculator.writeModalSplitKpi(outputDir);
        kpiCalculator.writeOccupancyRateKpi(outputDir);
        kpiCalculator.writeVehicleKMKpi(outputDir);
        kpiCalculator.writePassengerKMKpi(outputDir);
        kpiCalculator.writeSpeedKpi(outputDir);
        kpiCalculator.writeGHGKpi(outputDir);
        kpiCalculator.writeAccessToMobilityServicesKpi(outputDir);
        kpiCalculator.writeCongestionKpi(outputDir);
        kpiCalculator.writeTravelTimeKpi(outputDir);
        kpiCalculator.writeMobilitySpaceUsageKpi(outputDir);
        MemoryObserver.stop();
    }

    private static void summariseEventsHandled(String eventsFilePath, Map<String, AtomicInteger> eventCounts) {
        Integer eventCount = eventCounts.values()
                .stream()
                .mapToInt(AtomicInteger::intValue)
                .sum();
        LOGGER.info(String.format("Recorded %,d relevant MATSim events from %s", eventCount, eventsFilePath));
        try {
            LOGGER.info(new ObjectMapper().
                    writerWithDefaultPrettyPrinter().
                    writeValueAsString(eventCounts));
        } catch (JsonProcessingException e) {
            // swallow, we're only trying to display event type counts
            e.printStackTrace();
        }
    }
}
