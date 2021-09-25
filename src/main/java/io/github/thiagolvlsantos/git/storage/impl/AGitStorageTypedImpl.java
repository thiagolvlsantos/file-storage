package io.github.thiagolvlsantos.git.storage.impl;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

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
	public File location(File dir, Object... keys) {
		return storage.location(dir, type(), keys);
	}

	@Override
	public boolean exists(File dir, T example) {
		return storage.exists(dir, type(), example);
	}

	@Override
	public boolean exists(File dir, Object... keys) {
		return storage.exists(dir, type(), keys);
	}

	@Override
	public T write(File dir, T instance) {
		return storage.write(dir, type, instance);
	}

	@Override
	public T merge(File dir, T instance, Object... keys) {
		return storage.merge(dir, type, instance, keys);
	}

	@Override
	public T setAttribute(File dir, String attribute, String data, Object... keys) {
		return storage.setAttribute(dir, type, attribute, data, keys);
	}

	@Override
	public T setResource(File dir, Resource resource, Object... keys) {
		return storage.setResource(dir, type, resource, keys);
	}

	@Override
	public T read(File dir, T example) {
		return storage.read(dir, type, example);
	}

	@Override
	public T read(File dir, Object... keys) {
		return storage.read(dir, type, keys);
	}

	@Override
	public Object getAttribute(File dir, String attribute, Object... keys) {
		return storage.getAttribute(dir, type, attribute, keys);
	}

	@Override
	public Resource getResource(File dir, String path, Object... keys) {
		return storage.getResource(dir, type, path, keys);
	}

	@Override
	public List<Resource> allResources(File dir, Object... keys) {
		return storage.allResources(dir, type, keys);
	}

	@Override
	public T delete(File dir, T example) {
		return storage.delete(dir, type, example);
	}

	@Override
	public T delete(File dir, Object... keys) {
		return storage.delete(dir, type, keys);
	}

	@Override
	public T delResource(File dir, String path, Object... keys) {
		return storage.delResource(dir, type, path, keys);
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