package com.pyx4j.nxrm.report;

import java.util.concurrent.Callable;

import picocli.CommandLine;

@CommandLine.Command(description = "nxrm-report", name = "nxrm-report.jar", sortOptions = false, mixinStandardHelpOptions = true)
public class NxReportCommandArgs implements Callable<Integer> {

    @CommandLine.Parameters(index = "0",
            description = "repositories-summary|groups-summary",
            defaultValue = "repositories-summary")
    public String action;

    @CommandLine.Option(
            names = {"-u", "--url"},
            description = "Nexus Repository Manager URL", required = true,
            defaultValue = "${NEXUS_URL}")
    public String nexusServerUrl;

    public Integer call() throws Exception {
        int exitCode = 0;
        switch (action) {
            case "repositories-summary":
                exitCode = NxReport.generateReport(this);
                break;
            default:
                CommandLine.usage(this, System.out);
                break;
        }
        return exitCode;
    }

}
