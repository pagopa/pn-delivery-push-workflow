package it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.impl;

import it.pagopa.pn.api.dto.events.GenericEventHeader;
import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventAction;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventPool;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
@Slf4j
public class ReworkRequestEventPoolImpl implements ReworkRequestEventPool {

    private final MomProducer<ReworkRequestEvent> reworkRequestQueue;
    private final Clock clock;

    public ReworkRequestEventPoolImpl(MomProducer<ReworkRequestEvent> reworkRequestQueue,
                             Clock clock ) {
        this.reworkRequestQueue = reworkRequestQueue;
        this.clock = clock;
    }

    @Override
    public void scheduleFutureAction(ReworkRequestEventAction action, ReworkRequestEventType type) {
        addReworkRequestEventAction(action, type);
    }

    private void addReworkRequestEventAction(ReworkRequestEventAction action, ReworkRequestEventType reworkRequestEventType) {
        ReworkRequestEvent reworkRequestEvent = ReworkRequestEvent.builder()
                .header( GenericEventHeader.builder()
                        .publisher("pn-delivery-push-workflow")
                        .eventId(UUID.randomUUID().toString())
                        .createdAt( clock.instant() )
                        .eventType(reworkRequestEventType.name())
                        .build())
                .payload( action )
                .build();
        reworkRequestQueue.push(reworkRequestEvent);
    }
}
