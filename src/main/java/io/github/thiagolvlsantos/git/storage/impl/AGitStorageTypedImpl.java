package io.github.thiagolvlsantos.git.storage.impl;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import io.github.thiagolvlsantos.git.storage.IGitStorage;
import io.github.thiagolvlsantos.git.storage.IGitStorageTyped;
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
	public boolean exists(File dir, T example) {
		return storage.exists(dir, type, example);
	}

	@Override
	public boolean exists(File dir, Object... keys) {
		return storage.exists(dir, type, keys);
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
	public T read(File dir, Object... keys) {
		return storage.read(dir, type, keys);
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
	public List<T> all(File dir) {
		return storage.all(dir, type);
	}

	@Override
	public long count(File dir) {
		return storage.count(dir, type);
	}
}