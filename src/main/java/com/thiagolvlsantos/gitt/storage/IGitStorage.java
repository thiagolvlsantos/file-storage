package com.thiagolvlsantos.gitt.storage;

import java.io.File;

public interface IGitStorage {

	<T> boolean exists(File dir, Class<T> type, T reference);

	<T> boolean exists(File dir, Class<T> type, Object... keys);

	<T> T write(File dir, Class<T> type, T instance);

	<T> T read(File dir, Class<T> type, T reference);

	<T> T read(File dir, Class<T> type, Object... keys);

	<T> T delete(File dir, Class<T> type, T instance);

	<T> T delete(File dir, Class<T> type, Object... keys);
}