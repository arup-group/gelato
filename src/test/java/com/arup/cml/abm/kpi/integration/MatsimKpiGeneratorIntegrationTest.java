package com.arup.cml.abm.kpi.integration;

import com.arup.cml.abm.kpi.matsim.run.MatsimKpiGenerator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.core.utils.misc.CRCChecksum;

import picocli.CommandLine;

import java.io.File;
import java.nio.file.Paths;

import static java.lang.String.format;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class MatsimKpiGeneratorIntegrationTest {
    private static final String COMPRESSION_FILE_EXTENSION = ".gz";

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
        String[] expectedSupportingFiles = {
                "intermediate-pt-wait-time.csv" + COMPRESSION_FILE_EXTENSION,
                "intermediate-occupancy-rate.csv" + COMPRESSION_FILE_EXTENSION,
                "intermediate-congestion.csv" + COMPRESSION_FILE_EXTENSION,
                "intermediate-vehicle-km.csv" + COMPRESSION_FILE_EXTENSION,
                "intermediate-affordability.csv" + COMPRESSION_FILE_EXTENSION,
                "intermediate-travel-time.csv" + COMPRESSION_FILE_EXTENSION,
                "intermediate-access-to-mobility-services.csv" + COMPRESSION_FILE_EXTENSION,
                "supporting-data-vehicles.csv" + COMPRESSION_FILE_EXTENSION,
                "supporting-data-scheduleRoutes.csv" + COMPRESSION_FILE_EXTENSION,
                "supporting-data-scheduleStops.csv" + COMPRESSION_FILE_EXTENSION,
                "supporting-data-networkLinkModes.csv" + COMPRESSION_FILE_EXTENSION,
                "supporting-data-networkLinks.csv" + COMPRESSION_FILE_EXTENSION,
                "supporting-data-vehicleOccupancy.csv" + COMPRESSION_FILE_EXTENSION,
                "supporting-data-linkLog.csv" + COMPRESSION_FILE_EXTENSION,
                "supporting-data-trips.csv" + COMPRESSION_FILE_EXTENSION,
                "supporting-data-legs.csv" + COMPRESSION_FILE_EXTENSION,
                "supporting-person-mode-score-parameters.csv" + COMPRESSION_FILE_EXTENSION
        };
        for (int i = 0; i < expectedSupportingFiles.length; i++) {
            assertThat(generatedFiles)
                    .contains(expectedSupportingFiles[i])
                    .as(format("Check supporting data output file '%s' exists", expectedSupportingFiles[i]));
        }
    }

    private void assertKpiFilesWereGenerated(String expectedKpiDirectory, File kpiDirectory) {
        String[] generatedFiles = kpiDirectory.list();
        String[] expectedKpiFiles = {
                "kpi-congestion.csv" + COMPRESSION_FILE_EXTENSION,
                "kpi-speed.csv" + COMPRESSION_FILE_EXTENSION,
                "kpi-vehicle-km.csv" + COMPRESSION_FILE_EXTENSION,
                "kpi-occupancy-rate.csv" + COMPRESSION_FILE_EXTENSION,
                "kpi-modal-split.csv" + COMPRESSION_FILE_EXTENSION,
                "kpi-pt-wait-time.csv" + COMPRESSION_FILE_EXTENSION,
                "kpi-travel-time.csv" + COMPRESSION_FILE_EXTENSION,
                "kpi-affordability.csv" + COMPRESSION_FILE_EXTENSION,
                "kpi-access-to-mobility-services-access-to-bus.csv" + COMPRESSION_FILE_EXTENSION,
                "kpi-access-to-mobility-services-access-to-rail.csv" + COMPRESSION_FILE_EXTENSION,
                "kpi-access-to-mobility-services-access-to-pt-and-pt-used.csv" + COMPRESSION_FILE_EXTENSION,
        };
        for (int i = 0; i < expectedKpiFiles.length; i++) {
            String kpiFile = expectedKpiFiles[i];
            assertThat(generatedFiles)
                    .contains(kpiFile)
                    .as(format("Check KPI output file '%s' exists", kpiFile));
            File expectedKpiFile = new File(format("%s/expected-%s", expectedKpiDirectory, kpiFile));
            File currentKpiFile = new File(format("%s/%s", kpiDirectory, kpiFile));
            long expected = CRCChecksum.getCRCFromFile(expectedKpiFile.toString());
            long current = CRCChecksum.getCRCFromFile(currentKpiFile.toString());
            assertThat(expected).isEqualTo(current).as(format("Check %s KPI data matches expectation", kpiFile));
        }
    }
}
