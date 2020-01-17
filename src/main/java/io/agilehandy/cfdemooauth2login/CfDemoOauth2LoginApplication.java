package io.agilehandy.cfdemooauth2login;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

@SpringBootApplication
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class CfDemoOauth2LoginApplication {

	public static void main(String[] args) {
		SpringApplication.run(CfDemoOauth2LoginApplication.class, args);
	}

}
