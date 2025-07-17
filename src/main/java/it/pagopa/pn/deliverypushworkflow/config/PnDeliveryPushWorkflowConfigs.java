package it.pagopa.pn.deliverypushworkflow.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConfigurationProperties( prefix = "pn.delivery-push-workflow")
@Data
@Import({SharedAutoConfiguration.class})
public class PnDeliveryPushWorkflowConfigs {
    private Topics topics;

    @Data
    public static class Topics {
        private String newNotifications;
        private String fromExternalChannel;
        private String scheduledActions;
        private String nationalRegistriesEvents;
    }

    @PostConstruct
    public void init() {
        System.out.println(this);
    }
}