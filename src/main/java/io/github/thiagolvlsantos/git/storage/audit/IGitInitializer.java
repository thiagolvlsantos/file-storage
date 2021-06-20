package io.github.thiagolvlsantos.git.storage.audit;

public interface IGitInitializer {

	Object value(Object instance, String field, Class<?> type);
}
