package com.arup.cml.kpi.matsim;

import com.arup.cml.kpi.DataModel;
import com.arup.cml.kpi.matsim.handlers.LinkLogHandler;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class MATSimModel implements DataModel {
    private static final Logger log = LogManager.getLogger(MATSimModel.class);

    private final String matsimOutputDir;
    private final EventsManager eventsManager;

    public MATSimModel(String matsimOutputDir) {

        // there will be other stuff read for a matsim model if the KPIs require
        this.matsimOutputDir = matsimOutputDir;
//        Config config = ConfigUtils.loadConfig(
//                String.format("%s/../inputs/config.xml", matsimOutputDir)
//        );
//        Scenario scenario = ScenarioUtils.loadScenario(config);
        this.eventsManager = EventsUtils.createEventsManager();
        LinkLogHandler linkLogHandler = new LinkLogHandler();
        addEventsHandler(linkLogHandler);

        processEvents();
        linkLogHandler.toCsv();
        log.info("done");
    }

    public void addEventsHandler(EventHandler eventHandler) {
        this.eventsManager.addHandler(eventHandler);
    }

    public void processEvents() {
        // hardcoded name for events output file for now
        String outputEventsFile = String.format("%s/output_events.xml.gz", matsimOutputDir);
        new MatsimEventsReader(this.eventsManager).readFile(outputEventsFile);
    }
}
