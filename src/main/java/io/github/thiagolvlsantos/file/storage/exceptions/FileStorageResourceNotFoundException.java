package io.github.thiagolvlsantos.file.storage.exceptions;

@SuppressWarnings("serial")
public class FileStorageResourceNotFoundException extends FileStorageNotFoundException {

	public FileStorageResourceNotFoundException(String path, Throwable cause) {
		super("Resource not found: '" + path + "'.", cause);
	}
}
