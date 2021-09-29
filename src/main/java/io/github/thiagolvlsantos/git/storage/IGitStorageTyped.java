package io.github.thiagolvlsantos.git.storage;

import java.io.File;
import java.util.List;
import java.util.Map;

import io.github.thiagolvlsantos.git.storage.resource.Resource;

public interface IGitStorageTyped<T> {

	Class<T> type();

	IGitSerializer getSerializer();

	// +------------- ENTITY METHODS ------------------+

	File location(File dir, T example);

	File location(File dir, GitParams keys);

	boolean exists(File dir, T example);

	boolean exists(File dir, GitParams keys);

	T write(File dir, T instance);

	T merge(File dir, GitParams keys, T instance);

	T read(File dir, T example);

	T read(File dir, GitParams keys);

	T delete(File dir, T example);

	T delete(File dir, GitParams keys);

	long count(File dir, GitPaging paging);

	long count(File dir, GitQuery query, GitPaging paging);

	List<T> list(File dir, GitPaging paging);

	List<T> list(File dir, GitQuery query, GitPaging paging);

	// +------------- ATTRIBUTE METHODS ------------------+

	T setAttribute(File dir, GitParams keys, String attribute, Object data);

	Object getAttribute(File dir, GitParams keys, String attribute);

	Map<String, Object> attributes(File dir, GitParams keys, GitParams names);

	// +------------- RESOURCE METHODS ------------------+

	File locationResource(File dir, GitParams keys);

	File locationResource(File dir, GitParams keys, String path);

	T setResource(File dir, GitParams keys, Resource resource);

	Resource getResource(File dir, GitParams keys, String path);

	long countResources(File dir, GitParams keys);

	long countResources(File dir, GitParams keys, GitPaging paging);

	long countResources(File dir, GitParams keys, GitQuery query, GitPaging paging);

	List<Resource> listResources(File dir, GitParams keys);

	List<Resource> listResources(File dir, GitParams keys, GitPaging paging);

	List<Resource> listResources(File dir, GitParams keys, GitQuery query, GitPaging paging);

	T deleteResource(File dir, GitParams keys, String path);
}