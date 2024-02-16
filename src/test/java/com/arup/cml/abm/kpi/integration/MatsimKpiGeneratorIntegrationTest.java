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
        assertKpiFilesWereGenerated(format("%s/expected-kpis", testDataDirRoot), appOutputDir.getRoot());
        assertSupportingFilesWereGenerated(appOutputDir.getRoot());
    }

    @Test
    public void testAppWithDrt() throws Exception {
        String testDataDirRoot = format("%s/integration-test-data/drt-matsim-outputs/",
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
        assertKpiFilesWereGenerated(format("%s/expected-kpis", testDataDirRoot), appOutputDir.getRoot());
        assertSupportingFilesWereGenerated(appOutputDir.getRoot());
    }

    private void assertSupportingFilesWereGenerated(File kpiDirectory) {
        String[] generatedFiles = kpiDirectory.list();
        String [] expectedSupportingFiles = {
                "intermediate-pt-wait-time.csv",
                "intermediate-occupancy-rate.csv",
                "intermediate-congestion.csv",
                "intermediate-vehicle-km.csv",
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
            assertThat(generatedFiles)
                    .contains(expectedSupportingFiles[i])
                    .as(format("Check supporting data output file '%s' exists", expectedSupportingFiles[i]));
        }
    }

    private void assertKpiFilesWereGenerated(String expectedKpiDirectory, File kpiDirectory) {
        String[] generatedFiles = kpiDirectory.list();
        String [] expectedKpiFiles = {
                "kpi-congestion.csv",
                "kpi-speed.csv",
                "kpi-vehicle-km.csv",
                "kpi-occupancy-rate.csv",
                "kpi-modal-split.csv",
                "kpi-pt-wait-time.csv",
        };
        for (int i = 0; i < expectedKpiFiles.length; i++) {
            String kpiFile = expectedKpiFiles[i];
            assertThat(generatedFiles)
                    .contains(kpiFile)
                    .as(format("Check KPI output file '%s' exists", kpiFile));
            File expectedKpiFile = new File(format("%s/expected-%s", expectedKpiDirectory, kpiFile));
            assertThat(new File(format("%s/%s", kpiDirectory, kpiFile)))
                    .hasSameTextualContentAs(expectedKpiFile)
                    .as(format("Check %s KPI data matches expectation", kpiFile));
        }
    }
}
