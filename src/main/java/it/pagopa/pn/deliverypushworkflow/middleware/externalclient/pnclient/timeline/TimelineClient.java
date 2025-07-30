package it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.timeline;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NewTimelineElement;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.TimelineCategory;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.TimelineElement;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.TimelineElementDetails;

import java.util.List;

public interface TimelineClient {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_TIMELINE_SERVICE;
    String ADD_TIMELINE_ELEMENT = "ADD TIMELINE ELEMENT";
    String RETRIEVE_AND_INCREMENT_COUNTER_FOR_TIMELINE_EVENT = "RETRIEVE AND INCREMENT COUNTER FOR TIMELINE EVENT";
    String GET_TIMELINE_ELEMENT = "GET TIMELINE ELEMENT";
    String GET_TIMELINE_ELEMENT_DETAILS = "GET TIMELINE ELEMENT DETAILS";
    String GET_TIMELINE_ELEMENT_DETAIL_FOR_SPECIFIC_RECIPIENT = "GET TIMELINE ELEMENT DETAIL FOR SPECIFIC RECIPIENT";
    String GET_TIMELINE_ELEMENT_FOR_SPECIFIC_RECIPIENT = "GET TIMELINE ELEMENT FOR SPECIFIC RECIPIENT";
    String GET_TIMELINE = "GET TIMELINE";

    boolean addTimelineElement(TimelineElementInternal element, NotificationInt notification);

    Long retrieveAndIncrementCounterForTimelineEvent(String timelineId);

    TimelineElementInternal getTimelineElement(String iun, String timelineId, Boolean strongly);

    TimelineElementDetailsInt getTimelineElementDetails(String iun, String timelineId);

    TimelineElementDetailsInt getTimelineElementDetailForSpecificRecipient(String iun, Integer recIndex, Boolean confidentialInfoRequired, TimelineElementCategoryInt category);

    TimelineElementInternal getTimelineElementForSpecificRecipient(String iun, Integer recIndex, TimelineElementCategoryInt category);

    List<TimelineElementInternal> getTimeline(String iun, Boolean confidentialInfoRequired, Boolean strongly, String timelineId);

}
