package io.github.thiagolvlsantos.file.storage.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import io.github.thiagolvlsantos.file.storage.FileEntityName;
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
	public Object next(File dir, Object instance, PairValue<FileId> info) {
		if (instance == null) {
			throw new FileStorageException("Invalid object 'null'.", null);
		}
		Long current = 0L;
		synchronized (lock) {
			String fileName = prefix(instance.getClass()) + ".current";
			File file = new File(dir, fileName);
			if (file.exists()) {
				current = Long.valueOf(Files.readString(file.toPath()));
			}
			current = current + 1;
			log.info("Next id for '{}'={}, in {}", info.getName(), current, fileName);
			Files.write(file.toPath(), String.valueOf(current).getBytes(), StandardOpenOption.CREATE,
					StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
		}
		return current;
	}

	private String prefix(Class<?> type) {
		if (type != null) {
			FileEntityName name = AnnotationUtils.findAnnotation(type, FileEntityName.class);
			if (name != null) {
				return "." + name.value();
			}
		}
		return "";
	}

	@Override
	@SneakyThrows
	public <T> void bind(File dir, T instance) {
		if (instance == null) {
			throw new FileStorageException("Invalid argument: null", null);
		}
		Class<? extends Object> clazz = instance.getClass();

		File index = index(dir, clazz);
		if (!index.exists() && !index.mkdirs()) {
			throw new FileStorageException("Could not create index directory: " + index, null);
		}

		Object[] ids = UtilAnnotations.getIds(clazz, instance);
		Object[] keys = UtilAnnotations.getKeys(clazz, instance);

		File id2Keys = ids(dir, clazz, ids);
		File id2KeysParent = id2Keys.getParentFile();
		if (!id2KeysParent.exists() && !id2KeysParent.mkdirs()) {
			throw new FileStorageException("Could not create index keys directory: " + id2KeysParent, null);
		}
		Files.write(id2Keys.toPath(), Stream.of(keys).map(String::valueOf).collect(Collectors.joining("\n")).getBytes(),
				StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

		File keys2Id = keys(dir, clazz, keys);
		File keys2IdParent = keys2Id.getParentFile();
		if (!keys2IdParent.exists() && !keys2IdParent.mkdirs()) {
			throw new FileStorageException("Could not create index ids directory: " + keys2IdParent, null);
		}
		Files.write(keys2Id.toPath(), Stream.of(ids).map(String::valueOf).collect(Collectors.joining("\n")).getBytes(),
				StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	private File index(File dir, Class<?> type) {
		return new File(dir, prefix(type) + ".index");
	}

	@Override
	public <T> void unbind(File dir, T instance) {
		Class<? extends Object> clazz = instance.getClass();

		File ids2Keys = ids(dir, clazz, UtilAnnotations.getIds(clazz, instance));
		if (ids2Keys.exists()) {
			try {
				Files.delete(ids2Keys.toPath());
			} catch (IOException e) {
				throw new FileStorageException("Could no delete entity id->keys: " + ids2Keys, e);
			}
		}

		File keys2Id = keys(dir, clazz, UtilAnnotations.getKeys(clazz, instance));
		if (keys2Id.exists()) {
			try {
				Files.delete(keys2Id.toPath());
			} catch (IOException e) {
				throw new FileStorageException("Could no delete entity keys->id: " + keys2Id, e);
			}
		}
	}

	@Override
	public File directory(File dir, Class<?> type, String kind) {
		return new File(index(dir, type), kind);
	}

	private File ids(File dir, Class<?> type, Object... ids) {
		return flatName(directory(dir, type, IDS), ids);
	}

	private File keys(File dir, Class<?> type, Object... keys) {
		return flatName(directory(dir, type, KEYS), keys);
	}

	private File flatName(File dir, Object... objs) {
		return new File(dir, Stream.of(objs).map(String::valueOf).collect(Collectors.joining("_")));
	}
}