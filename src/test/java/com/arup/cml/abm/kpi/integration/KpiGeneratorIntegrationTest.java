package com.arup.cml.abm.kpi.integration;

import com.arup.cml.abm.kpi.matsim.run.KpiGenerator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import picocli.CommandLine;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class KpiGeneratorIntegrationTest {

    @Rule
    public TemporaryFolder appOutputDir = new TemporaryFolder();

    @Test
    public void testApp() throws Exception {
        CommandLine cmd = new CommandLine(new KpiGenerator());
        cmd.setOut(new PrintWriter(new StringWriter()));
        Path resourceDirectory = Paths.get("src","test","resources");

        int exitCode = cmd.execute(
                "-mc",
                String.format("%s/integration-test-data/smol-matsim-outputs/output_config.xml", resourceDirectory),
                "-mo",
                String.format("%s/integration-test-data/smol-matsim-outputs", resourceDirectory),
                "-o",
                appOutputDir.getRoot().getAbsolutePath()
        );

        assertThat(exitCode).isEqualTo(0).as("App return code should be zero");
        String[] outputFilesList = appOutputDir.getRoot().list();
        assertThat(outputFilesList).hasSize(6).as("Check number of output files created");
        assertThat(outputFilesList).contains("kpi.csv").as("Check KPI CSV file exists");
        File expectedKpiFile =
                new File(String.format("%s/integration-test-data/smol-matsim-outputs/expected-kpi.csv",
                        resourceDirectory));
        assertThat(new File(String.format("%s/kpi.csv", appOutputDir.getRoot())))
                .hasSameTextualContentAs(expectedKpiFile)
                .as("Check KPI data");
    }
}
