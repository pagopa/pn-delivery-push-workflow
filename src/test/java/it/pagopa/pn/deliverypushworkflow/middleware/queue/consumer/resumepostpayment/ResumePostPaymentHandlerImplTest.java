package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.resumepostpayment;

import it.pagopa.pn.deliverypushworkflow.action.completionworkflow.RegisteredLetterSender;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.service.PaperChannelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumePostPaymentHandlerImplTest {
    private static final String IUN = "IUN_01";
    private static final int REC_INDEX = 1;

    @Mock
    private ResumePostPaymentEligibilityService eligibilityService;
    @Mock
    private PaperChannelService paperChannelService;
    @Mock
    private RegisteredLetterSender registeredLetterSender;
    @Mock
    private NotificationInt notification;

    private ResumePostPaymentHandlerImpl handler;

    @BeforeEach
    void setUp() {
        handler = new ResumePostPaymentHandlerImpl(eligibilityService, paperChannelService, registeredLetterSender);
    }

    @Test
    void restartsFirstAttemptThroughPaperChannelService() {
        ResumePostPaymentEvent event = event(ResumeType.FIRST_ATTEMPT);
        when(eligibilityService.validate(event)).thenReturn(ResumeValidationResult.eligible(notification));

        handler.handle(event);

        verify(paperChannelService).prepareAnalogNotification(notification, REC_INDEX, 0);
        verifyNoInteractions(registeredLetterSender);
    }

    @Test
    void restartsSecondAttemptThroughPaperChannelService() {
        ResumePostPaymentEvent event = event(ResumeType.SECOND_ATTEMPT);
        when(eligibilityService.validate(event)).thenReturn(ResumeValidationResult.eligible(notification));

        handler.handle(event);

        verify(paperChannelService).prepareAnalogNotification(notification, REC_INDEX, 1);
        verifyNoInteractions(registeredLetterSender);
    }

    @Test
    void restartsSimpleRegisteredLetterThroughExistingSender() {
        ResumePostPaymentEvent event = event(ResumeType.SIMPLE_REGISTERED_LETTER);
        when(eligibilityService.validate(event)).thenReturn(ResumeValidationResult.eligible(notification));

        handler.handle(event);

        verify(registeredLetterSender).prepareSimpleRegisteredLetter(notification, REC_INDEX);
        verifyNoInteractions(paperChannelService);
    }

    @Test
    void doesNotRestartWhenEventIsNotEligible() {
        ResumePostPaymentEvent event = event(ResumeType.FIRST_ATTEMPT);
        when(eligibilityService.validate(event)).thenReturn(ResumeValidationResult.notEligible(
                ResumeValidationReason.PAYMENT_NOT_FOUND, notification));

        handler.handle(event);

        verifyNoInteractions(paperChannelService, registeredLetterSender);
    }

    @Test
    void doesNotRestartWhenEventWasAlreadyProcessed() {
        ResumePostPaymentEvent event = event(ResumeType.SECOND_ATTEMPT);
        when(eligibilityService.validate(event)).thenReturn(
            ResumeValidationResult.alreadyProcessed(notification, Collections.emptySet()));

        handler.handle(event);

        verifyNoInteractions(paperChannelService, registeredLetterSender);
    }

    @Test
    void propagatesEligibilityErrors() {
        ResumePostPaymentEvent event = event(ResumeType.FIRST_ATTEMPT);
        RuntimeException exception = new RuntimeException("timeline unavailable");
        when(eligibilityService.validate(event)).thenThrow(exception);

        assertThrows(RuntimeException.class, () -> handler.handle(event));

        verifyNoInteractions(paperChannelService, registeredLetterSender);
    }

    @Test
    void propagatesNativeServiceErrors() {
        ResumePostPaymentEvent event = event(ResumeType.FIRST_ATTEMPT);
        RuntimeException exception = new RuntimeException("paper channel unavailable");
        when(eligibilityService.validate(event)).thenReturn(ResumeValidationResult.eligible(notification));
        doThrow(exception).when(paperChannelService).prepareAnalogNotification(notification, REC_INDEX, 0);

        assertThrows(RuntimeException.class, () -> handler.handle(event));

        verifyNoInteractions(registeredLetterSender);
    }

    private ResumePostPaymentEvent event(ResumeType resumeType) {
        return ResumePostPaymentEvent.builder()
                .iun(IUN)
                .recIndex(REC_INDEX)
                .resumeType(resumeType)
                .build();
    }
}
