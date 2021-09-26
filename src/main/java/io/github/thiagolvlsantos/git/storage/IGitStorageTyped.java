package io.github.thiagolvlsantos.git.storage;

import java.io.File;
import java.util.List;

import io.github.thiagolvlsantos.git.storage.resource.Resource;

public interface IGitStorageTyped<T> {

	Class<T> type();

	IGitSerializer getSerializer();

	File location(File dir, T example);

	File location(File dir, GitParams keys);

	boolean exists(File dir, T example);

	boolean exists(File dir, GitParams keys);

	T write(File dir, T instance);

	T merge(File dir, GitParams keys, T instance);

	T setAttribute(File dir, GitParams keys, String attribute, Object data);

	T setResource(File dir, GitParams keys, Resource resource);

	T read(File dir, T example);

	T read(File dir, GitParams keys);

	Object getAttribute(File dir, GitParams keys, String attribute);

	Resource getResource(File dir, GitParams keys, String path);

	List<Resource> allResources(File dir, GitParams keys);

	T delete(File dir, T example);

	T delete(File dir, GitParams keys);

	T delResource(File dir, GitParams keys, String path);

	List<T> all(File dir);

	long count(File dir);

	List<T> search(File dir, String query);
}