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

	File location(File dir, FileParams keys);

	boolean exists(File dir, T example);

	boolean exists(File dir, FileParams keys);

	T write(File dir, T instance);

	T read(File dir, T example);

	T read(File dir, FileParams keys);

	T delete(File dir, T example);

	T delete(File dir, FileParams keys);

	long count(File dir, FilePredicate filter, FilePaging paging);

	List<T> list(File dir, FilePredicate filter, FilePaging paging, FileSorting sorting);

	// +------------- PROPERTY METHODS ------------------+

	T setProperty(File dir, FileParams keys, String property, Object data);

	List<T> setProperty(File dir, String property, Object data, FilePredicate filter, FilePaging paging,
			FileSorting sorting);

	Object getProperty(File dir, FileParams keys, String property);

	Map<String, Object> properties(File dir, FileParams keys, FileParams names);

	Map<String, Map<String, Object>> properties(File dir, FileParams names, FilePredicate filter, FilePaging paging,
			FileSorting sorting);

	// +------------- RESOURCE METHODS ------------------+

	File locationResource(File dir, FileParams keys, String path);

	boolean existsResource(File dir, FileParams keys, String path);

	T setResource(File dir, FileParams keys, Resource resource);

	Resource getResource(File dir, FileParams keys, String path);

	T deleteResource(File dir, FileParams keys, String path);

	long countResources(File dir, FileParams keys, FilePredicate filter, FilePaging paging);

	List<Resource> listResources(File dir, FileParams keys, FilePredicate filter, FilePaging paging,
			FileSorting sorting);
}