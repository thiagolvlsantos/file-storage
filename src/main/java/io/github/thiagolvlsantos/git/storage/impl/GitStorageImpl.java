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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import io.github.thiagolvlsantos.git.commons.file.FileUtils;
import io.github.thiagolvlsantos.git.storage.GitEntity;
import io.github.thiagolvlsantos.git.storage.GitFilter;
import io.github.thiagolvlsantos.git.storage.GitPaging;
import io.github.thiagolvlsantos.git.storage.GitParams;
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
import io.github.thiagolvlsantos.git.storage.exceptions.GitStorageNotFoundException;
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

	// +------------- ENTITY METHODS ------------------+

	@SuppressWarnings("unchecked")
	@Override
	public <T> File location(File dir, T example) {
		Class<T> type = (Class<T>) example.getClass();
		return location(dir, type, GitParams.of(UtilAnnotations.getKeys(type, example)));
	}

	@Override
	public <T> File location(File dir, Class<T> type, T example) {
		return location(dir, type, GitParams.of(UtilAnnotations.getKeys(type, example)));
	}

	@Override
	public <T> File location(File dir, Class<T> type, GitParams ref) {
		return entityDir(dir, type, ref);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> boolean exists(File dir, T example) {
		Class<T> type = (Class<T>) example.getClass();
		return exists(dir, type, GitParams.of(UtilAnnotations.getKeys(type, example)));
	}

	@Override
	public <T> boolean exists(File dir, Class<T> type, T example) {
		return exists(dir, type, GitParams.of(UtilAnnotations.getKeys(type, example)));
	}

	@Override
	public <T> boolean exists(File dir, Class<T> type, GitParams keys) {
		return entityDir(dir, type, keys).exists();
	}

	protected <T> File entityDir(File dir, Class<T> type, GitParams keys) {
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
		File file = entityFile(dir, type, GitParams.of(UtilAnnotations.getKeys(type, instance)));
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

	protected <T> File entityFile(File dir, Class<T> type, GitParams keys) {
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
	public <T> T merge(File dir, Class<T> type, GitParams keys, T instance) {
		verifyExists(dir, type, keys);
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

	protected <T> void verifyExists(File dir, Class<T> type, GitParams keys) {
		if (!exists(dir, type, keys)) {
			throw new GitStorageNotFoundException(
					"Object '" + type.getSimpleName() + "' with keys '" + keys + "' not found.", null);
		}
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
	public <T> T read(File dir, Class<T> type, T reference) {
		return read(dir, type, GitParams.of(UtilAnnotations.getKeys(type, reference)));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T read(File dir, T reference) {
		Class<T> type = (Class<T>) reference.getClass();
		return read(dir, type, GitParams.of(UtilAnnotations.getKeys(type, reference)));
	}

	@Override
	public <T> T read(File dir, Class<T> type, GitParams keys) {
		return read(entityFile(dir, type, keys), type);
	}

	protected <T> T read(File file, Class<T> type) {
		return serializer.readValue(file, type);
	}

	@Override
	public <T> T delete(File dir, Class<T> type, T reference) {
		return delete(dir, type, GitParams.of(UtilAnnotations.getKeys(type, reference)));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T delete(File dir, T reference) {
		Class<T> type = (Class<T>) reference.getClass();
		return delete(dir, type, GitParams.of(UtilAnnotations.getKeys(type, reference)));
	}

	@Override
	public <T> T delete(File dir, Class<T> type, GitParams keys) {
		T old = null;
		if (exists(dir, type, keys)) {
			old = read(dir, type, keys);
			File file = entityDir(dir, type, keys);
			try {
				FileUtils.delete(file); // remove all resources also
			} catch (IOException e) {
				throw new GitStorageException("Entity not deleted. File:" + file, e);
			}
			idManager.unbind(entityRoot(dir, type), old);
		}
		return old;
	}

	@Override
	public <T> long count(File dir, Class<T> type, GitPaging paging) {
		File[] files = idManager.directory(entityRoot(dir, type), IGitIndex.IDS).listFiles();
		GitPaging page = Optional.ofNullable(paging).orElse(GitPaging.builder().build());
		return page.getEnd(files.length) - page.getStart(files.length);
	}

	@Override
	public <T> long count(File dir, Class<T> type, GitFilter filter, GitPaging paging) {
		return list(dir, type, filter, paging).size();
	}

	@Override
	@SneakyThrows
	public <T> List<T> list(File dir, Class<T> type, GitPaging paging) {
		List<T> result = new LinkedList<>();
		File[] ids = idManager.directory(entityRoot(dir, type), IGitIndex.IDS).listFiles();
		if (ids != null) {
			for (File f : ids) {
				Object[] keys = Files.readAllLines(f.toPath()).toArray(new Object[0]);
				result.add(serializer.readValue(entityFile(dir, type, GitParams.of(keys)), type));
			}
		}
		return selectRange(paging, result);
	}

	@Override
	public <T> List<T> list(File dir, Class<T> type, GitFilter filter, GitPaging paging) {
		List<T> result = list(dir, type, null);
		result = filter(filter, result);
		return selectRange(paging, result);
	}

	protected <T> List<T> filter(GitFilter filter, List<T> result) {
		Predicate<Object> p = filter(filter);
		if (p != null) {
			result = result.stream().filter(p).collect(Collectors.toList());
		}
		return result;
	}

	protected Predicate<Object> filter(GitFilter filter) {
		return filter == null ? null : predicateFactory.read(filter.getFilter().getBytes());
	}

	protected <T> List<T> selectRange(GitPaging paging, List<T> result) {
		GitPaging page = Optional.ofNullable(paging).orElse(GitPaging.builder().build());
		Integer start = page.getStart(result.size());
		Integer end = page.getEnd(result.size());
		return start < end ? result.subList(start, end) : Collections.emptyList();
	}

	// +------------- ATTRIBUTE METHODS ------------------+

	@Override
	@SneakyThrows
	public <T> T setAttribute(File dir, Class<T> type, GitParams keys, String attribute, Object data) {
		verifyExists(dir, type, keys);

		T current = read(dir, type, keys);

		// check unchangeable attributes
		validateAttribute(GitId.class, type, attribute, current);
		validateAttribute(GitKey.class, type, attribute, current);
		validateAttribute(GitCreated.class, type, attribute, current);
		validateAttribute(GitRevision.class, type, attribute, current);
		validateAttribute(GitKeep.class, type, attribute, current);
		// TODO: create an interface IReplicator as an abstraction of this set.
		try {
			PropertyUtils.setProperty(current, attribute, data);
		} catch (NoSuchMethodException e) {
			throw new GitStorageNotFoundException(
					"Attribute '" + attribute + "' not found for type: " + current.getClass(), e);
		}

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
	public <T> Object getAttribute(File dir, Class<T> type, GitParams keys, String attribute) {
		verifyExists(dir, type, keys);

		T current = read(dir, type, keys);

		try {
			return PropertyUtils.getProperty(current, attribute);
		} catch (NoSuchMethodException e) {
			throw new GitStorageNotFoundException(
					"Attribute '" + attribute + "' not found for type: " + current.getClass(), e);
		}
	}

	@Override
	@SneakyThrows
	public <T> Map<String, Object> attributes(File dir, Class<T> type, GitParams keys, GitParams names) {
		verifyExists(dir, type, keys);

		T current = read(dir, type, keys);

		GitParams selection = names;
		if (selection == null) {
			PropertyDescriptor[] pds = PropertyUtils.getPropertyDescriptors(current);
			selection = GitParams.of(Stream.of(pds).map(p -> p.getName()).collect(Collectors.toList()));
		}

		Map<String, Object> result = new LinkedHashMap<>();
		for (Object n : selection) {
			try {
				String attribute = String.valueOf(n);
				Object value = PropertyUtils.getProperty(current, attribute);
				result.put(attribute, value);
			} catch (NoSuchMethodException e) {
				throw new GitStorageNotFoundException("Attribute '" + n + "' not found for type: " + current.getClass(),
						e);
			}
		}
		return result;
	}

	// +------------- RESOURCE METHODS ------------------+

	@Override
	public <T> File locationResource(File dir, Class<T> type, GitParams keys) {
		return locationResource(dir, type, keys, null);
	}

	@Override
	@SneakyThrows
	public <T> File locationResource(File dir, Class<T> type, GitParams keys, String path) {
		verifyExists(dir, type, keys);
		File root = resourceDir(entityDir(dir, type, keys));
		if (path != null) {
			File contentFile = new File(root, path);
			// SECURITY: avoid attempt to override files in higher locations as /etc
			if (!contentFile.getCanonicalPath().startsWith(root.getCanonicalPath())) {
				throw new GitStorageException("Cannot read location of resources in a higher file structure. " + path,
						null);
			}
			root = new File(root, path);
		}
		return root;
	}

	@Override
	@SneakyThrows
	public <T> T setResource(File dir, Class<T> type, GitParams keys, Resource resource) {
		verifyExists(dir, type, keys);
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
		T result = merge(dir, type, keys, read(dir, type, keys));

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
	@SneakyThrows
	public <T> Resource getResource(File dir, Class<T> type, GitParams keys, String path) {
		verifyExists(dir, type, keys);
		File root = resourceDir(entityDir(dir, type, keys));
		verifyResources(root, keys);

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

	protected void verifyResources(File root, GitParams keys) {
		if (!root.exists()) {
			throw new GitStorageNotFoundException("Resources for " + keys + " not found.", null);
		}
	}

	@Override
	public <T> long countResources(File dir, Class<T> type, GitParams keys) {
		return listResources(dir, type, keys).size();
	}

	@Override
	public <T> long countResources(File dir, Class<T> type, GitParams keys, GitPaging paging) {
		return listResources(dir, type, keys, paging).size();
	}

	@Override
	public <T> long countResources(File dir, Class<T> type, GitParams keys, GitFilter filter, GitPaging paging) {
		return listResources(dir, type, keys, filter, paging).size();
	}

	@Override
	@SneakyThrows
	public <T> List<Resource> listResources(File dir, Class<T> type, GitParams keys) {
		return listResources(dir, type, keys, null);
	}

	@Override
	@SneakyThrows
	public <T> List<Resource> listResources(File dir, Class<T> type, GitParams keys, GitPaging paging) {
		return listResources(dir, type, keys, null, paging);
	}

	@Override
	@SneakyThrows
	public <T> List<Resource> listResources(File dir, Class<T> type, GitParams keys, GitFilter filter,
			GitPaging paging) {
		verifyExists(dir, type, keys);
		File root = resourceDir(entityDir(dir, type, keys));
		verifyResources(root, keys);

		final Predicate<Object> predicate = filter(filter);

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
					Resource resource = Resource.builder().metadata(metadata).content(content).build();
					if (predicate != null) {
						if (predicate.test(resource)) {
							result.add(resource);
						}
					} else {
						result.add(resource);
					}
				}
				return FileVisitResult.CONTINUE;
			}
		});
		result.sort((a, b) -> a.getMetadata().getPath().compareTo(b.getMetadata().getPath()));
		return selectRange(paging, result);
	}

	@Override
	@SneakyThrows
	public <T> T deleteResource(File dir, Class<T> type, GitParams keys, String path) {
		verifyExists(dir, type, keys);
		File root = resourceDir(entityDir(dir, type, keys));
		verifyResources(root, keys);

		File contentFile = new File(root, path);
		// SECURITY: avoid attempt to override files in higher locations as /etc
		if (!contentFile.getCanonicalPath().startsWith(root.getCanonicalPath())) {
			throw new GitStorageException("Cannot delete resources from a higher file structure. " + path, null);
		}
		File metadataFile = resourceMeta(root, path);

		FileUtils.delete(contentFile);
		FileUtils.delete(metadataFile);

		// force change flags like revision and updated
		T result = merge(dir, type, keys, read(dir, type, keys));

		if (log.isInfoEnabled()) {
			log.info("Resource deleted: " + path);
		}

		return result;
	}
}