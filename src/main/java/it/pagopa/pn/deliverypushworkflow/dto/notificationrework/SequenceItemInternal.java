package it.pagopa.pn.deliverypushworkflow.dto.notificationrework;

import lombok.Data;

import java.util.List;

@Data
public class SequenceItemInternal {
    private String statusCode;
    private List<String> attachments;
}