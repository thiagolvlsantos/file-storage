package io.github.thiagolvlsantos.file.storage.impl;

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
import java.util.Comparator;
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
import org.apache.commons.collections.comparators.ComparatorChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import io.github.thiagolvlsantos.file.storage.FileEntity;
import io.github.thiagolvlsantos.file.storage.FilePaging;
import io.github.thiagolvlsantos.file.storage.FileParams;
import io.github.thiagolvlsantos.file.storage.FilePredicate;
import io.github.thiagolvlsantos.file.storage.FileSorting;
import io.github.thiagolvlsantos.file.storage.IFileIndex;
import io.github.thiagolvlsantos.file.storage.IFileSerializer;
import io.github.thiagolvlsantos.file.storage.IFileStorage;
import io.github.thiagolvlsantos.file.storage.annotations.FileKeep;
import io.github.thiagolvlsantos.file.storage.annotations.PairValue;
import io.github.thiagolvlsantos.file.storage.annotations.UtilAnnotations;
import io.github.thiagolvlsantos.file.storage.audit.FileChanged;
import io.github.thiagolvlsantos.file.storage.audit.FileCreated;
import io.github.thiagolvlsantos.file.storage.audit.IFileInitializer;
import io.github.thiagolvlsantos.file.storage.concurrency.FileRevision;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageAttributeNotFoundException;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageException;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageNotFoundException;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageResourceNotFoundException;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageSecurityException;
import io.github.thiagolvlsantos.file.storage.identity.FileId;
import io.github.thiagolvlsantos.file.storage.identity.FileKey;
import io.github.thiagolvlsantos.file.storage.resource.Resource;
import io.github.thiagolvlsantos.file.storage.resource.ResourceContent;
import io.github.thiagolvlsantos.file.storage.resource.ResourceMetadata;
import io.github.thiagolvlsantos.file.storage.util.comparator.ComparatorNullSafe;
import io.github.thiagolvlsantos.git.commons.file.FileUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FileStorageImpl implements IFileStorage {

	private @Autowired IFileSerializer serializer;
	private @Autowired IFileIndex idManager;

	@Override
	public IFileSerializer getSerializer() {
		return serializer;
	}

	// +------------- ENTITY METHODS ------------------+

	@SuppressWarnings("unchecked")
	@Override
	public <T> File location(File dir, T example) {
		return location(dir, (Class<T>) example.getClass(), example);
	}

	@Override
	public <T> File location(File dir, Class<T> type, T example) {
		return location(dir, type, FileParams.of(UtilAnnotations.getKeys(type, example)));
	}

	@Override
	public <T> File location(File dir, Class<T> type, FileParams ref) {
		return entityDir(dir, type, ref);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> boolean exists(File dir, T example) {
		return exists(dir, (Class<T>) example.getClass(), example);
	}

	@Override
	public <T> boolean exists(File dir, Class<T> type, T example) {
		return exists(dir, type, FileParams.of(UtilAnnotations.getKeys(type, example)));
	}

	@Override
	public <T> boolean exists(File dir, Class<T> type, FileParams keys) {
		return entityDir(dir, type, keys).exists();
	}

	protected <T> File entityDir(File dir, Class<T> type, FileParams keys) {
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
		FileEntity entity = AnnotationUtils.findAnnotation(type, FileEntity.class);
		if (log.isDebugEnabled()) {
			log.debug("entity: {}", entity);
		}
		if (entity == null) {
			throw new FileStorageException("Entity is not annotated with @FileEntity.", null);
		}
		return new File(dir, "@" + entity.value());
	}

	@Override
	@SneakyThrows
	public <T> T write(File dir, Class<T> type, T instance) {
		FileParams keys = FileParams.of(UtilAnnotations.getKeys(type, instance));
		File file = entityFile(dir, type, keys);
		T old = null;
		if (file.exists()) {
			old = read(file, type);
		}

		prepareCreated(dir, type, instance, file, old);
		prepareRevisions(dir, type, instance, file, old);
		prepareChanged(dir, type, instance, file, old);

		write(instance, file);

		// init @resources
		initResources(dir, type, keys);

		return instance;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T write(File dir, T instance) {
		return write(dir, (Class<T>) instance.getClass(), instance);
	}

	protected <T> File entityFile(File dir, Class<T> type, FileParams keys) {
		return new File(entityDir(dir, type, keys), "meta.json");
	}

	protected <T> void prepareCreated(File dir, Class<T> type, T instance, File target, T old) {
		PairValue<FileId>[] ids = UtilAnnotations.getValues(FileId.class, type, instance);
		if (log.isInfoEnabled()) {
			log.info("ids: {}", Arrays.toString(ids));
		}
		PairValue<FileCreated>[] created = UtilAnnotations.getValues(FileCreated.class, type, instance);
		if (log.isInfoEnabled()) {
			log.info("created: {}", Arrays.toString(created));
		}
		if (!target.exists()) {
			File parent = target.getParentFile();
			if (!parent.mkdirs()) {
				throw new FileStorageException("Could not create object directory: " + parent, null);
			}
			initializeFixed(dir, type, instance, ids, created);
		} else {
			keepFixed(old, ids, created, instance);
		}
	}

	@SneakyThrows
	protected Object value(Object instance, String name, Class<? extends IFileInitializer> initializer, Method m) {
		IFileInitializer factory = initializer.getConstructor().newInstance();
		return factory.value(instance, name, m.getReturnType());
	}

	protected <T> void initializeFixed(File dir, Class<T> type, T instance, PairValue<FileId>[] ids,
			PairValue<FileCreated>[] created) {
		for (PairValue<FileCreated> c : created) {
			Object obj = c.get(instance);
			if (obj == null) {
				c.set(instance, value(instance, c.getName(), c.getAnnotation().value(), c.getRead()));
				if (log.isInfoEnabled()) {
					log.info("new created: {}", c.get(instance));
				}
			}
		}
		for (PairValue<FileId> c : ids) {
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

	protected <T> void keepFixed(T old, PairValue<FileId>[] ids, PairValue<FileCreated>[] created, T instance) {
		for (PairValue<FileCreated> c : created) {
			Object obj = c.get(old);
			c.set(instance, obj);
			if (log.isInfoEnabled()) {
				log.info("keep created: {}", c.get(instance));
			}
		}
		for (PairValue<FileId> c : ids) {
			Object obj = c.get(old);
			c.set(instance, obj);
			if (log.isInfoEnabled()) {
				log.info("keep ids: {}", c.get(instance));
			}
		}
	}

	protected <T> void prepareRevisions(File dir, Class<T> type, T instance, File target, T old) {
		PairValue<FileRevision>[] revisions = UtilAnnotations.getValues(FileRevision.class, type, instance);
		if (log.isInfoEnabled()) {
			log.info("revisions: {}", Arrays.toString(revisions));
		}
		for (PairValue<FileRevision> c : revisions) {
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
					throw new FileStorageException("Invalid revision. Reload object and try again.", null);
				}
				c.set(instance, current.longValue() + 1);
			}
			if (log.isInfoEnabled()) {
				log.info("new revision: {}", c.get(instance));
			}
		}
	}

	protected <T> void prepareChanged(File dir, Class<T> type, T instance, File target, T old) {
		PairValue<FileChanged>[] changed = UtilAnnotations.getValues(FileChanged.class, type, instance);
		if (log.isInfoEnabled()) {
			log.info("changed: {}", Arrays.toString(changed));
		}
		for (PairValue<FileChanged> c : changed) {
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
	public <T> T merge(File dir, Class<T> type, FileParams keys, T instance) {
		verifyExists(dir, type, keys);
		// old objects
		T current = read(dir, type, keys);
		PairValue<FileId>[] currentIds = UtilAnnotations.getValues(FileId.class, type, current);
		PairValue<FileKey>[] currentKeys = UtilAnnotations.getValues(FileKey.class, type, current);
		PairValue<FileCreated>[] currentCreated = UtilAnnotations.getValues(FileCreated.class, type, current);
		PairValue<FileRevision>[] currentRevision = UtilAnnotations.getValues(FileRevision.class, type, current);
		PairValue<FileKeep>[] currentKeep = UtilAnnotations.getValues(FileKeep.class, type, current);
		// new object
		// why not create an interface IReplicator as an abstraction of this copy?
		BeanUtils.copyProperties(current, instance);
		// return unchangeable attributes
		reassignAttributes(FileId.class, current, currentIds);
		reassignAttributes(FileKey.class, current, currentKeys);
		reassignAttributes(FileCreated.class, current, currentCreated);
		reassignAttributes(FileRevision.class, current, currentRevision);
		reassignAttributes(FileKeep.class, current, currentKeep);
		// write resulting object
		return write(dir, current);
	}

	protected <T> void verifyExists(File dir, Class<T> type, FileParams keys) {
		if (!exists(dir, type, keys)) {
			throw new FileStorageNotFoundException(
					"Object '" + type.getSimpleName() + "' with keys '" + keys + "' not found.", null);
		}
	}

	protected <A extends Annotation, T> void reassignAttributes(Class<A> annotation, T current, PairValue<A>[] pairs)
			throws IllegalAccessException, InvocationTargetException {
		for (PairValue<A> c : pairs) {
			if (log.isInfoEnabled()) {
				log.info("Return " + annotation.getSimpleName() + ": {}={}", c.getName(), c.getValue());
			}
			trySetAttribute(current, c.getName(), c.getValue());
		}
	}

	protected <T> void trySetAttribute(T current, String name, Object value)
			throws IllegalAccessException, InvocationTargetException {
		try {
			PropertyUtils.setProperty(current, name, value);
		} catch (NoSuchMethodException e) {
			throw new FileStorageAttributeNotFoundException(name, current, e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T read(File dir, T example) {
		return read(dir, (Class<T>) example.getClass(), example);
	}

	@Override
	public <T> T read(File dir, Class<T> type, T example) {
		return read(dir, type, FileParams.of(UtilAnnotations.getKeys(type, example)));
	}

	@Override
	public <T> T read(File dir, Class<T> type, FileParams keys) {
		return read(entityFile(dir, type, keys), type);
	}

	protected <T> T read(File file, Class<T> type) {
		return serializer.readValue(file, type);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T delete(File dir, T example) {
		return delete(dir, (Class<T>) example.getClass(), example);
	}

	@Override
	public <T> T delete(File dir, Class<T> type, T example) {
		return delete(dir, type, FileParams.of(UtilAnnotations.getKeys(type, example)));
	}

	@Override
	public <T> T delete(File dir, Class<T> type, FileParams keys) {
		T old = null;
		if (exists(dir, type, keys)) {
			old = read(dir, type, keys);
			File file = entityDir(dir, type, keys);
			try {
				FileUtils.delete(file); // remove all resources also
			} catch (IOException e) {
				throw new FileStorageException("Entity not deleted. File:" + file, e);
			}
			idManager.unbind(entityRoot(dir, type), old);
		}
		return old;
	}

	@Override
	public <T> long count(File dir, Class<T> type, FilePredicate filter, FilePaging paging) {
		return list(dir, type, filter, paging, null).size();
	}

	@Override
	public <T> List<T> list(File dir, Class<T> type, FilePredicate filter, FilePaging paging, FileSorting sorting) {
		return range(paging, filter(filter, sort(sorting, all(dir, type, null))));
	}

	@SneakyThrows
	protected <T> List<T> all(File dir, Class<T> type, FilePaging paging) {
		List<T> result = new LinkedList<>();
		File[] ids = idManager.directory(entityRoot(dir, type), IFileIndex.IDS).listFiles();
		if (ids != null) {
			for (File f : ids) {
				Object[] keys = Files.readAllLines(f.toPath()).toArray(new Object[0]);
				try {
					result.add(serializer.readValue(entityFile(dir, type, FileParams.of(keys)), type));
				} catch (Throwable e) {
					if (log.isErrorEnabled()) {
						log.error("Could not read object for keys: " + Arrays.toString(keys) + ", check file system.",
								e);
					}
				}
			}
		}
		return range(paging, result);
	}

	@SuppressWarnings("unchecked")
	protected <T> List<T> sort(FileSorting sorting, List<T> result) {
		if (sorting != null) {
			List<Comparator<T>> comparators = new LinkedList<>();
			append(sorting, FileSorting::isValid, comparators);
			List<FileSorting> secondary = sorting.getSecondary();
			if (secondary != null) {
				for (FileSorting s : secondary) {
					append(s, o -> o.isValid() && !o.isSameProperty(sorting), comparators);
				}
			}
			if (!comparators.isEmpty()) {
				Collections.sort(result, new ComparatorChain(comparators));
			}
		}
		return result;
	}

	protected <T> void append(FileSorting sorting, Predicate<FileSorting> test, List<Comparator<T>> list) {
		if (test.test(sorting)) {
			Comparator<T> tmp = comparator(sorting);
			if (sorting.isDescending()) {
				tmp = tmp.reversed();
			}
			list.add(tmp);
		}
	}

	protected <T> ComparatorNullSafe<T> comparator(FileSorting sorting) {
		return new ComparatorNullSafe<>(sorting.getProperty(), sorting.isNullsFirst());
	}

	protected <T> List<T> filter(FilePredicate filter, List<T> result) {
		Predicate<Object> p = filter(filter);
		if (p != null) {
			result = result.stream().filter(p).collect(Collectors.toList());
		}
		return result;
	}

	protected Predicate<Object> filter(FilePredicate filter) {
		return filter == null ? null : filter.getFilter();
	}

	protected <T> List<T> range(FilePaging paging, List<T> result) {
		FilePaging page = Optional.ofNullable(paging).orElse(FilePaging.builder().build());
		Integer start = page.getStart(result.size());
		Integer end = page.getEnd(result.size());
		return start < end ? result.subList(start, end) : Collections.emptyList();
	}

	// +------------- ATTRIBUTE METHODS ------------------+

	@Override
	@SneakyThrows
	public <T> T setAttribute(File dir, Class<T> type, FileParams keys, String attribute, Object data) {
		verifyExists(dir, type, keys);

		T current = read(dir, type, keys);

		// check unchangeable attributes
		validateAttribute(FileId.class, type, attribute, current);
		validateAttribute(FileKey.class, type, attribute, current);
		validateAttribute(FileCreated.class, type, attribute, current);
		validateAttribute(FileRevision.class, type, attribute, current);
		validateAttribute(FileKeep.class, type, attribute, current);

		trySetAttribute(current, attribute, data);

		return write(dir, current);
	}

	protected <A extends Annotation, T> void validateAttribute(Class<A> annotation, Class<T> type, String attribute,
			T current) {
		PairValue<A>[] values = UtilAnnotations.getValues(annotation, type, current);
		for (PairValue<A> c : values) {
			if (c.getName().equalsIgnoreCase(attribute)) {
				throw new FileStorageException("Update of @" + annotation.getSimpleName() + " annotated attribute '"
						+ c.getName() + "' is not allowed.", null);
			}
		}
	}

	@Override
	@SneakyThrows
	public <T> Object getAttribute(File dir, Class<T> type, FileParams keys, String attribute) {
		verifyExists(dir, type, keys);

		T current = read(dir, type, keys);

		return tryGetAttribute(current, attribute);
	}

	protected <T> Object tryGetAttribute(T current, String attribute)
			throws IllegalAccessException, InvocationTargetException {
		try {
			return PropertyUtils.getProperty(current, attribute);
		} catch (NoSuchMethodException e) {
			throw new FileStorageAttributeNotFoundException(attribute, current, e);
		}
	}

	@Override
	@SneakyThrows
	public <T> Map<String, Object> attributes(File dir, Class<T> type, FileParams keys, FileParams names) {
		verifyExists(dir, type, keys);

		T current = read(dir, type, keys);

		FileParams selection = names;
		if (selection == null) {
			PropertyDescriptor[] pds = PropertyUtils.getPropertyDescriptors(current);
			selection = FileParams.of(Stream.of(pds).map(PropertyDescriptor::getName).collect(Collectors.toList()));
		}

		Map<String, Object> result = new LinkedHashMap<>();
		for (Object n : selection) {
			String attribute = String.valueOf(n);
			result.put(attribute, tryGetAttribute(current, attribute));
		}
		return result;
	}

	// +------------- RESOURCE METHODS ------------------+

	protected <T> void initResources(File dir, Class<T> type, FileParams keys) throws IOException {
		File resourceDir = resourceDir(entityDir(dir, type, keys));
		if (!resourceDir.exists()) {
			boolean created = resourceDir.mkdirs();
			if (created) {
				File keep = new File(resourceDir, ".keep");
				Files.write(keep.toPath(), "Forcing directory existence.".getBytes(), StandardOpenOption.CREATE,
						StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
			}
			if (log.isInfoEnabled()) {
				log.info("Resources created={}", created);
			}
		}
	}

	@Override
	@SneakyThrows
	public <T> File locationResource(File dir, Class<T> type, FileParams keys, String path) {
		verifyExists(dir, type, keys);
		File root = resourceDir(entityDir(dir, type, keys));
		if (path != null) {
			File contentFile = new File(root, path);
			// SECURITY: avoid attempt to override files in higher locations as /etc
			if (!contentFile.getCanonicalPath().startsWith(root.getCanonicalPath())) {
				throw new FileStorageException("Cannot read location of resources in a higher file structure. " + path,
						null);
			}
			root = new File(root, path);
		}
		return root;
	}

	@Override
	@SneakyThrows
	public <T> T setResource(File dir, Class<T> type, FileParams keys, Resource resource) {
		verifyExists(dir, type, keys);
		File root = resourceDir(entityDir(dir, type, keys));

		ResourceMetadata metadata = resource.getMetadata();
		String path = metadata.getPath();
		File contentFile = new File(root, path);
		verifySecurity(root, contentFile, path);

		FileUtils.prepare(contentFile);
		Files.write(contentFile.toPath(), resource.getContent().getData(), StandardOpenOption.CREATE,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

		File metadataFile = resourceMeta(root, path);
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

	protected File resourceDir(File entityDir) {
		return new File(entityDir, "@resources");
	}

	protected File resourceMeta(File entityDir, String path) {
		return new File(entityDir, path + ".meta.json");
	}

	@Override
	@SneakyThrows
	public <T> Resource getResource(File dir, Class<T> type, FileParams keys, String path) {
		verifyExists(dir, type, keys);
		File root = resourceDir(entityDir(dir, type, keys));
		verifyResources(root, keys);

		File contentFile = new File(root, path);
		verifySecurity(root, contentFile, path);
		verifyResourceExists(contentFile, path);

		ResourceContent content = new ResourceContent(Files.readAllBytes(contentFile.toPath()));
		File metadataFile = resourceMeta(root, path);
		ResourceMetadata meta = serializer.decode(Files.readAllBytes(metadataFile.toPath()), ResourceMetadata.class);
		return Resource.builder().metadata(meta).content(content).build();
	}

	protected void verifyResources(File root, FileParams keys) {
		if (!root.exists()) {
			throw new FileStorageNotFoundException("Resources for " + keys + " not found.", null);
		}
	}

	protected void verifySecurity(File root, File contentFile, String path) throws IOException {
		// SECURITY: avoid attempt to override files in higher locations as /etc
		if (!contentFile.getCanonicalPath().startsWith(root.getCanonicalPath())) {
			throw new FileStorageSecurityException(path, null);
		}
	}

	protected void verifyResourceExists(File contentFile, String path) {
		if (!contentFile.exists()) {
			throw new FileStorageResourceNotFoundException(path, null);
		}
	}

	@Override
	public <T> long countResources(File dir, Class<T> type, FileParams keys, FilePredicate filter, FilePaging paging) {
		return listResources(dir, type, keys, filter, paging, null).size();
	}

	@Override
	@SneakyThrows
	public <T> List<Resource> listResources(File dir, Class<T> type, FileParams keys, FilePredicate filter,
			FilePaging paging, FileSorting sorting) {
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
				if (!name.endsWith(".meta.json") && !name.equals(".keep")) {
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
		if (sorting == null) {
			result.sort(new ComparatorNullSafe<>("metadata.path", false));
		}
		return range(paging, sort(sorting, result));
	}

	@Override
	@SneakyThrows
	public <T> T deleteResource(File dir, Class<T> type, FileParams keys, String path) {
		verifyExists(dir, type, keys);
		File root = resourceDir(entityDir(dir, type, keys));
		verifyResources(root, keys);

		File contentFile = new File(root, path);
		verifySecurity(root, contentFile, path);
		verifyResourceExists(contentFile, path);

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