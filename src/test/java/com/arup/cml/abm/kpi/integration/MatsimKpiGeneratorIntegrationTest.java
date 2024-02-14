package com.arup.cml.abm.kpi.integration;

import com.arup.cml.abm.kpi.matsim.run.MatsimKpiGenerator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

import static java.lang.String.format;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class MatsimKpiGeneratorIntegrationTest {
    private final String compressionFileEnding = ".gz"; 

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

    private void assertSupportingFilesWereGenerated(File kpiDirectory) {
        String[] generatedFiles = kpiDirectory.list();
        String [] expectedSupportingFiles = {
                "intermediate-pt-wait-time.csv"+compressionFileEnding,
                "intermediate-occupancy-rate.csv"+compressionFileEnding,
                "intermediate-congestion.csv"+compressionFileEnding,
                "intermediate-vehicle-km.csv"+compressionFileEnding,
                "supporting-data-vehicles.csv"+compressionFileEnding,
                "supporting-data-scheduleRoutes.csv"+compressionFileEnding,
                "supporting-data-scheduleStops.csv"+compressionFileEnding,
                "supporting-data-networkLinkModes.csv"+compressionFileEnding,
                "supporting-data-networkLinks.csv"+compressionFileEnding,
                "supporting-data-vehicleOccupancy.csv"+compressionFileEnding,
                "supporting-data-linkLog.csv"+compressionFileEnding,
                "supporting-data-trips.csv"+compressionFileEnding,
                "supporting-data-legs.csv"+compressionFileEnding
        };
        for (int i = 0; i < expectedSupportingFiles.length; i++) {
            assertThat(generatedFiles)
                    .contains(expectedSupportingFiles[i])
                    .as(format("Check supporting data output file '%s' exists", expectedSupportingFiles[i]));
        }
    }

    private void assertKpiFilesWereGenerated(String expectedKpiDirectory, File kpiDirectory) throws Exception {
        String[] generatedFiles = kpiDirectory.list();
        String [] expectedKpiFiles = {
                "kpi-congestion.csv"+compressionFileEnding,
                "kpi-speed.csv"+compressionFileEnding,
                "kpi-vehicle-km.csv"+compressionFileEnding,
                "kpi-occupancy-rate.csv"+compressionFileEnding,
                "kpi-modal-split.csv"+compressionFileEnding,
                "kpi-pt-wait-time.csv"+compressionFileEnding,
        };
        for (int i = 0; i < expectedKpiFiles.length; i++) {
            String kpiFile = expectedKpiFiles[i];
            assertThat(generatedFiles)
                    .contains(kpiFile)
                    .as(format("Check KPI output file '%s' exists", kpiFile));
            File expectedKpiFile = new File(format("%s/expected-%s", expectedKpiDirectory, kpiFile));
            File currentKpiFile = new File(format("%s/%s", kpiDirectory, kpiFile));
            assertThat(readGzipFileToString(expectedKpiFile))
                    .isEqualTo(readGzipFileToString(currentKpiFile))
                    .as(format("Check %s KPI data matches expectation", kpiFile));
        }
    }

    private String readGzipFileToString(File gzipFile) throws IOException {
        String contentString = "";

        try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(gzipFile));
             ByteArrayOutputStream bytesOut = new ByteArrayOutputStream(1024)) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                bytesOut.write(buffer, 0, len);
            }
            contentString = new String(bytesOut.toByteArray());
        }

        // remove all line endings, tabs, etc., for cross platform compatibility
        return contentString.replaceAll("\\s+","");
    }
}
