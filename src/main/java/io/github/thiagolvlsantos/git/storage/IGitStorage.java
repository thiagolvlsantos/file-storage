package io.github.thiagolvlsantos.git.storage;

import java.io.File;
import java.util.List;

public interface IGitStorage {

	<T> boolean exists(File dir, Class<T> type, T reference);

	<T> boolean exists(File dir, Class<T> type, Object... keys);

	<T> T write(File dir, Class<T> type, T instance);

	<T> T read(File dir, Class<T> type, T reference);

	<T> T read(File dir, Class<T> type, Object... keys);

	<T> T delete(File dir, Class<T> type, T reference);

	<T> T delete(File dir, Class<T> type, Object... keys);

	<T> List<T> all(File dir, Class<T> type);

	<T> long count(File dir, Class<T> type);
}