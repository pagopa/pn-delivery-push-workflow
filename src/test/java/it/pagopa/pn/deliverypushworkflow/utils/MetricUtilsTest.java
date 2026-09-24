package it.pagopa.pn.deliverypushworkflow.utils;

import it.pagopa.pn.commons.log.dto.metrics.Dimension;
import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MetricUtilsTest {

    @Test
    void generateGeneralMetric() {
        long before = Instant.now().toEpochMilli();

        GeneralMetric metric = MetricUtils.generateGeneralMetric(
                MetricUtils.MetricName.VIEW_NOTIFICATION_ATTACHMENTS_CHECK_RESULT,
                1,
                List.of(new Dimension("Result", "KO"))
        );

        assertThat(metric.getNamespace()).isEqualTo("pn-delivery-push-workflow");
        assertThat(metric.getMetrics()).singleElement().satisfies(value -> {
            assertThat(value.getName()).isEqualTo("view_notification_attachments_check_result");
            assertThat(value.getValue()).isEqualTo(1);
        });
        assertThat(metric.getDimensions()).containsExactly(new Dimension("Result", "KO"));
        assertThat(metric.getTimestamp()).isBetween(before, Instant.now().toEpochMilli());
    }
}
