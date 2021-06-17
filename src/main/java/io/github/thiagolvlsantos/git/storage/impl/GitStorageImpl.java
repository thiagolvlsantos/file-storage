package io.github.thiagolvlsantos.git.storage.impl;

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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.github.thiagolvlsantos.git.commons.file.FileUtils;
import io.github.thiagolvlsantos.git.storage.GitEntity;
import io.github.thiagolvlsantos.git.storage.IGitIndex;
import io.github.thiagolvlsantos.git.storage.IGitStorage;
import io.github.thiagolvlsantos.git.storage.annotations.PairValue;
import io.github.thiagolvlsantos.git.storage.annotations.UtilAnnotations;
import io.github.thiagolvlsantos.git.storage.audit.GitChanged;
import io.github.thiagolvlsantos.git.storage.audit.GitCreated;
import io.github.thiagolvlsantos.git.storage.concurrency.GitRevision;
import io.github.thiagolvlsantos.git.storage.exceptions.GitStorageException;
import io.github.thiagolvlsantos.git.storage.identity.GitId;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class GitStorageImpl implements IGitStorage {

	private ObjectMapper mapper;
	private @Autowired IGitIndex idManager;

	@PostConstruct
	public void configure() {
		mapper = new ObjectMapper()// specific instance
				.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)//
				.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.registerModule(new JavaTimeModule());
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
		if (log.isDebugEnabled()) {
			log.debug("entity: {}", entity);
		}
		if (entity == null) {
			throw new GitStorageException("Entity is not annotated with @GitEntity", null);
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

	private <T> void prepareCreated(File dir, Class<T> type, T instance, File target, T old) {
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
				throw new GitStorageException("Could not create object directory: " + parent, null);
			}
			for (PairValue<GitCreated> c : created) {
				Object obj = c.get(instance);
				if (obj == null) {
					c.set(instance, currentTime(c.getRead()));
					if (log.isInfoEnabled()) {
						log.info("new created: {}", c.get(instance));
					}
				}
			}
			for (PairValue<GitId> c : ids) {
				Object obj = c.get(instance);
				if (obj == null) {
					Long nextId = idManager.next(entityRoot(dir, type));
					c.set(instance, nextId);
					idManager.bind(entityRoot(dir, type), instance);
					if (log.isInfoEnabled()) {
						log.info("new id: {}", c.get(instance));
					}
				}
			}
		} else {
			for (PairValue<GitCreated> c : created) {
				Object obj = c.get(old);
				c.set(instance, obj);
				if (log.isInfoEnabled()) {
					log.info("keep created: {}", c.get(instance));
				}
			}
			for (PairValue<GitId> c : ids) {
				Object obj = c.get(old);
				c.set(instance, obj);
				if (log.isInfoEnabled()) {
					log.info("keep ids: {}", c.get(instance));
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
			Number obj = (Number) c.get(instance);
			if (obj == null) {
				c.set(instance, 0);
			} else {
				Number current = null;
				if (old != null) {
					current = (Number) c.get(old);
				} else {
					current = 0L;
				}
				if (obj.longValue() < current.longValue()) {
					throw new GitStorageException("Invalid revision. Reload object and try again.", null);
				}
				c.set(instance, current.longValue() + 1);
			}
			if (log.isInfoEnabled()) {
				log.info("new revision: {}", c.get(instance));
			}
		}
	}

	private <T> void prepareChanged(File dir, Class<T> type, T instance, File target, T old) {
		PairValue<GitChanged>[] changed = UtilAnnotations.getValues(GitChanged.class, type, instance);
		if (log.isInfoEnabled()) {
			log.info("changed: {}", Arrays.toString(changed));
		}
		for (PairValue<GitChanged> c : changed) {
			Method read = c.getRead();
			c.set(instance, currentTime(read));
			if (log.isInfoEnabled()) {
				log.info("new changed: {}", c.get(instance));
			}
		}
	}

	private <T> void write(T instance, File file) {
		try {
			mapper.writeValue(file, instance);
		} catch (IOException e) {
			throw new GitStorageException("Could not write object.", e);
		}
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
				FileUtils.delete(file);
			} catch (IOException e) {
				throw new GitStorageException("Entity not deleted. File:" + file, e);
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