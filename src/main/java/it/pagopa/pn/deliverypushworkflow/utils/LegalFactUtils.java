package it.pagopa.pn.deliverypushworkflow.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;

@Slf4j
public class LegalFactUtils {

    private LegalFactUtils() {
    }

    public static int getNumberOfPageFromPdfBytes(byte[] pdf ) {
        try (PDDocument document = PDDocument.load(pdf)) {
            return document.getNumberOfPages();
        } catch (IOException ex) {
            log.error("Exception in getNumberOfPageFromPdfBytes for pdf - ex", ex);
            throw new PnInternalException("Cannot get numberOfPages for pdf ", ex.getMessage());
        }
    }
}
