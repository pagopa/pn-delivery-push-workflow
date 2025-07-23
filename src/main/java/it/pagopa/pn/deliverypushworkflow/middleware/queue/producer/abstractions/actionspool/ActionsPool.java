package it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool;


public interface ActionsPool {
    void addOnlyAction(Action action);
    void unscheduleFutureAction( String actionId );
}
