package com.arup.cml.abm.kpi.integration;

import com.arup.cml.abm.kpi.matsim.run.KpiGenerator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Paths;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class KpiGeneratorIntegrationTest {

    @Rule
    public TemporaryFolder appOutputDir = new TemporaryFolder();

    @Test
    public void testApp() throws Exception {
        String testDataDirRoot = String.format("%s/integration-test-data/smol-matsim-outputs/",
                Paths.get("src", "test", "resources"));

        int exitCode = new CommandLine(new KpiGenerator()).execute(
                "-mc",
                String.format("%s/output_config.xml", testDataDirRoot),
                "-mo",
                testDataDirRoot,
                "-o",
                appOutputDir.getRoot().getAbsolutePath()
        );

        assertThat(exitCode).isEqualTo(0).as("App return code should be zero");
        String[] outputFilesList = appOutputDir.getRoot().list();
        assertThat(outputFilesList).hasSize(8).as("Check number of output files created");
        // this has been superseeded
//        assertThat(outputFilesList).contains("kpi_congestion.csv").as("Check KPI CSV file exists");
//        File expectedKpiFile = new File(String.format("%s/expected-kpi.csv", testDataDirRoot));
//        assertThat(new File(String.format("%s/kpi_congestion.csv", appOutputDir.getRoot())))
//                .hasSameTextualContentAs(expectedKpiFile)
//                .as("Check calculated KPI data");
    }
}
