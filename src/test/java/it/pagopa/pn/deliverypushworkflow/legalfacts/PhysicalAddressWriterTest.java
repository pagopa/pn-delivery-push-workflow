package it.pagopa.pn.deliverypushworkflow.legalfacts;

import it.pagopa.pn.deliverypushworkflow.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypushworkflow.dto.ext.delivery.notification.NotificationRecipientInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;


class PhysicalAddressWriterTest {

	private PhysicalAddressWriter physicalAddressWriter;

	@BeforeEach
    void setup() {
		physicalAddressWriter = new PhysicalAddressWriter();
	}


	@Test
	void successNullSafePhysicalAddressToString() {
		// GIVEN
		NotificationInt notification = NotificationInt.builder()
				.recipients( Collections.singletonList(
						NotificationRecipientInt.builder()
								.denomination( "denomination" )
								.physicalAddress(PhysicalAddressInt.builder()
										.address( "address" )
										.municipality( "municipality" )
										.addressDetails( "addressDetail" )
										.at( "at" )
										.province( "province" )
										.zip( "zip" )
										.foreignState("foreign")
										.build()
								).build()
						)
				).build();


		// WHEN
		NotificationRecipientInt recipient = notification.getRecipients().getFirst();
		String output = physicalAddressWriter.nullSafePhysicalAddressToString( recipient.getPhysicalAddress(), recipient.getDenomination(), ";" );

		// THEN
		Assertions.assertEquals("denomination;at;addressDetail;address;zip municipality province;foreign", output, "Different notification data");
	}

	@Test
	void successNullSafePhysicalAddressToString_nullprovince() {
		// GIVEN
		NotificationInt notification = NotificationInt.builder()
				.recipients( Collections.singletonList(
								NotificationRecipientInt.builder()
										.denomination( "denomination" )
										.physicalAddress(PhysicalAddressInt.builder()
												.address( "address" )
												.municipality( "municipality" )
												.addressDetails( "addressDetail" )
												.at( "at" )
												.zip( "zip" )
												.build()
										).build()
						)
				).build();


		// WHEN
		NotificationRecipientInt recipient = notification.getRecipients().getFirst();
		String output = physicalAddressWriter.nullSafePhysicalAddressToString( recipient.getPhysicalAddress(), recipient.getDenomination(), ";" );

		// THEN
		Assertions.assertEquals("denomination;at;addressDetail;address;zip municipality", output, "Different notification data");
	}


    @Test
    void successNullSafePhysicalAddressToString_nullat() {
        // GIVEN
        NotificationInt notification = NotificationInt.builder()
                .recipients( Collections.singletonList(
                                NotificationRecipientInt.builder()
                                        .denomination( "denomination" )
                                        .physicalAddress(PhysicalAddressInt.builder()
                                                .address( "address" )
                                                .municipality( "municipality" )
                                                .municipalityDetails("mundetails")
                                                .addressDetails( "addressDetail" )
                                                .zip( "zip" )
                                                .at(null)
                                                .build()
                                        ).build()
                        )
                ).build();


        // WHEN
        NotificationRecipientInt recipient = notification.getRecipients().getFirst();
        String output = physicalAddressWriter.nullSafePhysicalAddressToString( recipient.getPhysicalAddress(), recipient.getDenomination(), ";" );

        // THEN
        Assertions.assertEquals("denomination;addressDetail;address;zip municipality mundetails", output, "Different notification data");
    }


    @Test
    void successNullSafePhysicalAddressToString_nullforeing() {
        NotificationInt notification = NotificationInt.builder()
                .recipients( Collections.singletonList(
                                NotificationRecipientInt.builder()
                                        .denomination( "denomination" )
                                        .physicalAddress(PhysicalAddressInt.builder()
                                                .address( "address" )
                                                .municipality( "municipality" )
                                                .municipalityDetails("mundetails")
                                                .addressDetails( "addressDetail" )
                                                .zip( "zip" )
                                                .at("at")
                                                .foreignState(null)
                                                .build()
                                        ).build()
                        )
                ).build();


        // WHEN
        NotificationRecipientInt recipient = notification.getRecipients().getFirst();
        String output = physicalAddressWriter.nullSafePhysicalAddressToString( recipient.getPhysicalAddress(), recipient.getDenomination(), ";" );

        // THEN
        Assertions.assertEquals("denomination;at;addressDetail;address;zip municipality mundetails", output, "Different notification data");
        Assertions.assertNull(recipient.getPhysicalAddress().getForeignState());
    }
}