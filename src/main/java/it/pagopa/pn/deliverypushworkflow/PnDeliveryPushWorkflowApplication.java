package it.pagopa.pn.deliverypushworkflow;

import it.pagopa.pn.commons.configs.listeners.TaskIdApplicationListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @RestController
    public static class HomeController {

        @GetMapping("")
        public String home() {
            return "Sono Vivo";
        }
    }
}