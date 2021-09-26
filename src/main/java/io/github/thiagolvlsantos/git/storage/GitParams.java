package io.github.thiagolvlsantos.git.storage;

import java.util.Arrays;
import java.util.Iterator;
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

	private Object[] keys;

	public static GitParams of(Object... objs) {
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
