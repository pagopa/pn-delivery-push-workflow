package it.pagopa.pn.deliverypushworkflow.service;

import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.AddTimelineElementResponse;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.timelineservice.model.NotificationHistoryResponse;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public interface TimelineService {

    AddTimelineElementResponse addTimelineElement(TimelineElementInternal element, NotificationInt notification);

    Long retrieveAndIncrementCounterForTimelineEvent(String timelineId);

    Optional<TimelineElementInternal> getTimelineElement(String iun, String timelineId);

    Optional<TimelineElementInternal> getTimelineElementStrongly(String iun, String timelineId);

    <T> Optional<T> getTimelineElementDetails(String iun, String timelineId, Class<T> timelineDetailsClass);

    <T> Optional<T> getTimelineElementDetailForSpecificRecipient(String iun, int recIndex, boolean confidentialInfoRequired, TimelineElementCategoryInt category, Class<T> timelineDetailsClass);

    Optional<TimelineElementInternal> getTimelineElementForSpecificRecipient(String iun, int recIndex, TimelineElementCategoryInt category);

    /**
     * Recupera gli elementi di timeline gestiti dal microservizio per uno IUN specifico.
     * <br/> <br/>
     * (Per gestiti dal microservizio si intende gli elementi la cui categoria e classe di dettaglio è censita nell'enum {@link TimelineElementCategoryInt})
     * @param iun iun della notifica
     * @param confidentialInfoRequired se true, recupera le informazioni sensibili presenti negli elementi di timeline
     * @return Set di elementi di timeline per lo IUN specificato.
     */
    Set<TimelineElementInternal> getTimeline(String iun, boolean confidentialInfoRequired);

    /**
     * Recupera gli elementi di timeline gestiti dal microservizio per uno IUN specifico. Richiedendo una consistent read su dynamo.
     * <br/>
     *
     * <p><strong>Nota:</strong> Vengono considerati solo gli elementi di timeline la cui categoria
     * e classe di dettaglio sono registrate nell'enum {@link TimelineElementCategoryInt}.
     * @param iun iun della notifica
     * @param confidentialInfoRequired {@code true} per includere le informazioni sensibili
     *                                negli elementi di timeline restituiti, {@code false} altrimenti
     * @return un {@code Set} contenente gli elementi di timeline che soddisfano i criteri specificati.
     *         Il set sarà vuoto se non vengono trovati elementi corrispondenti.
     */
    Set<TimelineElementInternal> getTimelineStrongly(String iun, boolean confidentialInfoRequired);

    /**
     * Recupera tutti gli elementi di timeline gestiti dal microservizio che corrispondono ai criteri specificati.
     *
     * <p>Il metodo filtra gli elementi per:
     * <ul>
     *   <li><strong>IUN</strong>: deve corrispondere esattamente al valore fornito</li>
     *   <li><strong>Timeline ID</strong>: deve iniziare con il prefisso specificato</li>
     * </ul>
     *
     * <p><strong>Nota:</strong> Vengono considerati solo gli elementi di timeline la cui categoria
     * e classe di dettaglio sono registrate nell'enum {@link TimelineElementCategoryInt}.
     *
     * @param iun  iun della notifica
     * @param timelineId il prefisso del timeline ID da utilizzare come filtro
     * @param confidentialInfoRequired {@code true} per includere le informazioni sensibili
     *                                negli elementi di timeline restituiti, {@code false} altrimenti
     *
     * @return un {@code Set} contenente gli elementi di timeline che soddisfano i criteri specificati.
     *         Il set sarà vuoto se non vengono trovati elementi corrispondenti.
     */
    Set<TimelineElementInternal> getTimelineByIunTimelineId(String iun, String timelineId, boolean confidentialInfoRequired);

    NotificationHistoryResponse getTimelineAndStatusHistory(String iun, int recipients, Instant createdAt);
}
