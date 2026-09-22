package it.pagopa.pn.deliverypushworkflow.middleware.queue.consumer.resumepostpayment;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import it.pagopa.pn.deliverypushworkflow.action.completionworkflow.RegisteredLetterSender;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.service.PaperChannelService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    private ResumePostPaymentHandler handler;
    private ch.qos.logback.classic.Logger logger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        handler = new ResumePostPaymentHandler(eligibilityService, paperChannelService, registeredLetterSender);
        logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ResumePostPaymentHandler.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Test
    void restartsFirstAttemptThroughPaperChannelService() {
        ResumePostPaymentEvent event = event(ResumeType.FIRST_ATTEMPT);
        when(eligibilityService.validate(event)).thenReturn(ResumeValidationResult.eligible(notification));

        handler.handle(event);

        verify(paperChannelService).prepareAnalogNotification(notification, REC_INDEX, 0);
        verifyNoInteractions(registeredLetterSender);
        assertLogContains(ResumePostPaymentHandler.SUCCESS_MARKER);
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
        assertLogContains(ResumePostPaymentHandler.ERROR_MARKER);
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

    private void assertLogContains(String marker) {
        assertTrue(logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .anyMatch(message -> message.contains(marker)));
    }
}
