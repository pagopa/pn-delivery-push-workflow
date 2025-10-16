package it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool;

public interface ReworkRequestEventPool {
    void scheduleFutureAction(ReworkRequestEventAction action, ReworkRequestEventType type);
}
