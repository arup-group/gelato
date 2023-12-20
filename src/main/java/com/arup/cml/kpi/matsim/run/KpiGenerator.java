package com.arup.cml.kpi.matsim.run;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;

@Command(name = "KpiGenerator", version = "1.0-SNAPSHOT", mixinStandardHelpOptions = true)
public class KpiGenerator implements Runnable {

    @Option(names = "-c", description = "Sets the check configuration file to use.")
    private File configurationFile;

    @Option(names = "-o", description = "Sets the output file. Defaults to stdout")
    private File outputFile;

    @Option(names = "-v", versionHelp = true, description = "Print product version and exit")
    private boolean versionHelpRequested;
    public static void main(String[] args) {
        int exitCode = new CommandLine(new KpiGenerator()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        System.out.println("Making KPI metrics...");
    }
}
