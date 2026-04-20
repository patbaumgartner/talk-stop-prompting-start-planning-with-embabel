package com.patbaumgartner.embabel.kyc;

import com.patbaumgartner.embabel.kyc.KycModels.ApiKycRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KycModelsTest {

	@Test
	void apiRequestToDomainMapsAllFields() {
		ApiKycRequest api = new ApiKycRequest("cust-42", "Jane Doe", "1990-01-01", "CH", "Engineer", "Salary");

		var domain = api.toDomain();

		assertEquals("cust-42", domain.customerId());
		assertEquals("Jane Doe", domain.fullName());
		assertEquals("1990-01-01", domain.dateOfBirth());
		assertEquals("CH", domain.nationality());
		assertEquals("Engineer", domain.occupation());
		assertEquals("Salary", domain.sourceOfFunds());
	}

}
