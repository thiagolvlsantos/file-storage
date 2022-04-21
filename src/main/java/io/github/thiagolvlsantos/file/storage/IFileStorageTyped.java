package io.github.thiagolvlsantos.file.storage;

import java.io.File;
import java.util.List;
import java.util.Map;

import io.github.thiagolvlsantos.file.storage.resource.Resource;

public interface IFileStorageTyped<T> {

	Class<T> type();

	IFileSerializer getSerializer();

	void setSerializer(IFileSerializer serializer);

	// +------------- ENTITY METHODS ------------------+

	File location(File dir, T example);

	File location(File dir, KeyParams keys);

	boolean exists(File dir, T example);

	boolean exists(File dir, KeyParams keys);

	T write(File dir, T instance);

	T read(File dir, T example);

	T read(File dir, KeyParams keys);

	T delete(File dir, T example);

	T delete(File dir, KeyParams keys);

	long count(File dir, SearchParams search);

	List<T> list(File dir, SearchParams search);

	// +------------- PROPERTY METHODS ------------------+

	T setProperty(File dir, KeyParams keys, String property, Object data);

	List<T> setProperty(File dir, String property, Object data, SearchParams search);

	Object getProperty(File dir, KeyParams keys, String property);

	Map<String, Object> properties(File dir, KeyParams keys, KeyParams names);

	Map<String, Map<String, Object>> properties(File dir, KeyParams names, SearchParams search);

	// +------------- RESOURCE METHODS ------------------+

	File locationResource(File dir, KeyParams keys, String path);

	boolean existsResource(File dir, KeyParams keys, String path);

	T setResource(File dir, KeyParams keys, Resource resource);

	Resource getResource(File dir, KeyParams keys, String path);

	T deleteResource(File dir, KeyParams keys, String path);

	long countResources(File dir, KeyParams keys, SearchParams search);

	List<Resource> listResources(File dir, KeyParams keys, SearchParams search);
}