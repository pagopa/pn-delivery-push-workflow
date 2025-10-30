package it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.impl;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ReworkRequestEventAction;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class ReworkRequestEvent implements GenericEvent<GenericEventHeader, ReworkRequestEventAction> {

    private GenericEventHeader header;

    private ReworkRequestEventAction payload;
}
