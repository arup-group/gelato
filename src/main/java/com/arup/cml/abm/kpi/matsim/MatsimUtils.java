package com.arup.cml.abm.kpi.matsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.groups.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.config.TransitConfigGroup;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

public class MatsimUtils {

    private Path matsimOutputDir;
    private Network matsimNetwork;
    private Config matsimConfig;
    private Scenario matsimScenario;

    public MatsimUtils(Path matsimOutputDir, Path matsimConfigFile) {
        this.matsimOutputDir = matsimOutputDir;
        this.matsimConfig = getConfig(matsimConfigFile.toString());
        this.matsimScenario = ScenarioUtils.loadScenario(matsimConfig);
        this.matsimNetwork = matsimScenario.getNetwork();
    }

    private final Set<String> necessaryConfigGroups = new HashSet<>(Arrays.asList(
            GlobalConfigGroup.GROUP_NAME,
            PlansConfigGroup.GROUP_NAME,
            FacilitiesConfigGroup.GROUP_NAME,
            HouseholdsConfigGroup.GROUP_NAME,
            TransitConfigGroup.GROUP_NAME,
            VehiclesConfigGroup.GROUP_NAME,
            NetworkConfigGroup.GROUP_NAME
    ));

    private Config getConfig(String matsimInputConfig) {
        Config config = ConfigUtils.createConfig();
        TreeMap<String, ConfigGroup> configuredModules = config.getModules();
        for (ConfigGroup module : configuredModules.values().stream().toList()) {
            if (necessaryConfigGroups.contains(module.getName())) {
                System.out.println("Config group " + module + " is read as is");
            } else {
                ReflectiveConfigGroup relaxedModule =
                        new ReflectiveConfigGroup(module.getName(), true) {};
                config.removeModule(module.getName());
                config.addModule(relaxedModule);
            }
        }
        ConfigUtils.loadConfig(config, String.format(matsimInputConfig));
        setOutputFilePaths(config);
        return config;
    }

    private void setOutputFilePaths(Config config) {
        TreeMap<String, ConfigGroup> modules = config.getModules();
        modules.get("network")
                .addParam("inputNetworkFile",
                        String.format("%s/output_network.xml.gz", this.matsimOutputDir));
        modules.get("transit")
                .addParam("transitScheduleFile",
                        String.format("%s/output_transitSchedule.xml.gz", this.matsimOutputDir));
        modules.get("transit")
                .addParam("vehiclesFile",
                        String.format("%s/output_transitVehicles.xml.gz", this.matsimOutputDir));
        modules.get("plans")
                .addParam("inputPlansFile",
                        String.format("%s/output_plans.xml.gz", this.matsimOutputDir));
        modules.get("households")
                .addParam("inputFile",
                        String.format("%s/output_households.xml.gz", this.matsimOutputDir));
        modules.get("facilities")
                .addParam("inputFacilitiesFile",
                        String.format("%s/output_facilities.xml.gz", this.matsimOutputDir));
        modules.get("vehicles")
                .addParam("vehiclesFile",
                        String.format("%s/output_vehicles.xml.gz", this.matsimOutputDir));
    }

    public Network getMatsimNetwork() {
        return matsimNetwork;
    }
}
