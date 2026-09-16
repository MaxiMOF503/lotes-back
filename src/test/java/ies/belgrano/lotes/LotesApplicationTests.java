package ies.belgrano.lotes;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LotesApplicationTests {

	@Test
	void applicationDeclaresSpringBootEntryPoint() {
		assertTrue(LotesApplication.class.isAnnotationPresent(SpringBootApplication.class));
	}

}
