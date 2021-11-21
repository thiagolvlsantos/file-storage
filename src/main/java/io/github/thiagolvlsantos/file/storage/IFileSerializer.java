package io.github.thiagolvlsantos.file.storage;

import java.io.File;
import java.lang.reflect.AnnotatedType;

public interface IFileSerializer {

	String getExtension();

	<T> T decode(byte[] data, Class<T> type);

	Object decode(String data, AnnotatedType type);

	String encode(Object instance);

	<T> T readValue(File file, Class<T> type);

	<T> void writeValue(File file, T instance);

}
