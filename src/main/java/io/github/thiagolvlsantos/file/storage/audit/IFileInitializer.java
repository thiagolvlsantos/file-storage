package io.github.thiagolvlsantos.file.storage.audit;

import io.github.thiagolvlsantos.file.storage.audit.impl.FileInitializerDefault;

public interface IFileInitializer {

	Object value(Object instance, String field, Class<?> type);

	IFileInitializer INSTANCE = new FileInitializerDefault();
}
