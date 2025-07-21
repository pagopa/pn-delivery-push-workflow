package it.pagopa.pn.deliverypushworkflow.dto.timeline.details;

import java.time.Instant;

public interface ElementTimestampTimelineElementDetails extends TimelineElementDetailsInt {

    Instant getElementTimestamp();
}
