package io.github.thiagolvlsantos.git.storage.impl;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import io.github.thiagolvlsantos.git.commons.file.FileUtils;
import io.github.thiagolvlsantos.git.storage.GitEntity;
import io.github.thiagolvlsantos.git.storage.IGitIndex;
import io.github.thiagolvlsantos.git.storage.IGitSerializer;
import io.github.thiagolvlsantos.git.storage.IGitStorage;
import io.github.thiagolvlsantos.git.storage.annotations.GitKeep;
import io.github.thiagolvlsantos.git.storage.annotations.PairValue;
import io.github.thiagolvlsantos.git.storage.annotations.UtilAnnotations;
import io.github.thiagolvlsantos.git.storage.audit.GitChanged;
import io.github.thiagolvlsantos.git.storage.audit.GitCreated;
import io.github.thiagolvlsantos.git.storage.audit.IGitInitializer;
import io.github.thiagolvlsantos.git.storage.concurrency.GitRevision;
import io.github.thiagolvlsantos.git.storage.exceptions.GitStorageException;
import io.github.thiagolvlsantos.git.storage.identity.GitId;
import io.github.thiagolvlsantos.git.storage.identity.GitKey;
import io.github.thiagolvlsantos.git.storage.resource.Resource;
import io.github.thiagolvlsantos.git.storage.resource.ResourceContent;
import io.github.thiagolvlsantos.git.storage.resource.ResourceMetadata;
import io.github.thiagolvlsantos.json.predicate.IPredicateFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class GitStorageImpl implements IGitStorage {

	private @Autowired IGitSerializer serializer;
	private @Autowired IGitIndex idManager;
	private @Autowired IPredicateFactory predicateFactory;

	@Override
	public IGitSerializer getSerializer() {
		return serializer;
	}

	@Override
	public <T> boolean exists(File dir, Class<T> type, T reference) {
		return exists(dir, type, UtilAnnotations.getKeys(type, reference));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> boolean exists(File dir, T reference) {
		Class<T> type = (Class<T>) reference.getClass();
		return exists(dir, type, UtilAnnotations.getKeys(type, reference));
	}

	@Override
	public <T> boolean exists(File dir, Class<T> type, Object... keys) {
		return entityDir(dir, type, keys).exists();
	}

	protected <T> File entityDir(File dir, Class<T> type, Object... keys) {
		File path = entityRoot(dir, type);
		for (Object k : keys) {
			path = new File(path, String.valueOf(k));
		}
		if (log.isDebugEnabled()) {
			log.debug("path: {}", path);
		}
		return path;
	}

	protected <T> File entityRoot(File dir, Class<T> type) {
		GitEntity entity = AnnotationUtils.findAnnotation(type, GitEntity.class);
		if (log.isDebugEnabled()) {
			log.debug("entity: {}", entity);
		}
		if (entity == null) {
			throw new GitStorageException("Entity is not annotated with @GitEntity.", null);
		}
		return new File(dir, "@" + entity.value());
	}

	@Override
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

	@SuppressWarnings("unchecked")
	@Override
	public <T> T write(File dir, T instance) {
		return write(dir, (Class<T>) instance.getClass(), instance);
	}

	protected <T> File entityFile(File dir, Class<T> type, Object... keys) {
		return new File(entityDir(dir, type, keys), "meta.json");
	}

	protected <T> void prepareCreated(File dir, Class<T> type, T instance, File target, T old) {
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
			initializeFixed(dir, type, instance, ids, created);
		} else {
			keepFixed(old, ids, created, instance);
		}
	}

	@SneakyThrows
	protected Object value(Object instance, String name, Class<? extends IGitInitializer> initializer, Method m) {
		IGitInitializer factory = initializer.getConstructor().newInstance();
		return factory.value(instance, name, m.getReturnType());
	}

	protected <T> void initializeFixed(File dir, Class<T> type, T instance, PairValue<GitId>[] ids,
			PairValue<GitCreated>[] created) {
		for (PairValue<GitCreated> c : created) {
			Object obj = c.get(instance);
			if (obj == null) {
				c.set(instance, value(instance, c.getName(), c.getAnnotation().value(), c.getRead()));
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
	}

	protected <T> void keepFixed(T old, PairValue<GitId>[] ids, PairValue<GitCreated>[] created, T instance) {
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

	protected <T> void prepareRevisions(File dir, Class<T> type, T instance, File target, T old) {
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

	protected <T> void prepareChanged(File dir, Class<T> type, T instance, File target, T old) {
		PairValue<GitChanged>[] changed = UtilAnnotations.getValues(GitChanged.class, type, instance);
		if (log.isInfoEnabled()) {
			log.info("changed: {}", Arrays.toString(changed));
		}
		for (PairValue<GitChanged> c : changed) {
			Method read = c.getRead();
			c.set(instance, value(instance, c.getName(), c.getAnnotation().value(), read));
			if (log.isInfoEnabled()) {
				log.info("new changed: {}", c.get(instance));
			}
		}
	}

	protected <T> void write(T instance, File file) {
		serializer.writeValue(file, instance);
	}

	@Override
	@SneakyThrows
	public <T> T merge(File dir, Class<T> type, T instance, Object... keys) {
		if (!exists(dir, type, keys)) {
			throw new GitStorageException(
					"Object '" + type.getSimpleName() + "' with keys '" + Arrays.toString(keys) + "' not found.", null);
		}
		// old objects
		T current = read(dir, type, keys);
		PairValue<GitId>[] currentIds = UtilAnnotations.getValues(GitId.class, type, current);
		PairValue<GitKey>[] currentKeys = UtilAnnotations.getValues(GitKey.class, type, current);
		PairValue<GitCreated>[] currentCreated = UtilAnnotations.getValues(GitCreated.class, type, current);
		PairValue<GitRevision>[] currentRevision = UtilAnnotations.getValues(GitRevision.class, type, current);
		PairValue<GitKeep>[] currentKeep = UtilAnnotations.getValues(GitKeep.class, type, current);
		// new object
		// TODO: create an interface IReplicator as an abstraction of this copy.
		BeanUtils.copyProperties(current, instance);
		// return unchangeable attributes
		reassignAttributes(GitId.class, current, currentIds);
		reassignAttributes(GitKey.class, current, currentKeys);
		reassignAttributes(GitCreated.class, current, currentCreated);
		reassignAttributes(GitRevision.class, current, currentRevision);
		reassignAttributes(GitKeep.class, current, currentKeep);
		// write resulting object
		return write(dir, current);
	}

	protected <A extends Annotation, T> void reassignAttributes(Class<A> annotation, T current, PairValue<A>[] pairs)
			throws IllegalAccessException, InvocationTargetException {
		for (PairValue<A> c : pairs) {
			if (log.isInfoEnabled()) {
				log.info("Return " + annotation.getSimpleName() + ": {}={}", c.getName(), c.getValue());
			}
			BeanUtils.setProperty(current, c.getName(), c.getValue());
		}
	}

	@Override
	@SneakyThrows
	public <T> T setAttribute(File dir, Class<T> type, String attribute, Object data, Object... keys) {
		if (!exists(dir, type, keys)) {
			throw new GitStorageException(
					"Object '" + type.getSimpleName() + "' with keys '" + Arrays.toString(keys) + "' not found.", null);
		}
		T current = read(dir, type, keys);
		PropertyDescriptor pd = PropertyUtils.getPropertyDescriptor(current, attribute);
		if (pd == null) {
			throw new GitStorageException("Attribute '" + attribute + "' not found for type: " + current.getClass(),
					null);
		}
		// check unchangeable attributes
		validateAttribute(GitId.class, type, attribute, current);
		validateAttribute(GitKey.class, type, attribute, current);
		validateAttribute(GitCreated.class, type, attribute, current);
		validateAttribute(GitRevision.class, type, attribute, current);
		validateAttribute(GitKeep.class, type, attribute, current);
		// TODO: create an interface IReplicator as an abstraction of this set.
		BeanUtils.setProperty(current, attribute, data);
		return write(dir, current);
	}

	protected <A extends Annotation, T> void validateAttribute(Class<A> annotation, Class<T> type, String attribute,
			T current) {
		PairValue<A>[] values = UtilAnnotations.getValues(annotation, type, current);
		for (PairValue<A> c : values) {
			if (c.getName().equalsIgnoreCase(attribute)) {
				throw new GitStorageException("Update of @" + annotation.getSimpleName() + " annotated attribute '"
						+ c.getName() + "' is not allowed.", null);
			}
		}
	}

	@Override
	@SneakyThrows
	public <T> T setResource(File dir, Class<T> type, Resource resource, Object... keys) {
		File root = resourceDir(entityDir(dir, type, keys));

		ResourceMetadata metadata = resource.getMetadata();

		File contentFile = new File(root, metadata.getPath());
		// SECURITY: avoid attempt to override files in higher locations as /etc
		if (!contentFile.getCanonicalPath().startsWith(root.getCanonicalPath())) {
			throw new GitStorageException("Cannot save resources in a higher file structure. " + metadata.getPath(),
					null);
		}
		FileUtils.prepare(contentFile);
		Files.write(contentFile.toPath(), resource.getContent().getData(), StandardOpenOption.CREATE,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

		File metadataFile = resourceMeta(root, metadata.getPath());
		FileUtils.prepare(metadataFile);
		metadata.setTimestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(contentFile.lastModified()),
				TimeZone.getDefault().toZoneId()));
		Files.write(metadataFile.toPath(), serializer.encode(metadata).getBytes(), StandardOpenOption.CREATE,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

		// force change flags like revision and updated
		T result = merge(dir, type, read(dir, type, keys), keys);

		if (log.isInfoEnabled()) {
			log.info("Resource written: " + metadata);
		}

		return result;
	}

	protected File resourceDir(File root) {
		return new File(root, "@resources");
	}

	protected File resourceMeta(File root, String path) {
		return new File(root, path + ".meta.json");
	}

	@Override
	public <T> T read(File dir, Class<T> type, T reference) {
		return read(dir, type, UtilAnnotations.getKeys(type, reference));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T read(File dir, T reference) {
		Class<T> type = (Class<T>) reference.getClass();
		return read(dir, type, UtilAnnotations.getKeys(type, reference));
	}

	@Override
	public <T> T read(File dir, Class<T> type, Object... keys) {
		return read(entityFile(dir, type, keys), type);
	}

	public <T> T read(File file, Class<T> type) {
		return serializer.readValue(file, type);
	}

	@Override
	@SneakyThrows
	public <T> Object getAttribute(File dir, Class<T> type, String attribute, Object... keys) {
		if (!exists(dir, type, keys)) {
			throw new GitStorageException(
					"Object '" + type.getSimpleName() + "' with keys '" + Arrays.toString(keys) + "' not found.", null);
		}
		T obj = read(dir, type, keys);
		PropertyDescriptor pd = PropertyUtils.getPropertyDescriptor(obj, attribute);
		if (pd == null) {
			throw new GitStorageException("Attribute '" + attribute + "' not found for type: " + obj.getClass(), null);
		}
		return pd.getReadMethod().invoke(obj);
	}

	@Override
	@SneakyThrows
	public <T> Resource getResource(File dir, Class<T> type, String path, Object... keys) {
		File root = resourceDir(entityDir(dir, type, keys));
		if (!root.exists()) {
			throw new GitStorageException("Resources for " + Arrays.toString(keys) + " not found.", null);
		}
		File contentFile = new File(root, path);
		// SECURITY: avoid attempt to override files in higher locations as /etc
		if (!contentFile.getCanonicalPath().startsWith(root.getCanonicalPath())) {
			throw new GitStorageException("Cannot read resources from a higher file structure. " + path, null);
		}
		ResourceContent content = new ResourceContent(Files.readAllBytes(contentFile.toPath()));
		File metadataFile = resourceMeta(root, path);
		ResourceMetadata meta = serializer.decode(Files.readAllBytes(metadataFile.toPath()), ResourceMetadata.class);
		return Resource.builder().metadata(meta).content(content).build();
	}

	@Override
	@SneakyThrows
	public <T> List<Resource> allResources(File dir, Class<T> type, Object... keys) {
		File root = resourceDir(entityDir(dir, type, keys));
		if (!root.exists()) {
			throw new GitStorageException("Resources for " + Arrays.toString(keys) + " not found.", null);
		}
		List<Resource> result = new LinkedList<>();
		Files.walkFileTree(Paths.get(root.toURI()), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path contentFile, BasicFileAttributes attrs) throws IOException {
				File file = contentFile.toFile();
				String name = file.getName();
				if (!name.endsWith(".meta.json")) {
					File metadataFile = resourceMeta(file.getParentFile(), name);
					if (log.isInfoEnabled()) {
						log.info("Loading..." + contentFile);
					}
					ResourceMetadata metadata = serializer.decode(Files.readAllBytes(metadataFile.toPath()),
							ResourceMetadata.class);
					ResourceContent content = ResourceContent.builder().data(Files.readAllBytes(contentFile)).build();
					result.add(Resource.builder().metadata(metadata).content(content).build());
				}
				return FileVisitResult.CONTINUE;
			}
		});
		result.sort((a, b) -> a.getMetadata().getPath().compareTo(b.getMetadata().getPath()));
		return result;
	}

	@Override
	public <T> T delete(File dir, Class<T> type, T reference) {
		return delete(dir, type, UtilAnnotations.getKeys(type, reference));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T delete(File dir, T reference) {
		Class<T> type = (Class<T>) reference.getClass();
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
		File[] ids = idManager.directory(entityRoot(dir, type), IGitIndex.IDS).listFiles();
		if (ids != null) {
			for (File f : ids) {
				Object[] keys = Files.readAllLines(f.toPath()).toArray(new Object[0]);
				result.add(serializer.readValue(entityFile(dir, type, keys), type));
			}
		}
		return result;
	}

	@Override
	public <T> long count(File dir, Class<T> type) {
		return idManager.directory(entityRoot(dir, type), IGitIndex.IDS).listFiles().length;
	}

	@Override
	public <T> List<T> search(File dir, Class<T> type, String query) {
		List<T> all = all(dir, type);
		Predicate<Object> p = predicateFactory.read(query.getBytes());
		return all.stream().filter(p).collect(Collectors.toList());
	}
}