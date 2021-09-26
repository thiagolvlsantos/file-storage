package io.github.thiagolvlsantos.git.storage.impl;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import io.github.thiagolvlsantos.git.storage.GitParams;
import io.github.thiagolvlsantos.git.storage.IGitSerializer;
import io.github.thiagolvlsantos.git.storage.IGitStorage;
import io.github.thiagolvlsantos.git.storage.IGitStorageTyped;
import io.github.thiagolvlsantos.git.storage.resource.Resource;
import lombok.Setter;

@Setter
public abstract class AGitStorageTypedImpl<T> implements IGitStorageTyped<T> {

	private Class<T> type;
	private @Autowired IGitStorage storage;

	protected AGitStorageTypedImpl(Class<T> type) {
		this.type = type;
	}

	@Override
	public Class<T> type() {
		return type;
	}

	@Override
	public IGitSerializer getSerializer() {
		return storage.getSerializer();
	}

	@Override
	public File location(File dir, T example) {
		return storage.location(dir, type(), example);
	}

	@Override
	public File location(File dir, GitParams keys) {
		return storage.location(dir, type(), keys);
	}

	@Override
	public boolean exists(File dir, T example) {
		return storage.exists(dir, type(), example);
	}

	@Override
	public boolean exists(File dir, GitParams keys) {
		return storage.exists(dir, type(), keys);
	}

	@Override
	public T write(File dir, T instance) {
		return storage.write(dir, type, instance);
	}

	@Override
	public T merge(File dir, GitParams keys, T instance) {
		return storage.merge(dir, type, keys, instance);
	}

	@Override
	public T setAttribute(File dir, GitParams keys, String attribute, Object data) {
		return storage.setAttribute(dir, type, keys, attribute, data);
	}

	@Override
	public T setResource(File dir, GitParams keys, Resource resource) {
		return storage.setResource(dir, type, keys, resource);
	}

	@Override
	public T read(File dir, T example) {
		return storage.read(dir, type, example);
	}

	@Override
	public T read(File dir, GitParams keys) {
		return storage.read(dir, type, keys);
	}

	@Override
	public Object getAttribute(File dir, GitParams keys, String attribute) {
		return storage.getAttribute(dir, type, keys, attribute);
	}

	@Override
	public Resource getResource(File dir, GitParams keys, String path) {
		return storage.getResource(dir, type, keys, path);
	}

	@Override
	public List<Resource> allResources(File dir, GitParams keys) {
		return storage.allResources(dir, type, keys);
	}

	@Override
	public T delete(File dir, T example) {
		return storage.delete(dir, type, example);
	}

	@Override
	public T delete(File dir, GitParams keys) {
		return storage.delete(dir, type, keys);
	}

	@Override
	public T delResource(File dir, GitParams keys, String path) {
		return storage.delResource(dir, type, keys, path);
	}

	@Override
	public List<T> all(File dir) {
		return storage.all(dir, type);
	}

	@Override
	public long count(File dir) {
		return storage.count(dir, type);
	}

	@Override
	public List<T> search(File dir, String query) {
		return storage.search(dir, type, query);
	}
}