package it.pagopa.pn.deliverypushworkflow.service;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;

public interface AuditLogService {
    PnAuditLogEvent buildAuditLogEvent(String iun, PnAuditLogEventType pnAuditLogEventType, String message, Object ... arguments);
    
    PnAuditLogEvent buildAuditLogEvent(String iun, Integer recIndex, PnAuditLogEventType pnAuditLogEventType, String message, Object ... arguments);
}
