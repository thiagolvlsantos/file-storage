package io.github.thiagolvlsantos.file.storage;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KeyParams implements Iterable<Object> {

	public static final String SEPARATOR = ";";

	private Object[] keys;

	public static KeyParams of(String parts) {
		if (parts == null) {
			return null;
		}
		return of(parts, SEPARATOR);
	}

	public static KeyParams of(String parts, String separator) {
		if (parts == null) {
			return null;
		}
		return of((Object[]) parts.split(separator));
	}

	public static KeyParams of(List<?> objs) {
		if (objs == null) {
			return null;
		}
		return of(objs.toArray());
	}

	public static KeyParams of(Object... objs) {
		if (objs == null) {
			return null;
		}
		return new KeyParams(objs);
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
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				return keys[index++];
			}
		};
	}

	@Override
	public String toString() {
		return Arrays.toString(keys);
	}
}
