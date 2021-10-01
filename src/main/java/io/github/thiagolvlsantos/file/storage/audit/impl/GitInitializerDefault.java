package io.github.thiagolvlsantos.file.storage.audit.impl;

import java.time.temporal.Temporal;

import io.github.thiagolvlsantos.file.storage.audit.IFileInitializer;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageException;

public class GitInitializerDefault implements IFileInitializer {

	@Override
	public Object value(Object instance, String name, Class<?> type) {
		Object current = null;
		try {
			if (Temporal.class.isAssignableFrom(type)) {
				current = type.getMethod("now").invoke(null);
			} else {
				current = System.currentTimeMillis();
			}
		} catch (Exception e) {
			throw new FileStorageException(e.getMessage(), e);
		}
		return current;
	}
}
