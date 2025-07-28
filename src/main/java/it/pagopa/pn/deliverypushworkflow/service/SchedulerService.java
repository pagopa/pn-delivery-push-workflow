package it.pagopa.pn.deliverypushworkflow.service;

import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionDetails;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.ActionType;

import java.time.Instant;

public interface SchedulerService {
    void scheduleEvent(String iun, Instant dateToSchedule, ActionType actionType);

    void scheduleEventNowOnlyIfAbsent(String iun, ActionType actionType, ActionDetails actionDetails);

    void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule, ActionType actionType);

    void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule, ActionType actionType, String timelineId);
    
    void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule, ActionType actionType, String timelineEventId, ActionDetails actionDetails);
        
    void unscheduleEvent(String iun, Integer recIndex, ActionType actionType, String timelineId);

    void scheduleEvent(String iun, Integer recIndex, Instant dateToSchedule, ActionType actionType, ActionDetails actionDetails);
}
