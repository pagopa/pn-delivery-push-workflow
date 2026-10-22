package it.pagopa.pn.deliverypushworkflow.dto.timeline;

import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

class TimelineElementInternalTest {


    @Test
    void compareToBase() {

        Instant tPrimo = Instant.EPOCH.plus(1, ChronoUnit.DAYS);
        Instant tSecondo = Instant.EPOCH.plus(2, ChronoUnit.DAYS);

        TimelineElementInternal progress = createElement(
                tPrimo, TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS);
        TimelineElementInternal laterProgress = createElement(
                tSecondo, TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS);

        Assertions.assertTrue(progress.compareTo(laterProgress) < 0);
        Assertions.assertTrue(laterProgress.compareTo(progress) > 0);

        TimelineElementInternal feedback = createElement(
                tPrimo, TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK);

        // A parità di timestamp, FEEDBACK precede PROGRESS per elementId.
        Assertions.assertTrue(progress.compareTo(feedback) > 0);
        Assertions.assertTrue(feedback.compareTo(progress) < 0);

        // Il timestamp ha precedenza sull'elementId.
        TimelineElementInternal laterFeedback = createElement(
                tSecondo, TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK);

        Assertions.assertTrue(progress.compareTo(laterFeedback) < 0);
        Assertions.assertTrue(laterFeedback.compareTo(progress) > 0);
        Assertions.assertTrue(laterProgress.compareTo(feedback) > 0);
    }

    @Test
    void compareToBaseAnalog() {
        Instant tPrimo = Instant.EPOCH.plus(1, ChronoUnit.DAYS);
        Instant tSecondo = Instant.EPOCH.plus(2, ChronoUnit.DAYS);

        TimelineElementInternal progress = createElement(
                tPrimo, TimelineElementCategoryInt.SEND_ANALOG_PROGRESS);
        TimelineElementInternal laterProgress = createElement(
                tSecondo, TimelineElementCategoryInt.SEND_ANALOG_PROGRESS);

        Assertions.assertTrue(progress.compareTo(laterProgress) < 0);
        Assertions.assertTrue(laterProgress.compareTo(progress) > 0);

        TimelineElementInternal feedback = createElement(
                tPrimo, TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);

        // A parità di timestamp, FEEDBACK precede PROGRESS per elementId.
        Assertions.assertTrue(progress.compareTo(feedback) > 0);
        Assertions.assertTrue(feedback.compareTo(progress) < 0);

        // Il timestamp ha precedenza sull'elementId.
        TimelineElementInternal laterFeedback = createElement(
                tSecondo, TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK);

        Assertions.assertTrue(progress.compareTo(laterFeedback) < 0);
        Assertions.assertTrue(laterFeedback.compareTo(progress) > 0);
        Assertions.assertTrue(laterProgress.compareTo(feedback) > 0);
    }

    @Test
    void compareTo() {
        Instant tPrimo = Instant.EPOCH.plus(1, ChronoUnit.DAYS);
        Instant tSecondo = Instant.EPOCH.plus(2, ChronoUnit.DAYS);

        TimelineElementInternal t1Progress = createElement(
                tPrimo, TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS);
        TimelineElementInternal t2Progress = createElement(
                tSecondo, TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS);

        Set<TimelineElementInternal> set = Set.of(t1Progress, t2Progress);
        List<TimelineElementInternal> list = set.stream()
                .sorted(Comparator.naturalOrder())
                .toList();

        Assertions.assertEquals(List.of(t1Progress, t2Progress), list);
    }

    @Test
    void compareToSame() {
        Instant timestamp = Instant.EPOCH.plus(1, ChronoUnit.DAYS);

        TimelineElementInternal progress = createElement(
                timestamp, TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS);
        TimelineElementInternal feedback = createElement(
                timestamp, TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK);

        Set<TimelineElementInternal> set = Set.of(progress, feedback);
        List<TimelineElementInternal> list = set.stream()
                .sorted(Comparator.naturalOrder())
                .toList();

        Assertions.assertEquals(List.of(feedback, progress), list);
    }

    @Test
    void compareToSameTimestampAndElementId() {
        TimelineElementInternal element = createElement(
                Instant.EPOCH, TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS);

        TimelineElementInternal other = element.toBuilder()
                .category(TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK)
                .build();

        Assertions.assertEquals(0, element.compareTo(other));
        Assertions.assertEquals(0, other.compareTo(element));
    }

    private TimelineElementInternal createElement(
            Instant timestamp, TimelineElementCategoryInt category) {
        return TimelineElementInternal.builder()
                .timestamp(timestamp)
                .elementId(category.name())
                .category(category)
                .build();
    }
}