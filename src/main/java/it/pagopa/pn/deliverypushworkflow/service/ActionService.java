package it.pagopa.pn.deliverypushworkflow.service;

import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.Action;

public interface ActionService {
    void addOnlyActionIfAbsent(Action action);
    void unSchedule(String actionId);
}
