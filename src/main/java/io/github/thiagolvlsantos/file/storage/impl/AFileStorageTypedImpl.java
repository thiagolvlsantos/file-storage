package io.github.thiagolvlsantos.file.storage.impl;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import io.github.thiagolvlsantos.file.storage.KeyParams;
import io.github.thiagolvlsantos.file.storage.IFileSerializer;
import io.github.thiagolvlsantos.file.storage.IFileStorage;
import io.github.thiagolvlsantos.file.storage.IFileStorageTyped;
import io.github.thiagolvlsantos.file.storage.SearchParams;
import io.github.thiagolvlsantos.file.storage.resource.Resource;
import lombok.Setter;

@Setter
public abstract class AFileStorageTypedImpl<T> implements IFileStorageTyped<T> {

	private Class<T> type;
	private @Autowired IFileStorage storage;

	protected AFileStorageTypedImpl(Class<T> type) {
		this.type = type;
	}

	@Override
	public Class<T> type() {
		return type;
	}

	@Override
	public IFileSerializer getSerializer() {
		return storage.getSerializer();
	}

	@Override
	public void setSerializer(IFileSerializer serializer) {
		storage.setSerializer(serializer);
	}

	// +------------- ENTITY METHODS ------------------+

	@Override
	public File location(File dir, T example) {
		return storage.location(dir, type(), example);
	}

	@Override
	public File location(File dir, KeyParams keys) {
		return storage.location(dir, type(), keys);
	}

	@Override
	public boolean exists(File dir, T example) {
		return storage.exists(dir, type(), example);
	}

	@Override
	public boolean exists(File dir, KeyParams keys) {
		return storage.exists(dir, type(), keys);
	}

	@Override
	public T write(File dir, T instance) {
		return storage.write(dir, type, instance);
	}

	@Override
	public T read(File dir, T example) {
		return storage.read(dir, type, example);
	}

	@Override
	public T read(File dir, KeyParams keys) {
		return storage.read(dir, type, keys);
	}

	@Override
	public T delete(File dir, T example) {
		return storage.delete(dir, type, example);
	}

	@Override
	public T delete(File dir, KeyParams keys) {
		return storage.delete(dir, type, keys);
	}

	@Override
	public long count(File dir, SearchParams search) {
		return storage.count(dir, type, search);
	}

	@Override
	public List<T> list(File dir, SearchParams search) {
		return storage.list(dir, type, search);
	}

	// +------------- PROPERTY METHODS ------------------+

	@Override
	public T setProperty(File dir, KeyParams keys, String property, Object data) {
		return storage.setProperty(dir, type, keys, property, data);
	}

	@Override
	public List<T> setProperty(File dir, String property, Object data, SearchParams search) {
		return storage.setProperty(dir, type, property, data, search);
	}

	@Override
	public Object getProperty(File dir, KeyParams keys, String property) {
		return storage.getProperty(dir, type, keys, property);
	}

	@Override
	public Map<String, Object> properties(File dir, KeyParams keys, KeyParams names) {
		return storage.properties(dir, type, keys, names);
	}

	@Override
	public Map<String, Map<String, Object>> properties(File dir, KeyParams names, SearchParams search) {
		return storage.properties(dir, type, names, search);
	}
	// +------------- RESOURCE METHODS ------------------+

	@Override
	public File locationResource(File dir, KeyParams keys, String path) {
		return storage.locationResource(dir, type, keys, path);
	}

	@Override
	public boolean existsResource(File dir, KeyParams keys, String path) {
		return storage.existsResource(dir, type, keys, path);
	}

	@Override
	public T setResource(File dir, KeyParams keys, Resource resource) {
		return storage.setResource(dir, type, keys, resource);
	}

	@Override
	public Resource getResource(File dir, KeyParams keys, String path) {
		return storage.getResource(dir, type, keys, path);
	}

	@Override
	public T deleteResource(File dir, KeyParams keys, String path) {
		return storage.deleteResource(dir, type, keys, path);
	}

	@Override
	public long countResources(File dir, KeyParams keys, SearchParams search) {
		return storage.countResources(dir, type, keys, search);
	}

	@Override
	public List<Resource> listResources(File dir, KeyParams keys, SearchParams search) {
		return storage.listResources(dir, type, keys, search);
	}
}