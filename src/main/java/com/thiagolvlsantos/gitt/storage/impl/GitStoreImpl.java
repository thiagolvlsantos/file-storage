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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
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

	@Override
	@SneakyThrows
	public <T> T write(File dir, Class<T> type, T instance) {
		// received fields
		GitEntity entity = AnnotationUtils.findAnnotation(type, GitEntity.class);
		if (log.isInfoEnabled()) {
			log.info("entity: {}", entity);
		}
		PairValue<GitId>[] ids = get(GitId.class, type, instance);
		if (log.isInfoEnabled()) {
			log.info("ids: {}", Arrays.toString(ids));
		}
		PairValue<GitKey>[] keys = get(GitKey.class, type, instance);
		Arrays.sort(keys, (a, b) -> a.annotation.order() - b.annotation.order());
		if (log.isInfoEnabled()) {
			log.info("keys: {}", Arrays.toString(keys));
		}
		// updateable fields
		PairValue<GitRevision>[] revisions = get(GitRevision.class, type, instance);
		if (log.isInfoEnabled()) {
			log.info("revisions: {}", Arrays.toString(revisions));
		}
		PairValue<GitCreated>[] created = get(GitCreated.class, type, instance);
		if (log.isInfoEnabled()) {
			log.info("created: {}", Arrays.toString(created));
		}
		PairValue<GitChanged>[] changed = get(GitChanged.class, type, instance);
		if (log.isInfoEnabled()) {
			log.info("changed: {}", Arrays.toString(changed));
		}

		File root = root(dir, entity);
		File fileEntity = entity(root, keys);
		File metadata = new File(fileEntity, "meta.json");
		T old = null;
		try {
			old = read(dir, type, Stream.of(keys).map(k -> k.value).toArray());
		} catch (Throwable e) {
			// silent
		}
		prepareCreated(instance, created, metadata, old);
		prepareRevisions(instance, revisions, metadata, old);
		prepareChanged(instance, changed, metadata, old);
		mapper.writeValue(metadata, instance);
		return instance;
	}

	private <T> void prepareCreated(T instance, PairValue<GitCreated>[] created, File metadata, T old)
			throws Exception {
		if (!metadata.exists()) {
			metadata.getParentFile().mkdirs();
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

	private <T> void prepareRevisions(T instance, PairValue<GitRevision>[] revisions, File metadata, T old)
			throws Exception {
		for (PairValue<GitRevision> c : revisions) {
			Number obj = (Number) c.getRead().invoke(instance);
			if (obj == null) {
				c.getWrite().invoke(instance, 0);
			} else {
				Number current = (Number) c.getRead().invoke(old);
				c.getWrite().invoke(instance, current.longValue() + 1);
			}
			if (log.isInfoEnabled()) {
				log.info("new revision: {}", c.getRead().invoke(instance));
			}
		}
	}

	private <T> void prepareChanged(T instance, PairValue<GitChanged>[] changed, File metadata, T old)
			throws Exception {
		for (PairValue<GitChanged> c : changed) {
			c.getWrite().invoke(instance, LocalDateTime.now());
			if (log.isInfoEnabled()) {
				log.info("new changed: {}", c.getRead().invoke(instance));
			}
		}
	}

	private File root(File dir, GitEntity entity) {
		return new File(dir, entity.value());
	}

	private File entity(File root, PairValue<GitKey>[] keys) {
		File tmp = root;
		for (PairValue<GitKey> k : keys) {
			tmp = new File(tmp, String.valueOf(k.getValue()));
		}
		if (log.isInfoEnabled()) {
			log.info("pack: {}", tmp);
		}
		return tmp;
	}

	@Override
	@SneakyThrows
	public <T> T read(File dir, Class<T> type, Object... keys) {
		GitEntity entity = AnnotationUtils.findAnnotation(type, GitEntity.class);
		if (log.isInfoEnabled()) {
			log.info("entity: {}", entity);
		}
		File root = root(dir, entity);
		File fileEntity = entity(root, keys);
		File metadata = new File(fileEntity, "meta.json");
		return mapper.readValue(metadata, type);
	}

	private File entity(File root, Object[] keys) {
		File tmp = root;
		for (Object k : keys) {
			tmp = new File(tmp, String.valueOf(k));
		}
		if (log.isInfoEnabled()) {
			log.info("pack: {}", tmp);
		}
		return tmp;
	}

	@Override
	@SneakyThrows
	public <T> T delete(File dir, Class<T> type, Object... keys) {
		GitEntity entity = AnnotationUtils.findAnnotation(type, GitEntity.class);
		if (log.isInfoEnabled()) {
			log.info("entity: {}", entity);
		}
		File root = root(dir, entity);
		File fileEntity = entity(root, keys);
		T old = null;
		if (fileEntity.exists()) {
			old = read(dir, type, keys);
			if (!FileUtils.delete(fileEntity.getParentFile())) {
				throw new RuntimeException("Project not deleted. File:" + fileEntity);
			} else {
				Thread.sleep(100);
			}

		}
		return old;
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

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Builder
	@ToString
	private static class PairValue<T> {
		private T annotation;
		private Field field;
		private Method read;
		private Method write;
		private String name;
		private Object value;
	}
}