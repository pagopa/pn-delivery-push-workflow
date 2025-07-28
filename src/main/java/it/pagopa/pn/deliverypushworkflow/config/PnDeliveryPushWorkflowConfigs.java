package it.pagopa.pn.deliverypushworkflow.config;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
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

@Configuration
@ConfigurationProperties( prefix = "pn.delivery-push-workflow")
@Data
@Import({SharedAutoConfiguration.class})
@Slf4j
public class PnDeliveryPushWorkflowConfigs {
    private Topics topics;
  
    private ErrorCorrectionLevel errorCorrectionLevelQrCode;

    private boolean additionalLangsEnabled;

    private String templatesEngineBaseUrl;

    private String deliveryBaseUrl;

    private String paperChannelBaseUrl;

    private String externalChannelBaseUrl;

    private String dataVaultBaseUrl;

    private String safeStorageBaseUrl;

    private String userAttributesBaseUrl;

    private String externalRegistryBaseUrl;

    private String nationalRegistriesBaseUrl;

    private String actionManagerBaseUrl;

    private String emdIntegrationBaseUrl;

    private String timelineClientBaseUrl;

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

    private String AAROnlyPECForRADDAndPF;

    private ExternalChannel externalChannel;

    private TimeParams timeParams;

    private Integer retentionAttachmentDaysAfterRefinement;

    private Instant featureUnreachableRefinementPostAARStartDate;

    private String activationDeceasedWorkflowDate;

    private int pagoPaNotificationBaseCost;

    private int pagoPaNotificationFee;

    private int pagoPaNotificationVat;

    private PaperChannel paperChannel;


    @Data
    public static class Topics {
        private String newNotifications;
        private String fromExternalChannel;
        private String scheduledActions;
        private String nationalRegistriesEvents;
    }

    @Data
    public static class Webapp {
        private String directAccessUrlTemplatePhysical;
        private String directAccessUrlTemplateLegal;
        private String faqUrlTemplateSuffix;
        private String faqSendHash;
        private String quickAccessUrlAarDetailSuffix;
        private String landingUrl;
        private String raddPhoneNumber;
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

    @PostConstruct
    public void init() {
        log.info("PnDeliveryPushWorkflowConfigs={}", this);
    }
}