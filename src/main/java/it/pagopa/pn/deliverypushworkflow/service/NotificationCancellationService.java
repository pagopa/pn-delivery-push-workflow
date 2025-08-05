package it.pagopa.pn.deliverypushworkflow.service;

public interface NotificationCancellationService {
    void continueCancellationProcess(String iun);
    void completeCancellationProcess(String iun, String legalFactId);
}
