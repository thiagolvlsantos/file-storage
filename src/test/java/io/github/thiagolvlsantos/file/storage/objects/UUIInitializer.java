package io.github.thiagolvlsantos.file.storage.objects;

import java.util.UUID;

import io.github.thiagolvlsantos.file.storage.audit.IFileInitializer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UUIInitializer implements IFileInitializer {

	@Override
	public Object value(Object instance, String name, Class<?> type) {
		log.info("INSTANCE: {}", instance);
		log.info("   FIELD: {}", name);
		log.info("    TYPE: {}", type);
		return UUID.randomUUID();
	}
}
