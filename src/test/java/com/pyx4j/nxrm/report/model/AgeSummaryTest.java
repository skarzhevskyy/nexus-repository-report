package com.pyx4j.nxrm.report.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sonatype.nexus.model.AssetXO;
import org.sonatype.nexus.model.ComponentXO;

/**
 * Unit tests for AgeSummary functionality.
 */
class AgeSummaryTest {

    @Test
    void ageSummary_withValidBuckets_shouldInitializeCorrectly() {
        List<String> ranges = Arrays.asList("0-7", "8-30", ">365");
        AgeSummary summary = new AgeSummary(ranges);

        assertThat(summary.getAgeBuckets()).hasSize(3);
        assertThat(summary.getTotalComponents()).isZero();
        assertThat(summary.getTotalSizeBytes()).isZero();
        assertThat(summary.isEnabled()).isTrue();
    }

    @Test
    void ageSummary_withEmptyBuckets_shouldThrowException() {
        assertThatThrownBy(() -> new AgeSummary(Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Age bucket ranges cannot be empty");
    }

    @Test
    void ageSummary_withNullBuckets_shouldThrowException() {
        assertThatThrownBy(() -> new AgeSummary(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Age bucket ranges cannot be null");
    }

    @Test
    void addComponent_withNullComponent_shouldThrowException() {
        AgeSummary summary = new AgeSummary(Arrays.asList("0-7"));

        assertThatThrownBy(() -> summary.addComponent(null, 1024))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Component cannot be null");
    }

    @Test
    void addComponent_withComponentInFirstBucket_shouldCategorizeCorrectly() {
        List<String> ranges = Arrays.asList("0-7", "8-30", ">365");
        AgeSummary summary = new AgeSummary(ranges);

        // Create component with asset created 3 days ago
        OffsetDateTime createdDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(3);
        ComponentXO component = createComponentWithAsset(createdDate);

        summary.addComponent(component, 1024);

        assertThat(summary.getTotalComponents()).isEqualTo(1);
        assertThat(summary.getTotalSizeBytes()).isEqualTo(1024);

        AgeBucket firstBucket = summary.getAgeBuckets().get(0);
        assertThat(firstBucket.getComponentCount()).isEqualTo(1);
        assertThat(firstBucket.getSizeBytes()).isEqualTo(1024);

        // Other buckets should be empty
        assertThat(summary.getAgeBuckets().get(1).getComponentCount()).isZero();
        assertThat(summary.getAgeBuckets().get(2).getComponentCount()).isZero();
    }

    @Test
    void addComponent_withComponentInLastBucket_shouldCategorizeCorrectly() {
        List<String> ranges = Arrays.asList("0-7", "8-30", ">365");
        AgeSummary summary = new AgeSummary(ranges);

        // Create component with asset created 400 days ago
        OffsetDateTime createdDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(400);
        ComponentXO component = createComponentWithAsset(createdDate);

        summary.addComponent(component, 2048);

        assertThat(summary.getTotalComponents()).isEqualTo(1);
        assertThat(summary.getTotalSizeBytes()).isEqualTo(2048);

        AgeBucket lastBucket = summary.getAgeBuckets().get(2);
        assertThat(lastBucket.getComponentCount()).isEqualTo(1);
        assertThat(lastBucket.getSizeBytes()).isEqualTo(2048);

        // Other buckets should be empty
        assertThat(summary.getAgeBuckets().get(0).getComponentCount()).isZero();
        assertThat(summary.getAgeBuckets().get(1).getComponentCount()).isZero();
    }

    @Test
    void addComponent_withMultipleAssets_shouldUseEarliestDate() {
        List<String> ranges = Arrays.asList("0-7", "8-30", ">365");
        AgeSummary summary = new AgeSummary(ranges);

        // Create component with multiple assets, earliest is 3 days ago
        ComponentXO component = new ComponentXO();
        
        AssetXO asset1 = new AssetXO();
        asset1.setBlobCreated(OffsetDateTime.now(ZoneOffset.UTC).minusDays(10)); // 10 days ago
        asset1.setFileSize(512L);
        
        AssetXO asset2 = new AssetXO();
        asset2.setBlobCreated(OffsetDateTime.now(ZoneOffset.UTC).minusDays(3)); // 3 days ago (earliest)
        asset2.setFileSize(512L);
        
        component.setAssets(Arrays.asList(asset1, asset2));

        summary.addComponent(component, 1024);

        // Should be categorized based on earliest date (3 days ago -> first bucket)
        AgeBucket firstBucket = summary.getAgeBuckets().get(0);
        assertThat(firstBucket.getComponentCount()).isEqualTo(1);
        assertThat(firstBucket.getSizeBytes()).isEqualTo(1024);
    }

    @Test
    void addComponent_withNoAssets_shouldBeSkipped() {
        List<String> ranges = Arrays.asList("0-7", "8-30", ">365");
        AgeSummary summary = new AgeSummary(ranges);

        ComponentXO component = new ComponentXO();
        component.setAssets(Collections.emptyList());

        summary.addComponent(component, 1024);

        // Component should be skipped
        assertThat(summary.getTotalComponents()).isZero();
        assertThat(summary.getTotalSizeBytes()).isZero();
    }

    @Test
    void addComponent_withNullAssets_shouldBeSkipped() {
        List<String> ranges = Arrays.asList("0-7", "8-30", ">365");
        AgeSummary summary = new AgeSummary(ranges);

        ComponentXO component = new ComponentXO();
        component.setAssets(null);

        summary.addComponent(component, 1024);

        // Component should be skipped
        assertThat(summary.getTotalComponents()).isZero();
        assertThat(summary.getTotalSizeBytes()).isZero();
    }

    @Test
    void addComponent_withAssetsWithoutBlobCreated_shouldBeSkipped() {
        List<String> ranges = Arrays.asList("0-7", "8-30", ">365");
        AgeSummary summary = new AgeSummary(ranges);

        ComponentXO component = new ComponentXO();
        AssetXO asset = new AssetXO();
        asset.setBlobCreated(null); // No creation date
        asset.setFileSize(1024L);
        component.setAssets(Arrays.asList(asset));

        summary.addComponent(component, 1024);

        // Component should be skipped
        assertThat(summary.getTotalComponents()).isZero();
        assertThat(summary.getTotalSizeBytes()).isZero();
    }

    @Test
    void addComponent_multipleComponents_shouldAccumulate() {
        List<String> ranges = Arrays.asList("0-7", "8-30", ">365");
        AgeSummary summary = new AgeSummary(ranges);

        // Add component to first bucket
        ComponentXO component1 = createComponentWithAsset(OffsetDateTime.now(ZoneOffset.UTC).minusDays(5));
        summary.addComponent(component1, 1024);

        // Add another component to first bucket
        ComponentXO component2 = createComponentWithAsset(OffsetDateTime.now(ZoneOffset.UTC).minusDays(2));
        summary.addComponent(component2, 512);

        // Add component to second bucket
        ComponentXO component3 = createComponentWithAsset(OffsetDateTime.now(ZoneOffset.UTC).minusDays(15));
        summary.addComponent(component3, 2048);

        assertThat(summary.getTotalComponents()).isEqualTo(3);
        assertThat(summary.getTotalSizeBytes()).isEqualTo(3584);

        AgeBucket firstBucket = summary.getAgeBuckets().get(0);
        assertThat(firstBucket.getComponentCount()).isEqualTo(2);
        assertThat(firstBucket.getSizeBytes()).isEqualTo(1536);

        AgeBucket secondBucket = summary.getAgeBuckets().get(1);
        assertThat(secondBucket.getComponentCount()).isEqualTo(1);
        assertThat(secondBucket.getSizeBytes()).isEqualTo(2048);

        AgeBucket thirdBucket = summary.getAgeBuckets().get(2);
        assertThat(thirdBucket.getComponentCount()).isZero();
        assertThat(thirdBucket.getSizeBytes()).isZero();
    }

    @Test
    void getAgeBuckets_shouldReturnUnmodifiableList() {
        List<String> ranges = Arrays.asList("0-7", "8-30");
        AgeSummary summary = new AgeSummary(ranges);

        List<AgeBucket> buckets = summary.getAgeBuckets();
        
        assertThatThrownBy(() -> buckets.add(new AgeBucket("31-90")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private ComponentXO createComponentWithAsset(OffsetDateTime blobCreated) {
        ComponentXO component = new ComponentXO();
        AssetXO asset = new AssetXO();
        asset.setBlobCreated(blobCreated);
        asset.setFileSize(1024L);
        component.setAssets(Arrays.asList(asset));
        return component;
    }
}