package io.github.thiagolvlsantos.file.storage.exceptions;

@SuppressWarnings("serial")
public class FileStorageSecurityException extends FileStorageNotFoundException {

	public FileStorageSecurityException(String path, Throwable cause) {
		super("Cannot work with resources from a higher file structure. " + path, cause);
	}
}
