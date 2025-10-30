package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.deliverypushworkflow.action.details.NotificationReworkRequestedDetails;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
import it.pagopa.pn.deliverypushworkflow.service.NotificationService;
import it.pagopa.pn.deliverypushworkflow.service.PaperChannelService;
import it.pagopa.pn.deliverypushworkflow.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationReworkRequestedHandler {

    private final TimelineService timelineService;
    private final NotificationService notificationService;
    private final PaperChannelService paperChannelService;

    public Mono<Void> handleNotificationReworkRequested(Action action) {
        return Mono.just(action)
                .flatMap(this::computeTimelineElementToInvalidate)
                .flatMap(this::startNotificationReworkProcess)
                .flatMap(this::updateAttachmentRetention)
                .flatMap(this::buildTimelineElement)
                .zipWith(Mono.just(notificationService.getNotificationByIun(action.getIun())))
                .flatMap(tuple -> addTimelineElement(tuple.getT1(), tuple.getT2()));

    }

    private Mono<Void> addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        return Mono.just(timelineService.addTimelineElement(element, notification)).then();
    }

    private Mono<TimelineElementInternal> buildTimelineElement(Action action) {
        //TODO da implementare quando sarà disponibile la logica
        return Mono.just(new TimelineElementInternal());
    }

    private Mono<Action> updateAttachmentRetention(Action action) {
        return Mono.just(action);
    }

    public Mono<Action> computeTimelineElementToInvalidate(Action action) {
        return Mono.just(action);
    }

    public Mono<Action> startNotificationReworkProcess(Action action) {
        NotificationReworkRequestedDetails details = (NotificationReworkRequestedDetails) action.getDetails();
        return Mono.just(paperChannelService.initNotificationRework(details.getReworkrequestId(), details.getReworkId())).thenReturn(action);
    }
}