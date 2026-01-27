package com.radioacademy.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:testdb",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.flyway.enabled=false",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"security.jwt.secret-key=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
		"resend.api.key=re_123456789",
		"stripe.api.key=sk_test_123456789",
		"app.admin.email=admin@test.com",
		"app.admin.password=admin"
})
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
