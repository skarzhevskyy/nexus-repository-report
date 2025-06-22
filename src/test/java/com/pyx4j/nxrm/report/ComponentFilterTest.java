package com.pyx4j.nxrm.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.sonatype.nexus.model.AssetXO;
import org.sonatype.nexus.model.ComponentXO;

class ComponentFilterTest {

    @Test
    void createFilter_withNullArgs_shouldThrowException() {
        assertThatThrownBy(() -> ComponentFilter.createFilter(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Command arguments cannot be null");
    }

    @Test
    void createFilter_withNoFilters_shouldAcceptAllComponents() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        Predicate<ComponentXO> filter = ComponentFilter.createFilter(args);

        ComponentXO component = createComponentWithAsset(
                OffsetDateTime.parse("2024-06-01T00:00:00Z"),
                OffsetDateTime.parse("2024-06-02T00:00:00Z"),
                OffsetDateTime.parse("2024-06-03T00:00:00Z")
        );

        assertThat(filter.test(component)).isTrue();
    }

    @Test
    void createFilter_withCreatedBeforeFilter_shouldFilterCorrectly() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        args.createdBefore = "2024-06-02T00:00:00Z";

        Predicate<ComponentXO> filter = ComponentFilter.createFilter(args);

        // Component created before the filter date should pass
        ComponentXO oldComponent = createComponentWithAsset(
                OffsetDateTime.parse("2024-06-01T00:00:00Z"), null, null
        );
        assertThat(filter.test(oldComponent)).isTrue();

        // Component created after the filter date should be filtered out
        ComponentXO newComponent = createComponentWithAsset(
                OffsetDateTime.parse("2024-06-03T00:00:00Z"), null, null
        );
        assertThat(filter.test(newComponent)).isFalse();
    }

    @Test
    void createFilter_withCreatedAfterFilter_shouldFilterCorrectly() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        args.createdAfter = "2024-06-02T00:00:00Z";

        Predicate<ComponentXO> filter = ComponentFilter.createFilter(args);

        // Component created before the filter date should be filtered out
        ComponentXO oldComponent = createComponentWithAsset(
                OffsetDateTime.parse("2024-06-01T00:00:00Z"), null, null
        );
        assertThat(filter.test(oldComponent)).isFalse();

        // Component created after the filter date should pass
        ComponentXO newComponent = createComponentWithAsset(
                OffsetDateTime.parse("2024-06-03T00:00:00Z"), null, null
        );
        assertThat(filter.test(newComponent)).isTrue();
    }

    @Test
    void createFilter_withUpdatedFilters_shouldFilterCorrectly() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        args.updatedBefore = "2024-06-05T00:00:00Z";
        args.updatedAfter = "2024-06-03T00:00:00Z";

        Predicate<ComponentXO> filter = ComponentFilter.createFilter(args);

        // Component updated within range should pass
        ComponentXO componentInRange = createComponentWithAsset(
                null, OffsetDateTime.parse("2024-06-04T00:00:00Z"), null
        );
        assertThat(filter.test(componentInRange)).isTrue();

        // Component updated before range should be filtered out
        ComponentXO tooOldComponent = createComponentWithAsset(
                null, OffsetDateTime.parse("2024-06-02T00:00:00Z"), null
        );
        assertThat(filter.test(tooOldComponent)).isFalse();

        // Component updated after range should be filtered out
        ComponentXO tooNewComponent = createComponentWithAsset(
                null, OffsetDateTime.parse("2024-06-06T00:00:00Z"), null
        );
        assertThat(filter.test(tooNewComponent)).isFalse();
    }

    @Test
    void createFilter_withDownloadedFilters_shouldFilterCorrectly() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        args.downloadedBefore = "2024-06-05T00:00:00Z";
        args.downloadedAfter = "2024-06-03T00:00:00Z";

        Predicate<ComponentXO> filter = ComponentFilter.createFilter(args);

        // Component downloaded within range should pass
        ComponentXO componentInRange = createComponentWithAsset(
                null, null, OffsetDateTime.parse("2024-06-04T00:00:00Z")
        );
        assertThat(filter.test(componentInRange)).isTrue();

        // Component downloaded before range should be filtered out
        ComponentXO tooOldComponent = createComponentWithAsset(
                null, null, OffsetDateTime.parse("2024-06-02T00:00:00Z")
        );
        assertThat(filter.test(tooOldComponent)).isFalse();

        // Component downloaded after range should be filtered out
        ComponentXO tooNewComponent = createComponentWithAsset(
                null, null, OffsetDateTime.parse("2024-06-06T00:00:00Z")
        );
        assertThat(filter.test(tooNewComponent)).isFalse();
    }

    @Test
    void createFilter_withNeverDownloadedFilter_shouldOnlyAcceptNeverDownloaded() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        args.neverDownloaded = true;

        Predicate<ComponentXO> filter = ComponentFilter.createFilter(args);

        // Component never downloaded should pass
        ComponentXO neverDownloaded = createComponentWithAsset(null, null, null);
        assertThat(filter.test(neverDownloaded)).isTrue();

        // Component that was downloaded should be filtered out
        ComponentXO downloaded = createComponentWithAsset(
                null, null, OffsetDateTime.parse("2024-06-01T00:00:00Z")
        );
        assertThat(filter.test(downloaded)).isFalse();
    }

    @Test
    void createFilter_withNeverDownloadedAndDownloadedFilter_shouldThrowException() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        args.neverDownloaded = true;
        args.downloadedBefore = "2024-06-01T00:00:00Z";

        assertThatThrownBy(() -> ComponentFilter.createFilter(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot combine --never-downloaded with --downloaded-before or --downloaded-after filters");
    }

    @Test
    void createFilter_withDaysAgoFormat_shouldWork() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        args.createdAfter = "7d"; // 7 days ago

        Predicate<ComponentXO> filter = ComponentFilter.createFilter(args);

        // Component created yesterday should pass
        ComponentXO recentComponent = createComponentWithAsset(
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(1), null, null
        );
        assertThat(filter.test(recentComponent)).isTrue();

        // Component created 10 days ago should be filtered out
        ComponentXO oldComponent = createComponentWithAsset(
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(10), null, null
        );
        assertThat(filter.test(oldComponent)).isFalse();
    }

    @Test
    void createFilter_withInvalidDateRange_shouldThrowException() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        args.createdBefore = "2024-06-01T00:00:00Z";
        args.createdAfter = "2024-06-10T00:00:00Z"; // After is later than before

        assertThatThrownBy(() -> ComponentFilter.createFilter(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid created filter");
    }

    @Test
    void createFilter_withComponentHavingMultipleAssets_shouldPassIfAnyAssetMatches() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        args.createdAfter = "2024-06-02T00:00:00Z";

        Predicate<ComponentXO> filter = ComponentFilter.createFilter(args);

        // Component with one old asset and one new asset should pass
        ComponentXO component = new ComponentXO();
        AssetXO oldAsset = new AssetXO();
        oldAsset.setBlobCreated(OffsetDateTime.parse("2024-06-01T00:00:00Z"));

        AssetXO newAsset = new AssetXO();
        newAsset.setBlobCreated(OffsetDateTime.parse("2024-06-03T00:00:00Z"));

        component.setAssets(List.of(oldAsset, newAsset));

        assertThat(filter.test(component)).isTrue();
    }

    @Test
    void createFilter_withComponentHavingNoAssets_shouldBeFilteredOut() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        args.createdAfter = "2024-06-01T00:00:00Z";

        Predicate<ComponentXO> filter = ComponentFilter.createFilter(args);

        ComponentXO emptyComponent = new ComponentXO();
        emptyComponent.setAssets(Collections.emptyList());

        assertThat(filter.test(emptyComponent)).isFalse();
    }

    @Test
    void createFilter_withNullComponent_shouldBeFilteredOut() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        Predicate<ComponentXO> filter = ComponentFilter.createFilter(args);

        assertThat(filter.test(null)).isFalse();
    }

    @Test
    void createFilter_withAssetsMissingTimestamps_shouldBeFilteredOutForTimeBasedFilters() {
        NxReportCommandArgs args = new NxReportCommandArgs();
        args.createdAfter = "2024-06-01T00:00:00Z";

        Predicate<ComponentXO> filter = ComponentFilter.createFilter(args);

        // Component with asset that has no creation timestamp should be filtered out
        ComponentXO component = createComponentWithAsset(null, null, null);
        assertThat(filter.test(component)).isFalse();
    }

    private ComponentXO createComponentWithAsset(OffsetDateTime blobCreated, OffsetDateTime lastModified, OffsetDateTime lastDownloaded) {
        ComponentXO component = new ComponentXO();
        AssetXO asset = new AssetXO();
        asset.setBlobCreated(blobCreated);
        asset.setLastModified(lastModified);
        asset.setLastDownloaded(lastDownloaded);
        component.setAssets(List.of(asset));
        return component;
    }
}
