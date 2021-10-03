package io.github.thiagolvlsantos.file.storage;

import java.io.File;
import java.util.List;
import java.util.Map;

import io.github.thiagolvlsantos.file.storage.resource.Resource;

public interface IFileStorage {

	IFileSerializer getSerializer();

	// +------------- ENTITY METHODS ------------------+

	<T> File location(File dir, T example);

	<T> File location(File dir, Class<T> type, T example);

	<T> File location(File dir, Class<T> type, FileParams keys);

	<T> boolean exists(File dir, T example);

	<T> boolean exists(File dir, Class<T> type, T example);

	<T> boolean exists(File dir, Class<T> type, FileParams keys);

	<T> T write(File dir, T instance);

	<T> T write(File dir, Class<T> type, T instance);

	<T> T merge(File dir, Class<T> type, FileParams keys, T instance);

	<T> T read(File dir, T example);

	<T> T read(File dir, Class<T> type, T example);

	<T> T read(File dir, Class<T> type, FileParams keys);

	<T> T delete(File dir, T example);

	<T> T delete(File dir, Class<T> type, T example);

	<T> T delete(File dir, Class<T> type, FileParams keys);

	<T> long count(File dir, Class<T> type, FilePredicate filter, FilePaging paging);

	<T> List<T> list(File dir, Class<T> type, FilePredicate filter, FilePaging paging, FileSorting sorting);

	// +------------- ATTRIBUTE METHODS ------------------+

	<T> T setAttribute(File dir, Class<T> type, FileParams keys, String attribute, Object data);

	<T> Object getAttribute(File dir, Class<T> type, FileParams keys, String attribute);

	<T> Map<String, Object> attributes(File dir, Class<T> type, FileParams keys, FileParams names);

	// +------------- RESOURCE METHODS ------------------+

	<T> File locationResource(File dir, Class<T> type, FileParams keys, String path);

	<T> T setResource(File dir, Class<T> type, FileParams keys, Resource resource);

	<T> Resource getResource(File dir, Class<T> type, FileParams keys, String path);

	<T> T deleteResource(File dir, Class<T> type, FileParams keys, String path);

	<T> long countResources(File dir, Class<T> type, FileParams keys, FilePredicate filter, FilePaging paging);

	<T> List<Resource> listResources(File dir, Class<T> type, FileParams keys, FilePredicate filter, FilePaging paging,
			FileSorting sorting);

}