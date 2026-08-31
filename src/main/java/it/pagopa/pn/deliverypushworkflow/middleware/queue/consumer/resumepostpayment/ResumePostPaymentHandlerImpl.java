package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.resumepostpayment;

import it.pagopa.pn.deliverypushworkflow.action.completionworkflow.RegisteredLetterSender;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.service.PaperChannelService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@CustomLog
public class ResumePostPaymentHandlerImpl implements ResumePostPaymentHandler {
    static final String ERROR_MARKER = "RESUME_POST_PAYMENT_ERROR";
    static final String SUCCESS_MARKER = "RESUME_POST_PAYMENT_SUCCESS";

    private final ResumePostPaymentEligibilityService eligibilityService;
    private final PaperChannelService paperChannelService;
    private final RegisteredLetterSender registeredLetterSender;

    @Override
    public void handle(ResumePostPaymentEvent event) {
        ResumeValidationResult validationResult = eligibilityService.validate(event);
        if (validationResult.outcome() != ResumeValidationOutcome.ELIGIBLE) {
            var recipientTimelineCategories = validationResult.recipientTimelineCategories().stream()
                    .map(Enum::name)
                    .sorted()
                    .toList();
            log.error("{} iun={} recIndex={} resumeType={} outcome={} reasonCode={} reasonDescription={} recipientTimelineCategories={}",
                    ERROR_MARKER,
                    event.getIun(),
                    event.getRecIndex(),
                    event.getResumeType(),
                    validationResult.outcome(),
                    validationResult.reason().name(),
                    validationResult.reason().getDescription(),
                    recipientTimelineCategories);
            return;
        }

        NotificationInt notification = validationResult.notification();
        String operation = switch (event.getResumeType()) {
            case FIRST_ATTEMPT -> {
                paperChannelService.prepareAnalogNotification(notification, event.getRecIndex(), 0);
                yield "PREPARE_ANALOG_NOTIFICATION_FIRST_ATTEMPT";
            }
            case SECOND_ATTEMPT -> {
                paperChannelService.prepareAnalogNotification(notification, event.getRecIndex(), 1);
                yield "PREPARE_ANALOG_NOTIFICATION_SECOND_ATTEMPT";
            }
            case SIMPLE_REGISTERED_LETTER -> {
                registeredLetterSender.prepareSimpleRegisteredLetter(notification, event.getRecIndex());
                yield "PREPARE_SIMPLE_REGISTERED_LETTER";
            }
        };

        log.info("{} iun={} recIndex={} resumeType={} operation={}",
                SUCCESS_MARKER, event.getIun(), event.getRecIndex(), event.getResumeType(), operation);
    }
}