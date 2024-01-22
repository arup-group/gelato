package com.arup.cml.kpi.matsim.run;

import com.arup.cml.kpi.KPIDomainModel;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import com.arup.cml.kpi.matsim.MATSimModel;
import java.io.File;

@Command(name = "KpiGenerator", version = "1.0-SNAPSHOT", mixinStandardHelpOptions = true)
public class KpiGenerator implements Runnable {

    @Option(names = "-c", description = "Sets the check configuration file to use.")
    private File configurationFile;

    @Option(names = "-mc", description = "Sets the MATSim config file to use.")
    private String matsimConfigFile;

    @Option(names = "-mo", description = "Sets the MATSim output directory use.")
    private String matsimOutputDirectory;

    @Option(names = "-o", description = "Sets the output directory. Defaults to stdout")
    private String outputDir;

    @Option(names = "-v", versionHelp = true, description = "Print product version and exit")
    private boolean versionHelpRequested;
    public static void main(String[] args) {
        int exitCode = new CommandLine(new KpiGenerator()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        System.out.println("Making KPI metrics...");
        MATSimModel matsimModel = new MATSimModel(matsimConfigFile, matsimOutputDirectory);
        matsimModel.write(outputDir);
        KPIDomainModel domainModel = new KPIDomainModel(matsimModel, outputDir);
        System.out.println(domainModel.ptWaitTime().print());
        System.out.println(domainModel.modalSplit().print());
        System.out.println(domainModel.occupancyRate().print());
        System.out.println(domainModel.congestion().print());

    }
}
