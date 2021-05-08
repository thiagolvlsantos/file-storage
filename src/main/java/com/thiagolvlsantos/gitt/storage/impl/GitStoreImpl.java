package com.thiagolvlsantos.gitt.storage.impl;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.thiagolvlsantos.gitt.storage.GitChanged;
import com.thiagolvlsantos.gitt.storage.GitCreated;
import com.thiagolvlsantos.gitt.storage.GitEntity;
import com.thiagolvlsantos.gitt.storage.GitId;
import com.thiagolvlsantos.gitt.storage.GitKey;
import com.thiagolvlsantos.gitt.storage.GitRevision;
import com.thiagolvlsantos.gitt.storage.IGitStorage;

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
		mapper = mapper.enable(SerializationFeature.INDENT_OUTPUT);
	}

	@Override
	public <T> boolean exists(File dir, Class<T> type, T reference) {
		return exists(dir, type, keys(type, reference));
	}

	private <T> Object[] keys(Class<T> type, T instance) {
		PairValue<GitKey>[] keys = get(GitKey.class, type, instance);
		Arrays.sort(keys, (a, b) -> a.annotation.order() - b.annotation.order());
		if (log.isInfoEnabled()) {
			log.info("keys: {}", Arrays.toString(keys));
		}
		return Stream.of(keys).map(v -> v.getValue()).toArray();
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
		GitEntity entity = AnnotationUtils.findAnnotation(type, GitEntity.class);
		if (log.isInfoEnabled()) {
			log.info("entity: {}", entity);
		}
		File path = new File(dir, entity.value());
		for (Object k : keys) {
			path = new File(path, String.valueOf(k));
		}
		if (log.isInfoEnabled()) {
			log.info("path: {}", path);
		}
		return path;
	}

	@Override
	@SneakyThrows
	public <T> T write(File dir, Class<T> type, T instance) {
		File file = entityFile(dir, type, keys(type, instance));
		T old = null;
		if (file.exists()) {
			old = read(file, type);
		}
		prepareCreated(type, instance, file, old);
		prepareRevisions(type, instance, file, old);
		prepareChanged(type, instance, file, old);

		write(instance, file);

		return instance;
	}

	private <T> void write(T instance, File file) throws Exception {
		mapper.writeValue(file, instance);
	}

	private <T> File entityFile(File dir, Class<T> type, Object... keys) {
		return new File(entityDir(dir, type, keys), "meta.json");
	}

	private <T> void prepareCreated(Class<T> type, T instance, File target, T old) throws Exception {
		PairValue<GitId>[] ids = get(GitId.class, type, instance);
		if (log.isInfoEnabled()) {
			log.info("ids: {}", Arrays.toString(ids));
		}

		PairValue<GitCreated>[] created = get(GitCreated.class, type, instance);
		if (log.isInfoEnabled()) {
			log.info("created: {}", Arrays.toString(created));
		}
		if (!target.exists()) {
			target.getParentFile().mkdirs();
			for (PairValue<GitCreated> c : created) {
				Object obj = c.getRead().invoke(instance);
				if (obj == null) {
					c.getWrite().invoke(instance, LocalDateTime.now());
					if (log.isInfoEnabled()) {
						log.info("new created: {}", c.getRead().invoke(instance));
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
		}
	}

	private <T> void prepareRevisions(Class<T> type, T instance, File target, T old) throws Exception {
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
				c.getWrite().invoke(instance, current.longValue() + 1);
			}
			if (log.isInfoEnabled()) {
				log.info("new revision: {}", c.getRead().invoke(instance));
			}
		}
	}

	private <T> void prepareChanged(Class<T> type, T instance, File target, T old) throws Exception {
		PairValue<GitChanged>[] changed = get(GitChanged.class, type, instance);
		if (log.isInfoEnabled()) {
			log.info("changed: {}", Arrays.toString(changed));
		}
		for (PairValue<GitChanged> c : changed) {
			c.getWrite().invoke(instance, LocalDateTime.now());
			if (log.isInfoEnabled()) {
				log.info("new changed: {}", c.getRead().invoke(instance));
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
				throw new RuntimeException("Project not deleted. File:" + file);
			}
		}
		return old;
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