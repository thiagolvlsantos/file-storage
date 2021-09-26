package io.github.thiagolvlsantos.git.storage;

import java.io.File;
import java.util.List;

import io.github.thiagolvlsantos.git.storage.resource.Resource;

public interface IGitStorage {

	IGitSerializer getSerializer();

	<T> File location(File dir, T example);

	<T> File location(File dir, Class<T> type, T example);

	<T> File location(File dir, Class<T> type, GitParams keys);

	<T> boolean exists(File dir, T example);

	<T> boolean exists(File dir, Class<T> type, T example);

	<T> boolean exists(File dir, Class<T> type, GitParams keys);

	<T> T write(File dir, T instance);

	<T> T write(File dir, Class<T> type, T instance);

	<T> T merge(File dir, Class<T> type, GitParams keys, T instance);

	<T> T setAttribute(File dir, Class<T> type, GitParams keys, String attribute, Object data);

	<T> T setResource(File dir, Class<T> type, GitParams keys, Resource resource);

	<T> T read(File dir, T example);

	<T> T read(File dir, Class<T> type, T example);

	<T> T read(File dir, Class<T> type, GitParams keys);

	<T> Object getAttribute(File dir, Class<T> type, GitParams keys, String attribute);

	<T> Resource getResource(File dir, Class<T> type, GitParams keys, String path);

	<T> List<Resource> allResources(File dir, Class<T> type, GitParams keys);

	<T> T delete(File dir, T example);

	<T> T delete(File dir, Class<T> type, T example);

	<T> T delete(File dir, Class<T> type, GitParams keys);

	<T> T delResource(File dir, Class<T> type, GitParams keys, String path);

	<T> List<T> all(File dir, Class<T> type, GitPaging paging);

	<T> long count(File dir, Class<T> type, GitPaging paging);

	<T> List<T> search(File dir, Class<T> type, GitQuery query, GitPaging paging);

}