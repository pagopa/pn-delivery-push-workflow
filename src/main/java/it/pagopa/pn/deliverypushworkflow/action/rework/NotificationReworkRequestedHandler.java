package it.pagopa.pn.deliverypushworkflow.action.rework;

import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;
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

    public Mono<Void> handleNotificationReworkRequested(Action action) {
        return Mono.just(action)
                .flatMap(this::computeTimelineElementToInvalidate)
                .flatMap(this::startNotificationReworkProcess)
                .flatMap(this::updateAttachmentRetention)
                .flatMap(this::buildTimelineElement)
                .flatMap(this::addTimelineElement)
                .then();

    }

    private Mono<Action> addTimelineElement(Action action) {
        return Mono.just(action);
    }

    private Mono<Action> buildTimelineElement(Action action) {
        return Mono.just(action);
    }

    private Mono<Action> updateAttachmentRetention(Action action) {
        return Mono.just(action);
    }

    public Mono<Action> computeTimelineElementToInvalidate(Action action) {
        return Mono.just(action);
    }

    public Mono<Action> startNotificationReworkProcess(Action action) {
        return Mono.just(action);
    }
}