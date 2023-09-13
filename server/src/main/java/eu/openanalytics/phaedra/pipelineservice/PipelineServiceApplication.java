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
import org.springframework.security.web.SecurityFilterChain;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import eu.openanalytics.phaedra.plateservice.client.config.PlateServiceClientAutoConfiguration;
import eu.openanalytics.phaedra.util.PhaedraRestTemplate;
import eu.openanalytics.phaedra.util.auth.AuthenticationConfigHelper;
import eu.openanalytics.phaedra.util.auth.AuthorizationServiceFactory;
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
    PlateServiceClientAutoConfiguration.class
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
		String url = environment.getProperty("DB_URL");
		if (url == null || url.trim().isEmpty()) {
			throw new RuntimeException("No database URL configured: " + environment.getProperty("DB_URL"));
		}
		String driverClassName = JDBCUtils.getDriverClassName(url);
		if (driverClassName == null) {
			throw new RuntimeException("Unsupported database type: " + url);
		}

		HikariConfig config = new HikariConfig();
		config.setAutoCommit(false);
		config.setMaximumPoolSize(20);
		config.setConnectionTimeout(60000);
		config.setJdbcUrl(url);
		config.setDriverClassName(driverClassName);
		config.setUsername(environment.getProperty("DB_USERNAME"));
		config.setPassword(environment.getProperty("DB_PASSWORD"));

		String schema = environment.getProperty("DB_SCHEMA");
		if (schema != null && !schema.trim().isEmpty()) {
			config.setConnectionInitSql("set search_path to " + schema);
		}

		return new HikariDataSource(config);
	}

	@Bean
	public OpenAPI customOpenAPI() {
		Server server = new Server().url((environment.getProperty("API_URL"))).description("Default Server URL");
		return new OpenAPI().addServersItem(server);
	}
	
	@Bean
	public IAuthorizationService authService() {
		return AuthorizationServiceFactory.create();
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
