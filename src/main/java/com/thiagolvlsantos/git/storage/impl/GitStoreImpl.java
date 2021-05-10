package com.thiagolvlsantos.git.storage.impl;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.thiagolvlsantos.git.storage.GitAlias;
import com.thiagolvlsantos.git.storage.GitEntity;
import com.thiagolvlsantos.git.storage.GitId;
import com.thiagolvlsantos.git.storage.GitKey;
import com.thiagolvlsantos.git.storage.IGitStorage;
import com.thiagolvlsantos.git.storage.audit.GitChanged;
import com.thiagolvlsantos.git.storage.audit.GitCreated;
import com.thiagolvlsantos.git.storage.concurrency.GitRevision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class GitStoreImpl implements IGitStorage {

	private @Autowired ObjectMapper mapper;

	@PostConstruct
	public void configure() {
		mapper = mapper//
				.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)//
				.enable(SerializationFeature.INDENT_OUTPUT);
	}

	@Override
	public <T> boolean exists(File dir, Class<T> type, T reference) {
		return exists(dir, type, keys(type, reference));
	}

	private Object[] keys(Class<?> type, Object instance) {
		PairValue<GitKey>[] keys = get(GitKey.class, type, instance);
		Arrays.sort(keys, (a, b) -> a.annotation.order() - b.annotation.order());
		List<Object> path = new LinkedList<>();
		Stream.of(keys).forEach(v -> {
			Object value = v.getValue();
			if (value != null) {
				Class<?> innerType = value.getClass();
				GitAlias alias = AnnotationUtils.findAnnotation(innerType, GitAlias.class);
				if (alias != null) {
					Object[] in = keys((Class<?>) innerType, value);
					for (Object o : in) {
						path.add(o);
					}
				} else {
					path.add(value);
				}
			}
		});
		if (log.isInfoEnabled()) {
			log.info("keys: {}", path);
		}
		return path.toArray(new Object[0]);
	}

	@SuppressWarnings("unchecked")
	@SneakyThrows
	private <T extends Annotation> PairValue<T>[] get(Class<T> annotation, Class<?> type, Object instance) {
		List<PairValue<T>> result = new LinkedList<>();
		Field[] fields = type.getDeclaredFields();
		for (Field f : fields) {
			Annotation a = AnnotationUtils.findAnnotation(f, annotation);
			if (a != null) {
				PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(type, f.getName());
				Method read = pd.getReadMethod();
				Method write = pd.getWriteMethod();
				result.add((PairValue<T>) PairValue.builder().annotation(a).field(f).read(read).write(write)
						.name(f.getName()).value(read.invoke(instance)).build());
			}
		}
		Method[] methods = type.getDeclaredMethods();
		for (Method m : methods) {
			Annotation a = AnnotationUtils.findAnnotation(m, annotation);
			if (a != null) {
				result.add((PairValue<T>) PairValue.builder().annotation(a).read(m).name(m.getName())
						.value(m.invoke(instance)).build());
			}
		}
		return result.toArray(new PairValue[0]);
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
		if (log.isInfoEnabled()) {
			log.info("path: {}", path);
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
		File file = entityFile(dir, type, keys(type, instance));
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

	private <T> void write(T instance, File file) throws Exception {
		mapper.writeValue(file, instance);
	}

	private <T> File entityFile(File dir, Class<T> type, Object... keys) {
		return new File(entityDir(dir, type, keys), "meta.json");
	}

	private <T> void prepareCreated(File dir, Class<T> type, T instance, File target, T old) throws Exception {
		PairValue<GitId>[] ids = get(GitId.class, type, instance);
		if (log.isInfoEnabled()) {
			log.info("ids: {}", Arrays.toString(ids));
		}
		PairValue<GitCreated>[] created = get(GitCreated.class, type, instance);
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
					c.getWrite().invoke(instance, LocalDateTime.now());
					if (log.isInfoEnabled()) {
						log.info("new created: {}", c.getRead().invoke(instance));
					}
				}
			}
			for (PairValue<GitId> c : ids) {
				Object obj = c.getRead().invoke(instance);
				if (obj == null) {
					Long nextId = generateId(dir, type);
					generateIndex(dir, type, instance, nextId);
					c.getWrite().invoke(instance, nextId);
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
	private <T> Long generateId(File dir, Class<T> type) {
		Long next = currentId(dir, type) + 1;
		Files.write(currentIdFile(dir, type).toPath(), String.valueOf(next).getBytes(), StandardOpenOption.CREATE,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
		return next;
	}

	@SneakyThrows
	private <T> Long currentId(File dir, Class<T> type) {
		Long next = 0L;
		File current = currentIdFile(dir, type);
		if (current.exists()) {
			next = Long.valueOf(Files.readString(current.toPath()));
		}
		return next;
	}

	private <T> File currentIdFile(File dir, Class<T> type) {
		return new File(entityRoot(dir, type), ".current");
	}

	@SneakyThrows
	private <T> void generateIndex(File dir, Class<T> type, T instance, Long nextId) {
		File index = directoryIndex(dir, type);
		if (!index.exists() && !index.mkdirs()) {
			throw new RuntimeException("Could not create index directory: " + index);
		}
		File fileRef = fileRef(dir, type, nextId);
		Object[] keys = keys(type, instance);
		Files.write(fileRef.toPath(),
				Stream.of(keys).map(k -> String.valueOf(k)).collect(Collectors.joining("\n")).getBytes(),
				StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	private <T> File directoryIndex(File dir, Class<T> type) {
		return new File(entityRoot(dir, type), ".all");
	}

	private <T> File fileRef(File dir, Class<T> type, Object... ids) {
		return new File(directoryIndex(dir, type),
				Stream.of(ids).map(k -> String.valueOf(k)).collect(Collectors.joining("_")));
	}

	@SneakyThrows
	private <T> void prepareRevisions(File dir, Class<T> type, T instance, File target, T old) {
		PairValue<GitRevision>[] revisions = get(GitRevision.class, type, instance);
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
		PairValue<GitChanged>[] changed = get(GitChanged.class, type, instance);
		if (log.isInfoEnabled()) {
			log.info("changed: {}", Arrays.toString(changed));
		}
		for (PairValue<GitChanged> c : changed) {
			Method read = c.getRead();
			c.getWrite().invoke(instance, read.getReturnType().getMethod("now").invoke(null));
			if (log.isInfoEnabled()) {
				log.info("new changed: {}", read.invoke(instance));
			}
		}
	}

	@Override
	public <T> T read(File dir, Class<T> type, T reference) {
		return read(dir, type, keys(type, reference));
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
		return delete(dir, type, keys(type, reference));
	}

	@Override
	public <T> T delete(File dir, Class<T> type, Object... keys) {
		File file = entityFile(dir, type, keys);
		T old = null;
		if (file.exists()) {
			old = read(dir, type, keys);
			if (!FileUtils.delete(file.getParentFile())) {
				throw new RuntimeException("Entity not deleted. File:" + file);
			}
			File ref = fileRef(dir, type, ids(type, old));
			if (ref.exists() && !ref.delete()) {
				throw new RuntimeException("Could no delete entity index: " + file);
			}
		}
		return old;
	}

	private Object[] ids(Class<?> type, Object instance) {
		PairValue<GitId>[] ids = get(GitId.class, type, instance);
		Object[] result = Stream.of(ids).map(v -> v.getValue()).toArray();
		if (log.isInfoEnabled()) {
			log.info("ids: {}", Arrays.toString(result));
		}
		return result;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Builder
	@ToString(onlyExplicitlyIncluded = true)
	private static class PairValue<T> {
		private T annotation;
		private Field field;
		private Method read;
		private Method write;
		@ToString.Include
		private String name;
		@ToString.Include
		private Object value;
	}
}