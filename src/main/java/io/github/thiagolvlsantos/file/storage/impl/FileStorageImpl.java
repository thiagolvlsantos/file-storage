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
import java.util.ArrayList;
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

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import io.github.thiagolvlsantos.file.storage.IFileIndex;
import io.github.thiagolvlsantos.file.storage.IFileSerializer;
import io.github.thiagolvlsantos.file.storage.IFileStorage;
import io.github.thiagolvlsantos.file.storage.KeyParams;
import io.github.thiagolvlsantos.file.storage.SearchParams;
import io.github.thiagolvlsantos.file.storage.annotations.FileKeep;
import io.github.thiagolvlsantos.file.storage.annotations.PairValue;
import io.github.thiagolvlsantos.file.storage.annotations.UtilAnnotations;
import io.github.thiagolvlsantos.file.storage.audit.FileChanged;
import io.github.thiagolvlsantos.file.storage.audit.FileChangedBy;
import io.github.thiagolvlsantos.file.storage.audit.FileCreated;
import io.github.thiagolvlsantos.file.storage.audit.FileCreatedBy;
import io.github.thiagolvlsantos.file.storage.audit.IFileAudit;
import io.github.thiagolvlsantos.file.storage.audit.IFileInitializer;
import io.github.thiagolvlsantos.file.storage.audit.impl.FileAuditDefault;
import io.github.thiagolvlsantos.file.storage.audit.impl.FileAuditHelper;
import io.github.thiagolvlsantos.file.storage.audit.impl.FileInitializerDefault;
import io.github.thiagolvlsantos.file.storage.audit.impl.FileInitializerHelper;
import io.github.thiagolvlsantos.file.storage.concurrency.FileRevision;
import io.github.thiagolvlsantos.file.storage.entity.FileName;
import io.github.thiagolvlsantos.file.storage.entity.FileRepo;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageException;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageNotFoundException;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStoragePropertyNotFoundException;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageResourceNotFoundException;
import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageSecurityException;
import io.github.thiagolvlsantos.file.storage.identity.FileId;
import io.github.thiagolvlsantos.file.storage.identity.FileKey;
import io.github.thiagolvlsantos.file.storage.resource.Resource;
import io.github.thiagolvlsantos.file.storage.resource.ResourceContent;
import io.github.thiagolvlsantos.file.storage.resource.ResourceMetadata;
import io.github.thiagolvlsantos.file.storage.search.FileFilter;
import io.github.thiagolvlsantos.file.storage.search.FilePaging;
import io.github.thiagolvlsantos.file.storage.search.FileSorting;
import io.github.thiagolvlsantos.file.storage.util.comparator.ComparatorNullSafe;
import io.github.thiagolvlsantos.git.commons.file.FileUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FileStorageImpl implements IFileStorage {

	private @Autowired ApplicationContext context;
	private @Autowired IFileSerializer serializer;
	private @Autowired IFileIndex idManager;

	@Override
	public IFileSerializer getSerializer() {
		return serializer;
	}

	@Override
	public void setSerializer(IFileSerializer serializer) {
		this.serializer = serializer;
	}

	// +------------- ENTITY METHODS ------------------+

	@SuppressWarnings("unchecked")
	@Override
	public <T> File location(File dir, T example) {
		return location(dir, (Class<T>) example.getClass(), example);
	}

	@Override
	public <T> File location(File dir, Class<T> type, T example) {
		return location(dir, type, KeyParams.of(UtilAnnotations.getKeys(type, example)));
	}

	@Override
	public <T> File location(File dir, Class<T> type, KeyParams ref) {
		return entityDir(dir, type, ref);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> boolean exists(File dir, T example) {
		return exists(dir, (Class<T>) example.getClass(), example);
	}

	@Override
	public <T> boolean exists(File dir, Class<T> type, T example) {
		return exists(dir, type, KeyParams.of(UtilAnnotations.getKeys(type, example)));
	}

	@Override
	public <T> boolean exists(File dir, Class<T> type, KeyParams keys) {
		return entityFile(dir, type, keys).exists();
	}

	protected <T> File entityDir(File dir, Class<T> type, KeyParams keys) {
		File path = entityRoot(dir, type);
		for (Object k : keys) {
			path = new File(path, String.valueOf(k));
		}
		log.debug("path: {}", path);
		return path;
	}

	protected <T> File entityRoot(File dir, Class<T> type) {
		FileRepo entity = AnnotationUtils.findAnnotation(type, FileRepo.class);
		log.debug("entity: {}", entity);
		if (entity == null) {
			throw new FileStorageException(
					"Entity '" + type.getName() + "' is not annotated with @" + FileRepo.class.getSimpleName() + ".",
					null);
		}
		return new File(dir, "@" + entity.value().replace("/", "/@"));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T write(File dir, T instance) {
		return write(dir, (Class<T>) instance.getClass(), instance);
	}

	@Override
	@SneakyThrows
	public <T> T write(File dir, Class<T> type, T instance) {
		KeyParams keys = KeyParams.of(UtilAnnotations.getKeys(type, instance));
		File file = entityFile(dir, type, keys);
		T old = null;
		if (file.exists()) {
			old = read(file, type);
		}

		PairValue<FileId>[] idFields = UtilAnnotations.getValues(FileId.class, type, instance);
		log.info("ids: {}", Arrays.toString(idFields));
		PairValue<FileCreated>[] createdFields = UtilAnnotations.getValues(FileCreated.class, type, instance);
		log.info("created: {}", Arrays.toString(createdFields));
		PairValue<FileCreatedBy>[] createdByFields = UtilAnnotations.getValues(FileCreatedBy.class, type, instance);
		log.info("createdBy: {}", Arrays.toString(createdByFields));
		if (!file.exists()) {
			File parent = file.getParentFile();
			if (!parent.exists() && !parent.mkdirs()) {
				throw new FileStorageException("Could not create object directory: " + parent, null);
			}
			initIds(dir, type, idFields, instance);
			initCreated(dir, type, createdFields, instance);
			initCreatedBy(dir, type, createdByFields, instance);
		} else {
			keepValues(old, idFields, instance);
			keepValues(old, createdFields, instance);
			keepValues(old, createdByFields, instance);

			PairValue<FileKeep>[] keepFields = UtilAnnotations.getValues(FileKeep.class, type, instance);
			log.info("keep: {}", Arrays.toString(keepFields));
			keepValues(old, keepFields, instance);
		}

		PairValue<FileRevision>[] revisions = UtilAnnotations.getValues(FileRevision.class, type, instance);
		log.info("revisions: {}", Arrays.toString(revisions));
		prepareRevisions(dir, type, revisions, instance, old);

		PairValue<FileChanged>[] changed = UtilAnnotations.getValues(FileChanged.class, type, instance);
		log.info("changed: {}", Arrays.toString(changed));
		prepareChanged(dir, type, changed, instance);

		PairValue<FileChangedBy>[] changedBy = UtilAnnotations.getValues(FileChangedBy.class, type, instance);
		log.info("changedBy: {}", Arrays.toString(changedBy));
		prepareChangedBy(dir, type, changedBy, instance);

		writeToFile(file, instance);

		// init @resources
		initResources(dir, type, keys);

		return instance;
	}

	protected <T> File entityFile(File dir, Class<T> type, KeyParams keys) {
		return new File(entityDir(dir, type, keys), serializer.getFile(type));
	}

	protected <T> void initIds(File dir, Class<T> type, PairValue<FileId>[] ids, T instance) {
		for (PairValue<FileId> c : ids) {
			Object obj = c.get(instance);
			if (obj == null) {
				Object nextId = idManager.next(entityRoot(dir, type), type, c);
				c.set(instance, nextId);
				idManager.bind(entityRoot(dir, type), instance);
				log.info("new id: {}", c.get(instance));
			}
		}
	}

	protected <T> void initCreated(File dir, Class<T> type, PairValue<FileCreated>[] created, T instance) {
		log.trace("initCreated.dir:{}, type:{}", dir, type);
		for (PairValue<FileCreated> c : created) {
			Object obj = c.get(instance);
			if (obj == null) {
				c.set(instance, value(instance, c.getName(), c.getAnnotation().value(), c.getRead()));
				log.info("new created: {}", c.get(instance));
			}
		}
	}

	@SneakyThrows
	protected Object value(Object instance, String name, Class<? extends IFileInitializer> initializer, Method m) {
		IFileInitializer factory = initializer != FileInitializerDefault.class
				? initializer.getConstructor().newInstance()
				: FileInitializerHelper.initializer(context);
		return factory.value(instance, name, m.getReturnType());
	}

	protected <T> void initCreatedBy(File dir, Class<T> type, PairValue<FileCreatedBy>[] createdBy, T instance) {
		log.trace("initCreatedBy.dir:{}, type:{}", dir, type);
		if (createdBy.length > 1) {
			invalidMultipleFields(FileCreatedBy.class,
					Arrays.stream(createdBy).map(f -> f.getName()).collect(Collectors.joining(", ")));
		}
		for (PairValue<FileCreatedBy> c : createdBy) {
			Object obj = c.get(instance);
			if (obj == null) {
				c.set(instance, valueBy(instance, c.getName(), c.getAnnotation().value(), c.getRead()));
				log.info("new created by: {}", c.get(instance));
			}
		}
	}

	protected void invalidMultipleFields(Class<? extends Annotation> annotation, String names) {
		throw new FileStorageException("Multiple fields with annotation @" + annotation.getSimpleName()
				+ " are not allowed. Fields: " + names + ".", null);
	}

	@SneakyThrows
	protected Object valueBy(Object instance, String name, Class<? extends IFileAudit> audit, Method m) {
		IFileAudit factory = audit != FileAuditDefault.class ? audit.getConstructor().newInstance()
				: FileAuditHelper.audit(context);
		return factory.author();
	}

	protected <T> void prepareChanged(File dir, Class<T> type, PairValue<FileChanged>[] changed, T instance) {
		log.trace("prepareChanged.dir:{}, type:{}", dir, type);
		for (PairValue<FileChanged> c : changed) {
			c.set(instance, value(instance, c.getName(), c.getAnnotation().value(), c.getRead()));
			log.info("new changed: {}", c.get(instance));
		}
	}

	protected <T> void prepareChangedBy(File dir, Class<T> type, PairValue<FileChangedBy>[] changedBy, T instance) {
		log.trace("prepareChangedBy.dir:{}, type:{}", dir, type);
		if (changedBy.length > 1) {
			invalidMultipleFields(FileChangedBy.class,
					Arrays.stream(changedBy).map(f -> f.getName()).collect(Collectors.joining(", ")));
		}
		for (PairValue<FileChangedBy> c : changedBy) {
			c.set(instance, valueBy(instance, c.getName(), c.getAnnotation().value(), c.getRead()));
			log.info("new changed by: {}", c.get(instance));
		}
	}

	protected <T> void keepValues(T old, PairValue<?>[] values, T instance) {
		for (PairValue<?> c : values) {
			Object obj = c.get(old);
			c.set(instance, obj);
			log.info("'{}', keeped: {}", c.getName(), c.get(instance));
		}
	}

	@SneakyThrows
	protected <T> void prepareRevisions(File dir, Class<T> type, PairValue<FileRevision>[] revisions, T instance,
			T old) {
		for (PairValue<FileRevision> c : revisions) {
			Class<?> fieldType = c.getRead().getReturnType();
			if (!Number.class.isAssignableFrom(fieldType)) {
				throw new FileStorageException(
						"@FileRevision." + c.getName() + " type must be a subclass of Number.class.", null);
			}
			Number obj = (Number) c.get(instance);
			if (obj == null) {
				obj = (Number) fieldType.cast(0L);
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
				obj = current.longValue() + 1;
			}
			c.set(instance, obj);
			log.info("new revision: {}", obj);
		}
	}

	protected <T> void writeToFile(File target, T instance) {
		serializer.writeValue(target, instance);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T read(File dir, T example) {
		return read(dir, (Class<T>) example.getClass(), example);
	}

	@Override
	public <T> T read(File dir, Class<T> type, T example) {
		return read(dir, type, KeyParams.of(UtilAnnotations.getKeys(type, example)));
	}

	@Override
	public <T> T read(File dir, Class<T> type, KeyParams keys) {
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
		return delete(dir, type, KeyParams.of(UtilAnnotations.getKeys(type, example)));
	}

	@Override
	public <T> T delete(File dir, Class<T> type, KeyParams keys) {
		T old = null;
		if (exists(dir, type, keys)) {
			old = read(dir, type, keys);
			File file = entityFile(dir, type, keys);
			try {
				FileUtils.delete(file);
			} catch (IOException e) {
				throw new FileStorageException("Could not delete file: " + file, e);
			}
			File root = entityDir(dir, type, keys);
			File resources = resourceDir(root, type);
			try {
				FileUtils.delete(resources);
			} catch (IOException e) {
				throw new FileStorageException("Could not delete resources: " + resources, e);
			}
			if (root.listFiles().length == 0) {
				try {
					FileUtils.delete(root);
				} catch (IOException e) {
					throw new FileStorageException("Could not delete root: " + root, e);
				}
			}
			idManager.unbind(entityRoot(dir, type), old);
		}
		return old;
	}

	@Override
	public <T> long count(File dir, Class<T> type, SearchParams search) {
		return list(dir, type, search).size();
	}

	@Override
	public <T> List<T> list(File dir, Class<T> type, SearchParams search) {
		return range(search != null ? search.getPaging() : null, //
				filter(search != null ? search.getFilter() : null, //
						sort(search != null ? search.getSorting() : null, //
								all(dir, type, null))));
	}

	@SneakyThrows
	protected <T> List<T> all(File dir, Class<T> type, FilePaging paging) {
		List<T> result = new LinkedList<>();
		File[] ids = idManager.directory(entityRoot(dir, type), type, IFileIndex.IDS).listFiles();
		if (ids != null) {
			for (File f : ids) {
				Object[] keys = Files.readAllLines(f.toPath()).toArray(new Object[0]);
				try {
					result.add(serializer.readValue(entityFile(dir, type, KeyParams.of(keys)), type));
				} catch (Throwable e) {
					log.error("Could not read object for keys: " + Arrays.toString(keys) + ", check file system.", e);
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

	protected <T> List<T> filter(FileFilter filter, List<T> result) {
		Predicate<Object> p = filter(filter);
		if (p != null) {
			result = result.stream().filter(p).collect(Collectors.toList());
		}
		return result;
	}

	protected Predicate<Object> filter(FileFilter filter) {
		return filter == null ? null : filter.getFilter();
	}

	protected <T> List<T> range(FilePaging paging, List<T> result) {
		FilePaging page = Optional.ofNullable(paging).orElse(FilePaging.builder().build());
		Integer start = page.getStart();
		Integer end = page.getEnd(result.size());
		return start < end ? result.subList(start, end) : Collections.emptyList();
	}

	// +------------- PROPERTY METHODS ------------------+

	@Override
	@SneakyThrows
	public <T> T setProperty(File dir, Class<T> type, KeyParams keys, String property, Object data) {
		verifyExists(dir, type, keys);

		T current = read(dir, type, keys);

		return setProperty(dir, type, property, data, current);
	}

	protected <T> void verifyExists(File dir, Class<T> type, KeyParams keys) {
		if (!exists(dir, type, keys)) {
			throw new FileStorageNotFoundException(
					"Object '" + type.getSimpleName() + "' with keys '" + keys + "' not found.", null);
		}
	}

	protected <T> T setProperty(File dir, Class<T> type, String property, Object data, T current)
			throws IllegalAccessException, InvocationTargetException {
		// check unchangeable properties
		validateProperty(FileId.class, type, property, current);
		validateProperty(FileKey.class, type, property, current);
		validateProperty(FileCreated.class, type, property, current);
		validateProperty(FileCreatedBy.class, type, property, current);
		validateProperty(FileRevision.class, type, property, current);
		validateProperty(FileKeep.class, type, property, current);

		trySetProperty(current, property, data);

		return write(dir, current);
	}

	protected <A extends Annotation, T> void validateProperty(Class<A> annotation, Class<T> type, String property,
			T current) {
		PairValue<A>[] values = UtilAnnotations.getValues(annotation, type, current);
		for (PairValue<A> c : values) {
			if (c.getName().equalsIgnoreCase(property)) {
				throw new FileStorageException("Update of @" + annotation.getSimpleName() + " annotated property '"
						+ c.getName() + "' is not allowed.", null);
			}
		}
	}

	protected <T> void trySetProperty(T current, String name, Object value)
			throws IllegalAccessException, InvocationTargetException {
		try {
			PropertyUtils.setProperty(current, name, value);
		} catch (NoSuchMethodException e) {
			throw new FileStoragePropertyNotFoundException(name, current, e);
		}
	}

	@Override
	@SneakyThrows
	public <T> List<T> setProperty(File dir, Class<T> type, String property, Object data, SearchParams search) {
		List<T> list = list(dir, type, search);

		List<T> result = new ArrayList<>(list.size());

		for (T current : list) {
			result.add(setProperty(dir, type, property, data, current));
		}

		return result;
	}

	@Override
	@SneakyThrows
	public <T> Object getProperty(File dir, Class<T> type, KeyParams keys, String property) {
		verifyExists(dir, type, keys);

		T current = read(dir, type, keys);

		return tryGetProperty(current, property);
	}

	protected <T> Object tryGetProperty(T current, String property)
			throws IllegalAccessException, InvocationTargetException {
		try {
			return PropertyUtils.getProperty(current, property);
		} catch (NoSuchMethodException e) {
			throw new FileStoragePropertyNotFoundException(property, current, e);
		}
	}

	@Override
	@SneakyThrows
	public <T> Map<String, Object> properties(File dir, Class<T> type, KeyParams keys, KeyParams names) {
		verifyExists(dir, type, keys);

		T current = read(dir, type, keys);

		return getProperties(names, current);
	}

	protected <T> Map<String, Object> getProperties(KeyParams names, T current)
			throws IllegalAccessException, InvocationTargetException {
		KeyParams selection = names;
		if (selection == null) {
			PropertyDescriptor[] pds = PropertyUtils.getPropertyDescriptors(current);
			selection = KeyParams.of(Stream.of(pds).map(PropertyDescriptor::getName).collect(Collectors.toList()));
		}

		Map<String, Object> result = new LinkedHashMap<>();
		for (Object n : selection) {
			String property = String.valueOf(n).trim();
			result.put(property, tryGetProperty(current, property));
		}
		return result;
	}

	@Override
	@SneakyThrows
	public <T> Map<String, Map<String, Object>> properties(File dir, Class<T> type, KeyParams names,
			SearchParams search) {
		Map<String, Map<String, Object>> result = new LinkedHashMap<>();

		List<T> list = list(dir, type, search);

		for (T current : list) {
			result.put(UtilAnnotations.getKeysChain(type, current), getProperties(names, current));
		}
		return result;
	}

	// +------------- RESOURCE METHODS ------------------+

	protected <T> void initResources(File dir, Class<T> type, KeyParams keys) throws IOException {
		File resourceDir = resourceDir(entityDir(dir, type, keys), type);
		if (!resourceDir.exists()) {
			boolean created = resourceDir.mkdirs();
			if (created) {
				File keep = new File(resourceDir, ".keep");
				Files.write(keep.toPath(), "Forcing directory existence.".getBytes(), StandardOpenOption.CREATE,
						StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
			}
			log.info("Resources created={}", created);
		}
	}

	@Override
	@SneakyThrows
	public <T> File locationResource(File dir, Class<T> type, KeyParams keys, String path) {
		verifyExists(dir, type, keys);
		File root = resourceDir(entityDir(dir, type, keys), type);
		if (path != null) {
			File contentFile = new File(root, path);
			verifySecurity(root, contentFile, path);
			root = new File(root, path);
		}
		return root;
	}

	@Override
	public <T> boolean existsResource(File dir, Class<T> type, KeyParams keys, String path) {
		return locationResource(dir, type, keys, path).exists();
	}

	@Override
	@SneakyThrows
	public <T> T setResource(File dir, Class<T> type, KeyParams keys, Resource resource) {
		verifyExists(dir, type, keys);
		File root = resourceDir(entityDir(dir, type, keys), type);

		ResourceMetadata metadata = resource.getMetadata();
		String path = metadata.getPath();
		File contentFile = new File(root, path);
		verifySecurity(root, contentFile, path);

		FileUtils.prepare(contentFile);
		Files.write(contentFile.toPath(), resource.getContent().getData(), StandardOpenOption.CREATE,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

		File metadataFile = resourceMeta(root, path, type);
		FileUtils.prepare(metadataFile);
		metadata.setTimestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(contentFile.lastModified()),
				TimeZone.getDefault().toZoneId()));
		Files.write(metadataFile.toPath(), serializer.encode(metadata).getBytes(), StandardOpenOption.CREATE,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

		// force change flags like revision and updated
		T result = write(dir, type, read(dir, type, keys));

		log.info("Resource written: {}", metadata);

		return result;
	}

	protected File resourceDir(File entityDir, Class<?> type) {
		return new File(entityDir, preffix(type) + "@resources");
	}

	private String preffix(Class<?> type) {
		if (type != null) {
			FileName name = AnnotationUtils.findAnnotation(type, FileName.class);
			if (name != null) {
				return name.value();
			}
		}
		return "data";
	}

	protected File resourceMeta(File entityDir, String path, Class<?> type) {
		return new File(entityDir, path + "." + serializer.getFile(type));
	}

	@Override
	@SneakyThrows
	public <T> Resource getResource(File dir, Class<T> type, KeyParams keys, String path) {
		verifyExists(dir, type, keys);
		File root = resourceDir(entityDir(dir, type, keys), type);
		verifyResources(root, keys);

		File contentFile = new File(root, path);
		verifySecurity(root, contentFile, path);
		verifyResourceExists(contentFile, path);

		ResourceContent content = new ResourceContent(Files.readAllBytes(contentFile.toPath()));
		File metadataFile = resourceMeta(root, path, type);
		ResourceMetadata meta = serializer.decode(Files.readAllBytes(metadataFile.toPath()), ResourceMetadata.class);
		return Resource.builder().metadata(meta).content(content).build();
	}

	protected void verifyResources(File root, KeyParams keys) {
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
	public <T> long countResources(File dir, Class<T> type, KeyParams keys, SearchParams search) {
		return listResources(dir, type, keys, search).size();
	}

	@Override
	@SneakyThrows
	public <T> List<Resource> listResources(File dir, Class<T> type, KeyParams keys, SearchParams search) {
		verifyExists(dir, type, keys);
		File root = resourceDir(entityDir(dir, type, keys), type);
		verifyResources(root, keys);

		final Predicate<Object> predicate = filter(safeFilter(search));

		List<Resource> result = new LinkedList<>();
		final String ignoreFile = "." + serializer.getFile(type);
		Files.walkFileTree(Paths.get(root.toURI()), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path contentFile, BasicFileAttributes attrs) throws IOException {
				File file = contentFile.toFile();
				String name = file.getName();
				if (!name.endsWith(ignoreFile) && !name.equals(".keep")) {
					File metadataFile = resourceMeta(file.getParentFile(), name, type);
					log.info("Loading... {}", contentFile);
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
		if (search == null || search.getSorting() == null) {
			result.sort(new ComparatorNullSafe<>("metadata.path", false));
		}
		return range(safePaging(search), sort(safeSort(search), result));
	}

	protected FileFilter safeFilter(SearchParams search) {
		return search != null ? search.getFilter() : null;
	}

	protected FilePaging safePaging(SearchParams search) {
		return search != null ? search.getPaging() : null;
	}

	protected FileSorting safeSort(SearchParams search) {
		return search != null ? search.getSorting() : null;
	}

	@Override
	@SneakyThrows
	public <T> T deleteResource(File dir, Class<T> type, KeyParams keys, String path) {
		verifyExists(dir, type, keys);
		File root = resourceDir(entityDir(dir, type, keys), type);
		verifyResources(root, keys);

		File contentFile = new File(root, path);
		verifySecurity(root, contentFile, path);
		verifyResourceExists(contentFile, path);

		File metadataFile = resourceMeta(root, path, type);

		FileUtils.delete(contentFile);
		FileUtils.delete(metadataFile);

		// force change flags like revision and updated
		T result = write(dir, type, read(dir, type, keys));

		log.info("Resource deleted: {}", path);

		return result;
	}

}