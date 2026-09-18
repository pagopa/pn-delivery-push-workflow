package it.pagopa.pn.deliverypushworkflow.config;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import it.pagopa.pn.deliverypushworkflow.dto.raddalt.RaddSearchModeInt;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.middleware.queue.producer.abstractions.actionspool.impl.TimeParams;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Configuration
@ConfigurationProperties( prefix = "pn.delivery-push-workflow")
@Data
@Import({SharedAutoConfiguration.class})
@Slf4j
public class PnDeliveryPushWorkflowConfigs {
    private Topics topics;
  
    private ErrorCorrectionLevel errorCorrectionLevelQrCode;

    private boolean additionalLangsEnabled;

    private boolean checkAttachmentsForViewedEnabled;

    private String templatesEngineBaseUrl;

    private String deliveryBaseUrl;

    private String paperChannelBaseUrl;

    private String externalChannelBaseUrl;

    private String dataVaultBaseUrl;

    private String safeStorageBaseUrl;

    private String raddAltBaseUrl;

    private String userAttributesBaseUrl;

    private String externalRegistryBaseUrl;

    private String notificationCostServiceBaseUrl;

    private String nationalRegistriesBaseUrl;

    private String actionManagerBaseUrl;

    private String emdIntegrationBaseUrl;

    private String timelineClientBaseUrl;

    private String deliveryPushBaseUrl;

    private String templateURLforPEC;

    private String externalchannelCxId;

    private String externalchannelSenderPec;

    private String externalchannelSenderEmail;

    private String externalchannelSenderSms;

    private Webapp webapp;

    private List<String> pnSendMode;

    private List<String> raddExperimentationStoresName;

    private String safeStorageCxId;

    private String safeStorageCxIdUpdatemetadata;

    private DocumentCreationRequestDao documentCreationRequestDao;

    private FailedNotificationDao failedNotificationDao;

    private String pfNewWorkflowStart;

    private String pfNewWorkflowStop;

    private String aarOnlyPecForRaddAndPf;

    private ExternalChannel externalChannel;

    private TimeParams timeParams;

    private Integer retentionAttachmentDaysAfterRefinement;

    private Instant featureUnreachableRefinementPostAARStartDate;

    private String activationDeceasedWorkflowDate;

    private int pagoPaNotificationBaseCost;

    private PaperChannel paperChannel;

    private int retentionAttachmentDaysAfterDeliveryTimeout;

    private String paperTrackerBaseUrl;

    private Instant startAnalogWorkflowTimeoutFeatureDate;

    private RaddSearchModeInt raddSearchMode;

    private int reworkTTLAddressRange;

    private int notificationReworkDocumentExpiringRange;

    private List<String> invalidableCategories;

    private CourtesyRetry courtesyRetry;

    @Data
    public static class Topics {
        private String newNotifications;
        private String fromExternalChannel;
        private String scheduledActions;
        private String nationalRegistriesEvents;
        private String notificationReworkUpdaterEvent;
        private String analogResponseEvents;
    }

    @Data
    public static class Webapp {
        private String aarSenderLogoUrlTemplate;
    }

    @Data
    public static class DocumentCreationRequestDao {
        private String tableName;
    }

    @Data
    public static class FailedNotificationDao {
        private String tableName;
    }

    @Data
    public static class ExternalChannel {

        private List<String> digitalCodesProgress;
        private List<String> digitalCodesSuccess;
        private List<String> digitalCodesFail;
        private List<String> digitalCodesRetryable;

        private List<String> digitalCodesFatallog;

        private int digitalRetryCount;
        private Duration digitalRetryDelay;
        private Duration digitalSendNoresponseTimeout;

    }

    @Data
    public static class SenderAddress {
        private String fullname;
        private String address;
        private String zipcode;
        private String city;
        private String pr;
        private String country;
    }

    @Data
    public static class PaperChannel {

        private SenderAddress senderAddress;

        public PhysicalAddressInt getSenderPhysicalAddress(){
            return PhysicalAddressInt.builder()
                    .fullname(senderAddress.getFullname())
                    .address(senderAddress.getAddress())
                    .zip(senderAddress.getZipcode())
                    .province(senderAddress.getPr())
                    .municipality(senderAddress.getCity())
                    .foreignState(senderAddress.getCountry())
                    .build();
        }
    }

    @Data
    public static class CourtesyRetry {
        private IntervalsMinutes intervalsMinutes;
        private Duration plannedAddressTtl;

        @Data
        public static class IntervalsMinutes {
            private List<Integer> io;
            private List<Integer> sms;
            private List<Integer> email;
            private List<Integer> tpp;
        }
    }

    @PostConstruct
    public void init() {
        log.info("PnDeliveryPushWorkflowConfigs={}", this);
        validatePlannedAddressTtl();
    }

    private void validatePlannedAddressTtl() {
        if (courtesyRetry == null || courtesyRetry.getPlannedAddressTtl() == null || courtesyRetry.getPlannedAddressTtl().isNegative() || courtesyRetry.getPlannedAddressTtl().isZero()) {
            throw new IllegalStateException("Property pn.delivery-push-workflow.courtesy-retry.planned-address-ttl must be set to a positive duration");
        }

        Duration longestRetryWindow = longestCourtesyRetryWindow();
        if (courtesyRetry.getPlannedAddressTtl().compareTo(longestRetryWindow) <= 0) {
            throw new IllegalStateException("Property pn.delivery-push-workflow.courtesy-retry.planned-address-ttl (" + courtesyRetry.getPlannedAddressTtl()
                    + ") must be greater than the longest courtesy retry window (" + longestRetryWindow + ")");
        }
    }

    private Duration longestCourtesyRetryWindow() {
        CourtesyRetry.IntervalsMinutes intervalsMinutes = courtesyRetry.getIntervalsMinutes();
        if (intervalsMinutes == null) {
            return Duration.ZERO;
        }
        return Stream.of(intervalsMinutes.getIo(), intervalsMinutes.getSms(), intervalsMinutes.getEmail(), intervalsMinutes.getTpp())
                .filter(Objects::nonNull)
                .map(intervals -> Duration.ofMinutes(intervals.stream().mapToLong(Integer::longValue).sum()))
                .max(Duration::compareTo)
                .orElse(Duration.ZERO);
    }
}