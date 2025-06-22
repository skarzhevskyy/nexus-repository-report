package com.pyx4j.nxrm.report.model;

/**
 * Stats for a specific group (e.g., Maven groupId, npm scope).
 */
public class GroupStats {

    private long componentCount;

    private long sizeBytes;

    public GroupStats() {
        this.componentCount = 0;
        this.sizeBytes = 0;
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