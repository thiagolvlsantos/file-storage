package com.thiagolvlsantos.git.storage.impl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.thiagolvlsantos.git.storage.GitEntity;
import com.thiagolvlsantos.git.storage.IGitIndex;
import com.thiagolvlsantos.git.storage.IGitStorage;
import com.thiagolvlsantos.git.storage.audit.GitChanged;
import com.thiagolvlsantos.git.storage.audit.GitCreated;
import com.thiagolvlsantos.git.storage.concurrency.GitRevision;
import com.thiagolvlsantos.git.storage.exceptions.GitStorageException;
import com.thiagolvlsantos.git.storage.identity.GitId;
import com.thiagolvlsantos.git.storage.util.annotations.PairValue;
import com.thiagolvlsantos.git.storage.util.annotations.UtilAnnotations;

import io.github.thiagolvlsantos.git.commons.file.FileUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class GitStorageImpl implements IGitStorage {

	private @Autowired ObjectMapper mapper;
	private @Autowired IGitIndex idManager;

	@PostConstruct
	public void configure() {
		mapper = mapper//
				.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)//
				.enable(SerializationFeature.INDENT_OUTPUT);
	}

	@Override
	public <T> boolean exists(File dir, Class<T> type, T reference) {
		return exists(dir, type, UtilAnnotations.getKeys(type, reference));
	}

	@Override
	public <T> boolean exists(File dir, Class<T> type, Object... keys) {
		return entityDir(dir, type, keys).exists();
	}

	private <T> File entityDir(File dir, Class<T> type, Object... keys) {
		File path = entityRoot(dir, type);
		for (Object k : keys) {
			path = new File(path, String.valueOf(k));
		}
		if (log.isDebugEnabled()) {
			log.debug("path: {}", path);
		}
		return path;
	}

	private <T> File entityRoot(File dir, Class<T> type) {
		GitEntity entity = AnnotationUtils.findAnnotation(type, GitEntity.class);
		if (log.isInfoEnabled()) {
			log.info("entity: {}", entity);
		}
		return new File(dir, entity.value());
	}

	@Override
	@SneakyThrows
	public <T> T write(File dir, Class<T> type, T instance) {
		File file = entityFile(dir, type, UtilAnnotations.getKeys(type, instance));
		T old = null;
		if (file.exists()) {
			old = read(file, type);
		}

		prepareCreated(dir, type, instance, file, old);
		prepareRevisions(dir, type, instance, file, old);
		prepareChanged(dir, type, instance, file, old);

		write(instance, file);

		return instance;
	}

	private <T> File entityFile(File dir, Class<T> type, Object... keys) {
		return new File(entityDir(dir, type, keys), "meta.json");
	}

	private <T> void prepareCreated(File dir, Class<T> type, T instance, File target, T old) throws Exception {
		PairValue<GitId>[] ids = UtilAnnotations.getValues(GitId.class, type, instance);
		if (log.isInfoEnabled()) {
			log.info("ids: {}", Arrays.toString(ids));
		}
		PairValue<GitCreated>[] created = UtilAnnotations.getValues(GitCreated.class, type, instance);
		if (log.isInfoEnabled()) {
			log.info("created: {}", Arrays.toString(created));
		}
		if (!target.exists()) {
			File parent = target.getParentFile();
			if (!parent.mkdirs()) {
				throw new RuntimeException("Could not create object directory: " + parent);
			}
			for (PairValue<GitCreated> c : created) {
				Object obj = c.getRead().invoke(instance);
				if (obj == null) {
					c.getWrite().invoke(instance, currentTime(c.getRead()));
					if (log.isInfoEnabled()) {
						log.info("new created: {}", c.getRead().invoke(instance));
					}
				}
			}
			for (PairValue<GitId> c : ids) {
				Object obj = c.getRead().invoke(instance);
				if (obj == null) {
					Long nextId = idManager.next(entityRoot(dir, type));
					c.getWrite().invoke(instance, nextId);
					idManager.bind(entityRoot(dir, type), instance);
					if (log.isInfoEnabled()) {
						log.info("new id: {}", c.getRead().invoke(instance));
					}
				}
			}
		} else {
			for (PairValue<GitCreated> c : created) {
				Object obj = c.getRead().invoke(old);
				c.getWrite().invoke(instance, obj);
				if (log.isInfoEnabled()) {
					log.info("keep created: {}", c.getRead().invoke(instance));
				}
			}
			for (PairValue<GitId> c : ids) {
				Object obj = c.getRead().invoke(old);
				c.getWrite().invoke(instance, obj);
				if (log.isInfoEnabled()) {
					log.info("keep ids: {}", c.getRead().invoke(instance));
				}
			}
		}
	}

	@SneakyThrows
	private Object currentTime(Method m) {
		Object current = null;
		Class<?> returnType = m.getReturnType();
		if (Temporal.class.isAssignableFrom(returnType)) {
			current = returnType.getMethod("now").invoke(null);
		} else {
			current = System.currentTimeMillis();
		}
		return current;
	}

	@SneakyThrows
	private <T> void prepareRevisions(File dir, Class<T> type, T instance, File target, T old) {
		PairValue<GitRevision>[] revisions = UtilAnnotations.getValues(GitRevision.class, type, instance);
		if (log.isInfoEnabled()) {
			log.info("revisions: {}", Arrays.toString(revisions));
		}
		for (PairValue<GitRevision> c : revisions) {
			Number obj = (Number) c.getRead().invoke(instance);
			if (obj == null) {
				c.getWrite().invoke(instance, 0);
			} else {
				Number current = null;
				if (old != null) {
					current = (Number) c.getRead().invoke(old);
				} else {
					current = 0L;
				}
				if (obj.longValue() < current.longValue()) {
					throw new RuntimeException("Invalid revision. Reload object and try again.");
				}
				c.getWrite().invoke(instance, current.longValue() + 1);
			}
			if (log.isInfoEnabled()) {
				log.info("new revision: {}", c.getRead().invoke(instance));
			}
		}
	}

	private <T> void prepareChanged(File dir, Class<T> type, T instance, File target, T old) throws Exception {
		PairValue<GitChanged>[] changed = UtilAnnotations.getValues(GitChanged.class, type, instance);
		if (log.isInfoEnabled()) {
			log.info("changed: {}", Arrays.toString(changed));
		}
		for (PairValue<GitChanged> c : changed) {
			Method read = c.getRead();
			c.getWrite().invoke(instance, currentTime(read));
			if (log.isInfoEnabled()) {
				log.info("new changed: {}", read.invoke(instance));
			}
		}
	}

	private <T> void write(T instance, File file) throws Exception {
		mapper.writeValue(file, instance);
	}

	@Override
	public <T> T read(File dir, Class<T> type, T reference) {
		return read(dir, type, UtilAnnotations.getKeys(type, reference));
	}

	@Override
	public <T> T read(File dir, Class<T> type, Object... keys) {
		return read(entityFile(dir, type, keys), type);
	}

	@SneakyThrows
	public <T> T read(File file, Class<T> type) {
		return mapper.readValue(file, type);
	}

	@Override
	public <T> T delete(File dir, Class<T> type, T reference) {
		return delete(dir, type, UtilAnnotations.getKeys(type, reference));
	}

	@Override
	public <T> T delete(File dir, Class<T> type, Object... keys) {
		T old = null;
		if (exists(dir, type, keys)) {
			old = read(dir, type, keys);
			File file = entityDir(dir, type, keys);
			try {
				if (!FileUtils.delete(file)) {
					throw new RuntimeException("Entity not deleted. File:" + file);
				}
			} catch (IOException e) {
				throw new GitStorageException(e.getMessage(), e);
			}
			idManager.unbind(entityRoot(dir, type), old);
		}
		return old;
	}

	@Override
	@SneakyThrows
	public <T> List<T> all(File dir, Class<T> type) {
		List<T> result = new LinkedList<>();
		File[] ids = idManager.directory(entityRoot(dir, type), "ids").listFiles();
		for (File f : ids) {
			Object[] keys = Files.readAllLines(f.toPath()).toArray(new Object[0]);
			result.add(mapper.readValue(entityFile(dir, type, keys), type));
		}
		return result;
	}

	@Override
	public <T> long count(File dir, Class<T> type) {
		return idManager.directory(entityRoot(dir, type), "ids").listFiles().length;
	}
}