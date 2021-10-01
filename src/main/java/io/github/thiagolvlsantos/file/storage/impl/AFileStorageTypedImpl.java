package io.github.thiagolvlsantos.file.storage.impl;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import io.github.thiagolvlsantos.file.storage.FilePaging;
import io.github.thiagolvlsantos.file.storage.FileParams;
import io.github.thiagolvlsantos.file.storage.FilePredicate;
import io.github.thiagolvlsantos.file.storage.IFileSerializer;
import io.github.thiagolvlsantos.file.storage.IFileStorage;
import io.github.thiagolvlsantos.file.storage.IFileStorageTyped;
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

	// +------------- ENTITY METHODS ------------------+

	@Override
	public File location(File dir, T example) {
		return storage.location(dir, type(), example);
	}

	@Override
	public File location(File dir, FileParams keys) {
		return storage.location(dir, type(), keys);
	}

	@Override
	public boolean exists(File dir, T example) {
		return storage.exists(dir, type(), example);
	}

	@Override
	public boolean exists(File dir, FileParams keys) {
		return storage.exists(dir, type(), keys);
	}

	@Override
	public T write(File dir, T instance) {
		return storage.write(dir, type, instance);
	}

	@Override
	public T merge(File dir, FileParams keys, T instance) {
		return storage.merge(dir, type, keys, instance);
	}

	@Override
	public T read(File dir, T example) {
		return storage.read(dir, type, example);
	}

	@Override
	public T read(File dir, FileParams keys) {
		return storage.read(dir, type, keys);
	}

	@Override
	public T delete(File dir, T example) {
		return storage.delete(dir, type, example);
	}

	@Override
	public T delete(File dir, FileParams keys) {
		return storage.delete(dir, type, keys);
	}

	@Override
	public long count(File dir, FilePaging paging) {
		return storage.count(dir, type, paging);
	}

	@Override
	public long count(File dir, FilePredicate filter, FilePaging paging) {
		return storage.count(dir, type, filter, paging);
	}

	@Override
	public List<T> list(File dir, FilePaging paging) {
		return storage.list(dir, type, paging);
	}

	@Override
	public List<T> list(File dir, FilePredicate filter, FilePaging paging) {
		return storage.list(dir, type, filter, paging);
	}

	// +------------- ATTRIBUTE METHODS ------------------+

	@Override
	public T setAttribute(File dir, FileParams keys, String attribute, Object data) {
		return storage.setAttribute(dir, type, keys, attribute, data);
	}

	@Override
	public Object getAttribute(File dir, FileParams keys, String attribute) {
		return storage.getAttribute(dir, type, keys, attribute);
	}

	@Override
	public Map<String, Object> attributes(File dir, FileParams keys, FileParams names) {
		return storage.attributes(dir, type, keys, names);
	}

	// +------------- RESOURCE METHODS ------------------+

	@Override
	public File locationResource(File dir, FileParams keys) {
		return storage.locationResource(dir, type, keys);
	}

	@Override
	public File locationResource(File dir, FileParams keys, String path) {
		return storage.locationResource(dir, type, keys, path);
	}

	@Override
	public T setResource(File dir, FileParams keys, Resource resource) {
		return storage.setResource(dir, type, keys, resource);
	}

	@Override
	public Resource getResource(File dir, FileParams keys, String path) {
		return storage.getResource(dir, type, keys, path);
	}

	@Override
	public long countResources(File dir, FileParams keys) {
		return storage.countResources(dir, type, keys);
	}

	@Override
	public long countResources(File dir, FileParams keys, FilePaging paging) {
		return storage.countResources(dir, type, keys, paging);
	}

	@Override
	public long countResources(File dir, FileParams keys, FilePredicate filter, FilePaging paging) {
		return storage.countResources(dir, type, keys, filter, paging);
	}

	@Override
	public List<Resource> listResources(File dir, FileParams keys) {
		return storage.listResources(dir, type, keys);
	}

	@Override
	public List<Resource> listResources(File dir, FileParams keys, FilePaging paging) {
		return storage.listResources(dir, type, keys, paging);
	}

	@Override
	public List<Resource> listResources(File dir, FileParams keys, FilePredicate filter, FilePaging paging) {
		return storage.listResources(dir, type, keys, filter, paging);
	}

	@Override
	public T deleteResource(File dir, FileParams keys, String path) {
		return storage.deleteResource(dir, type, keys, path);
	}

}