package io.github.thiagolvlsantos.git.storage.objects;

import java.util.UUID;

import io.github.thiagolvlsantos.git.storage.audit.IGitInitializer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UUIInitializer implements IGitInitializer {

	@Override
	public Object value(Object instance, String name, Class<?> type) {
		log.info("INSTANCE: {}", instance);
		log.info("   FIELD: {}", name);
		log.info("    TYPE: {}", type);
		return UUID.randomUUID();
	}
}
