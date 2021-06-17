package io.github.thiagolvlsantos.git.storage.exceptions;

@SuppressWarnings("serial")
public class GitStorageException extends RuntimeException {

	public GitStorageException(String message, Throwable cause) {
		super(message, cause);
	}
}
