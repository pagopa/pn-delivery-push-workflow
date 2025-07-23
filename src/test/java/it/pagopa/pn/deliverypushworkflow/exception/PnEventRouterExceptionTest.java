package it.pagopa.pn.deliverypushworkflow.exception;

import it.pagopa.pn.deliverypushworkflow.exceptions.PnEventRouterException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PnEventRouterExceptionTest {
    @Test
    void exceptionContainsCorrectMessageAndErrorCode() {
        PnEventRouterException exception = new PnEventRouterException("Test message", "ERROR_CODE");
        assertEquals("ERROR_CODE", exception.getProblem().getErrors().get(0).getCode());
    }

    @Test
    void exceptionContainsCauseWhenProvided() {
        Throwable cause = new RuntimeException("Cause message");
        PnEventRouterException exception = new PnEventRouterException("Test message", "ERROR_CODE", cause);
        assertEquals("ERROR_CODE", exception.getProblem().getErrors().get(0).getCode());
        assertEquals(cause, exception.getCause());
    }
}