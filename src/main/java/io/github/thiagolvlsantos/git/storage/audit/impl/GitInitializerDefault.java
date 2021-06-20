package io.github.thiagolvlsantos.git.storage.audit.impl;

import java.time.temporal.Temporal;

import io.github.thiagolvlsantos.git.storage.audit.IGitInitializer;
import io.github.thiagolvlsantos.git.storage.exceptions.GitStorageException;

public class GitInitializerDefault implements IGitInitializer {

	@Override
	public Object value(Class<?> type) {
		Object current = null;
		try {
			if (Temporal.class.isAssignableFrom(type)) {
				current = type.getMethod("now").invoke(null);
			} else {
				current = System.currentTimeMillis();
			}
		} catch (Exception e) {
			throw new GitStorageException(e.getMessage(), e);
		}
		return current;
	}
}
