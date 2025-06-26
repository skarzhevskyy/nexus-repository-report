package com.pyx4j.nxrm.report;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.pyx4j.nxrm.report.model.AgeSummary;
import com.pyx4j.nxrm.report.model.GroupsSummary;
import com.pyx4j.nxrm.report.model.RepositoryComponentsSummary;
import org.sonatype.nexus.model.ComponentXO;

public class JsonReportWriter implements ReportWriter {

    private final ObjectMapper objectMapper;
    private final Writer writer;

    public JsonReportWriter(Writer writer) {
        this.writer = writer;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public void writeRepositoryComponentsSummary(RepositoryComponentsSummary summary, SortBy sortBy) throws IOException {
        objectMapper.writeValue(writer, summary);
    }

    @Override
    public void writeGroupsSummary(GroupsSummary summary, SortBy sortBy, int topGroups) throws IOException {
        objectMapper.writeValue(writer, summary);
    }

    @Override
    public void writeAgeSummary(AgeSummary summary) throws IOException {
        objectMapper.writeValue(writer, summary);
    }

    @Override
    public void writeComponents(List<ComponentXO> components) throws IOException {
        objectMapper.writeValue(writer, components);
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
