package io.github.thiagolvlsantos.file.storage;

import java.io.File;

import io.github.thiagolvlsantos.file.storage.annotations.PairValue;
import io.github.thiagolvlsantos.file.storage.identity.FileId;

public interface IFileIndex {

	String IDS = "ids";

	String KEYS = "keys";

	Object next(File dir, Class<?> type, PairValue<FileId> info);

	<T> void bind(File dir, T instance);

	<T> void unbind(File dir, T instance);

	File directory(File dir, Class<?> type, String kind);
}