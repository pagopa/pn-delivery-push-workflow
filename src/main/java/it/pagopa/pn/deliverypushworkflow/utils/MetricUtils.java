package it.pagopa.pn.deliverypushworkflow.utils;

import it.pagopa.pn.commons.log.dto.metrics.Dimension;
import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import it.pagopa.pn.commons.log.dto.metrics.Metric;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

public final class MetricUtils {
    private static final String METRIC_NAMESPACE = "pn-delivery-push-workflow";

    private MetricUtils() {
    }

    @Getter
    public enum MetricName {
        VIEW_NOTIFICATION_ATTACHMENTS_CHECK_RESULT("view_notification_attachments_check_result");

        private final String value;

        MetricName(String value) {
            this.value = value;
        }
    }

    public static GeneralMetric generateGeneralMetric(
            MetricName metricName,
            int metricValue,
            List<Dimension> dimensions
    ) {
        GeneralMetric generalMetric = new GeneralMetric();
        generalMetric.setNamespace(METRIC_NAMESPACE);
        generalMetric.setMetrics(List.of(new Metric(metricName.getValue(), metricValue)));
        generalMetric.setDimensions(dimensions);
        generalMetric.setTimestamp(Instant.now().toEpochMilli());
        return generalMetric;
    }
}
