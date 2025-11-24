package it.pagopa.pn.deliverypushworkflow.service.mapper;

import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.generated.openapi.msclient.raddalt.model.CheckCoverageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RaddAltMapperTest {

    private RaddAltMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new RaddAltMapper();
    }

    @Test
    void fromRequestIntToRequestExt_shouldMapFieldsCorrectly() {
        PhysicalAddressInt address = PhysicalAddressInt.builder()
                .zip("12345")
                .municipality("Milano")
                .address("Via Roma 1")
                .fullname("fullname")
                .province("NA")
                .municipalityDetails("details")
                .foreignState("ROM")
                .addressDetails("details")
                .build();

        CheckCoverageRequest result = mapper.fromRequestIntToRequestExt(address);

        assertNotNull(result);
        assertEquals("12345", result.getCap());
        assertEquals("Milano", result.getCity());
        assertEquals("Via Roma 1", result.getAddressRow());
        assertEquals("fullname", result.getNameRow2());
        assertEquals("NA", result.getPr());
        assertEquals("ROM", result.getCountry());
        assertEquals("12345",result.getCap());
        assertEquals("details", result.getAddressRow2());
    }

    @Test
    void fromRequestIntToRequestExt_shouldReturnNullIfInputIsNull() {
        assertNull(mapper.fromRequestIntToRequestExt(null));
    }
}
