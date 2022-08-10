package io.github.thiagolvlsantos.file.storage.util.repository;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.thiagolvlsantos.file.storage.IFileStorage;
import io.github.thiagolvlsantos.file.storage.KeyParams;
import io.github.thiagolvlsantos.file.storage.SearchParams;
import io.github.thiagolvlsantos.file.storage.resource.Resource;
import io.github.thiagolvlsantos.file.storage.search.FileFilter;
import io.github.thiagolvlsantos.file.storage.search.FilePaging;
import io.github.thiagolvlsantos.file.storage.search.FileSorting;
import lombok.Getter;
import lombok.SneakyThrows;

@Getter
public abstract class AbstractFileRepository<T> {
	private @Autowired IFileStorage storage;
	private @Autowired IPredicateConverter predicateConverter;

	private Class<T> type;
	private T reference;

	@SneakyThrows
	protected AbstractFileRepository(Class<T> type) {
		this.type = type;
		this.reference = type.getConstructor().newInstance();
	}

	// +------------- ENTITY METHODS ------------------+

	@SneakyThrows
	public File location(File dir, KeyParams keys) {
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
	public T read(File dir, KeyParams keys) {
		return storage.read(dir, type, keys);
	}

	@SneakyThrows
	public T delete(File dir, KeyParams keys) {
		return storage.delete(dir, type, keys);
	}

	@SneakyThrows
	public Long count(File dir, String filter, String paging) {
		return storage.count(dir, type, SearchParams.builder().filter(filter(filter)).paging(paging(paging)).build());
	}

	@SneakyThrows
	public List<T> list(File dir, String filter, String paging, String sorting) {
		return storage.list(dir, type,
				SearchParams.builder().filter(filter(filter)).paging(paging(paging)).sorting(sorting(sorting)).build());
	}

	public FileFilter filter(String filter) {
		return filter != null ? FileFilter.builder().filter(predicateConverter.toPredicate(filter)).build() : null;
	}

	public FilePaging paging(String paging) {
		return paging != null ? storage.getSerializer().decode(paging.getBytes(), FilePaging.class) : null;
	}

	public FileSorting sorting(String sorting) {
		return sorting != null ? storage.getSerializer().decode(sorting.getBytes(), FileSorting.class) : null;
	}

	// +------------- PROPERTY METHODS ------------------+

	@SneakyThrows
	public T setProperty(File dir, KeyParams keys, String property, Object data) {
		return storage.setProperty(dir, type, keys, property, newValue(property, data, read(dir, keys)));
	}

	@SneakyThrows
	public Object newValue(String property, Object data, Object reference) {
		PropertyDescriptor pd = PropertyUtils.getPropertyDescriptor(reference, property);
		AnnotatedType attType = pd.getReadMethod().getAnnotatedReturnType();
		return storage.getSerializer().decode(String.valueOf(data), attType);
	}

	@SneakyThrows
	public List<T> setProperty(File dir, String property, Object data, String filter, String paging, String sorting) {
		return storage.setProperty(dir, type, property, newValue(property, data, type.getConstructor().newInstance()),
				SearchParams.builder().filter(filter(filter)).paging(paging(paging)).sorting(sorting(sorting)).build());
	}

	@SneakyThrows
	public Object getProperty(File dir, KeyParams keys, String property) {
		return storage.getProperty(dir, type, keys, property);
	}

	@SneakyThrows
	public Map<String, Object> properties(File dir, KeyParams keys, KeyParams names) {
		return storage.properties(dir, type, keys, names);
	}

	@SneakyThrows
	public Map<String, Map<String, Object>> properties(File dir, KeyParams names, String filter, String paging,
			String sorting) {
		return storage.properties(dir, type, names,
				SearchParams.builder().filter(filter(filter)).paging(paging(paging)).sorting(sorting(sorting)).build());
	}

	// +------------- RESOURCE METHODS ------------------+

	@SneakyThrows
	public File locationResources(File dir, KeyParams keys, String path) {
		return storage.locationResource(dir, type, keys, path);
	}

	@SneakyThrows
	public boolean existsResources(File dir, KeyParams keys, String path) {
		return storage.existsResource(dir, type, keys, path);
	}

	@SneakyThrows
	public T setResource(File dir, KeyParams keys, Resource resource) {
		return storage.setResource(dir, type, keys, resource);
	}

	@SneakyThrows
	public Resource getResource(File dir, KeyParams keys, String path) {
		return storage.getResource(dir, type, keys, path);
	}

	@SneakyThrows
	public Long countResources(File dir, KeyParams keys, String filter, String paging) {
		return storage.countResources(dir, type, keys,
				SearchParams.builder().filter(filter(filter)).paging(paging(paging)).build());
	}

	@SneakyThrows
	public List<Resource> listResources(File dir, KeyParams keys, String filter, String paging, String sorting) {
		return storage.listResources(dir, type, keys,
				SearchParams.builder().filter(filter(filter)).paging(paging(paging)).sorting(sorting(sorting)).build());
	}

	@SneakyThrows
	public T deleteResource(File dir, KeyParams keys, String path) {
		return storage.deleteResource(dir, type, keys, path);
	}

}
