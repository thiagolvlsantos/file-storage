package io.github.thiagolvlsantos.file.storage.exceptions;

@SuppressWarnings("serial")
public class FileStorageException extends RuntimeException {

	public FileStorageException(String message, Throwable cause) {
		super(message, cause);
	}
}
