package io.github.thiagolvlsantos.file.storage.util.repository;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.thiagolvlsantos.file.storage.FilePaging;
import io.github.thiagolvlsantos.file.storage.FileParams;
import io.github.thiagolvlsantos.file.storage.FilePredicate;
import io.github.thiagolvlsantos.file.storage.FileSorting;
import io.github.thiagolvlsantos.file.storage.IFileStorage;
import io.github.thiagolvlsantos.file.storage.resource.Resource;
import lombok.Getter;
import lombok.SneakyThrows;

@Getter
public abstract class AbstractFileRepository<T> {
	private @Autowired IFileStorage storage;
	private @Autowired IPredicateConverter predicateConverter;

	private Class<T> type;

	public AbstractFileRepository(Class<T> type) {
		this.type = type;
	}

	// +------------- ENTITY METHODS ------------------+

	@SneakyThrows
	public File location(File dir, FileParams keys) {
		return storage.location(dir, type, keys);
	}

	@SneakyThrows
	public boolean exists(File dir, T obj) {
		return storage.exists(dir, type, obj);
	}

	@SneakyThrows
	public T write(File dir, T obj) {
		return storage.write(dir, type, obj);
	}

	@SneakyThrows
	public T read(File dir, FileParams keys) {
		return storage.read(dir, type, keys);
	}

	@SneakyThrows
	public T delete(File dir, FileParams keys) {
		return storage.delete(dir, type, keys);
	}

	@SneakyThrows
	public Long count(File dir, String filter, String paging) {
		return storage.count(dir, type, filter(filter), paging(paging));
	}

	@SneakyThrows
	public List<T> list(File dir, String filter, String paging, String sorting) {
		return storage.list(dir, type, filter(filter), paging(paging), sorting(sorting));
	}

	public FilePredicate filter(String filter) {
		return filter != null ? FilePredicate.builder().filter(predicateConverter.toPredicate(filter)).build() : null;
	}

	public FilePaging paging(String paging) {
		return paging != null ? storage.getSerializer().decode(paging.getBytes(), FilePaging.class) : null;
	}

	public FileSorting sorting(String sorting) {
		return sorting != null ? storage.getSerializer().decode(sorting.getBytes(), FileSorting.class) : null;
	}

	// +------------- PROPERTY METHODS ------------------+

	@SneakyThrows
	public T setProperty(File dir, FileParams keys, String property, Object data) {
		return storage.setProperty(dir, type, keys, property, newValue(property, data, read(dir, keys)));
	}

	protected Object newValue(String property, Object data, Object reference)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		PropertyDescriptor pd = PropertyUtils.getPropertyDescriptor(reference, property);
		AnnotatedType attType = pd.getReadMethod().getAnnotatedReturnType();
		Object newValue = storage.getSerializer().decode(String.valueOf(data), attType);
		return newValue;
	}

	@SneakyThrows
	public List<T> setProperty(File dir, String property, Object data, String filter, String paging, String sorting) {
		return storage.setProperty(dir, type, property, newValue(property, data, type.getConstructor().newInstance()),
				filter(filter), paging(paging), sorting(sorting));
	}

	@SneakyThrows
	public Object getProperty(File dir, FileParams keys, String property) {
		return storage.getProperty(dir, type, keys, property);
	}

	@SneakyThrows
	public Map<String, Object> properties(File dir, FileParams keys, FileParams names) {
		return storage.properties(dir, type, keys, names);
	}

	@SneakyThrows
	public Map<String, Map<String, Object>> properties(File dir, FileParams names, String filter, String paging,
			String sorting) {
		return storage.properties(dir, type, names, filter(filter), paging(paging), sorting(sorting));
	}

	// +------------- RESOURCE METHODS ------------------+

	@SneakyThrows
	public File locationResources(File dir, FileParams keys, String path) {
		return storage.locationResource(dir, type, keys, path);
	}

	@SneakyThrows
	public boolean existsResources(File dir, FileParams keys, String path) {
		return storage.existsResource(dir, type, keys, path);
	}

	@SneakyThrows
	public T setResource(File dir, FileParams keys, Resource resource) {
		return storage.setResource(dir, type, keys, resource);
	}

	@SneakyThrows
	public Resource getResource(File dir, FileParams keys, String path) {
		return storage.getResource(dir, type, keys, path);
	}

	@SneakyThrows
	public Long countResources(File dir, FileParams keys, String filter, String paging) {
		return storage.countResources(dir, type, keys, filter(filter), paging(paging));
	}

	@SneakyThrows
	public List<Resource> listResources(File dir, FileParams keys, String filter, String paging, String sorting) {
		return storage.listResources(dir, type, keys, filter(filter), paging(paging), sorting(sorting));
	}

	@SneakyThrows
	public T deleteResource(File dir, FileParams keys, String path) {
		return storage.deleteResource(dir, type, keys, path);
	}

}
