package it.pagopa.pn.deliverypushworkflow.utils;

import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.service.CheckCoverageAreaService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CheckRADDExperimentation {
    private static final String[] EXPERIMENTAL_COUNTRIES = {"it", "italia", "italy"};
    private final CheckCoverageAreaService checkCoverageAreaService;

    public CheckRADDExperimentation(CheckCoverageAreaService checkCoverageAreaService) {
        this.checkCoverageAreaService = checkCoverageAreaService;
    }

    private boolean isAnExperimentalCountry(final String countryToCheck) {
        if (StringUtils.isBlank(countryToCheck)) return true;

        for (String currentCountry : CheckRADDExperimentation.EXPERIMENTAL_COUNTRIES) {
            if (StringUtils.equalsIgnoreCase(currentCountry, countryToCheck)) return true;
        }
        return false;
    }

    public boolean checkAddress(PhysicalAddressInt toCheck, NotificationInt notificationInt) {

        if (isAnExperimentalCountry(toCheck.getForeignState())) {
            return checkCoverageAreaService.isAreaCovered(toCheck, notificationInt);
        } else {
            log.trace("Country {} not in admitted countries", toCheck.getForeignState());
        }
        return false;
    }

}
