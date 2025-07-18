package it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao;

import it.pagopa.pn.commons.abstractions.KeyValueStore;

import it.pagopa.pn.deliverypushworkflow.middleware.dao.failednotificationdao.dynamo.entity.PaperNotificationFailedEntity;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Set;

public interface PaperNotificationFailedEntityDao extends KeyValueStore<Key, PaperNotificationFailedEntity> {
    Set<PaperNotificationFailedEntity> findByRecipientId(String recipientId);
}
