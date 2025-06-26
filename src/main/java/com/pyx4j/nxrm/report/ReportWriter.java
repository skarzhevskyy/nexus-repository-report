package com.pyx4j.nxrm.report;

import java.io.IOException;
import java.util.List;

import com.pyx4j.nxrm.report.model.AgeSummary;
import com.pyx4j.nxrm.report.model.GroupsSummary;
import com.pyx4j.nxrm.report.model.RepositoryComponentsSummary;
import org.sonatype.nexus.model.ComponentXO;

public interface ReportWriter extends AutoCloseable {

    void writeRepositoryComponentsSummary(RepositoryComponentsSummary summary, SortBy sortBy) throws IOException;

    void writeGroupsSummary(GroupsSummary summary, SortBy sortBy, int topGroups) throws IOException;

    void writeAgeSummary(AgeSummary summary) throws IOException;

    void writeComponents(List<ComponentXO> components) throws IOException;

    @Override
    void close() throws IOException;
}
