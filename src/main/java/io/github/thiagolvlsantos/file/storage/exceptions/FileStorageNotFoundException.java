package io.github.thiagolvlsantos.file.storage.exceptions;

@SuppressWarnings("serial")
public class FileStorageNotFoundException extends FileStorageException {

	public FileStorageNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
