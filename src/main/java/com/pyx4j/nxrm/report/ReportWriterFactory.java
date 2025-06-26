package com.pyx4j.nxrm.report;

import java.io.FileWriter;
import java.io.IOException;

public class ReportWriterFactory {

    public static ReportWriter create(String filePath) throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        if (filePath.endsWith(".json")) {
            return new JsonReportWriter(new FileWriter(filePath));
        }

        if (filePath.endsWith(".csv")) {
            return new CsvReportWriter(new FileWriter(filePath));
        }

        throw new IllegalArgumentException("Unsupported file format: " + filePath);
    }
}
