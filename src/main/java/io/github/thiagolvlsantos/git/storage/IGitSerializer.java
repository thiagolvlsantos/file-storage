package io.github.thiagolvlsantos.git.storage;

import java.io.File;

public interface IGitSerializer {

	<T> T fromString(String data, Class<T> type);

	<T> String asString(T instance);

	<T> T readValue(File file, Class<T> type);

	<T> void writeValue(File file, T instance);

}
