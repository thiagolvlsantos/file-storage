package io.github.thiagolvlsantos.file.storage.audit;

public interface IFileInitializer {

	Object value(Object instance, String field, Class<?> type);
}
