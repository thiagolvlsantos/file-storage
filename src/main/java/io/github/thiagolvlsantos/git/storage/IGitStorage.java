package io.github.thiagolvlsantos.git.storage;

import java.io.File;
import java.util.List;

public interface IGitStorage {

	IGitSerializer getSerializer();

	<T> boolean exists(File dir, T example);

	<T> boolean exists(File dir, Class<T> type, T example);

	<T> boolean exists(File dir, Class<T> type, Object... keys);

	<T> T write(File dir, T instance);

	<T> T write(File dir, Class<T> type, T instance);

	<T> T update(File dir, Class<T> type, T instance, Object... keys);

	<T> T updateAttribute(File dir, Class<T> type, String attribute, String data, Object... keys);

	<T> T read(File dir, T example);

	<T> T read(File dir, Class<T> type, T example);

	<T> T read(File dir, Class<T> type, Object... keys);

	<T> Object readAttribute(File dir, Class<T> type, String attribute, Object... keys);

	<T> T delete(File dir, T example);

	<T> T delete(File dir, Class<T> type, T example);

	<T> T delete(File dir, Class<T> type, Object... keys);

	<T> List<T> all(File dir, Class<T> type);

	<T> long count(File dir, Class<T> type);

	<T> List<T> search(File dir, Class<T> type, String query);

}