package com.kkalchake.enlightenment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "jwt.secret=test-secret-key-must-be-32-chars-long")
class EnlightenmentApplicationTests {

	@Test
	void contextLoads() {
	}

}
