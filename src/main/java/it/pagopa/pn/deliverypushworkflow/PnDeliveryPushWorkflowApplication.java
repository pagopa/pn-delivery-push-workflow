package it.pagopa.pn.deliverypushworkflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class PnDeliveryPushWorkflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(PnDeliveryPushWorkflowApplication.class, args);
    }
}