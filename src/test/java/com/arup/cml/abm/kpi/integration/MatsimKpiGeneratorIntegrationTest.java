package com.arup.cml.abm.kpi.integration;

import com.arup.cml.abm.kpi.matsim.run.MatsimKpiGenerator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Paths;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class MatsimKpiGeneratorIntegrationTest {

    @Rule
    public TemporaryFolder appOutputDir = new TemporaryFolder();

    @Test
    public void testApp() throws Exception {
        String testDataDirRoot = String.format("%s/integration-test-data/smol-matsim-outputs/",
                Paths.get("src", "test", "resources"));

        int exitCode = new CommandLine(new MatsimKpiGenerator()).execute(
                "-mc",
                String.format("%s/output_config.xml", testDataDirRoot),
                "-mo",
                testDataDirRoot,
                "-o",
                appOutputDir.getRoot().getAbsolutePath()
        );

        assertThat(exitCode).isEqualTo(0).as("App return code should be zero");
        String[] outputFilesList = appOutputDir.getRoot().list();
        assertThat(outputFilesList).hasSize(17).as("Check number of output files created");
        assertThat(outputFilesList).contains("kpi-congestion.csv").as("Check KPI CSV file exists");
        File expectedKpiFile = new File(String.format("%s/expected-kpi.csv", testDataDirRoot));
        assertThat(new File(String.format("%s/kpi-congestion.csv", appOutputDir.getRoot())))
                .hasSameTextualContentAs(expectedKpiFile)
                .as("Check calculated KPI data");
    }
}
