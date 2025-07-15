package it.pagopa.pn.deliverypushworkflow.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConfigurationProperties( prefix = "pn.delivery-push-workflow")
@Data
@Import({SharedAutoConfiguration.class})
public class PnDeliveryPushWorkflowConfigs {
}
