package it.pagopa.pn.deliverypushworkflow.service.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.CheckCoverageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RaddAltMapperTest {

    private RaddAltMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new RaddAltMapper(new ObjectMapper());
    }

    @Test
    void fromRequestIntToRequestExt_shouldMapFieldsCorrectly() {
        PhysicalAddressInt address = PhysicalAddressInt.builder()
                .zip("12345")
                .municipality("Milano")
                .build();

        CheckCoverageRequest result = mapper.fromRequestIntToRequestExt(address);

        assertNotNull(result);
        assertEquals("12345", result.getCap());
        assertEquals("Milano", result.getCity());
        assertNull(result.getPr());
        assertNull(result.getCountry());
    }

    @Test
    void fromRequestIntToRequestExt_shouldReturnNullIfInputIsNull() {
        assertNull(mapper.fromRequestIntToRequestExt(null));
    }
}
