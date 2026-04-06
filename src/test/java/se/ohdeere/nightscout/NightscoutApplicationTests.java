package se.ohdeere.nightscout;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfig.class)
class NightscoutApplicationTests {

	@Test
	void contextLoads() {
	}

}
