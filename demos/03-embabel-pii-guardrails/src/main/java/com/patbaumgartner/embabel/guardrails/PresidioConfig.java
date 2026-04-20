package com.patbaumgartner.embabel.guardrails;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Creates the Presidio HTTP client bean. Uses Spring's declarative HTTP interface support
 * (Spring Framework 6+).
 */
@Configuration
@EnableConfigurationProperties(PresidioProperties.class)
public class PresidioConfig {

	@Bean
	public PresidioAnalyzerClient presidioAnalyzerClient(PresidioProperties properties,
			RestClient.Builder restClientBuilder) {
		RestClient restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
		RestClientAdapter adapter = RestClientAdapter.create(restClient);
		HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
		return factory.createClient(PresidioAnalyzerClient.class);
	}

}
