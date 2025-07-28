package it.pagopa.pn.deliverypushworkflow.dto.ext.safestorage;

import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.pnsafestorage.model.FileCreationRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FileCreationWithContentRequest extends FileCreationRequest {
    private byte[] content;
}
