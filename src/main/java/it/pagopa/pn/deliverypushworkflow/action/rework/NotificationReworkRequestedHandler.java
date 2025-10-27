package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkRequestedDetails;
import it.pagopa.pn.deliverypushworkflow.config.PnDeliveryPushWorkflowConfigs;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.SendAnalogProgressDetailsInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationReworkRequestedHandler {

    private final String ATTEMPT_1 = "ATTEMPT_1";
    private final String ATTEMPT_0 = "ATTEMPT_0";
    private final String REC = "REC";

    private final TimelineService timelineService;
    private final PnDeliveryPushWorkflowConfigs pnDeliveryPushWorkflowConfigs;

    public Mono<Action> computeTimelineElementToInvalidate(Action action) {
        return Flux.fromIterable(timelineService.getTimeline(action.getIun(), false))
                .filter(elem -> pnDeliveryPushWorkflowConfigs.getInalidableCategories().contains(elem.getCategory().name()))
                .filter(elem -> elem.getElementId().contains(((NotificationReworkRequestedDetails) action.getDetails()).getRecIndex()))
                .filter(elem -> this.checkAttemptId(elem, ((NotificationReworkRequestedDetails) action.getDetails()).getAttempt()))
                .filter(this::checkDeliveryDetailCode)
                .flatMap(elem -> Mono.just(elem.getElementId()))
                .collectList()
                .thenReturn(action);

    }

    private boolean checkAttemptId(TimelineElementInternal elem, String attempt) {
        if (ATTEMPT_0.equals(attempt)) {
            return !TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE.name().equals(elem.getCategory().name());
        } else {
            return !TimelineElementCategoryInt.PREPARE_ANALOG_DOMICILE.name().equals(elem.getCategory().name()) &&
                    elem.getElementId().contains(ATTEMPT_1);
        }
    }

    private boolean checkDeliveryDetailCode(TimelineElementInternal elem) {
        if (TimelineElementCategoryInt.SEND_ANALOG_PROGRESS.name().equals(elem.getCategory().name())) {
            return ((SendAnalogProgressDetailsInt) elem.getDetails()).getDeliveryDetailCode().startsWith(REC);
        }
        return true;
    }
}