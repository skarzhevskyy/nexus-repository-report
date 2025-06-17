package com.pyx4j.nxrm.report;

import picocli.CommandLine;

public class Application {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new NxReportCommandArgs()).execute(args);
        System.exit(exitCode);
    }

}
