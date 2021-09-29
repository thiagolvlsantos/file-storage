package io.github.thiagolvlsantos.git.storage;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GitParams implements Iterable<Object> {

	private static final String SEPARATOR = "_";

	private Object[] keys;

	public static GitParams of(String parts) {
		if (parts == null) {
			return null;
		}
		return of(parts, SEPARATOR);
	}

	public static GitParams of(String parts, String separator) {
		if (parts == null) {
			return null;
		}
		return of((Object[]) parts.split(separator));
	}

	public static GitParams of(List<?> objs) {
		if (objs == null) {
			return null;
		}
		return of(objs.toArray());
	}

	public static GitParams of(Object... objs) {
		if (objs == null) {
			return null;
		}
		return new GitParams(objs);
	}

	@Override
	public Iterator<Object> iterator() {
		return new Iterator<Object>() {
			private int index = 0;

			@Override
			public boolean hasNext() {
				return !Objects.isNull(keys) && index < keys.length;
			}

			@Override
			public Object next() {
				return keys[index++];
			}
		};
	}

	@Override
	public String toString() {
		return Arrays.toString(keys);
	}
}
