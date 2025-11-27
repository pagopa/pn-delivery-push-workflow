package it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool;

import it.pagopa.pn.deliverypushworkflow.action.details.*;
import it.pagopa.pn.deliverypushworkflow.dto.timeline.TimelineEventIdParser;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.deliverypush.StringUtil;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Getter
public enum ActionType {
  
  NOTIFICATION_CANCELLATION(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("notification_cancellation_iun_%s", action.getIun());
    }
  },

  CHECK_ATTACHMENT_RETENTION(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("check_attachment_retention_iun_%s_scheduling-date_%s",
              action.getIun(),
              action.getNotBefore()
      );
    }
  },
  
  START_RECIPIENT_WORKFLOW(RecipientsWorkflowDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("%s_start_recipient_workflow_%d",
              action.getIun(), 
              action.getRecipientIndex());
    }
  },

  CHOOSE_DELIVERY_MODE(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("%s_choose_delivery_mode_%d", action.getIun(),
          action.getRecipientIndex());
    }
  },

  ANALOG_WORKFLOW(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("%s_analog_workflow_e_%d", action.getIun(), action.getRecipientIndex());
    }
  },

  DIGITAL_WORKFLOW_NEXT_ACTION(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("%s_digital_workflow_e_%d_timelineid_%s", action.getIun(), action.getRecipientIndex(), action.getTimelineId() == null ? "" : action.getTimelineId());
    }
  },

  DIGITAL_WORKFLOW_NEXT_EXECUTE_ACTION(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("%s_digital_workflow_execute_e_%d_timelineid_%s", action.getIun(), action.getRecipientIndex(), action.getTimelineId() == null ? "" : action.getTimelineId());
    }
  },
  
  DIGITAL_WORKFLOW_NO_RESPONSE_TIMEOUT_ACTION(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("%s_digital_workflow_no_response_timeount_e_%d_%s", action.getIun(),
          action.getRecipientIndex(), action.getTimelineId() == null ? "" : action.getTimelineId());
    }
  },

  DIGITAL_WORKFLOW_RETRY_ACTION(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("%s_digital_workflow_retry_e_%d_%s", action.getIun(),
          action.getRecipientIndex(), action.getTimelineId() == null ? "" : action.getTimelineId());
    }
  },

  SEND_DIGITAL_FINAL_STATUS_RESPONSE(SendDigitalFinalStatusResponseDetails.class) {
    @Override
    public String buildActionId(Action action) {
      SendDigitalFinalStatusResponseDetails details = (SendDigitalFinalStatusResponseDetails) action.getDetails();

      return String.format("%s_send_digital_final_status_response_feedback-timeline-id_%s", 
              action.getIun(),
              details.getLastAttemptAddressInfo().getRelatedFeedbackTimelineId());
    }
  },

  REFINEMENT_NOTIFICATION(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {
      Optional<String> reworkIndexFull = TimelineEventIdParser.parse(action.getTimelineId()).reworkIndexFull();
      if(StringUtils.hasText(action.getTimelineId()) && reworkIndexFull.isPresent()){
        return String.format("%s_refinement_notification_%d_%s", action.getIun(),
                action.getRecipientIndex(), reworkIndexFull);
      }
      return String.format("%s_refinement_notification_%d", action.getIun(),
          action.getRecipientIndex());
    }
  },
  
  SENDER_ACK(NotHandledDetails.class) {

    @Override
    public String buildActionId(Action action) {
      return String.format("%s_start", action.getIun());
    }
  },

  DOCUMENT_CREATION_RESPONSE(DocumentCreationResponseActionDetails.class) {
    @Override
    public String buildActionId(Action action) {
        return String.format("safe_storage_response_timelineId=%s",
                action.getTimelineId()
        );
    }
    
  },

  POST_ACCEPTED_PROCESSING_COMPLETED(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("%s_post_accepted_processing",
              action.getIun());
    }
  },

  SEND_ANALOG_FINAL_STATUS_RESPONSE(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {

      return String.format("send_analog_final_status_response_feedback-timeline-id_%s",
              action.getTimelineId());
    }
  },

  ANALOG_WORKFLOW_NO_FEEDBACK_TIMEOUT(AnalogWorkflowTimeoutDetails.class) {
    @Override
    public String buildActionId(Action action) {
      AnalogWorkflowTimeoutDetails details = (AnalogWorkflowTimeoutDetails) action.getDetails();
      return String.format("%s_analog_workflow_timeout_recIndex_%d_attempt_%d",
              action.getIun(),
              action.getRecipientIndex(),
              details.getSentAttemptMade()
      );
    }
  },

  NOTIFICATION_REWORK_VALIDATION(NotHandledDetails.class) {
    @Override
    public String buildActionId(Action action) {
      return String.format("%s_notification_rework_validation", action.getIun());
    }
  },

  NOTIFICATION_REWORK_REQUESTED(NotificationReworkRequestedDetails.class) {
    @Override
    public String buildActionId(Action action) {

      return String.format("notification_rework_requested_%s",
              action.getIun());
    }
  };

  private final Class<? extends ActionDetails> detailsJavaClass;

  ActionType(Class<? extends ActionDetails> detailsJavaClass) {
    this.detailsJavaClass = detailsJavaClass;
  }

  public String buildActionId(Action action) {
    throw new UnsupportedOperationException("Must be implemented for each action type");
  }

}
