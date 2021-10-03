package io.github.thiagolvlsantos.file.storage;

import java.io.File;
import java.util.List;
import java.util.Map;

import io.github.thiagolvlsantos.file.storage.resource.Resource;

public interface IFileStorageTyped<T> {

	Class<T> type();

	IFileSerializer getSerializer();

	// +------------- ENTITY METHODS ------------------+

	File location(File dir, T example);

	File location(File dir, FileParams keys);

	boolean exists(File dir, T example);

	boolean exists(File dir, FileParams keys);

	T write(File dir, T instance);

	T merge(File dir, FileParams keys, T instance);

	T read(File dir, T example);

	T read(File dir, FileParams keys);

	T delete(File dir, T example);

	T delete(File dir, FileParams keys);

	long count(File dir, FilePredicate filter, FilePaging paging);

	List<T> list(File dir, FilePredicate filter, FilePaging paging, FileSorting sorting);

	// +------------- ATTRIBUTE METHODS ------------------+

	T setAttribute(File dir, FileParams keys, String attribute, Object data);

	Object getAttribute(File dir, FileParams keys, String attribute);

	Map<String, Object> attributes(File dir, FileParams keys, FileParams names);

	// +------------- RESOURCE METHODS ------------------+

	File locationResource(File dir, FileParams keys, String path);

	T setResource(File dir, FileParams keys, Resource resource);

	Resource getResource(File dir, FileParams keys, String path);

	T deleteResource(File dir, FileParams keys, String path);

	long countResources(File dir, FileParams keys, FilePredicate filter, FilePaging paging);

	List<Resource> listResources(File dir, FileParams keys, FilePredicate filter, FilePaging paging,
			FileSorting sorting);
}