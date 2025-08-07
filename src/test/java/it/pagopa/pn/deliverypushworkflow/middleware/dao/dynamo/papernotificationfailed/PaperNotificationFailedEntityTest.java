package it.pagopa.pn.deliverypushworkflow.middleware.dao.dynamo.papernotificationfailed;

import it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao.dynamo.entity.PaperNotificationFailedEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaperNotificationFailedEntityTest {

    @Test
    void testGetterSetter() {
        PaperNotificationFailedEntity entity = new PaperNotificationFailedEntity();
        entity.setRecipientId("rec123");
        entity.setIun("iun456");

        assertEquals("rec123", entity.getRecipientId());
        assertEquals("iun456", entity.getIun());
    }

    @Test
    void testBuilder() {
        PaperNotificationFailedEntity entity = PaperNotificationFailedEntity.builder()
                .recipientId("rec789")
                .iun("iun012")
                .build();

        assertEquals("rec789", entity.getRecipientId());
        assertEquals("iun012", entity.getIun());
    }

    @Test
    void testEqualsAndHashCode() {
        PaperNotificationFailedEntity entity1 = PaperNotificationFailedEntity.builder()
                .recipientId("recA")
                .iun("iunB")
                .build();

        PaperNotificationFailedEntity entity2 = PaperNotificationFailedEntity.builder()
                .recipientId("recA")
                .iun("iunB")
                .build();

        assertEquals(entity1, entity2);
        assertEquals(entity1.hashCode(), entity2.hashCode());
    }
}


