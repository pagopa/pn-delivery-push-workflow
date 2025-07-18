package it.pagopa.pn.deliverypushworkflow.config;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;
@Configuration
@ConfigurationProperties( prefix = "pn.delivery-push-workflow")
@Data
@Import({SharedAutoConfiguration.class})
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

    @PostConstruct
    public void init() {
        System.out.println(this);
    }
}