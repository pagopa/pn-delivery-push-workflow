package it.pagopa.pn.deliverypushworkflow;

import it.pagopa.pn.commons.configs.listeners.TaskIdApplicationListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PnDeliveryPushWorkflowApplication {
    public static void main(String[] args) {
        buildSpringApplicationWithListener().run(args);
    }
    static SpringApplication buildSpringApplicationWithListener() {
        SpringApplication app = new SpringApplication(PnDeliveryPushWorkflowApplication.class);
        app.addListeners(new TaskIdApplicationListener());
        return app;
    }
}