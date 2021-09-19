package io.github.thiagolvlsantos.git.storage;

import java.io.File;
import java.util.List;

public interface IGitStorageTyped<T> {

	Class<T> type();

	boolean exists(File dir, T example);

	boolean exists(File dir, Object... keys);

	T write(File dir, T instance);

	T update(File dir, T instance, Object... keys);

	T updateAttribute(File dir, String attribute, String data, Object... keys);

	T read(File dir, T example);

	T read(File dir, Object... keys);

	T delete(File dir, T example);

	T delete(File dir, Object... keys);

	List<T> all(File dir);

	long count(File dir);

	List<T> search(File dir, String query);
}