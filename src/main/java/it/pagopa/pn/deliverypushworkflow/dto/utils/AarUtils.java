package it.pagopa.pn.deliverypushworkflow.dto.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypushworkflow.dto.delivery.notification.NotificationInt;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

import static it.pagopa.pn.deliverypushworkflow.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_GENERATEPDFFAILED;


@Component
@AllArgsConstructor
@Slf4j
public class AarUtils {
    public static final String START_DYNAMIC_PROPERTY_CHARACTER = "<";
    public static final String END_DYNAMIC_PROPERTY_CHARACTER = ">";
    public static final String RADD_DYNAMIC_TEMPLATE_VALUE = "RADD_TEMPLATE_DEFINITION";

    //private final SaveLegalFactsService saveLegalFactsService;
    //private final TimelineUtils timelineUtils;
    //private final TimelineService timelineService;
    //private final NotificationUtils notificationUtils;
    //private final DocumentCreationRequestService documentCreationRequestService;

    public void generateAARAndSaveInSafeStorageAndAddTimelineEvent(NotificationInt notification, Integer recIndex, String quickAccessToken) {
        try {
            //Todo implementare in seguito
        } catch (Exception e) {
            log.error("Cannot generate AAR pdf iun={} recIndex={} ex=", notification.getIun(), recIndex, e);
            throw new PnInternalException("cannot generate AAR pdf", ERROR_CODE_DELIVERYPUSH_GENERATEPDFFAILED, e);
        }
    }

//    public boolean addAarGenerationToTimeline(NotificationInt notification, Integer recIndex, PdfInfo pdfInfo) {
//        return false; //Todo implementare in seguito
//    }

//    public AarGenerationDetailsInt getAarGenerationDetails(NotificationInt notification, Integer recIndex) {
//        // ricostruisco il timelineid della  genrazione dell'aar
//        String aarGenerationEventId = TimelineEventId.AAR_GENERATION.buildEventId(
//                EventId.builder()
//                        .iun(notification.getIun())
//                        .recIndex(recIndex)
//                        .build()
//        );
//
//        Optional<AarGenerationDetailsInt> detailOpt =
//                timelineService.getTimelineElementDetails(notification.getIun(), aarGenerationEventId, AarGenerationDetailsInt.class);
//
//        if (detailOpt.isEmpty() || !StringUtils.hasText(detailOpt.get().getGeneratedAarUrl()) || detailOpt.get().getNumberOfPages() == null) {
//            log.error("Cannot retreieve AAR pdf safestoragekey iun={} detail={}", notification.getIun(), detailOpt);
//            throw new PnInternalException("cannot retreieve AAR pdf safestoragekey", ERROR_CODE_DELIVERYPUSH_GENERATEPDFFAILED);
//        }
//        return detailOpt.get();
//    }

//    public AarCreationRequestDetailsInt getAarCreationRequestDetailsInt(NotificationInt notification, Integer recIndex) {
//        String elementId = timelineUtils.createEventIdForAarCreationRequest(notification.getIun(), recIndex);
//
//        return timelineService.getTimelineElementDetails(notification.getIun(), elementId, AarCreationRequestDetailsInt.class)
//                .orElseThrow(() -> new PnInternalException("Cannot retrieve AarCreationRequestDetails with elementId " + elementId, ERROR_CODE_DELIVERYPUSH_NOTFOUND));
//    }

    public static boolean needDynamicAarRADDDefinition(String aarTemplateType) {
        String dynamicPropertyValue = org.apache.commons.lang3.StringUtils.substringBetween(aarTemplateType, START_DYNAMIC_PROPERTY_CHARACTER, END_DYNAMIC_PROPERTY_CHARACTER);
        return RADD_DYNAMIC_TEMPLATE_VALUE.equals(dynamicPropertyValue);
    }
}
