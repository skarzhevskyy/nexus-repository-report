package com.pyx4j.nxrm.report;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import com.opencsv.CSVWriter;
import com.pyx4j.nxrm.report.model.*;
import org.sonatype.nexus.model.ComponentXO;

public class CsvReportWriter implements ReportWriter {

    private final CSVWriter csvWriter;

    public CsvReportWriter(Writer writer) {
        this.csvWriter = new CSVWriter(writer);
    }

    @Override
    public void writeRepositoryComponentsSummary(RepositoryComponentsSummary summary, SortBy sortBy) throws IOException {
        csvWriter.writeNext(new String[]{"Repository", "Format", "Components", "Total Size"});
        summary.getRepositoryStats().forEach((repoName, stats) -> {
            csvWriter.writeNext(new String[]{
                    repoName,
                    stats.getFormat(),
                    String.valueOf(stats.getComponentCount()),
                    String.valueOf(stats.getSizeBytes())
            });
        });
        csvWriter.writeNext(new String[]{
                "TOTAL",
                "-",
                String.valueOf(summary.getTotalComponents()),
                String.valueOf(summary.getTotalSizeBytes())
        });
    }

    @Override
    public void writeGroupsSummary(GroupsSummary summary, SortBy sortBy, int topGroups) throws IOException {
        csvWriter.writeNext(new String[]{"Group", "Components", "Total Size"});
        summary.getGroupStats().entrySet().stream()
                .limit(topGroups)
                .forEach(entry -> {
                    GroupStats stats = entry.getValue();
                    csvWriter.writeNext(new String[]{
                            entry.getKey(),
                            String.valueOf(stats.getComponentCount()),
                            String.valueOf(stats.getSizeBytes())
                    });
                });
    }

    @Override
    public void writeAgeSummary(AgeSummary summary) throws IOException {
        csvWriter.writeNext(new String[]{"Age Range", "Components", "Total Size"});
        for (AgeBucket bucket : summary.getAgeBuckets()) {
            csvWriter.writeNext(new String[]{
                    bucket.getOriginalRange(),
                    String.valueOf(bucket.getComponentCount()),
                    String.valueOf(bucket.getSizeBytes())
            });
        }
        csvWriter.writeNext(new String[]{
                "TOTAL",
                String.valueOf(summary.getTotalComponents()),
                String.valueOf(summary.getTotalSizeBytes())
        });
    }

    @Override
    public void writeComponents(List<ComponentXO> components) throws IOException {
        csvWriter.writeNext(new String[]{"Repository", "Group", "Name", "Version", "Size"});
        for (ComponentXO component : components) {
            csvWriter.writeNext(new String[]{
                    component.getRepository(),
                    component.getGroup(),
                    component.getName(),
                    component.getVersion(),
                    String.valueOf(calculateComponentSize(component))
            });
        }
    }

    private long calculateComponentSize(ComponentXO component) {
        if (component == null || component.getAssets() == null) {
            return 0;
        }

        return component.getAssets().stream()
                .filter(asset -> asset.getFileSize() != null)
                .mapToLong(asset -> asset.getFileSize() != null ? asset.getFileSize() : 0)
                .sum();
    }

    @Override
    public void close() throws IOException {
        csvWriter.close();
    }
}
