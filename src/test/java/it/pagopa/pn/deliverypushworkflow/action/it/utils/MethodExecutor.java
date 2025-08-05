package it.pagopa.pn.deliverypushworkflow.action.it.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
public class MethodExecutor {
    static int millisToWaitEachExecution = 300;
    static int maxMillisToWait = 20000;

    public static void waitForExecution(Supplier<Optional<?>> methodSupplier) {
        log.debug("[TEST] start waitForExecution");
        int waitedTime = 0;
        boolean conditionRespected = false;
        while (waitedTime < maxMillisToWait && ! conditionRespected) {
            try {
                Thread.sleep(millisToWaitEachExecution);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.debug("[TEST] start check in waitForExecution");
            waitedTime = waitedTime + millisToWaitEachExecution;
            Optional<?> result = Optional.empty();
            try {
                result = methodSupplier.get();
            } catch (Exception e) {
                log.info("[TEST] Error in waitForExecution", e);
            }
            if (result.isEmpty()) {
                log.info("[TEST] Result is not present");
            } else {
                log.info("[TEST] Result is present: {}", result.get());
                conditionRespected = true;
            }
            log.debug("[TEST] end check in waitForExecution with condition: {}", conditionRespected);
        }
        if(! conditionRespected){
            log.error("[TEST] Time has expired and the condition has not occurred : {}",methodSupplier);
            throw new RuntimeException("[TEST] Time has expired and the condition has not occurred");
        }
    }
}
