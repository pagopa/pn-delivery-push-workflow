package it.pagopa.pn.deliverypushworkflow.dto.notificationrework;

import java.util.List;

public class NotificationReworkConstant {
    public static final String REC_INDEX = "RECINDEX_";
    public static final String STATUS_VIEWED = "VIEWED";

    public static final List<String> MONO_REC_NOTIFICATION_VALID_STATUS = List.of("EFFECTIVE_DATE", "RETURNED_TO_SENDER", "VIEWED");
    public static final List<String> MULTI_REC_NOTIFICATION_VALID_STATUS = List.of("DELIVERING", "DELIVERED", "EFFETCTIVE_DATE", "VIEWED", "RETURNED_TO_SENDER", "UNREACHABLE");
    public static final String ATTEMPT_0 = "ATTEMPT_0";
    public static final String ATTEMPT_1 = "ATTEMPT_1";
    public static final String KO = "KO";
    public static final String CON = "CON";
}
