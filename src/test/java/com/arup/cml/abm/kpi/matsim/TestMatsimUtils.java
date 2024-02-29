package com.arup.cml.abm.kpi.matsim;

import org.junit.Test;

import java.nio.file.Paths;

import static java.lang.String.format;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestMatsimUtils {

    @Test
    public void doesNotLoadMatsimPopulation() throws Exception {
        String testDataDir = format("%s/integration-test-data/smol-matsim-outputs/",
                Paths.get("src", "test", "resources"));
        String configFilePath = String.format("%s/%s", testDataDir, "output_config.xml");

        MatsimUtils matsimUtils = new MatsimUtils(Paths.get(testDataDir), Paths.get(configFilePath));

        assertThat(matsimUtils.getMatsimConfig().plans().getInputFile())
                .withFailMessage("Plans input file should have been set to null in config, but was not")
                .isNull();
        int numberOfPersons = matsimUtils.getMatsimScenario().getPopulation().getPersons().size();
        assertThat(numberOfPersons)
                .withFailMessage(String.format("Population should be empty, but has %s persons", numberOfPersons))
                .isEqualTo(0);
    }
}
