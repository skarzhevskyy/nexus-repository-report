package com.pyx4j.nxrm.report.model;

/**
 * Stats for a specific repository.
 */
public class RepositoryStats {

    private final String format;

    private long componentCount;

    private long sizeBytes;

    public RepositoryStats(String format) {
        this.format = format;
    }

    public String getFormat() {
        return format;
    }

    public void addComponents(long componentCount, long sizeBytes) {
        this.componentCount += componentCount;
        this.sizeBytes += sizeBytes;
    }

    public long getComponentCount() {
        return componentCount;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }
}
