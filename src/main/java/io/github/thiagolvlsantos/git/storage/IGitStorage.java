package io.github.thiagolvlsantos.git.storage;

import java.io.File;
import java.util.List;

import io.github.thiagolvlsantos.git.storage.resource.Resource;

public interface IGitStorage {

	IGitSerializer getSerializer();

	<T> boolean exists(File dir, T example);

	<T> boolean exists(File dir, Class<T> type, T example);

	<T> boolean exists(File dir, Class<T> type, Object... keys);

	<T> T write(File dir, T instance);

	<T> T write(File dir, Class<T> type, T instance);

	<T> T merge(File dir, Class<T> type, T instance, Object... keys);

	<T> T setAttribute(File dir, Class<T> type, String attribute, Object data, Object... keys);

	<T> T setResource(File dir, Class<T> type, Resource resource, Object... keys);

	<T> T read(File dir, T example);

	<T> T read(File dir, Class<T> type, T example);

	<T> T read(File dir, Class<T> type, Object... keys);

	<T> Object getAttribute(File dir, Class<T> type, String attribute, Object... keys);

	<T> Resource getResource(File dir, Class<T> type, String path, Object... keys);

	<T> List<Resource> allResources(File dir, Class<T> type, Object... keys);

	<T> T delete(File dir, T example);

	<T> T delete(File dir, Class<T> type, T example);

	<T> T delete(File dir, Class<T> type, Object... keys);

	<T> T delResource(File dir, Class<T> type, String path, Object... keys);

	<T> List<T> all(File dir, Class<T> type);

	<T> long count(File dir, Class<T> type);

	<T> List<T> search(File dir, Class<T> type, String query);

}