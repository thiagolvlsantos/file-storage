package io.github.thiagolvlsantos.git.storage.objects;

import java.util.UUID;

import io.github.thiagolvlsantos.git.storage.audit.IGitInitializer;

public class UUIInitializer implements IGitInitializer {

	@Override
	public Object value(Class<?> type) {
		return UUID.randomUUID();
	}
}
