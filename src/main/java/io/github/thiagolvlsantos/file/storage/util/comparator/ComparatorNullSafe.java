package io.github.thiagolvlsantos.file.storage.util.comparator;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;

import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.PropertyUtils;

import io.github.thiagolvlsantos.file.storage.exceptions.FileStorageException;

public class ComparatorNullSafe<T> implements Comparator<T> {

	private String property;
	private boolean nullsFirst;

	private Comparator<Object> comparator = new Comparator<Object>() {

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public int compare(Object o1, Object o2) {
			if (o1 instanceof Comparable && o2 instanceof Comparable) {
				return ((Comparable) o1).compareTo(o2);
			}
			return String.valueOf(o1).compareTo(String.valueOf(o2));
		}
	};

	public ComparatorNullSafe(String property, boolean nullsFirst) {
		this.property = property;
		this.nullsFirst = nullsFirst;
	}

	public int compare(T o1, T o2) {
		try {
			Object value1 = getNullSafeProperty(o1, property);
			Object value2 = getNullSafeProperty(o2, property);
			if (null == value1 && null == value2) {
				return 0;
			}
			if (null == value1) {
				return nullsFirst ? -1 : +1;
			}
			if (null == value2) {
				return nullsFirst ? -1 : +1;
			}
			return comparator.compare(value1, value2);
		} catch (IllegalAccessException iae) {
			throw new FileStorageException("IllegalAccessException: " + iae.toString(), null);
		} catch (InvocationTargetException ite) {
			throw new FileStorageException("InvocationTargetException: " + ite.toString(), null);
		} catch (NoSuchMethodException nsme) {
			throw new FileStorageException("NoSuchMethodException: " + nsme.toString(), null);
		}
	}

	protected Object getNullSafeProperty(Object o1, String property)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Object val;
		try {
			val = PropertyUtils.getProperty(o1, property);
		} catch (NestedNullException ex) {
			val = null;
		}
		return val;
	}
}