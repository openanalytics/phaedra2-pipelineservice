package eu.openanalytics.phaedra.pipelineservice;

import javax.sql.DataSource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

import eu.openanalytics.phaedra.measurementservice.client.config.MeasurementServiceClientAutoConfiguration;
import eu.openanalytics.phaedra.metadataservice.client.config.MetadataServiceClientAutoConfiguration;
import eu.openanalytics.phaedra.plateservice.client.config.PlateServiceClientAutoConfiguration;
import eu.openanalytics.phaedra.util.PhaedraRestTemplate;
import eu.openanalytics.phaedra.util.auth.AuthenticationConfigHelper;
import eu.openanalytics.phaedra.util.auth.AuthorizationServiceFactory;
import eu.openanalytics.phaedra.util.auth.ClientCredentialsTokenGenerator;
import eu.openanalytics.phaedra.util.auth.IAuthorizationService;
import eu.openanalytics.phaedra.util.jdbc.JDBCUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableKafka
@EnableWebSecurity
@Import({
    PlateServiceClientAutoConfiguration.class,
    MetadataServiceClientAutoConfiguration.class,
    MeasurementServiceClientAutoConfiguration.class
})
public class PipelineServiceApplication {

	private final Environment environment;

	public PipelineServiceApplication(Environment environment) {
		this.environment = environment;
	}

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(PipelineServiceApplication.class);
		app.run(args);
	}

	@Bean
	public DataSource dataSource() {
		return JDBCUtils.createDataSource(environment);
	}

	@Bean
	public OpenAPI customOpenAPI() {
		Server server = new Server().url((environment.getProperty("API_URL"))).description("Default Server URL");
		return new OpenAPI().addServersItem(server);
	}
	
    @Bean
    public ClientCredentialsTokenGenerator ccTokenGenerator(ClientRegistrationRepository clientRegistrationRepository) {
    	return new ClientCredentialsTokenGenerator("keycloak", clientRegistrationRepository);
    }

	@Bean
	public IAuthorizationService authService(ClientCredentialsTokenGenerator ccTokenGenerator) {
		return AuthorizationServiceFactory.create(ccTokenGenerator);
	}

	@Bean
	public SecurityFilterChain httpSecurity(HttpSecurity http) throws Exception {
		return AuthenticationConfigHelper.configure(http);
	}

    @Bean
    public PhaedraRestTemplate restTemplate() {
        PhaedraRestTemplate restTemplate = new PhaedraRestTemplate();
        return restTemplate;
    }
}
