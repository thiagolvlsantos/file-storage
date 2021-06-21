package io.github.thiagolvlsantos.git.storage;

import java.io.File;

public interface IGitSerializer {

	<T> T readValue(File file, Class<T> type);

	<T> void writeValue(File file, T instance);
}
