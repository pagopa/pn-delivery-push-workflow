package it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.impl;

import lombok.Data;

import java.time.Duration;

@Data
public class TimeParams {
    private Duration waitingForReadCourtesyMessage;
    private Duration schedulingDaysSuccessDigitalRefinement;
    private Duration schedulingDaysFailureDigitalRefinement;
    private Duration schedulingDaysSuccessAnalogRefinement;
    private Duration schedulingDaysFailureAnalogRefinement;
    private Duration secondNotificationWorkflowWaitingTime;
    private String notificationNonVisibilityTime;
    private Duration timeToAddInNonVisibilityTimeCase;
    private Duration checkAttachmentTimeBeforeExpiration;
    private Duration attachmentTimeToAddAfterExpiration;
    private Duration scheduleAnalogWorkflowTimeoutOffset;
}
