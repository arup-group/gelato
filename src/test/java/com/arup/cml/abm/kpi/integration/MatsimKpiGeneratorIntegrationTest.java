package com.arup.cml.abm.kpi.integration;

import com.arup.cml.abm.kpi.matsim.run.MatsimKpiGenerator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Paths;

import static java.lang.String.format;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class MatsimKpiGeneratorIntegrationTest {

    @Rule
    public TemporaryFolder appOutputDir = new TemporaryFolder();

    @Test
    public void testApp() throws Exception {
        String testDataDirRoot = format("%s/integration-test-data/smol-matsim-outputs/",
                Paths.get("src", "test", "resources"));

        int exitCode = new CommandLine(new MatsimKpiGenerator()).execute(
                "-mc",
                format("%s/output_config.xml", testDataDirRoot),
                "-mo",
                testDataDirRoot,
                "-o",
                appOutputDir.getRoot().getAbsolutePath()
        );

        assertThat(exitCode).isEqualTo(0).as("App return code should be zero");
        String[] generatedFiles = appOutputDir.getRoot().list();
        assertKpiFilesWereGenerated(format("%s/expected-kpis", testDataDirRoot), generatedFiles);
        assertSupportingFilesWereGenerated(generatedFiles);
    }

    private void assertSupportingFilesWereGenerated(String[] outputFilesList) {
        String [] expectedSupportingFiles = {
                "pt-wait-time.csv",
                "occupancy-rate.csv",
                "congestion.csv",
                "vehicle-km.csv",
                "supporting-data-vehicles.csv",
                "supporting-data-scheduleRoutes.csv",
                "supporting-data-scheduleStops.csv",
                "supporting-data-networkLinkModes.csv",
                "supporting-data-networkLinks.csv",
                "supporting-data-vehicleOccupancy.csv",
                "supporting-data-linkLog.csv",
                "supporting-data-trips.csv",
                "supporting-data-legs.csv"
        };
        for (int i = 0; i < expectedSupportingFiles.length; i++) {
            assertThat(outputFilesList)
                    .contains(expectedSupportingFiles[i])
                    .as(format("Check supporting data output file '%s' exists", expectedSupportingFiles[i]));
        }
    }

    private void assertKpiFilesWereGenerated(String expectedKpiDirectory, String[] outputFilesList) {
        String [] expectedKpiFiles = {
                "kpi-congestion.csv",
                "kpi-speed.csv",
                "kpi-vehicle-km.csv",
                "kpi-occupancy-rate.csv",
                "kpi-modal-split.csv",
                "kpi-pt-wait-time.csv",
        };
        for (int i = 0; i < expectedKpiFiles.length; i++) {
            String expectedFile = expectedKpiFiles[i];
            assertThat(outputFilesList)
                    .contains(expectedFile)
                    .as(format("Check KPI output file '%s' exists", expectedFile));
            File expectedKpiFile = new File(format("%s/expected-%s", expectedKpiDirectory, expectedFile));
            assertThat(new File(format("%s/%s", appOutputDir.getRoot(), expectedFile)))
                    .hasSameTextualContentAs(expectedKpiFile)
                    .as(format("Check %s KPI data matches expectation", expectedFile));
        }
    }
}
