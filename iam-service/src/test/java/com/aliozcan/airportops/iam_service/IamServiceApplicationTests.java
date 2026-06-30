package com.aliozcan.airportops.iam_service;

import com.aliozcan.airportops.iam_service.testsupport.TestJwtDecoderConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestJwtDecoderConfig.class)
@SpringBootTest
class IamServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
