package io.github.thiagolvlsantos.file.storage.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import io.github.thiagolvlsantos.file.storage.IFileIndex;
import io.github.thiagolvlsantos.file.storage.annotations.PairValue;
import io.github.thiagolvlsantos.file.storage.annotations.UtilAnnotations;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageException;
import io.github.thiagolvlsantos.file.storage.identity.FileId;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FileIndexImpl implements IFileIndex {

	private Object lock = new Object();

	@Override
	@SneakyThrows
	public Object next(File dir, PairValue<FileId> info) {
		Long current = 0L;
		synchronized (lock) {
			File file = new File(dir, ".current");
			if (file.exists()) {
				current = Long.valueOf(Files.readString(file.toPath()));
			}
			current = current + 1;
			log.info("Next id for '{}'={}", info.getName(), current);
			Files.write(file.toPath(), String.valueOf(current).getBytes(), StandardOpenOption.CREATE,
					StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
		}
		return current;
	}

	@Override
	@SneakyThrows
	public <T> void bind(File dir, T instance) {
		if (instance == null) {
			throw new FileStorageException("Invalid argument: null", null);
		}
		File index = index(dir);
		if (!index.exists() && !index.mkdirs()) {
			throw new FileStorageException("Could not create index directory: " + index, null);
		}

		Class<? extends Object> clazz = instance.getClass();
		Object[] ids = UtilAnnotations.getIds(clazz, instance);
		Object[] keys = UtilAnnotations.getKeys(clazz, instance);

		File id2Keys = ids(dir, ids);
		File id2KeysParent = id2Keys.getParentFile();
		if (!id2KeysParent.exists() && !id2KeysParent.mkdirs()) {
			throw new FileStorageException("Could not create index keys directory: " + id2KeysParent, null);
		}
		Files.write(id2Keys.toPath(), Stream.of(keys).map(String::valueOf).collect(Collectors.joining("\n")).getBytes(),
				StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

		File keys2Id = keys(dir, keys);
		File keys2IdParent = keys2Id.getParentFile();
		if (!keys2IdParent.exists() && !keys2IdParent.mkdirs()) {
			throw new FileStorageException("Could not create index ids directory: " + keys2IdParent, null);
		}
		Files.write(keys2Id.toPath(), Stream.of(ids).map(String::valueOf).collect(Collectors.joining("\n")).getBytes(),
				StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	private File index(File dir) {
		return new File(dir, ".index");
	}

	@Override
	public <T> void unbind(File dir, T instance) {
		Class<? extends Object> clazz = instance.getClass();

		File ids2Keys = ids(dir, UtilAnnotations.getIds(clazz, instance));
		if (ids2Keys.exists()) {
			try {
				Files.delete(ids2Keys.toPath());
			} catch (IOException e) {
				throw new FileStorageException("Could no delete entity id->keys: " + ids2Keys, e);
			}
		}

		File keys2Id = keys(dir, UtilAnnotations.getKeys(clazz, instance));
		if (keys2Id.exists()) {
			try {
				Files.delete(keys2Id.toPath());
			} catch (IOException e) {
				throw new FileStorageException("Could no delete entity keys->id: " + keys2Id, e);
			}
		}
	}

	@Override
	public File directory(File dir, String kind) {
		return new File(index(dir), kind);
	}

	private File ids(File dir, Object... ids) {
		return flatName(directory(dir, IDS), ids);
	}

	private File keys(File dir, Object... keys) {
		return flatName(directory(dir, KEYS), keys);
	}

	private File flatName(File dir, Object... objs) {
		return new File(dir, Stream.of(objs).map(String::valueOf).collect(Collectors.joining("_")));
	}
}