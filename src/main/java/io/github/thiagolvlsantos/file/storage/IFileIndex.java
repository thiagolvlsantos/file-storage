package io.github.thiagolvlsantos.file.storage;

import java.io.File;

public interface IFileIndex {

	String IDS = "ids";

	String KEYS = "keys";

	Long next(File dir);

	<T> void bind(File dir, T instance);

	<T> void unbind(File dir, T instance);

	File directory(File dir, String kind);
}