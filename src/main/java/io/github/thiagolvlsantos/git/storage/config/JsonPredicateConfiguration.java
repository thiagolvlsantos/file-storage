package io.github.thiagolvlsantos.git.storage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.thiagolvlsantos.json.predicate.IPredicateFactory;
import io.github.thiagolvlsantos.json.predicate.impl.PredicateFactoryJson;

@Configuration
public class JsonPredicateConfiguration {

	@Bean
	public IPredicateFactory factory() {
		return new PredicateFactoryJson();
	}
}
